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

class AdManager(
    private val context: Context
) {

    companion object {

        // Google official TEST ad unit IDs.
        // Testing ke liye inhi ko use karo.
        private const val TEST_INTERSTITIAL_ID =
            "ca-app-pub-3940256099942544/1033173712"

        private const val TEST_REWARDED_ID =
            "ca-app-pub-3940256099942544/5224354917"
    }

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    init {
        loadInterstitial()
        loadRewarded()
    }

    /*
     * INTERSTITIAL
     */

    private fun loadInterstitial() {

        val request = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            TEST_INTERSTITIAL_ID,
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
                    loadAdError: LoadAdError
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
        interstitialAd = null
    }

    /*
     * REWARDED
     */

    private fun loadRewarded() {

        val request = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            TEST_REWARDED_ID,
            request,
            object : RewardedAdLoadCallback() {

                override fun onAdLoaded(ad: RewardedAd) {

                    rewardedAd = ad

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
                }

                override fun onAdFailedToLoad(
                    loadAdError: LoadAdError
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

        var rewardGiven = false

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

            if (!rewardGiven) {

                rewardGiven = true
                onReward()
            }
        }

        rewardedAd = null
    }
}
