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

        // YOUR REAL ADMOB INTERSTITIAL AD UNIT ID
        private const val INTERSTITIAL_AD_UNIT_ID =
            "ca-app-pub-2230624605934075/4037109000"

        // YOUR REAL ADMOB REWARDED AD UNIT ID
        private const val REWARDED_AD_UNIT_ID =
            "ca-app-pub-2230624605934075/7372934967"
    }

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    init {
        loadInterstitial()
        loadRewarded()
    }

    // ---------------- INTERSTITIAL ----------------

    private fun loadInterstitial() {

        val request = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            request,
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

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial()
                    onFinished()
                }

                override fun onAdFailedToShowFullScreenContent(
                    adError: AdError
                ) {
                    interstitialAd = null
                    loadInterstitial()
                    onFinished()
                }
            }

        ad.show(activity)
    }

    // ---------------- REWARDED ----------------

    private fun loadRewarded() {

        val request = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            request,
            object : RewardedAdLoadCallback() {

                override fun onAdLoaded(ad: RewardedAd) {

                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(
                    error: LoadAdError
                ) {
                    rewardedAd = null
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

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewarded()
                }

                override fun onAdFailedToShowFullScreenContent(
                    adError: AdError
                ) {
                    rewardedAd = null
                    loadRewarded()
                }
            }

        ad.show(activity) {
            onReward()
        }
    }
}
