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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import kotlinx.coroutines.delay

private enum class AppPage {
    MENU,
    LEVELS,
    GAME,
    WITHDRAW,
    TASK
}

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {}

        enableEdgeToEdge()

        setContent {

            FRUITCANDYCRUSHCARZYTheme {

                val context =
                    LocalContext.current

                val lifecycleOwner =
                    LocalLifecycleOwner.current

                val scoreRepository =
                    remember {
                        ScoreRepository(context)
                    }

                val viewModel:
                    GameViewModel =
                    viewModel(
                        factory =
                            object :
                                ViewModelProvider.Factory {

                                override fun <T : ViewModel>
                                    create(
                                        modelClass:
                                        Class<T>
                                    ): T {

                                    @Suppress(
                                        "UNCHECKED_CAST"
                                    )

                                    return GameViewModel(
                                        scoreRepository
                                    ) as T
                                }
                            }
                    )

                val uiState by
                    viewModel.uiState.collectAsState()

                val soundManager =
                    remember {
                        SoundManager(context)
                    }

                val vibrationManager =
                    remember {
                        VibrationManager(context)
                    }

                val adManager =
                    remember {
                        AdManager(context)
                    }

                var currentPage by
                    remember {
                        mutableStateOf(
                            AppPage.MENU
                        )
                    }

                /*
                 * =========================================
                 * HIGHEST LEVEL
                 * =========================================
                 */

                val highestLevel by
                    scoreRepository
                        .highestLevelFlow
                        .collectAsState(
                            initial = 1
                        )

                /*
                 * =========================================
                 * MUSIC
                 * =========================================
                 */

                val mediaPlayer =
                    remember {

                        android.media.MediaPlayer
                            .create(
                                context,
                                R.raw.xtremefreddy_loop1
                            )
                            ?.apply {
                                isLooping = true
                            }
                    }

                DisposableEffect(
                    lifecycleOwner,
                    uiState.isMusicEnabled
                ) {

                    val observer =
                        LifecycleEventObserver {
                                _,
                                event ->

                            when (event) {

                                Lifecycle.Event.ON_RESUME -> {

                                    if (
                                        uiState.isMusicEnabled &&
                                        mediaPlayer != null &&
                                        !mediaPlayer.isPlaying
                                    ) {
                                        mediaPlayer.start()
                                    }
                                }

                                Lifecycle.Event.ON_PAUSE -> {

                                    if (
                                        mediaPlayer != null &&
                                        mediaPlayer.isPlaying
                                    ) {
                                        mediaPlayer.pause()
                                    }
                                }

                                else -> Unit
                            }
                        }

                    lifecycleOwner.lifecycle
                        .addObserver(observer)

                    onDispose {

                        lifecycleOwner.lifecycle
                            .removeObserver(
                                observer
                            )
                    }
                }

                /*
                 * =========================================
                 * MUSIC TOGGLE
                 * =========================================
                 */

                LaunchedEffect(
                    uiState.isMusicEnabled
                ) {

                    if (
                        uiState.isMusicEnabled
                    ) {

                        if (
                            mediaPlayer != null &&
                            !mediaPlayer.isPlaying
                        ) {
                            mediaPlayer.start()
                        }

                    } else {

                        if (
                            mediaPlayer != null &&
                            mediaPlayer.isPlaying
                        ) {
                            mediaPlayer.pause()
                        }
                    }
                }

                /*
                 * =========================================
                 * CLEANUP
                 * =========================================
                 */

                DisposableEffect(Unit) {

                    onDispose {

                        try {
                            mediaPlayer?.stop()
                        } catch (_: Exception) {
                        }

                        mediaPlayer?.release()

                        soundManager.release()
                    }
                }

                /*
                 * =========================================
                 * APP OPEN AD
                 * =========================================
                 */

                LaunchedEffect(Unit) {

                    delay(3500)

                    adManager.showAppOpenAd(
                        this@MainActivity
                    ) {}
                }

                /*
                 * =========================================
                 * GAME EVENTS
                 * =========================================
                 */

                LaunchedEffect(Unit) {

                    viewModel.events.collect {
                        event ->

                        when (event) {

                            GameEvent.MATCH -> {

                                if (
                                    viewModel
                                        .uiState
                                        .value
                                        .isSoundEnabled
                                ) {
                                    soundManager
                                        .playMatch()
                                }

                                if (
                                    viewModel
                                        .uiState
                                        .value
                                        .isVibrationEnabled
                                ) {
                                    vibrationManager
                                        .vibrate(50)
                                }
                            }

                            GameEvent.SWAP -> {

                                if (
                                    viewModel
                                        .uiState
                                        .value
                                        .isSoundEnabled
                                ) {
                                    soundManager
                                        .playSwap()
                                }
                            }

                            GameEvent.SPECIAL_EXPLOSION -> {

                                if (
                                    viewModel
                                        .uiState
                                        .value
                                        .isSoundEnabled
                                ) {
                                    soundManager
                                        .playExplosion()
                                }

                                if (
                                    viewModel
                                        .uiState
                                        .value
                                        .isVibrationEnabled
                                ) {
                                    vibrationManager
                                        .vibrate(100)
                                }
                            }

                            GameEvent.LEVEL_UP -> {

                                if (
                                    viewModel
                                        .uiState
                                        .value
                                        .isSoundEnabled
                                ) {
                                    soundManager
                                        .playLevelUp()
                                }

                                if (
                                    viewModel
                                        .uiState
                                        .value
                                        .isVibrationEnabled
                                ) {
                                    vibrationManager
                                        .vibrate(200)
                                }
                            }

                            GameEvent.REQUEST_REWARDED_AD -> {

                                adManager.showRewarded(
                                    this@MainActivity
                                ) {

                                    viewModel
                                        .grantRewardMoves(10)
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

                                    startActivity(
                                        marketIntent
                                    )

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
                 * =========================================
                 * PAGES
                 * =========================================
                 */

                when (currentPage) {

                    /*
                     * =====================================
                     * MENU
                     * =====================================
                     */

                    AppPage.MENU -> {

                        MainMenuScreen(

                            walletBalance =
                                uiState.walletBalance,

                            onPlay = {

                                currentPage =
                                    AppPage.LEVELS
                            },

                            onSettings = {

                                viewModel
                                    .toggleSettings()
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

                    /*
                     * =====================================
                     * LEVELS
                     * =====================================
                     */

                    AppPage.LEVELS -> {

                        LevelMapScreen(

                            highestLevel =
                                highestLevel,

                            onBack = {

                                currentPage =
                                    AppPage.MENU
                            },

                            onLevelSelected = {
                                level ->

                                if (
                                    level <=
                                    highestLevel
                                ) {

                                    currentPage =
                                        AppPage.GAME
                                }
                            }
                        )
                    }

                    /*
                     * =====================================
                     * GAME
                     * =====================================
                     */

                    AppPage.GAME -> {

                        Box(
                            modifier =
                                Modifier.fillMaxSize()
                        ) {

                            GameScreen(
                                viewModel =
                                    viewModel
                            )

                            Button(

                                onClick = {

                                    currentPage =
                                        AppPage.LEVELS
                                },

                                modifier =
                                    Modifier
                                        .align(
                                            Alignment
                                                .TopStart
                                        )
                                        .padding(8.dp)
                            ) {

                                Text(
                                    "← Levels"
                                )
                            }
                        }
                    }

                    /*
                     * =====================================
                     * WITHDRAW
                     * =====================================
                     */

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

                    /*
                     * =====================================
                     * TASK
                     * =====================================
                     */

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

    Box(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(0xFF101820)
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(22.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                "🍓",
                fontSize = 62.sp
            )

            Text(
                "FRUIT CRUSH",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                "🍬 Match • Crush • Win 🍬",
                color =
                    Color(0xFFFFD54F),
                fontSize = 15.sp
            )

            Spacer(
                Modifier.height(25.dp)
            )

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFF263238)
                    ),

                shape =
                    RoundedCornerShape(18.dp)
            ) {

                Column(

                    modifier =
                        Modifier.padding(18.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        "💰 Wallet",
                        color =
                            Color.LightGray
                    )

                    Text(
                        "₹$walletBalance",
                        color =
                            Color(0xFFFFD54F),
                        fontSize = 28.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Spacer(
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
                    "🎮 PLAY GAME",
                    fontSize = 20.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                Modifier.height(10.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                MenuButton(
                    "📋 TASK",
                    Modifier.weight(1f),
                    onTask
                )

                MenuButton(
                    "💸 WITHDRAW",
                    Modifier.weight(1f),
                    onWithdrawal
                )
            }

            Spacer(
                Modifier.height(10.dp)
            )

            Button(

                onClick = onSettings,

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(14.dp)
            ) {

                Text(
                    "⚙️ SETTINGS",
                    fontSize = 16.sp
                )
            }

            Spacer(
                Modifier.height(18.dp)
            )

            Text(
                "Complete levels and earn rewards",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}


/*
 * ============================================================
 * MENU BUTTON
 * ============================================================
 */

@Composable
private fun MenuButton(

    text: String,

    modifier: Modifier,

    onClick: () -> Unit

) {

    Button(

        onClick = onClick,

        modifier = modifier,

        shape =
            RoundedCornerShape(14.dp)
    ) {

        Text(
            text,
            fontSize = 13.sp
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

    onLevelSelected:
        (Int) -> Unit

) {

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(0xFF101820)
                )
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
                Modifier.width(12.dp)
            )

            Text(
                "🍬 LEVELS",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }

        HorizontalDivider()

        Text(
            "Unlocked: $highestLevel / 1000",

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),

            color =
                Color(0xFFFFD54F),

            fontSize = 17.sp,

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

                    unlocked =
                        unlocked,

                    onClick = {

                        if (unlocked) {

                            onLevelSelected(
                                level
                            )
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
                if (unlocked)
                    "🍬"
                else
                    "🔒",

                fontSize = 20.sp
            )

            Text(
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

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(0xFF101820)
                )
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
                Text("←")
            }

            Spacer(
                Modifier.width(12.dp)
            )

            Text(
                "💸 WITHDRAW",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            Modifier.height(40.dp)
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
                    Modifier.padding(24.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    "Available Balance",
                    color =
                        Color.LightGray
                )

                Text(
                    "₹$balance",
                    color =
                        Color(0xFFFFD54F),

                    fontSize = 34.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(20.dp)
                )

                Text(
                    "UPI withdrawal will be available here.",
                    color =
                        Color.Gray,

                    textAlign =
                        TextAlign.Center
                )

                Spacer(
                    Modifier.height(20.dp)
                )

                Button(
                    onClick = {

                        // Withdrawal backend
                        // next step mein connect hoga.

                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "REQUEST WITHDRAWAL"
                    )
                }
            }
        }
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
                .background(
                    Color(0xFF101820)
                )
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
                Text("←")
            }

            Spacer(
                Modifier.width(12.dp)
            )

            Text(
                "📋 TASKS",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            Modifier.height(30.dp)
        )

        Card(

            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(18.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFF263238)
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(22.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    "🎁 Daily Task",
                    color =
                        Color(0xFFFFD54F),

                    fontSize = 22.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(12.dp)
                )

                Text(
                    "Complete game levels to earn rewards.",
                    color =
                        Color.LightGray,

                    textAlign =
                        TextAlign.Center
                )

                Spacer(
                    Modifier.height(20.dp)
                )

                Button(
                    onClick = {

                        // Task action
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "START TASK"
                    )
                }
            }
        }
    }
}
