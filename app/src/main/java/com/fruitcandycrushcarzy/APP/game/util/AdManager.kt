package com.fruitcandycrushcarzy.APP.game.util

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdManager(private val context: Context) {

    companion object {

        private const val INTERSTITIAL_AD_UNIT_ID =
            "ca-app-pub-6146868530948467/2406181764"

        private const val REWARDED_AD_UNIT_ID =
            "ca-app-pub-6146868530948467/9108834256"
    }

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    init {
        loadInterstitial()
        loadRewarded()
    }

    private fun loadInterstitial() {

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(ad: InterstitialAd) {

                    interstitialAd = ad

                    ad.fullScreenContentCallback =
                        object : FullScreenContentCallback() {

                            override fun onAdDismissedFullScreenContent() {
                                interstitialAd = null
                                loadInterstitial()
                            }

                            override fun onAdFailedToShowFullScreenContent(
                                adError: AdError
                            ) {
                                interstitialAd = null
                                loadInterstitial()
                            }
                        }
                }

                override fun onAdFailedToLoad(
                    error: LoadAdError
                ) {
                    interstitialAd = null

                    android.util.Log.e(
                        "ADMOB",
                        "Interstitial: ${error.code} ${error.message}"
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

    private fun loadRewarded() {

        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad

                    android.util.Log.d(
                        "ADMOB",
                        "Rewarded loaded"
                    )
                }

                override fun onAdFailedToLoad(
                    error: LoadAdError
                ) {
                    rewardedAd = null

                    android.util.Log.e(
                        "ADMOB",
                        "Rewarded: ${error.code} ${error.message}"
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
