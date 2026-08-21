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

    private val _uiState =
        MutableStateFlow(GameState())

    val uiState: StateFlow<GameState> =
        _uiState.asStateFlow()

    private val _events =
        MutableSharedFlow<GameEvent>()

    val events: SharedFlow<GameEvent> =
        _events.asSharedFlow()

    private var timerJob: Job? = null

    init {

        _uiState.update {
            it.copy(
                grid = GameLogic.createInitialGrid()
            )
        }

        // ==============================
        // HIGH SCORE
        // ==============================

        viewModelScope.launch {

            scoreRepository.highScoreFlow
                .collectLatest { high ->

                    _uiState.update {
                        it.copy(
                            highScore = high
                        )
                    }
                }
        }

        // ==============================
        // WALLET
        // ==============================

        viewModelScope.launch {

            scoreRepository.walletBalanceFlow
                .collectLatest { balance ->

                    _uiState.update {
                        it.copy(
                            walletBalance = balance
                        )
                    }
                }
        }

        // ==============================
        // SOUND
        // ==============================

        viewModelScope.launch {

            scoreRepository.soundEnabledFlow
                .collectLatest { enabled ->

                    _uiState.update {
                        it.copy(
                            isSoundEnabled = enabled
                        )
                    }
                }
        }

        // ==============================
        // MUSIC
        // ==============================

        viewModelScope.launch {

            scoreRepository.musicEnabledFlow
                .collectLatest { enabled ->

                    _uiState.update {
                        it.copy(
                            isMusicEnabled = enabled
                        )
                    }
                }
        }

        // ==============================
        // VIBRATION
        // ==============================

        viewModelScope.launch {

            scoreRepository.vibrationEnabledFlow
                .collectLatest { enabled ->

                    _uiState.update {
                        it.copy(
                            isVibrationEnabled = enabled
                        )
                    }
                }
        }

        // ==============================
        // RATE APP
        // ==============================

        viewModelScope.launch {

            scoreRepository.gamesPlayedFlow
                .collectLatest { games ->

                    if (games >= 3 && games % 5 == 0) {

                        scoreRepository.hasRatedFlow
                            .collectLatest { rated ->

                                if (!rated) {

                                    _uiState.update {
                                        it.copy(
                                            showRateDialog = true
                                        )
                                    }
                                }
                            }
                    }
                }
        }

        startGameSequence()
    }

    // =========================================================
    // START GAME
    // =========================================================

    private fun startGameSequence() {

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isStarting = true
                )
            }

            delay(1000)

            _uiState.update {
                it.copy(
                    isStarting = false
                )
            }

            startTimer()
        }
    }

    // =========================================================
    // RESET GAME
    // =========================================================

    fun resetGame() {

        timerJob?.cancel()

        val old =
            _uiState.value

        _uiState.value =
            GameState(
                grid =
                    GameLogic.createInitialGrid(),

                highScore =
                    old.highScore,

                walletBalance =
                    old.walletBalance,

                level =
                    old.level,

                targetScore =
                    old.targetScore,

                isSoundEnabled =
                    old.isSoundEnabled,

                isMusicEnabled =
                    old.isMusicEnabled,

                isVibrationEnabled =
                    old.isVibrationEnabled
            )

        viewModelScope.launch {

            scoreRepository.incrementGamesPlayed()
        }

        startGameSequence()
    }

    // =========================================================
    // START TIMER
    // =========================================================

    private fun startTimer() {

        timerJob?.cancel()

        timerJob =
            viewModelScope.launch {

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
                                    (
                                        it.timeLeftSeconds - 1
                                    ).coerceAtLeast(0)
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

                    _events.emit(
                        GameEvent.GAME_OVER
                    )
                }
            }
    }

    // =========================================================
    // CELL CLICK
    // =========================================================

    fun onCellClick(
        position: Position
    ) {

        val state =
            _uiState.value

        if (
            state.isProcessing ||
            state.isStarting ||
            state.movesLeft <= 0 ||
            state.timeLeftSeconds <= 0 ||
            state.isLevelUp
        ) {
            return
        }

        val selected =
            state.selectedPosition

        if (selected == null) {

            _uiState.update {
                it.copy(
                    selectedPosition = position
                )
            }

        } else {

            if (
                GameLogic.isAdjacent(
                    selected,
                    position
                )
            ) {

                swapAndProcess(
                    selected,
                    position
                )

            } else {

                _uiState.update {
                    it.copy(
                        selectedPosition = position
                    )
                }
            }
        }
    }

    // =========================================================
    // SWAP
    // =========================================================

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

            _events.emit(
                GameEvent.SWAP
            )

            val currentGrid =
                copyGrid(
                    _uiState.value.grid
                )

            val temp =
                currentGrid[p1.row][p1.col]

            currentGrid[p1.row][p1.col] =
                currentGrid[p2.row][p2.col]

            currentGrid[p2.row][p2.col] =
                temp

            _uiState.update {
                it.copy(
                    grid = currentGrid
                )
            }

            delay(150)

            val matches =
                GameLogic.findMatchGroups(
                    currentGrid
                )

            if (matches.isEmpty()) {

                val revertGrid =
                    copyGrid(
                        _uiState.value.grid
                    )

                val back =
                    revertGrid[p1.row][p1.col]

                revertGrid[p1.row][p1.col] =
                    revertGrid[p2.row][p2.col]

                revertGrid[p2.row][p2.col] =
                    back

                _uiState.update {
                    it.copy(
                        grid = revertGrid
                    )
                }

            } else {

                _uiState.update {

                    it.copy(
                        movesLeft =
                            (
                                it.movesLeft - 1
                            ).coerceAtLeast(0)
                    )
                }

                processMatches(
                    currentGrid,
                    p2
                )
            }

            _uiState.update {
                it.copy(
                    isProcessing = false
                )
            }

            checkMovesAvailable()
        }
    }

    // =========================================================
    // CHECK MOVES
    // =========================================================

    private fun checkMovesAvailable() {

        val available =
            GameLogic.hasAvailableMoves(
                _uiState.value.grid
            )

        _uiState.update {
            it.copy(
                hasMoves = available
            )
        }

        if (
            !available &&
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

                    shuffleBoard(
                        isAuto = true
                    )
                }
            }
        }
    }

    // =========================================================
    // MATCH PROCESSING
    // =========================================================

    private suspend fun processMatches(
        grid: Array<Array<Fruit?>>,
        triggeredBy: Position? = null
    ) {

        var currentGrid =
            copyGrid(grid)

        var combo = 0

        while (true) {

            val groups =
                GameLogic.findMatchGroups(
                    currentGrid
                )

            if (groups.isEmpty()) {
                break
            }

            combo++

            val matchPositions =
                groups
                    .flatten()
                    .toSet()

            val specials =
                mutableListOf<
                    Triple<
                        Position,
                        FruitType,
                        SpecialType
                    >
                >()

            groups.forEach { group ->

                if (group.size >= 4) {

                    val fruit =
                        currentGrid[
                            group[0].row
                        ][
                            group[0].col
                        ] ?: return@forEach

                    val specialType =

                        when {

                            group.size >= 5 ->
                                SpecialType.BOMB

                            group.size == 4 &&
                                group.all {
                                    it.row ==
                                        group[0].row
                                } ->
                                SpecialType.ROW_BLAST

                            else ->
                                SpecialType.COL_BLAST
                        }

                    val specialPosition =

                        if (
                            triggeredBy != null &&
                            triggeredBy in group
                        ) {

                            triggeredBy

                        } else {

                            group[
                                group.size / 2
                            ]
                        }

                    specials.add(
                        Triple(
                            specialPosition,
                            fruit.type,
                            specialType
                        )
                    )
                }
            }

            val affected =
                GameLogic.getAffectedPositions(
                    currentGrid,
                    matchPositions
                )

            val basePoints =
                affected.size * 10

            val comboBonus =
                (combo - 1) * 15

            val specialBonus =
                if (
                    affected.size >
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

            _events.emit(
                GameEvent.MATCH
            )

            if (
                affected.size >
                matchPositions.size
            ) {

                _events.emit(
                    GameEvent.SPECIAL_EXPLOSION
                )
            }

            // Clear affected positions
            affected.forEach { position ->

                if (
                    position.row in
                    0 until GameLogic.GRID_SIZE &&
                    position.col in
                    0 until GameLogic.GRID_SIZE
                ) {

                    currentGrid[
                        position.row
                    ][
                        position.col
                    ] = null
                }
            }

            // Create special candies
            specials.forEach {

                (
                    position,
                    fruitType,
                    specialType
                ) ->

                currentGrid[
                    position.row
                ][
                    position.col
                ] =
                    Fruit(
                        type = fruitType,
                        special = specialType
                    )
            }

            val newScore =
                _uiState.value.score +
                points

            _uiState.update {

                it.copy(
                    grid =
                        copyGrid(
                            currentGrid
                        ),

                    score =
                        newScore,

                    lastComboCount =
                        combo
                )
            }

            if (
                newScore >
                _uiState.value.highScore
            ) {

                scoreRepository
                    .updateHighScore(
                        newScore
                    )
            }

            delay(300)

            GameLogic.applyGravity(
                currentGrid
            )

            _uiState.update {
                it.copy(
                    grid =
                        copyGrid(
                            currentGrid
                        )
                )
            }

            delay(200)

            GameLogic.refillGrid(
                currentGrid
            )

            _uiState.update {
                it.copy(
                    grid =
                        copyGrid(
                            currentGrid
                        )
                )
            }

            delay(250)
        }

        // =====================================================
        // LEVEL COMPLETE
        // =====================================================

        if (
            _uiState.value.score >=
            _uiState.value.targetScore
        ) {

            levelUp()
        }
    }

    // =========================================================
    // LEVEL UP + UNLOCK
    // =========================================================

    private fun levelUp() {

        viewModelScope.launch {

            val completedLevel =
                _uiState.value.level

            // ---------------------------------------------
            // EARNING
            // ---------------------------------------------

            val earning =

                when {

                    completedLevel in 1..50 ->
                        2

                    completedLevel in 51..100 ->
                        3

                    completedLevel in 101..150 ->
                        5

                    completedLevel in 151..200 ->
                        10

                    else ->
                        15
                }

            scoreRepository.addEarning(
                earning
            )

            // ---------------------------------------------
            // UNLOCK NEXT LEVEL
            // ---------------------------------------------

            val nextLevel =
                (completedLevel + 1)
                    .coerceAtMost(1000)

            scoreRepository.unlockLevel(
                nextLevel
            )

            // ---------------------------------------------
            // LEVEL UP EVENT
            // ---------------------------------------------

            _events.emit(
                GameEvent.LEVEL_UP
            )

            _uiState.update {
                it.copy(
                    isLevelUp = true
                )
            }

            delay(1200)

            // ---------------------------------------------
            // NEW TARGET
            // ---------------------------------------------

            val nextTarget =
                calculateTargetScore(
                    nextLevel
                )

            _uiState.update {

                it.copy(

                    level =
                        nextLevel,

                    targetScore =
                        nextTarget,

                    movesLeft =
                        calculateMoves(
                            nextLevel
                        ),

                    timeLeftSeconds =
                        calculateTime(
                            nextLevel
                        ),

                    isLevelUp =
                        false,

                    score =
                        0,

                    grid =
                        GameLogic.createInitialGrid()
                )
            }

            startTimer()
        }
    }

    // =========================================================
    // TARGET SCORE
    // =========================================================

    private fun calculateTargetScore(
        level: Int
    ): Int {

        return when {

            level <= 50 ->
                300 + ((level - 1) * 50)

            level <= 100 ->
                2800 + ((level - 50) * 75)

            level <= 150 ->
                6550 + ((level - 100) * 100)

            level <= 200 ->
                11550 + ((level - 150) * 125)

            level <= 500 ->
                17800 + ((level - 200) * 180)

            else ->
                71800 + ((level - 500) * 250)
        }
    }

    // =========================================================
    // MOVES
    // =========================================================

    private fun calculateMoves(
        level: Int
    ): Int {

        return when {

            level <= 20 ->
                25

            level <= 50 ->
                24

            level <= 100 ->
                23

            level <= 200 ->
                22

            level <= 500 ->
                21

            else ->
                20
        }
    }

    // =========================================================
    // TIME
    // =========================================================

    private fun calculateTime(
        level: Int
    ): Int {

        return when {

            level <= 20 ->
                70

            level <= 100 ->
                65

            level <= 500 ->
                60

            else ->
                55
        }
    }

    // =========================================================
    // SHUFFLE
    // =========================================================

    fun shuffleBoard(
        isAuto: Boolean = false
    ) {

        if (
            _uiState.value.isProcessing ||
            _uiState.value.isLevelUp
        ) {
            return
        }

        val cost =

            if (isAuto) {
                0
            } else {
                2
            }

        if (
            _uiState.value.movesLeft <
            cost
        ) {
            return
        }

        viewModelScope.launch {

            _uiState.update {

                it.copy(
                    isProcessing = true,

                    movesLeft =
                        (
                            it.movesLeft - cost
                        ).coerceAtLeast(0)
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

            delay(250)

            _uiState.update {
                it.copy(
                    isProcessing = false
                )
            }

            checkMovesAvailable()
        }
    }

    // =========================================================
    // SETTINGS
    // =========================================================

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

    // =========================================================
    // REWARDED AD
    // =========================================================

    fun requestRewardedAd() {

        viewModelScope.launch {

            _events.emit(
                GameEvent.REQUEST_REWARDED_AD
            )
        }
    }

    // =========================================================
    // REWARD MOVES
    // =========================================================

    fun grantRewardMoves(
        count: Int = 10
    ) {

        if (count <= 0) return

        _uiState.update {

            it.copy(
                movesLeft =
                    it.movesLeft + count
            )
        }

        checkMovesAvailable()
    }

    // =========================================================
    // RATE APP
    // =========================================================

    fun onRateApp() {

        viewModelScope.launch {

            scoreRepository.setHasRated(
                true
            )

            _uiState.update {
                it.copy(
                    showRateDialog = false
                )
            }

            _events.emit(
                GameEvent.RATE_APP
            )
        }
    }

    fun onDismissRateDialog() {

        _uiState.update {

            it.copy(
                showRateDialog = false
            )
        }
    }

    // =========================================================
    // SWIPE
    // =========================================================

    fun onSwipe(
        position: Position,
        direction: DragDirection
    ) {

        val state =
            _uiState.value

        if (
            state.isProcessing ||
            state.isStarting ||
            state.movesLeft <= 0 ||
            state.timeLeftSeconds <= 0 ||
            state.isLevelUp
        ) {
            return
        }

        val target =
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
            target.row in
            0 until GameLogic.GRID_SIZE &&
            target.col in
            0 until GameLogic.GRID_SIZE
        ) {

            swapAndProcess(
                position,
                target
            )
        }
    }

    // =========================================================
    // COPY GRID
    // =========================================================

    private fun copyGrid(
        original: Array<Array<Fruit?>>
    ): Array<Array<Fruit?>> {

        return Array(
            GameLogic.GRID_SIZE
        ) { row ->

            Array(
                GameLogic.GRID_SIZE
            ) { col ->

                original[row][col]
            }
        }
    }

    // =========================================================
    // CLEAR
    // =========================================================

    override fun onCleared() {

        timerJob?.cancel()

        super.onCleared()
    }
}
