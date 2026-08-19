package com.fruitcandycrushcarzy.APP

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {}

        enableEdgeToEdge()

        setContent {

            FRUITCANDYCRUSHCARZYTheme {

                val context = androidx.compose.ui.platform.LocalContext.current
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
                            return GameViewModel(scoreRepository) as T
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
                 * MUSIC
                 *
                 * Music is controlled by the game setting.
                 * It also pauses when the app goes to background
                 * or the phone is locked.
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

                    lifecycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                LaunchedEffect(uiState.isMusicEnabled) {

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
                 * GAME EVENTS
                 */

                LaunchedEffect(Unit) {

                    viewModel.events.collect { event ->

                        when (event) {

                            GameEvent.MATCH -> {

                                if (viewModel.uiState.value.isSoundEnabled) {
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
                                    viewModel.uiState.value
                                        .isVibrationEnabled
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
                                    viewModel.uiState.value
                                        .isVibrationEnabled
                                ) {
                                    vibrationManager.vibrate(200)
                                }

                                /*
                                 * Interstitial test ad.
                                 * Keep this as test ad while developing.
                                 */
                                adManager.showInterstitial(
                                    this@MainActivity
                                ) {}
                            }

                            GameEvent.REQUEST_REWARDED_AD -> {

                                /*
                                 * Rewarded test ad.
                                 * After the complete ad the player
                                 * receives 5 extra moves.
                                 */
                                adManager.showRewarded(
                                    this@MainActivity
                                ) {
                                    viewModel.grantRewardMoves(5)
                                }
                            }

                            GameEvent.RATE_APP -> {

                                val intent = Intent(
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

                GameScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
