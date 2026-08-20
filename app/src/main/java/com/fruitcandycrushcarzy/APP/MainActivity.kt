package com.fruitcandycrushcarzy.APP

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    GAME
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {}

        enableEdgeToEdge()

        setContent {

            FRUITCANDYCRUSHCARZYTheme {

                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

                val scoreRepository = remember {
                    ScoreRepository(context)
                }

                val viewModel: GameViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {

                        override fun <T : ViewModel> create(
                            modelClass: Class<T>
                        ): T {

                            @Suppress("UNCHECKED_CAST")

                            return GameViewModel(
                                scoreRepository
                            ) as T
                        }
                    }
                )

                val uiState by viewModel.uiState.collectAsState()

                val soundManager = remember {
                    SoundManager(context)
                }

                val vibrationManager = remember {
                    VibrationManager(context)
                }

                val adManager = remember {
                    AdManager(context)
                }

                /*
                 * ============================================
                 * MUSIC
                 * ============================================
                 */

                val mediaPlayer = remember {

                    android.media.MediaPlayer.create(
                        context,
                        R.raw.xtremefreddy_loop1
                    )?.apply {
                        isLooping = true
                    }
                }

                DisposableEffect(
                    lifecycleOwner,
                    uiState.isMusicEnabled
                ) {

                    val observer =
                        LifecycleEventObserver { _, event ->

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

                    lifecycleOwner.lifecycle.addObserver(
                        observer
                    )

                    onDispose {

                        lifecycleOwner.lifecycle.removeObserver(
                            observer
                        )
                    }
                }

                LaunchedEffect(
                    uiState.isMusicEnabled
                ) {

                    if (uiState.isMusicEnabled) {

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
                 * ============================================
                 * APP OPEN AD
                 * ============================================
                 */

                LaunchedEffect(Unit) {

                    delay(4000)

                    adManager.showAppOpenAd(
                        this@MainActivity
                    ) {}
                }

                /*
                 * ============================================
                 * GAME EVENTS
                 * ============================================
                 */

                LaunchedEffect(Unit) {

                    viewModel.events.collect { event ->

                        when (event) {

                            GameEvent.MATCH -> {

                                if (
                                    viewModel.uiState.value
                                        .isSoundEnabled
                                ) {
                                    soundManager.playMatch()
                                }

                                if (
                                    viewModel.uiState.value
                                        .isVibrationEnabled
                                ) {
                                    vibrationManager.vibrate(50)
                                }
                            }

                            GameEvent.SWAP -> {

                                if (
                                    viewModel.uiState.value
                                        .isSoundEnabled
                                ) {
                                    soundManager.playSwap()
                                }
                            }

                            GameEvent.SPECIAL_EXPLOSION -> {

                                if (
                                    viewModel.uiState.value
                                        .isSoundEnabled
                                ) {
                                    soundManager.playExplosion()
                                }

                                if (
                                    viewModel.uiState.value
                                        .isVibrationEnabled
                                ) {
                                    vibrationManager.vibrate(100)
                                }
                            }

                            GameEvent.LEVEL_UP -> {

                                if (
                                    viewModel.uiState.value
                                        .isSoundEnabled
                                ) {
                                    soundManager.playLevelUp()
                                }

                                if (
                                    viewModel.uiState.value
                                        .isVibrationEnabled
                                ) {
                                    vibrationManager.vibrate(200)
                                }

                                /*
                                 * Har 2 completed levels ke baad
                                 * interstitial lagane ke liye
                                 * baad mein counter connect karenge.
                                 */
                            }

                            GameEvent.REQUEST_REWARDED_AD -> {

                                adManager.showRewarded(
                                    this@MainActivity
                                ) {

                                    viewModel.grantRewardMoves(
                                        10
                                    )
                                }
                            }

                            GameEvent.RATE_APP -> {

                                val intent =
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(
                                            "market://details?id=$packageName"
                                        )
                                    )

                                try {

                                    startActivity(intent)

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
                 * ============================================
                 * PAGE NAVIGATION
                 * ============================================
                 */

                var currentPage by remember {
                    mutableStateOf(AppPage.MENU)
                }

                /*
                 * Highest unlocked level.
                 *
                 * Abhi Level 1 unlocked.
                 * Next step mein GameViewModel se
                 * automatically unlock karenge.
                 */

                val preferences =
                    remember {

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

                when (currentPage) {

                    /*
                     * ========================================
                     * MAIN MENU
                     * ========================================
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

                                viewModel.toggleSettings()
                            },

                            onWithdrawal = {

                                // Withdrawal screen
                                // next step mein connect karenge.
                            },

                            onTask = {

                                // Task screen
                                // next step mein connect karenge.
                            }
                        )
                    }

                    /*
                     * ========================================
                     * LEVEL MAP
                     * ========================================
                     */

                    AppPage.LEVELS -> {

                        LevelMapScreen(

                            highestLevel =
                                highestLevel,

                            onBack = {

                                currentPage =
                                    AppPage.MENU
                            },

                            onLevelSelected = { level ->

                                if (
                                    level <= highestLevel
                                ) {

                                    currentPage =
                                        AppPage.GAME

                                    /*
                                     * GameViewModel mein
                                     * selected level connect
                                     * next step mein karenge.
                                     */
                                }
                            }
                        )
                    }

                    /*
                     * ========================================
                     * GAME
                     * ========================================
                     */

                    AppPage.GAME -> {

                        Box(
                            modifier =
                                Modifier.fillMaxSize()
                        ) {

                            GameScreen(
                                viewModel = viewModel
                            )

                            /*
                             * Back button
                             */

                            Button(
                                onClick = {

                                    currentPage =
                                        AppPage.LEVELS
                                },

                                modifier =
                                    Modifier
                                        .align(
                                            Alignment.TopStart
                                        )
                                        .padding(8.dp)
                            ) {

                                Text("← Levels")
                            }
                        }
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
                text = "🍓",
                fontSize = 60.sp
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
                        text = "💰 Wallet",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )

                    Text(
                        text = "₹$walletBalance",
                        color = Color(0xFFFFD54F),
                        fontSize = 28.sp,
                        fontWeight =
                            FontWeight.Bold
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
                    fontWeight =
                        FontWeight.Bold
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

                MenuButton(
                    text = "📋 TASK",
                    modifier =
                        Modifier.weight(1f),
                    onClick = onTask
                )

                MenuButton(
                    text = "💸 WITHDRAW",
                    modifier =
                        Modifier.weight(1f),
                    onClick = onWithdrawal
                )
            }

            Spacer(
                modifier =
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
                    text = "⚙️ SETTINGS",
                    fontSize = 16.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            Text(
                text = "Complete levels and earn rewards",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

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
            text = text,
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

    onLevelSelected: (Int) -> Unit

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
                modifier =
                    Modifier.width(12.dp)
            )

            Text(
                text = "🍬 LEVELS",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }

        HorizontalDivider()

        Text(
            text =
                "Choose a level",

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

            color =
                Color(0xFFFFD54F),

            fontSize = 18.sp,

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
                (1..50).toList()
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

                fontSize = 20.sp
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
