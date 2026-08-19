package com.fruitcandycrushcarzy.APP.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fruitcandycrushcarzy.APP.game.model.Fruit
import com.fruitcandycrushcarzy.APP.game.model.FruitType
import com.fruitcandycrushcarzy.APP.game.model.Position
import com.fruitcandycrushcarzy.APP.game.viewmodel.DragDirection
import com.fruitcandycrushcarzy.APP.game.viewmodel.GameViewModel
import kotlin.math.abs

@Composable
fun GameScreen(
    viewModel: GameViewModel
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101820))
    ) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            item {

                Text(
                    text = "🍓 Fruit Candy Crush 🍬",
                    color = Color.White,
                    fontSize = 26.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    InfoBox("LEVEL", state.level.toString())
                    InfoBox("SCORE", state.score.toString())
                    InfoBox("MOVES", state.movesLeft.toString())
                    InfoBox("TIME", state.timeLeftSeconds.toString())
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "Target: ${state.targetScore}",
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        GameBoard(
                            grid = state.grid,
                            selectedPosition = state.selectedPosition,
                            enabled =
                                !state.isProcessing &&
                                !state.isStarting &&
                                !state.isLevelUp,
                            onCellClick = {
                                viewModel.onCellClick(it)
                            },
                            onSwipe = { position, direction ->
                                viewModel.onSwipe(
                                    position,
                                    direction
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "💰 Wallet: ₹${state.walletBalance}",
                    color = Color(0xFFFFD54F),
                    fontSize = 22.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "🏆 High Score: ${state.highScore}",
                    color = Color.White,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.Center
                ) {

                    Button(
                        onClick = {
                            viewModel.shuffleBoard()
                        },
                        enabled =
                            !state.isProcessing &&
                            !state.isLevelUp
                    ) {
                        Text("🔀 Shuffle")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            viewModel.requestRewardedAd()
                        },
                        enabled =
                            !state.isProcessing &&
                            !state.isLevelUp
                    ) {
                        Text("🎁 Get Moves")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        viewModel.resetGame()
                    }
                ) {
                    Text("🔄 New Game")
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Earnings per level",
                    color = Color.White,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text =
                        "Level 1-50  → ₹2\n" +
                        "Level 51-100 → ₹3\n" +
                        "Level 101-150 → ₹5\n" +
                        "Level 151-200 → ₹10\n" +
                        "Level 201+ → ₹15",
                    color = Color.LightGray,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Recent Transactions",
                    color = Color.White,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (state.transactions.isEmpty()) {

                    Text(
                        text = "No transactions yet",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )

                } else {

                    state.transactions
                        .take(10)
                        .forEach { transaction ->

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement =
                                        Arrangement.SpaceBetween
                                ) {

                                    Text(
                                        text = transaction.description
                                    )

                                    Text(
                                        text = "+₹${transaction.amount}"
                                    )
                                }
                            }
                        }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        if (state.isStarting) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "🍓 GET READY! 🍬",
                    color = Color.White,
                    fontSize = 30.sp
                )
            }
        }

        if (state.isLevelUp) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "🎉 LEVEL UP! 🎉",
                        color = Color.White,
                        fontSize = 32.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Next Level: ${state.level + 1}",
                        color = Color(0xFFFFD54F),
                        fontSize = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoBox(
    title: String,
    value: String
) {

    Card(
        modifier = Modifier.size(
            width = 82.dp,
            height = 70.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = title,
                fontSize = 11.sp
            )

            Text(
                text = value,
                fontSize = 19.sp
            )
        }
    }
}

@Composable
private fun GameBoard(
    grid: Array<Array<Fruit?>>,
    selectedPosition: Position?,
    enabled: Boolean,
    onCellClick: (Position) -> Unit,
    onSwipe: (Position, DragDirection) -> Unit
) {

    Column(
        modifier = Modifier
            .background(
                Color(0xFF263238),
                RoundedCornerShape(12.dp)
            )
            .padding(5.dp)
    ) {

        grid.forEachIndexed { row, fruits ->

            Row {

                fruits.forEachIndexed { col, fruit ->

                    val position = Position(row, col)

                    FruitCell(
                        fruit = fruit,
                        selected =
                            selectedPosition == position,
                        enabled = enabled,
                        onClick = {
                            onCellClick(position)
                        },
                        onSwipe = { direction ->
                            onSwipe(
                                position,
                                direction
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FruitCell(
    fruit: Fruit?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onSwipe: (DragDirection) -> Unit
) {

    val fruitColor = when (fruit?.type) {

        FruitType.APPLE ->
            Color(0xFFE53935)

        FruitType.BANANA ->
            Color(0xFFFFD600)

        FruitType.GRAPE ->
            Color(0xFF8E24AA)

        FruitType.ORANGE ->
            Color(0xFFFB8C00)

        FruitType.STRAWBERRY ->
            Color(0xFFD81B60)

        FruitType.WATERMELON ->
            Color(0xFF43A047)

        null ->
            Color(0xFF455A64)

        else ->
            Color(0xFF00ACC1)
    }

    val fruitSymbol = when (fruit?.type) {

        FruitType.APPLE -> "A"
        FruitType.BANANA -> "B"
        FruitType.GRAPE -> "G"
        FruitType.ORANGE -> "O"
        FruitType.STRAWBERRY -> "S"
        FruitType.WATERMELON -> "W"

        null -> ""

        else -> "C"
    }

    Box(
        modifier = Modifier
            .size(52.dp)
            .padding(2.dp)
            .background(
                if (selected) {
                    Color(0xFFFFD54F)
                } else {
                    Color(0xFF37474F)
                },
                RoundedCornerShape(10.dp)
            )
            .pointerInput(enabled) {

                if (enabled) {

                    detectDragGestures(
                        onDragStart = {
                            // Start of swipe
                        },

                        onDrag = { _, _ ->
                            // Movement handled on end
                        },

                        onDragEnd = {
                            // Swipe detection is handled below
                        },

                        onDragCancel = {
                            // Nothing
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {

        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(46.dp),
            shape = CircleShape
        ) {

            Text(
                text = fruitSymbol,
                fontSize = 20.sp
            )
        }
    }
}
