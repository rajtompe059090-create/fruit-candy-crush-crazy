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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "🍓 Fruit Candy Crush 🍬",
                color = Color.White,
                fontSize = 25.sp
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoBox("LEVEL", state.level.toString())
                InfoBox("SCORE", state.score.toString())
                InfoBox("MOVES", state.movesLeft.toString())
                InfoBox("TIME", state.timeLeftSeconds.toString())
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Target: ${state.targetScore}",
                color = Color.White,
                fontSize = 17.sp
            )

            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {

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

            Spacer(Modifier.height(8.dp))

            Text(
                text = "💰 Wallet: ₹${state.walletBalance}",
                color = Color(0xFFFFD54F),
                fontSize = 21.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "🏆 High Score: ${state.highScore}",
                color = Color.White,
                fontSize = 17.sp
            )

            Spacer(Modifier.height(8.dp))

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

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        viewModel.requestRewardedAd()
                    },
                    enabled =
                        !state.isProcessing &&
                        !state.isLevelUp
                ) {
                    Text("🎁 +5 Moves")
                }
            }

            Spacer(Modifier.height(6.dp))

            Button(
                onClick = {
                    viewModel.resetGame()
                }
            ) {
                Text("🔄 New Game")
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Level Rewards",
                color = Color.White,
                fontSize = 17.sp
            )

            Text(
                text =
                    "1-50 ₹2  •  51-100 ₹3  •  " +
                    "101-150 ₹5  •  151-200 ₹10  •  201+ ₹15",
                color = Color.LightGray,
                fontSize = 13.sp
            )
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

                    Spacer(Modifier.height(12.dp))

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
            width = 80.dp,
            height = 65.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = title,
                fontSize = 10.sp
            )

            Text(
                text = value,
                fontSize = 18.sp
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

                    val position =
                        Position(row, col)

                    FruitCell(
                        fruit = fruit,
                        selected =
                            selectedPosition == position,
                        enabled = enabled,
                        onClick = {
                            onCellClick(position)
                        },
                        onSwipe = {
                            onSwipe(
                                position,
                                it
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

    val emoji =
        fruit?.emoji ?: "🍬"

    Box(
        modifier = Modifier
            .size(42.dp)
            .padding(1.dp)
            .background(
                if (selected)
                    Color(0xFFFFD54F)
                else
                    Color(0xFF455A64),
                RoundedCornerShape(7.dp)
            )
            .pointerInput(enabled) {

                if (enabled) {

                    detectDragGestures(
                        onDragStart = {},
                        onDragCancel = {},
                        onDragEnd = {},

                        onDrag = { change, dragAmount ->

                            change.consume()

                            val dx =
                                dragAmount.x

                            val dy =
                                dragAmount.y

                            if (
                                abs(dx) >
                                abs(dy)
                            ) {

                                if (abs(dx) > 8f) {

                                    onSwipe(
                                        if (dx > 0)
                                            DragDirection.RIGHT
                                        else
                                            DragDirection.LEFT
                                    )
                                }

                            } else {

                                if (abs(dy) > 8f) {

                                    onSwipe(
                                        if (dy > 0)
                                            DragDirection.DOWN
                                        else
                                            DragDirection.UP
                                    )
                                }
                            }
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = emoji,
            fontSize = 25.sp
        )
    }
}
