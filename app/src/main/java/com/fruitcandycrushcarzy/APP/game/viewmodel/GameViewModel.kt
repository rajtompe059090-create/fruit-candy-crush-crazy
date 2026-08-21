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

    val movesLeft: Int = 25,

    val selectedPosition: Position? = null,

    val isProcessing: Boolean = false,

    val level: Int = 1,

    val timeLeftSeconds: Int = 90,

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

        viewModelScope.launch {
            scoreRepository.highScoreFlow.collectLatest { high ->

                _uiState.update {
                    it.copy(
                        highScore = high
                    )
                }
            }
        }

        viewModelScope.launch {
            scoreRepository.walletBalanceFlow.collectLatest { balance ->

                _uiState.update {
                    it.copy(
                        walletBalance = balance
                    )
                }
            }
        }

        viewModelScope.launch {
            scoreRepository.soundEnabledFlow.collectLatest { enabled ->

                _uiState.update {
                    it.copy(
                        isSoundEnabled = enabled
                    )
                }
            }
        }

        viewModelScope.launch {
            scoreRepository.musicEnabledFlow.collectLatest { enabled ->

                _uiState.update {
                    it.copy(
                        isMusicEnabled = enabled
                    )
                }
            }
        }

        viewModelScope.launch {
            scoreRepository.vibrationEnabledFlow.collectLatest { enabled ->

                _uiState.update {
                    it.copy(
                        isVibrationEnabled = enabled
                    )
                }
            }
        }

        viewModelScope.launch {

            scoreRepository.gamesPlayedFlow.collectLatest { games ->

                scoreRepository.hasRatedFlow.collectLatest { hasRated ->

                    if (
                        !hasRated &&
                        games >= 3 &&
                        games % 5 == 0
                    ) {

                        _uiState.update {
                            it.copy(
                                showRateDialog = true
                            )
                        }
                    }
                }
            }
        }

        startGameSequence()
    }

    /*
     * =========================================================
     * START SELECTED LEVEL
     * =========================================================
     */

    fun startLevel(levelNumber: Int) {

        if (levelNumber < 1) return

        timerJob?.cancel()

        val safeLevel =
            levelNumber.coerceIn(1, 1000)

        _uiState.update {

            it.copy(

                grid =
                    GameLogic.createInitialGrid(),

                score = 0,

                level = safeLevel,

                targetScore =
                    getTargetScore(safeLevel),

                movesLeft =
                    getMovesForLevel(safeLevel),

                timeLeftSeconds =
                    getTimeForLevel(safeLevel),

                selectedPosition = null,

                isProcessing = false,

                isLevelUp = false,

                isStarting = true,

                hasMoves = true
            )
        }

        startGameSequence()
    }

    /*
     * =========================================================
     * LEVEL DIFFICULTY
     * =========================================================
     */

    private fun getTargetScore(level: Int): Int {

        return when {

            level <= 50 ->
                300 + ((level - 1) * 75)

            level <= 100 ->
                3975 + ((level - 50) * 125)

            level <= 200 ->
                10225 + ((level - 100) * 175)

            level <= 500 ->
                27725 + ((level - 200) * 250)

            else ->
                102725 + ((level - 500) * 400)
        }
    }

    private fun getMovesForLevel(level: Int): Int {

        return when {

            level <= 20 -> 25

            level <= 50 -> 24

            level <= 100 -> 23

            level <= 200 -> 22

            level <= 500 -> 21

            else -> 20
        }
    }

    private fun getTimeForLevel(level: Int): Int {

        return when {

            level <= 20 -> 90

            level <= 50 -> 85

            level <= 100 -> 80

            level <= 200 -> 75

            level <= 500 -> 70

            else -> 60
        }
    }

    /*
     * =========================================================
     * GAME START
     * =========================================================
     */

    private fun startGameSequence() {

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isStarting = true
                )
            }

            delay(800)

            _uiState.update {
                it.copy(
                    isStarting = false
                )
            }

            startTimer()
        }
    }

    /*
     * =========================================================
     * RESET
     * =========================================================
     */

    fun resetGame() {

        val currentLevel =
            _uiState.value.level

        startLevel(currentLevel)

        viewModelScope.launch {
            scoreRepository.incrementGamesPlayed()
        }
    }

    /*
     * =========================================================
     * TIMER
     * =========================================================
     */

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

    /*
     * =========================================================
     * CELL CLICK
     * =========================================================
     */

    fun onCellClick(position: Position) {

        if (

            _uiState.value.isProcessing ||

            _uiState.value.isStarting ||

            _uiState.value.movesLeft <= 0 ||

            _uiState.value.timeLeftSeconds <= 0 ||

            _uiState.value.isLevelUp

        ) return

        val selected =
            _uiState.value.selectedPosition

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

    /*
     * =========================================================
     * SWIPE
     * =========================================================
     */

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

    /*
     * =========================================================
     * SWAP + MATCH
     * =========================================================
     */

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

            delay(160)

            val matches =
                GameLogic.findMatchGroups(
                    currentGrid
                )

            if (matches.isEmpty()) {

                val revertGrid =
                    copyGrid(
                        _uiState.value.grid
                    )

                val tempBack =
                    revertGrid[p1.row][p1.col]

                revertGrid[p1.row][p1.col] =
                    revertGrid[p2.row][p2.col]

                revertGrid[p2.row][p2.col] =
                    tempBack

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

    /*
     * =========================================================
     * PROCESS MATCHES
     * =========================================================
     */

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

            val specialFruitsToCreate =
                mutableListOf<
                    Triple<
                        Position,
                        FruitType,
                        SpecialType
                    >
                >()

            /*
             * 4 = LINE BLAST
             * 5+ = BOMB
             */

            groups.forEach { group ->

                if (group.size >= 4) {

                    val firstFruit =
                        currentGrid[
                            group[0].row
                        ][
                            group[0].col
                        ] ?: return@forEach

                    val fruitType =
                        firstFruit.type

                    val specialType =

                        when {

                            group.size >= 5 ->
                                SpecialType.BOMB

                            group.size == 4 &&
                                group[0].row ==
                                group[1].row ->
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

                    specialFruitsToCreate.add(

                        Triple(

                            specialPosition,

                            fruitType,

                            specialType
                        )
                    )
                }
            }

            /*
             * SPECIAL EXPLOSION
             */

            val affectedPositions =
                GameLogic.getAffectedPositions(

                    currentGrid,

                    matchPositions
                )

            if (
                affectedPositions.size >
                matchPositions.size
            ) {

                _events.emit(
                    GameEvent.SPECIAL_EXPLOSION
                )
            }

            /*
             * SCORE
             */

            val basePoints =
                affectedPositions.size * 15

            val comboBonus =
                (combo - 1) * 25

            val specialBonus =
                if (
                    affectedPositions.size >
                    matchPositions.size
                ) {
                    100
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

            /*
             * CLEAR
             */

            val positionsToClear =
                affectedPositions
                    .filter { position ->

                        specialFruitsToCreate.none {

                            it.first ==
                                position
                        }
                    }
                    .toSet()

            positionsToClear.forEach { position ->

                currentGrid[
                    position.row
                ][
                    position.col
                ] = null
            }

            /*
             * CREATE SPECIAL FRUITS
             */

            specialFruitsToCreate.forEach {

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

            delay(280)

            /*
             * GRAVITY
             */

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

            delay(180)

            /*
             * REFILL
             */

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

            delay(180)
        }

        /*
         * LEVEL COMPLETE
         */

        if (

            _uiState.value.score >=
            _uiState.value.targetScore

        ) {

            levelUp()
        }
    }

    /*
     * =========================================================
     * LEVEL UP
     * =========================================================
     */

    private fun levelUp() {

        viewModelScope.launch {

            val completedLevel =
                _uiState.value.level

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

            /*
             * ADD WALLET REWARD
             */

            scoreRepository.addEarning(
                earning
            )

            /*
             * SAVE HIGHEST LEVEL
             */

            val context =
                scoreRepository.context

            val preferences =
                context.getSharedPreferences(
                    "game_progress",
                    android.content.Context.MODE_PRIVATE
                )

            val nextLevel =
                (
                    completedLevel + 1
                ).coerceAtMost(1000)

            val oldHighest =
                preferences.getInt(
                    "highest_level",
                    1
                )

            if (nextLevel > oldHighest) {

                preferences.edit()
                    .putInt(
                        "highest_level",
                        nextLevel
                    )
                    .apply()
            }

            _events.emit(
                GameEvent.LEVEL_UP
            )

            _uiState.update {

                it.copy(
                    isLevelUp = true
                )
            }

            delay(1200)

            if (completedLevel >= 1000) {

                _uiState.update {

                    it.copy(
                        isLevelUp = false
                    )
                }

                return@launch
            }

            _uiState.update {

                it.copy(

                    level =
                        nextLevel,

                    targetScore =
                        getTargetScore(
                            nextLevel
                        ),

                    movesLeft =
                        getMovesForLevel(
                            nextLevel
                        ),

                    timeLeftSeconds =
                        getTimeForLevel(
                            nextLevel
                        ),

                    isLevelUp = false,

                    score = 0,

                    selectedPosition = null,

                    grid =
                        GameLogic.createInitialGrid()
                )
            }

            startTimer()
        }
    }

    /*
     * =========================================================
     * SHUFFLE
     * =========================================================
     */

    fun shuffleBoard(
        isAuto: Boolean = false
    ) {

        if (

            _uiState.value.isProcessing ||

            _uiState.value.isLevelUp

        ) return

        val cost =

            if (isAuto) {
                0
            } else {
                2
            }

        if (
            _uiState.value.movesLeft <
            cost
        ) return

        viewModelScope.launch {

            _uiState.update {

                it.copy(

                    isProcessing = true,

                    movesLeft =
                        it.movesLeft - cost
                )
            }

            var newGrid =
                GameLogic.createInitialGrid()

            /*
             * Try to make sure
             * board has a possible move.
             */

            var attempts = 0

            while (

                !GameLogic.hasAvailableMoves(
                    newGrid
                ) &&

                attempts < 20

            ) {

                newGrid =
                    GameLogic.createInitialGrid()

                attempts++
            }

            _uiState.update {

                it.copy(

                    grid = newGrid,

                    hasMoves = true
                )
            }

            delay(180)

            _uiState.update {

                it.copy(
                    isProcessing = false
                )
            }

            checkMovesAvailable()
        }
    }

    /*
     * =========================================================
     * CHECK AVAILABLE MOVES
     * =========================================================
     */

    private fun checkMovesAvailable() {

        val hasMoves =
            GameLogic.hasAvailableMoves(
                _uiState.value.grid
            )

        _uiState.update {

            it.copy(
                hasMoves = hasMoves
            )
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

                    shuffleBoard(
                        isAuto = true
                    )
                }
            }
        }
    }

    /*
     * =========================================================
     * SETTINGS
     * =========================================================
     */

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

    /*
     * =========================================================
     * REWARDED AD
     * =========================================================
     */

    fun requestRewardedAd() {

        viewModelScope.launch {

            _events.emit(
                GameEvent.REQUEST_REWARDED_AD
            )
        }
    }

    fun grantRewardMoves(
        count: Int = 10
    ) {

        _uiState.update {

            it.copy(

                movesLeft =
                    it.movesLeft + count,

                timeLeftSeconds =
                    maxOf(
                        it.timeLeftSeconds,
                        30
                    )
            )
        }

        checkMovesAvailable()
    }

    /*
     * =========================================================
     * RATE APP
     * =========================================================
     */

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

    /*
     * =========================================================
     * COPY GRID
     * =========================================================
     */

    private fun copyGrid(
        original: Array<Array<Fruit?>>
    ): Array<Array<Fruit?>> {

        return Array(
            GameLogic.GRID_SIZE
        ) { r ->

            Array(
                GameLogic.GRID_SIZE
            ) { c ->

                original[r][c]
            }
        }
    }

    /*
     * =========================================================
     * CLEAR
     * =========================================================
     */

    override fun onCleared() {

        timerJob?.cancel()

        super.onCleared()
    }
}
