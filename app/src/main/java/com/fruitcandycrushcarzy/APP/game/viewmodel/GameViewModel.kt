package com.fruitcandycrushcarzy.APP.game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fruitcandycrushcarzy.APP.game.data.ScoreRepository
import com.fruitcandycrushcarzy.APP.game.logic.GameLogic
import com.fruitcandycrushcarzy.APP.game.model.Fruit
import com.fruitcandycrushcarzy.APP.game.model.FruitType
import com.fruitcandycrushcarzy.APP.game.model.Position
import com.fruitcandycrushcarzy.APP.game.model.SpecialType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GameEvent {
    MATCH,
    SWAP,
    GAME_OVER,
    LEVEL_UP,
    SPECIAL_EXPLOSION,
    REQUEST_REWARDED_AD,
    RATE_APP
}

enum class DragDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT
}

data class GameState(
    val grid: Array<Array<Fruit?>> =
        Array(GameLogic.GRID_SIZE) {
            arrayOfNulls<Fruit>(GameLogic.GRID_SIZE)
        },
    val score: Int = 0,
    val walletBalance: Int = 0,
    val highScore: Int = 0,
    val movesLeft: Int = 20,
    val selectedPosition: Position? = null,
    val isProcessing: Boolean = false,
    val level: Int = 1,
    val timeLeftSeconds: Int = 60,
    val targetScore: Int = 300,
    val lastComboCount: Int = 0,
    val hasMoves: Boolean = true,
    val isLevelUp: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val isMusicEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val showSettings: Boolean = false,
    val isStarting: Boolean = true,
    val showRateDialog: Boolean = false
)

