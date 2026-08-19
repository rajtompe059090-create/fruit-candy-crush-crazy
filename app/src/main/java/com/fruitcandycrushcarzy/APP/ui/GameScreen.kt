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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "🍓 Fruit Crush",
                    color = Color.White,
                    fontSize = 23.sp
                )

                Button(
                    onClick = {
                        viewModel.toggleSettings()
                    }
                ) {
                    Text("⚙️")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SmallInfo("LVL", state.level.toString())
                SmallInfo("SCORE", state.score.toString())
                SmallInfo("MOVES", state.movesLeft.toString())
                SmallInfo("TIME", state.timeLeftSeconds.toString())
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Target: ${state.targetScore}",
                color = Color(0xFFFFD54F),
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(5.dp))

            GameBoard(
                grid = state.grid,
                selectedPosition = state.selectedPosition,
                enabled =
                    !state.isProcessing &&
                    !state.isStarting &&
                    !state.isLevelUp,
                onSwipe = { position, direction ->
                    viewModel.onSwipe(position, direction)
                }
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = "💰 ₹${state.walletBalance}",
                color = Color(0xFFFFD54F),
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

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
                    Text("🔀")
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
                    Text("🎁 +5")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        viewModel.resetGame()
                    }
                ) {
                    Text("🔄")
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Swipe fruits to match 3 or more",
                color = Color.LightGray,
                fontSize = 13.sp
            )
        }

        if (state.isStarting) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD000000)),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "🍓 GET READY! 🍬",
                    color = Color.White,
                    fontSize = 28.sp
                )
            }
        }

        if (state.isLevelUp) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD000000)),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "🎉 LEVEL UP! 🎉",
                        color = Color.White,
                        fontSize = 30.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Level ${state.level + 1}",
                        color = Color(0xFFFFD54F),
                        fontSize = 22.sp
                    )
                }
            }
        }

        if (state.showSettings) {

            SettingsPanel(
                soundEnabled = state.isSoundEnabled,
                musicEnabled = state.isMusicEnabled,
                vibrationEnabled = state.isVibrationEnabled,
                onSound = {
                    viewModel.toggleSound()
                },
                onMusic = {
                    viewModel.toggleMusic()
                },
                onVibration = {
                    viewModel.toggleVibration()
                },
                onClose = {
                    viewModel.toggleSettings()
                }
            )
        }
    }
}

@Composable
private fun SmallInfo(
    title: String,
    value: String
) {

    Card(
        modifier = Modifier
            .width(78.dp)
            .height(48.dp),
        shape = RoundedCornerShape(10.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = title,
                fontSize = 10.sp
            )

            Text(
                text = value,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun GameBoard(
    grid: Array<Array<Fruit?>>,
    selectedPosition: Position?,
    enabled: Boolean,
    onSwipe: (Position, DragDirection) -> Unit
) {

    Column(
        modifier = Modifier
            .background(
                Color(0xFF263238),
                RoundedCornerShape(12.dp)
            )
            .padding(4.dp)
    ) {

        grid.forEachIndexed { row, fruits ->

            Row {

                fruits.forEachIndexed { col, fruit ->

                    FruitCell(
                        fruit = fruit,
                        selected = selectedPosition ==
                            Position(row, col),
                        enabled = enabled,
                        onSwipe = { direction ->
                            onSwipe(
                                Position(row, col),
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
    onSwipe: (DragDirection) -> Unit
) {

    val fruitText = when (fruit?.type) {

        FruitType.APPLE -> "🍎"
        FruitType.ORANGE -> "🍊"
        FruitType.GRAPE -> "🍇"
        FruitType.STRAWBERRY -> "🍓"
        FruitType.BANANA -> "🍌"
        FruitType.KIWI -> "🥝"
        FruitType.PEACH -> "🍑"
        FruitType.CHERRY -> "🍒"

        null -> "❔"
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .padding(2.dp)
            .background(
                if (selected)
                    Color(0xFFFFD54F)
                else
                    Color(0xFF37474F),
                RoundedCornerShape(8.dp)
            )
            .pointerInput(enabled) {

                if (enabled) {

                    detectDragGestures(
                        onDragEnd = {},
                        onDragCancel = {},
                        onDrag = { _, _ -> },
                        onDragStart = { _ -> }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled) {

                    if (enabled) {

                        detectDragGestures(

                            onDragStart = {},

                            onDrag = { _, _ -> },

                            onDragCancel = {},

                            onDragEnd = {}
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = fruitText,
                fontSize = 25.sp
            )
        }
    }
}

@Composable
private fun SettingsPanel(
    soundEnabled: Boolean,
    musicEnabled: Boolean,
    vibrationEnabled: Boolean,
    onSound: () -> Unit,
    onMusic: () -> Unit,
    onVibration: () -> Unit,
    onClose: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE101820)),
        contentAlignment = Alignment.Center
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp),
            shape = RoundedCornerShape(20.dp)
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "⚙️ Settings",
                    fontSize = 25.sp
                )

                Spacer(modifier = Modifier.height(15.dp))

                SettingRow(
                    title = "🔊 Sound",
                    enabled = soundEnabled,
                    onClick = onSound
                )

                HorizontalDivider()

                SettingRow(
                    title = "🎵 Music",
                    enabled = musicEnabled,
                    onClick = onMusic
                )

                HorizontalDivider()

                SettingRow(
                    title = "📳 Vibration",
                    enabled = vibrationEnabled,
                    onClick = onVibration
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CLOSE")
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = title,
            fontSize = 17.sp
        )

        Switch(
            checked = enabled,
            onCheckedChange = {
                onClick()
            }
        )
    }
}
