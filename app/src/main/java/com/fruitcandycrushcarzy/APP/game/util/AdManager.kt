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

        private const val APP_OPEN_AD_UNIT_ID =
            "ca-app-pub-6146868530948467/3515500297"

        private const val INTERSTITIAL_AD_UNIT_ID =
            "ca-app-pub-6146868530948467/2406181764"

        private const val REWARDED_AD_UNIT_ID =
            "ca-app-pub-6146868530948467/9108834256"
    }

    // =========================================================
    // APP OPEN
    // =========================================================

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAppOpen = false
    private var isShowingAppOpen = false

    // =========================================================
    // INTERSTITIAL
    // =========================================================

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private var isShowingInterstitial = false

    // =========================================================
    // REWARDED
    // =========================================================

    private var rewardedAd: RewardedAd? = null
    private var isLoadingRewarded = false
    private var isShowingRewarded = false

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

        Log.d(TAG, "APP OPEN: Loading...")

        AppOpenAd.load(
            context,
            APP_OPEN_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {

                override fun onAdLoaded(ad: AppOpenAd) {

                    isLoadingAppOpen = false
                    appOpenAd = ad

                    Log.d(TAG, "APP OPEN: Loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {

                    isLoadingAppOpen = false
                    appOpenAd = null

                    Log.e(
                        TAG,
                        "APP OPEN: Load failed ${error.code} ${error.message}"
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
            onFinished()
            return
        }

        val ad = appOpenAd

        if (ad == null) {

            Log.d(TAG, "APP OPEN: Not ready")

            loadAppOpenAd()

            onFinished()
            return
        }

        appOpenAd = null
        isShowingAppOpen = true

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "APP OPEN: Shown")
                }

                override fun onAdDismissedFullScreenContent() {

                    Log.d(TAG, "APP OPEN: Closed")

                    isShowingAppOpen = false

                    loadAppOpenAd()

                    onFinished()
                }

                override fun onAdFailedToShowFullScreenContent(
                    adError: AdError
                ) {

                    Log.e(
                        TAG,
                        "APP OPEN: Show failed ${adError.message}"
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

        if (
            isLoadingInterstitial ||
            interstitialAd != null
        ) {
            return
        }

        isLoadingInterstitial = true

        Log.d(TAG, "INTERSTITIAL: Loading...")

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(
                    ad: InterstitialAd
                ) {

                    isLoadingInterstitial = false
                    interstitialAd = ad

                    Log.d(TAG, "INTERSTITIAL: Loaded")
                }

                override fun onAdFailedToLoad(
                    error: LoadAdError
                ) {

                    isLoadingInterstitial = false
                    interstitialAd = null

                    Log.e(
                        TAG,
                        "INTERSTITIAL: Load failed ${error.code} ${error.message}"
                    )
                }
            }
        )
    }

    fun showInterstitial(
        activity: Activity,
        onFinished: () -> Unit = {}
    ) {

        if (isShowingInterstitial) {
            onFinished()
            return
        }

        val ad = interstitialAd

        if (ad == null) {

            Log.d(
                TAG,
                "INTERSTITIAL: Not ready - loading"
            )

            loadInterstitial()

            onFinished()
            return
        }

        interstitialAd = null
        isShowingInterstitial = true

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdShowedFullScreenContent() {

                    Log.d(
                        TAG,
                        "INTERSTITIAL: Shown"
                    )
                }

                override fun onAdDismissedFullScreenContent() {

                    Log.d(
                        TAG,
                        "INTERSTITIAL: Closed"
                    )

                    isShowingInterstitial = false

                    loadInterstitial()

                    onFinished()
                }

                override fun onAdFailedToShowFullScreenContent(
                    adError: AdError
                ) {

                    Log.e(
                        TAG,
                        "INTERSTITIAL: Show failed ${adError.message}"
                    )

                    isShowingInterstitial = false

                    loadInterstitial()

                    onFinished()
                }
            }

        Log.d(
            TAG,
            "INTERSTITIAL: Showing level complete ad"
        )

        ad.show(activity)
    }

    // =========================================================
    // REWARDED AD
    // =========================================================

    private fun loadRewarded() {

        if (
            isLoadingRewarded ||
            rewardedAd != null
        ) {
            return
        }

        isLoadingRewarded = true

        Log.d(TAG, "REWARDED: Loading...")

        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {

                override fun onAdLoaded(
                    ad: RewardedAd
                ) {

                    isLoadingRewarded = false
                    rewardedAd = ad

                    Log.d(
                        TAG,
                        "REWARDED: Loaded"
                    )
                }

                override fun onAdFailedToLoad(
                    error: LoadAdError
                ) {

                    isLoadingRewarded = false
                    rewardedAd = null

                    Log.e(
                        TAG,
                        "REWARDED: Load failed ${error.code} ${error.message}"
                    )
                }
            }
        )
    }

    fun showRewarded(
        activity: Activity,
        onReward: () -> Unit
    ) {

        if (isShowingRewarded) {
            return
        }

        val ad = rewardedAd

        if (ad == null) {

            Log.d(
                TAG,
                "REWARDED: Not ready - loading"
            )

            loadRewarded()

            return
        }

        rewardedAd = null
        isShowingRewarded = true

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdShowedFullScreenContent() {

                    Log.d(
                        TAG,
                        "REWARDED: Shown"
                    )
                }

                override fun onAdDismissedFullScreenContent() {

                    Log.d(
                        TAG,
                        "REWARDED: Closed"
                    )

                    isShowingRewarded = false

                    loadRewarded()
                }

                override fun onAdFailedToShowFullScreenContent(
                    adError: AdError
                ) {

                    Log.e(
                        TAG,
                        "REWARDED: Show failed ${adError.message}"
                    )

                    isShowingRewarded = false

                    loadRewarded()
                }
            }

        ad.show(activity) {

            Log.d(
                TAG,
                "REWARDED: Reward SUCCESS"
            )

            onReward()
        }
    }
}
