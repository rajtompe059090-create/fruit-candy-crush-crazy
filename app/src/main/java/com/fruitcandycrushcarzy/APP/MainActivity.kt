package com.fruitcandycrushcarzy.APP

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fruitcandycrushcarzy.APP.game.data.ScoreRepository
import com.fruitcandycrushcarzy.APP.game.util.AdManager
import com.fruitcandycrushcarzy.APP.game.util.SoundManager
import com.fruitcandycrushcarzy.APP.game.util.VibrationManager
import com.fruitcandycrushcarzy.APP.game.viewmodel.GameEvent
import com.fruitcandycrushcarzy.APP.game.viewmodel.GameViewModel
import com.fruitcandycrushcarzy.APP.ui.GameScreen
import com.fruitcandycrushcarzy.APP.ui.theme.FRUITCANDYCRUSHCARZYTheme
import com.google.android.gms.ads.MobileAds

private enum class AppPage {
    MENU,
    LEVELS,
    GAME,
    WITHDRAW,
    TASK
}

/*
 * Telegram withdrawal account
 */
private const val TELEGRAM_USERNAME = "Earning_adda0590"

/*
 * Minimum withdrawal amount
 */
private const val MIN_WITHDRAWAL = 100

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {}

        enableEdgeToEdge()

        setContent {

            FRUITCANDYCRUSHCARZYTheme {

                val context = LocalContext.current

                val repository = remember {
                    ScoreRepository(context)
                }

                val viewModel: GameViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {

                        override fun <T : ViewModel> create(
                            modelClass: Class<T>
                        ): T {

                            @Suppress("UNCHECKED_CAST")

                            return GameViewModel(repository) as T
                        }
                    }
                )

                val uiState by viewModel.uiState.collectAsState()

                val adManager = remember {
                    AdManager(context)
                }

                val soundManager = remember {
                    SoundManager(context)
                }

                val vibrationManager = remember {
                    VibrationManager(context)
                }

                var currentPage by remember {
                    mutableStateOf(AppPage.MENU)
                }

                /*
                 * ========================================================
                 * LEVEL PROGRESS
                 * ========================================================
                 */

                val preferences = remember {

                    context.getSharedPreferences(
                        "game_progress",
                        MODE_PRIVATE
                    )
                }

                var highestLevel by remember {

                    mutableIntStateOf(
                        preferences.getInt(
                            "highest_level",
                            1
                        )
                    )
                }

                /*
                 * ========================================================
                 * GAME EVENTS
                 * ========================================================
                 */

                LaunchedEffect(Unit) {

                    viewModel.events.collect { event ->

                        when (event) {

                            GameEvent.MATCH -> {

                                if (
                                    viewModel.uiState.value.isSoundEnabled
                                ) {
                                    soundManager.playMatch()
                                }

                                if (
                                    viewModel.uiState.value.isVibrationEnabled
                                ) {
                                    vibrationManager.vibrate(40)
                                }
                            }

                            GameEvent.SWAP -> {

                                if (
                                    viewModel.uiState.value.isSoundEnabled
                                ) {
                                    soundManager.playSwap()
                                }
                            }

                            GameEvent.SPECIAL_EXPLOSION -> {

                                if (
                                    viewModel.uiState.value.isSoundEnabled
                                ) {
                                    soundManager.playExplosion()
                                }

                                if (
                                    viewModel.uiState.value.isVibrationEnabled
                                ) {
                                    vibrationManager.vibrate(100)
                                }
                            }

                            GameEvent.LEVEL_UP -> {

                                if (
                                    viewModel.uiState.value.isSoundEnabled
                                ) {
                                    soundManager.playLevelUp()
                                }

                                if (
                                    viewModel.uiState.value.isVibrationEnabled
                                ) {
                                    vibrationManager.vibrate(200)
                                }

                                val nextLevel =
                                    (viewModel.uiState.value.level + 1)
                                        .coerceAtMost(1000)

                                if (nextLevel > highestLevel) {

                                    highestLevel = nextLevel

                                    preferences.edit()
                                        .putInt(
                                            "highest_level",
                                            highestLevel
                                        )
                                        .apply()
                                }
                            }

                            GameEvent.REQUEST_REWARDED_AD -> {

                                adManager.showRewarded(
                                    this@MainActivity
                                ) {

                                    viewModel.grantRewardMoves(10)
                                }
                            }

                            GameEvent.RATE_APP -> {

                                val marketIntent =
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(
                                            "market://details?id=$packageName"
                                        )
                                    )

                                try {

                                    startActivity(marketIntent)

                                } catch (_: Exception) {

                                    startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(
                                                "https://play.google.com/store/apps/details?id=$packageName"
                                            )
                                        )
                                    )
                                }
                            }

                            GameEvent.GAME_OVER -> Unit
                        }
                    }
                }

                /*
                 * ========================================================
                 * PAGE NAVIGATION
                 * ========================================================
                 */

                when (currentPage) {

                    AppPage.MENU -> {

                        MainMenuScreen(

                            walletBalance =
                                uiState.walletBalance,

                            onPlay = {

                                currentPage =
                                    AppPage.LEVELS
                            },

                            onSettings = {

                                viewModel.toggleSettings()
                            },

                            onWithdrawal = {

                                currentPage =
                                    AppPage.WITHDRAW
                            },

                            onTask = {

                                currentPage =
                                    AppPage.TASK
                            }
                        )
                    }

                    AppPage.LEVELS -> {

                        LevelMapScreen(

                            highestLevel =
                                highestLevel,

                            onBack = {

                                currentPage =
                                    AppPage.MENU
                            },

                            onLevelSelected = { level ->

                                if (level <= highestLevel) {

                                    currentPage =
                                        AppPage.GAME

                                    viewModel.resetGame()
                                }
                            }
                        )
                    }

                    AppPage.GAME -> {

                        Box(
                            modifier =
                                Modifier.fillMaxSize()
                        ) {

                            GameScreen(
                                viewModel = viewModel
                            )

                            Button(

                                onClick = {

                                    currentPage =
                                        AppPage.LEVELS
                                },

                                modifier =
                                    Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)

                            ) {

                                Text("← Levels")
                            }
                        }
                    }

                    AppPage.WITHDRAW -> {

                        WithdrawalScreen(

                            balance =
                                uiState.walletBalance,

                            onBack = {

                                currentPage =
                                    AppPage.MENU
                            }
                        )
                    }

                    AppPage.TASK -> {

                        TaskScreen(

                            onBack = {

                                currentPage =
                                    AppPage.MENU
                            }
                        )
                    }
                }
            }
        }
    }
}