class GameViewModel(
    private val scoreRepository: ScoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameState())

    val uiState: StateFlow<GameState> =
        _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>()

    val events: SharedFlow<GameEvent> =
        _events.asSharedFlow()

    private var timerJob: Job? = null

    init {
        _uiState.update {
            it.copy(
                grid = GameLogic.createInitialGrid()
            )
        }

        viewModelScope.launch {
            scoreRepository.highScoreFlow.collectLatest { high ->
                _uiState.update {
                    it.copy(highScore = high)
                }
            }
        }

        viewModelScope.launch {
            scoreRepository.walletBalanceFlow.collectLatest { balance ->
                _uiState.update {
                    it.copy(walletBalance = balance)
                }
            }
        }

        viewModelScope.launch {
            scoreRepository.soundEnabledFlow.collectLatest { enabled ->
                _uiState.update {
                    it.copy(isSoundEnabled = enabled)
                }
            }
        }

        viewModelScope.launch {
            scoreRepository.musicEnabledFlow.collectLatest { enabled ->
                _uiState.update {
                    it.copy(isMusicEnabled = enabled)
                }
            }
        }

        viewModelScope.launch {
            scoreRepository.vibrationEnabledFlow.collectLatest { enabled ->
                _uiState.update {
                    it.copy(isVibrationEnabled = enabled)
                }
            }
        }

        viewModelScope.launch {
            scoreRepository.gamesPlayedFlow.collectLatest { games ->
                scoreRepository.hasRatedFlow.collectLatest { hasRated ->
                    if (!hasRated && games >= 3 && games % 5 == 0) {
                        _uiState.update {
                            it.copy(showRateDialog = true)
                        }
                    }
                }
            }
        }

        startGameSequence()
    }

    private fun startGameSequence() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isStarting = true)
            }

            delay(1000)

            _uiState.update {
                it.copy(isStarting = false)
            }

            startTimer()
        }
    }

    fun resetGame() {
        timerJob?.cancel()

        _uiState.update {
            GameState(
                grid = GameLogic.createInitialGrid(),
                highScore = it.highScore,
                walletBalance = it.walletBalance,
                isSoundEnabled = it.isSoundEnabled,
                isMusicEnabled = it.isMusicEnabled,
                isVibrationEnabled = it.isVibrationEnabled
            )
        }

        viewModelScope.launch {
            scoreRepository.incrementGamesPlayed()
        }

        startGameSequence()
    }

    private fun startTimer() {
        timerJob?.cancel()

        timerJob = viewModelScope.launch {
            while (
                _uiState.value.timeLeftSeconds > 0 &&
                _uiState.value.movesLeft > 0 &&
                !_uiState.value.isLevelUp
            ) {
                if (
                    !_uiState.value.showSettings &&
                    !_uiState.value.isStarting
                ) {
                    delay(1000)

                    _uiState.update {
                        it.copy(
                            timeLeftSeconds =
                                (it.timeLeftSeconds - 1)
                                    .coerceAtLeast(0)
                        )
                    }
                } else {
                    delay(100)
                }
            }

            if (
                _uiState.value.timeLeftSeconds <= 0 ||
                _uiState.value.movesLeft <= 0
            ) {
                _events.emit(GameEvent.GAME_OVER)
            }
        }
    }

    fun onCellClick(position: Position) {
        if (
            _uiState.value.isProcessing ||
            _uiState.value.isStarting ||
            _uiState.value.movesLeft <= 0 ||
            _uiState.value.timeLeftSeconds <= 0 ||
            _uiState.value.isLevelUp
        ) return

        val selected = _uiState.value.selectedPosition

        if (selected == null) {
            _uiState.update {
                it.copy(selectedPosition = position)
            }
        } else {
            if (GameLogic.isAdjacent(selected, position)) {
                swapAndProcess(selected, position)
            } else {
                _uiState.update {
                    it.copy(selectedPosition = position)
                }
            }
        }
    }

    private fun swapAndProcess(
        p1: Position,
        p2: Position
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    selectedPosition = null
                )
            }

            _events.emit(GameEvent.SWAP)

            val currentGrid = copyGrid(_uiState.value.grid)

            val temp = currentGrid[p1.row][p1.col]

            currentGrid[p1.row][p1.col] =
                currentGrid[p2.row][p2.col]

            currentGrid[p2.row][p2.col] = temp

            _uiState.update {
                it.copy(grid = currentGrid)
            }

            delay(120)

            val matches =
                GameLogic.findMatchGroups(currentGrid)

            if (matches.isEmpty()) {
                val revertGrid = copyGrid(_uiState.value.grid)

                val tempBack = revertGrid[p1.row][p1.col]

                revertGrid[p1.row][p1.col] =
                    revertGrid[p2.row][p2.col]

                revertGrid[p2.row][p2.col] = tempBack

                _uiState.update {
                    it.copy(grid = revertGrid)
                }
            } else {
                _uiState.update {
                    it.copy(
                        movesLeft =
                            (it.movesLeft - 1)
                                .coerceAtLeast(0)
                    )
                }

                processMatches(
                    currentGrid,
                    p2
                )
            }

            _uiState.update {
                it.copy(isProcessing = false)
            }

            checkMovesAvailable()
        }
    }

    private fun checkMovesAvailable() {
        val hasMoves =
            GameLogic.hasAvailableMoves(
                _uiState.value.grid
            )

        _uiState.update {
            it.copy(hasMoves = hasMoves)
        }

        if (
            !hasMoves &&
            _uiState.value.movesLeft > 0 &&
            !_uiState.value.isProcessing
        ) {
            viewModelScope.launch {
                delay(400)

                if (
                    !GameLogic.hasAvailableMoves(
                        _uiState.value.grid
                    )
                ) {
                    shuffleBoard(isAuto = true)
                }
            }
        }
    }

    private suspend fun processMatches(
        grid: Array<Array<Fruit?>>,
        triggeredBy: Position? = null
    ) {
        var currentGrid = copyGrid(grid)
        var combo = 0

        while (true) {
            val groups =
                GameLogic.findMatchGroups(currentGrid)

            if (groups.isEmpty()) {
                break
            }

            combo++

            val matchPositions =
                groups.flatten().toSet()

            val specialFruitsToCreate =
                mutableListOf<
                    Triple<Position, FruitType, SpecialType>
                >()

            groups.forEach { group ->
                if (group.size >= 4) {
                    val firstFruit =
                        currentGrid[group[0].row][group[0].col]
                            ?: return@forEach

                    val fruitType = firstFruit.type

                    val specialType =
                        when {
                            group.size >= 5 ->
                                SpecialType.BOMB

                            group.size >= 4 &&
                                group[0].row == group[1].row ->
                                SpecialType.COL_BLAST

                            else ->
                                SpecialType.ROW_BLAST
                        }

                    val specialPosition =
                        if (
                            triggeredBy != null &&
                            triggeredBy in group
                        ) {
                            triggeredBy
                        } else {
                            group[group.size / 2]
                        }

                    specialFruitsToCreate.add(
                        Triple(
                            specialPosition,
                            fruitType,
                            specialType
                        )
                    )
                }
            }

            val positionsToClear =
                matchPositions.filter { position ->
                    specialFruitsToCreate.none {
                        it.first == position
                    }
                }.toSet()

            val affectedPositions =
                GameLogic.getAffectedPositions(
                    currentGrid,
                    matchPositions
                )

            val basePoints =
                affectedPositions.size * 10

            val comboBonus =
                (combo - 1) * 15

            val specialBonus =
                if (
                    affectedPositions.size >
                    matchPositions.size
                ) {
                    50
                } else {
                    0
                }

            val points =
                (
                    basePoints +
                        comboBonus +
                        specialBonus
                    ) * combo

            _events.emit(GameEvent.MATCH)

            if (
                affectedPositions.size >
                matchPositions.size
            ) {
                _events.emit(
                    GameEvent.SPECIAL_EXPLOSION
                )
            }

            positionsToClear.forEach { position ->
                currentGrid[position.row][position.col] = null
            }

            specialFruitsToCreate.forEach {
                (position, fruitType, specialType) ->

                currentGrid[position.row][position.col] =
                    Fruit(
                        type = fruitType,
                        special = specialType
                    )
            }

            val newScore =
                _uiState.value.score + points

            _uiState.update {
                it.copy(
                    grid = copyGrid(currentGrid),
                    score = newScore,
                    lastComboCount = combo
                )
            }

            if (
                newScore >
                _uiState.value.highScore
            ) {
                scoreRepository.updateHighScore(newScore)
            }

            delay(300)

            GameLogic.applyGravity(currentGrid)

            _uiState.update {
                it.copy(
                    grid = copyGrid(currentGrid)
                )
            }

            delay(200)

            GameLogic.refillGrid(currentGrid)

            _uiState.update {
                it.copy(
                    grid = copyGrid(currentGrid)
                )
            }

            delay(200)
        }

        if (
            _uiState.value.score >=
            _uiState.value.targetScore
        ) {
            levelUp()
        }
    }

    private fun levelUp() {
        viewModelScope.launch {
            val completedLevel =
                _uiState.value.level

            val earning =
                when {
                    completedLevel in 1..50 -> 2
                    completedLevel in 51..100 -> 3
                    completedLevel in 101..150 -> 5
                    completedLevel in 151..200 -> 10
                    else -> 15
                }

            scoreRepository.addEarning(earning)

            _events.emit(GameEvent.LEVEL_UP)

            _uiState.update {
                it.copy(isLevelUp = true)
            }

            delay(1000)

            val nextLevel =
                completedLevel + 1

            val nextTarget =
                when {
                    nextLevel <= 50 ->
                        300 + ((nextLevel - 1) * 50)

                    nextLevel <= 100 ->
                        2800 + ((nextLevel - 50) * 75)

                    nextLevel <= 150 ->
                        6550 + ((nextLevel - 100) * 100)

                    nextLevel <= 200 ->
                        11550 + ((nextLevel - 150) * 125)

                    else ->
                        17800 + ((nextLevel - 200) * 150)
                }

            _uiState.update {
                it.copy(
                    level = nextLevel,
                    targetScore = nextTarget,
                    movesLeft = 20,
                    timeLeftSeconds = 60,
                    isLevelUp = false,
                    score = 0
                )
            }

            startTimer()
        }
    }

    fun shuffleBoard(isAuto: Boolean = false) {
        if (
            _uiState.value.isProcessing ||
            _uiState.value.isLevelUp
        ) return

        val cost =
            if (isAuto) {
                0
            } else {
                if (_uiState.value.hasMoves) 2 else 0
            }

        if (_uiState.value.movesLeft < cost) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    movesLeft = it.movesLeft - cost
                )
            }

            val newGrid =
                GameLogic.createInitialGrid()

            _uiState.update {
                it.copy(
                    grid = newGrid,
                    hasMoves = true
                )
            }

            delay(150)

            _uiState.update {
                it.copy(isProcessing = false)
            }

            checkMovesAvailable()
        }
    }

    fun toggleSound() {
        viewModelScope.launch {
            scoreRepository.toggleSound(
                !_uiState.value.isSoundEnabled
            )
        }
    }

    fun toggleMusic() {
        viewModelScope.launch {
            scoreRepository.toggleMusic(
                !_uiState.value.isMusicEnabled
            )
        }
    }

    fun toggleVibration() {
        viewModelScope.launch {
            scoreRepository.toggleVibration(
                !_uiState.value.isVibrationEnabled
            )
        }
    }

    fun toggleSettings() {
        _uiState.update {
            it.copy(
                showSettings =
                    !it.showSettings
            )
        }
    }

    fun requestRewardedAd() {
        viewModelScope.launch {
            _events.emit(
                GameEvent.REQUEST_REWARDED_AD
            )
        }
    }

    fun grantRewardMoves(count: Int = 5) {
        _uiState.update {
            it.copy(
                movesLeft =
                    it.movesLeft + count
            )
        }

        checkMovesAvailable()
    }

    fun onRateApp() {
        viewModelScope.launch {
            scoreRepository.setHasRated(true)

            _uiState.update {
                it.copy(showRateDialog = false)
            }

            _events.emit(GameEvent.RATE_APP)
        }
    }

    fun onDismissRateDialog() {
        _uiState.update {
            it.copy(showRateDialog = false)
        }
    }

    fun onSwipe(
        position: Position,
        direction: DragDirection
    ) {
        if (
            _uiState.value.isProcessing ||
            _uiState.value.isStarting ||
            _uiState.value.movesLeft <= 0 ||
            _uiState.value.timeLeftSeconds <= 0 ||
            _uiState.value.isLevelUp
        ) return

        val targetPos =
            when (direction) {
                DragDirection.UP ->
                    Position(
                        position.row - 1,
                        position.col
                    )

                DragDirection.DOWN ->
                    Position(
                        position.row + 1,
                        position.col
                    )

                DragDirection.LEFT ->
                    Position(
                        position.row,
                        position.col - 1
                    )

                DragDirection.RIGHT ->
                    Position(
                        position.row,
                        position.col + 1
                    )
            }

        if (
            targetPos.row in
            0 until GameLogic.GRID_SIZE &&
            targetPos.col in
            0 until GameLogic.GRID_SIZE
        ) {
            swapAndProcess(
                position,
                targetPos
            )
        }
    }

    private fun copyGrid(
        original: Array<Array<Fruit?>>
    ): Array<Array<Fruit?>> {
        return Array(GameLogic.GRID_SIZE) { r ->
            Array(GameLogic.GRID_SIZE) { c ->
                original[r][c]
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
