package com.fruitcandycrushcarzy.APP.game.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdManager(private val context: Context) {

    companion object {
        private const val TAG = "ADMOB"

        // APP OPEN
        private const val APP_OPEN_AD_UNIT_ID =
            "ca-app-pub-6146868530948467/3515500297"

        // INTERSTITIAL
        private const val INTERSTITIAL_AD_UNIT_ID =
            "ca-app-pub-6146868530948467/2406181764"

        // REWARDED
        private const val REWARDED_AD_UNIT_ID =
            "ca-app-pub-6146868530948467/9108834256"
    }

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAppOpen = false
    private var isShowingAppOpen = false

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    init {
        loadAppOpenAd()
        loadInterstitial()
        loadRewarded()
    }

    // =========================================================
    // APP OPEN AD
    // =========================================================

    private fun loadAppOpenAd() {

        if (isLoadingAppOpen || appOpenAd != null) {
            return
        }

        isLoadingAppOpen = true

        Log.d(TAG, "Loading App Open Ad...")

        AppOpenAd.load(
            context,
            APP_OPEN_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {

                override fun onAdLoaded(ad: AppOpenAd) {

                    isLoadingAppOpen = false
                    appOpenAd = ad

                    Log.d(TAG, "APP OPEN LOADED")

                    ad.fullScreenContentCallback =
                        object : FullScreenContentCallback() {

                            override fun onAdShowedFullScreenContent() {
                                Log.d(TAG, "APP OPEN SHOWN")
                            }

                            override fun onAdDismissedFullScreenContent() {

                                Log.d(TAG, "APP OPEN CLOSED")

                                appOpenAd = null
                                isShowingAppOpen = false

                                loadAppOpenAd()
                            }

                            override fun onAdFailedToShowFullScreenContent(
                                adError: AdError
                            ) {

                                Log.e(
                                    TAG,
                                    "APP OPEN SHOW FAILED: ${adError.message}"
                                )

                                appOpenAd = null
                                isShowingAppOpen = false

                                loadAppOpenAd()
                            }
                        }
                }

                override fun onAdFailedToLoad(
                    error: LoadAdError
                ) {

                    isLoadingAppOpen = false
                    appOpenAd = null

                    Log.e(
                        TAG,
                        "APP OPEN LOAD FAILED: ${error.code} ${error.message}"
                    )
                }
            }
        )
    }

    fun showAppOpenAd(
        activity: Activity,
        onFinished: () -> Unit = {}
    ) {

        if (isShowingAppOpen) {
            return
        }

        val ad = appOpenAd

        if (ad == null) {

            Log.d(TAG, "APP OPEN NOT READY - loading again")

            loadAppOpenAd()

            onFinished()
            return
        }

        isShowingAppOpen = true
        appOpenAd = null

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "APP OPEN SHOWN")
                }

                override fun onAdDismissedFullScreenContent() {

                    Log.d(TAG, "APP OPEN CLOSED")

                    isShowingAppOpen = false

                    loadAppOpenAd()

                    onFinished()
                }

                override fun onAdFailedToShowFullScreenContent(
                    adError: AdError
                ) {

                    Log.e(
                        TAG,
                        "APP OPEN SHOW FAILED: ${adError.message}"
                    )

                    isShowingAppOpen = false

                    loadAppOpenAd()

                    onFinished()
                }
            }

        ad.show(activity)
    }

    // =========================================================
    // INTERSTITIAL AD
    // =========================================================

    private fun loadInterstitial() {

        if (interstitialAd != null) {
            return
        }

        Log.d(TAG, "Loading Interstitial...")

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(
                    ad: InterstitialAd
                ) {

                    interstitialAd = ad

                    Log.d(TAG, "INTERSTITIAL LOADED")
                }

                override fun onAdFailedToLoad(
                    error: LoadAdError
                ) {

                    interstitialAd = null

                    Log.e(
                        TAG,
                        "INTERSTITIAL FAILED: ${error.code} ${error.message}"
                    )
                }
            }
        )
    }

    fun showInterstitial(
        activity: Activity,
        onFinished: () -> Unit = {}
    ) {

        val ad = interstitialAd

        if (ad == null) {

            Log.d(TAG, "INTERSTITIAL NOT READY")

            loadInterstitial()

            onFinished()
            return
        }

        interstitialAd = null

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "INTERSTITIAL SHOWN")
                }

                override fun onAdDismissedFullScreenContent() {

                    Log.d(TAG, "INTERSTITIAL CLOSED")

                    loadInterstitial()

                    onFinished()
                }

                override fun onAdFailedToShowFullScreenContent(
                    adError: AdError
                ) {

                    Log.e(
                        TAG,
                        "INTERSTITIAL SHOW FAILED: ${adError.message}"
                    )

                    loadInterstitial()

                    onFinished()
                }
            }

        ad.show(activity)
    }

    // =========================================================
    // REWARDED AD
    // =========================================================

    private fun loadRewarded() {

        if (rewardedAd != null) {
            return
        }

        Log.d(TAG, "Loading Rewarded...")

        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {

                override fun onAdLoaded(
                    ad: RewardedAd
                ) {

                    rewardedAd = ad

                    Log.d(TAG, "REWARDED LOADED")
                }

                override fun onAdFailedToLoad(
                    error: LoadAdError
                ) {

                    rewardedAd = null

                    Log.e(
                        TAG,
                        "REWARDED FAILED: ${error.code} ${error.message}"
                    )
                }
            }
        )
    }

    fun showRewarded(
        activity: Activity,
        onReward: () -> Unit
    ) {

        val ad = rewardedAd

        if (ad == null) {

            Log.d(TAG, "REWARDED NOT READY")

            loadRewarded()

            return
        }

        rewardedAd = null

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "REWARDED SHOWN")
                }

                override fun onAdDismissedFullScreenContent() {

                    Log.d(TAG, "REWARDED CLOSED")

                    loadRewarded()
                }

                override fun onAdFailedToShowFullScreenContent(
                    adError: AdError
                ) {

                    Log.e(
                        TAG,
                        "REWARDED SHOW FAILED: ${adError.message}"
                    )

                    loadRewarded()
                }
            }

        ad.show(activity) {

            Log.d(TAG, "REWARDED SUCCESS")

            onReward()
        }
    }
}