/*
 * ============================================================
 * MAIN MENU
 * ============================================================
 */

@Composable
private fun MainMenuScreen(
    walletBalance: Int,
    onPlay: () -> Unit,
    onSettings: () -> Unit,
    onWithdrawal: () -> Unit,
    onTask: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFF101820))
                .padding(22.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier =
                Modifier.height(40.dp)
        )

        Text(
            text = "🍓",
            fontSize = 64.sp
        )

        Text(
            text = "FRUIT CRUSH",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "🍬 Match • Crush • Win 🍬",
            color = Color(0xFFFFD54F),
            fontSize = 15.sp
        )

        Spacer(
            modifier =
                Modifier.height(25.dp)
        )

        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(20.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFF263238)
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(18.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text = "💰 WALLET",
                    color = Color.LightGray
                )

                Text(
                    text = "₹$walletBalance",
                    color = Color(0xFFFFD54F),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Button(
            onClick = onPlay,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(58.dp),

            shape =
                RoundedCornerShape(16.dp)
        ) {

            Text(
                text = "🎮 PLAY GAME",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            Button(
                onClick = onTask,

                modifier =
                    Modifier.weight(1f)
            ) {

                Text("📋 TASK")
            }

            Button(
                onClick = onWithdrawal,

                modifier =
                    Modifier.weight(1f)
            ) {

                Text("💸 WITHDRAW")
            }
        }

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        Button(
            onClick = onSettings,

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text("⚙️ SETTINGS")
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Text(
            text =
                "Complete levels and earn rewards",

            color =
                Color.Gray,

            fontSize =
                12.sp
        )
    }
}

/*
 * ============================================================
 * LEVEL MAP
 * ============================================================
 */

@Composable
private fun LevelMapScreen(
    highestLevel: Int,
    onBack: () -> Unit,
    onLevelSelected: (Int) -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFF101820))
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Button(
                onClick = onBack
            ) {

                Text("←")
            }

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            Text(
                text = "🍬 LEVELS",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text =
                "Unlocked: $highestLevel / 1000",

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),

            color =
                Color(0xFFFFD54F),

            textAlign =
                TextAlign.Center
        )

        LazyVerticalGrid(
            columns =
                GridCells.Fixed(4),

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp),

            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            items(
                (1..1000).toList()
            ) { level ->

                val unlocked =
                    level <= highestLevel

                LevelButton(
                    level = level,
                    unlocked = unlocked,

                    onClick = {

                        if (unlocked) {
                            onLevelSelected(level)
                        }
                    }
                )
            }
        }
    }
}

