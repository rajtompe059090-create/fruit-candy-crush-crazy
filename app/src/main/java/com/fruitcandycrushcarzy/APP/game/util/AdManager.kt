package com.fruitcandycrushcarzy.APP.game.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
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

        AppOpenAd.load(
            context,
            APP_OPEN_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : AppOpenAdLoadCallback() {

                override fun onAdLoaded(ad: AppOpenAd) {

                    isLoadingAppOpen = false
                    appOpenAd = ad

                    Log.d(TAG, "App Open Ad loaded")

                    ad.fullScreenContentCallback =
                        object : FullScreenContentCallback() {

                            override fun onAdDismissedFullScreenContent() {
                                appOpenAd = null
                                isShowingAppOpen = false
                                loadAppOpenAd()
                            }

                            override fun onAdFailedToShowFullScreenContent(
                                adError: AdError
                            ) {
                                appOpenAd = null
                                isShowingAppOpen = false
                                loadAppOpenAd()
                            }

                            override fun onAdShowedFullScreenContent() {
                                isShowingAppOpen = true
                                appOpenAd = null
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
                        "App Open failed: ${error.code} ${error.message}"
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

            Log.d(
                TAG,
                "App Open not ready"
            )

            loadAppOpenAd()
            onFinished()
            return
        }

        isShowingAppOpen = true

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdShowedFullScreenContent() {
                    appOpenAd = null
                }

                override fun onAdDismissedFullScreenContent() {

                    isShowingAppOpen = false
                    appOpenAd = null

                    loadAppOpenAd()

                    onFinished()
                }

                override fun onAdFailedToShowFullScreenContent(
                    adError: AdError
                ) {

                    isShowingAppOpen = false
                    appOpenAd = null

                    loadAppOpenAd()

                    onFinished()
                }
            }

        ad.show(activity)
    }

    // =========================================================
    // INTERSTITIAL
    // =========================================================

    private fun loadInterstitial() {

        if (interstitialAd != null) {
            return
        }

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(
                    ad: InterstitialAd
                ) {

                    interstitialAd = ad

                    Log.d(
                        TAG,
                        "Interstitial loaded"
                    )
                }

                override fun onAdFailedToLoad(
                    error: LoadAdError
                ) {

                    interstitialAd = null

                    Log.e(
                        TAG,
                        "Interstitial failed: ${error.code} ${error.message}"
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

            loadInterstitial()
            onFinished()
            return
        }

        interstitialAd = null

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {

                    loadInterstitial()
                    onFinished()
                }

                override fun onAdFailedToShowFullScreenContent(
                    adError: AdError
                ) {

                    loadInterstitial()
                    onFinished()
                }
            }

        ad.show(activity)
    }

    // =========================================================
    // REWARDED
    // =========================================================

    private fun loadRewarded() {

        if (rewardedAd != null) {
            return
        }

        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {

                override fun onAdLoaded(
                    ad: RewardedAd
                ) {

                    rewardedAd = ad

                    Log.d(
                        TAG,
                        "Rewarded loaded"
                    )
                }

                override fun onAdFailedToLoad(
                    error: LoadAdError
                ) {

                    rewardedAd = null

                    Log.e(
                        TAG,
                        "Rewarded failed: ${error.code} ${error.message}"
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

            loadRewarded()
            return
        }

        rewardedAd = null

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    loadRewarded()
                }

                override fun onAdFailedToShowFullScreenContent(
                    adError: AdError
                ) {
                    loadRewarded()
                }
            }

        ad.show(activity) {
            onReward()
        }
    }
}
