package com.fruitcandycrushcarzy.APP.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fruitcandycrushcarzy.APP.game.model.Position
import com.fruitcandycrushcarzy.APP.game.viewmodel.GameViewModel
import com.fruitcandycrushcarzy.APP.ui.components.AdBanner
import com.fruitcandycrushcarzy.APP.ui.components.FruitCell
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    viewModel: GameViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    val themeColors = remember(uiState.level) {
        when (uiState.level % 4) {
            1 -> Triple(
                listOf(Color(0xFF1A237E), Color(0xFF4A148C)),
                Color.Yellow,
                Color(0xFF311B92)
            )
            2 -> Triple(
                listOf(Color(0xFF004D40), Color(0xFF00BCD4)),
                Color(0xFFFFD600),
                Color(0xFF006064)
            )
            3 -> Triple(
                listOf(Color(0xFF3E2723), Color(0xFFBF360C)),
                Color(0xFFFFAB40),
                Color(0xFF5D4037)
            )
            else -> Triple(
                listOf(Color(0xFF263238), Color(0xFF37474F)),
                Color(0xFF00E676),
                Color(0xFF212121)
            )
        }
    }

    val bgColors = themeColors.first
    val accentColor = themeColors.second
    val cardBg = themeColors.third

    val animatedBgStart by animateColorAsState(
        targetValue = bgColors[0],
        animationSpec = tween(1500),
        label = "bgStart"
    )

    val animatedBgEnd by animateColorAsState(
        targetValue = bgColors[1],
        animationSpec = tween(1500),
        label = "bgEnd"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(animatedBgStart, animatedBgEnd)
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(35.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {
                    Text(
                        text = "LEVEL ${uiState.level}",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = "🍓 FRUIT CRUSH 🍬",
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.toggleSettings()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBg.copy(alpha = 0.7f)
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    StatCard(
                        "SCORE",
                        uiState.score.toString(),
                        accentColor
                    )

                    StatCard(
                        "BEST",
                        uiState.highScore.toString(),
                        Color.White
                    )

                    StatCard(
                        "TARGET",
                        uiState.targetScore.toString(),
                        Color.Yellow
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                StatCard(
                    "MOVES",
                    uiState.movesLeft.toString(),
                    if (uiState.movesLeft <= 5)
                        Color.Red
                    else
                        Color.White
                )

                StatCard(
                    "TIME",
                    "${uiState.timeLeftSeconds}s",
                    if (uiState.timeLeftSeconds <= 15)
                        Color.Red
                    else
                        Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(25.dp),
                color = Color.Black.copy(alpha = 0.35f)
            ) {

                Column(
                    modifier = Modifier.padding(8.dp)
                ) {

                    for (r in 0 until 8) {

                        Row(
                            modifier = Modifier.weight(1f)
                        ) {

                            for (c in 0 until 8) {

                                val pos = Position(r, c)

                                FruitCell(
                                    fruit = uiState.grid[r][c],
                                    isSelected =
                                        uiState.selectedPosition == pos,

                                    onClick = {
                                        viewModel.onCellClick(pos)
                                    },

                                    onSwipe = { direction ->
                                        viewModel.onSwipe(
                                            pos,
                                            direction
                                        )
                                    },

                                    modifier = Modifier
                                        .weight(1f)
                                        .scale(
                                            if (uiState.isStarting)
                                                0.8f
                                            else
                                                1f
                                        )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "💰 Wallet: ₹${uiState.walletBalance}",
                color = Color.Yellow,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Button(
                    onClick = {
                        viewModel.shuffleBoard()
                    },
                    modifier = Modifier.weight(1f),
                    enabled =
                        !uiState.isProcessing &&
                        !uiState.isStarting &&
                        !uiState.isLevelUp
                ) {
                    Text("🔀 SHUFFLE")
                }

                Button(
                    onClick = {
                        viewModel.requestRewardedAd()
                    },
                    modifier = Modifier.weight(1f),
                    enabled =
                        !uiState.isProcessing &&
                        !uiState.isLevelUp
                ) {
                    Text("🎁 +5")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    viewModel.resetGame()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔄 NEW GAME")
            }

            Spacer(modifier = Modifier.height(8.dp))

            AdBanner()
        }

        if (uiState.isStarting) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "🍓 GET READY! 🍬",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        if (uiState.isLevelUp) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "🎉 LEVEL UP! 🎉\n\nNEXT LEVEL ${uiState.level + 1}",
                    color = Color.Yellow,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (uiState.showSettings) {

            SettingsOverlay(
                isSoundEnabled = uiState.isSoundEnabled,
                isMusicEnabled = uiState.isMusicEnabled,
                isVibrationEnabled = uiState.isVibrationEnabled,

                onToggleSound = {
                    viewModel.toggleSound()
                },

                onToggleMusic = {
                    viewModel.toggleMusic()
                },

                onToggleVibration = {
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
private fun StatCard(
    label: String,
    value: String,
    color: Color
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = value,
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun SettingsOverlay(
    isSoundEnabled: Boolean,
    isMusicEnabled: Boolean,
    isVibrationEnabled: Boolean,
    onToggleSound: () -> Unit,
    onToggleMusic: () -> Unit,
    onToggleVibration: () -> Unit,
    onClose: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable {
                onClose()
            },
        contentAlignment = Alignment.Center
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(25.dp)
        ) {

            Column(
                modifier = Modifier.padding(25.dp)
            ) {

                Text(
                    text = "⚙️ SETTINGS",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(20.dp))

                SettingRow(
                    "🔊 Sound",
                    isSoundEnabled,
                    onToggleSound
                )

                SettingRow(
                    "🎵 Music",
                    isMusicEnabled,
                    onToggleMusic
                )

                SettingRow(
                    "📳 Vibration",
                    isVibrationEnabled,
                    onToggleVibration
                )

                Spacer(modifier = Modifier.height(15.dp))

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
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(title)

        Switch(
            checked = enabled,
            onCheckedChange = {
                onClick()
            }
        )
    }
}