/*
 * ============================================================
 * LEVEL BUTTON
 * ============================================================
 */

@Composable
private fun LevelButton(
    level: Int,
    unlocked: Boolean,
    onClick: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .size(70.dp)
                .background(
                    if (unlocked)
                        Color(0xFFFFB300)
                    else
                        Color(0xFF37474F),

                    CircleShape
                )
                .clickable(
                    enabled = unlocked,
                    onClick = onClick
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    if (unlocked)
                        "🍬"
                    else
                        "🔒",

                fontSize =
                    20.sp
            )

            Text(
                text =
                    level.toString(),

                color =
                    if (unlocked)
                        Color.Black
                    else
                        Color.LightGray,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

/*
 * ============================================================
 * WITHDRAWAL SCREEN
 * ============================================================
 */

@Composable
private fun WithdrawalScreen(
    balance: Int,
    onBack: () -> Unit
) {

    val context = LocalContext.current

    var errorMessage by remember {
        mutableStateOf("")
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFF101820))
                .padding(20.dp)
    ) {

        Button(
            onClick = onBack
        ) {

            Text("← Back")
        }

        Spacer(
            modifier =
                Modifier.height(30.dp)
        )

        Text(
            text = "💸 WITHDRAW",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(20.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFF263238)
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(20.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text = "WALLET BALANCE",
                    color = Color.Gray,
                    fontSize = 13.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )

                Text(
                    text = "₹$balance",
                    color = Color(0xFFFFD54F),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Text(
            text =
                "Minimum withdrawal: ₹$MIN_WITHDRAWAL",

            color =
                Color.White,

            fontSize =
                15.sp,

            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(15.dp)
        )

        if (balance < MIN_WITHDRAWAL) {

            Text(
                text =
                    "❌ Minimum ₹100 balance required for withdrawal.",

                color =
                    Color.Red,

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight.Bold,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),

                textAlign =
                    TextAlign.Center
            )
        }

        if (errorMessage.isNotEmpty()) {

            Text(
                text =
                    errorMessage,

                color =
                    Color.Red,

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight.Bold,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),

                textAlign =
                    TextAlign.Center
            )
        }

        Button(
            onClick = {

                if (balance < MIN_WITHDRAWAL) {

                    errorMessage =
                        "❌ Withdrawal failed: Minimum ₹100 required."

                } else {

                    errorMessage = ""

                    val telegramUrl =
                        "https://t.me/$TELEGRAM_USERNAME"

                    try {

                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(telegramUrl)
                            )
                        )

                    } catch (_: Exception) {

                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    "https://telegram.me/$TELEGRAM_USERNAME"
                                )
                            )
                        )
                    }
                }
            },

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(58.dp),

            shape =
                RoundedCornerShape(16.dp),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        Color(0xFF2196F3)
                )
        ) {

            Text(
                text =
                    "💬 CONTACT ON TELEGRAM",

                fontWeight =
                    FontWeight.Bold,

                fontSize =
                    16.sp
            )
        }

        Spacer(
            modifier =
                Modifier.height(15.dp)
        )

        Text(
            text =
                "Telegram: @$TELEGRAM_USERNAME",

            color =
                Color.LightGray,

            fontSize =
                13.sp,

            modifier =
                Modifier.fillMaxWidth(),

            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                "Withdrawal requests are manually verified.",

            color =
                Color.Gray,

            fontSize =
                12.sp,

            textAlign =
                TextAlign.Center,

            modifier =
                Modifier.fillMaxWidth()
        )
    }
}

/*
 * ============================================================
 * TASK SCREEN
 * ============================================================
 */

@Composable
private fun TaskScreen(
    onBack: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFF101820))
                .padding(20.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Button(
                onClick = onBack
            ) {

                Text("← Back")
            }

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            Text(
                text = "📋 TASK",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier =
                Modifier.height(40.dp)
        )

        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(24.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFF263238)
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(24.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text = "🎯 COMPLETE TASKS",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(15.dp)
                )

                Text(
                    text =
                        "Complete available tasks and earn rewards.",

                    color =
                        Color.LightGray,

                    fontSize =
                        15.sp,

                    textAlign =
                        TextAlign.Center
                )

                Spacer(
                    modifier =
                        Modifier.height(25.dp)
                )

                Button(
                    onClick = {
                        // Task system can be connected later.
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(55.dp),

                    shape =
                        RoundedCornerShape(16.dp)
                ) {

                    Text(
                        text =
                            "VIEW AVAILABLE TASKS",

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Text(
            text =
                "More tasks coming soon",

            color =
                Color.Gray,

            fontSize =
                13.sp
        )
    }
}
