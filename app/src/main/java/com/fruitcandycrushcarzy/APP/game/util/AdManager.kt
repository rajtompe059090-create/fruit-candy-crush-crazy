package com.fruitcandycrushcarzy.APP.game.util

import android.app.Activity
import android.content.Context
import android.util.Log
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

        private const val TAG = "FruitCandyAd"

        private const val INTERSTITIAL_AD_UNIT_ID =
            "ca-app-pub-2230624605934075/4037109000"

        private const val REWARDED_AD_UNIT_ID =
            "ca-app-pub-2230624605934075/7372934967"
    }

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private var isLoadingInterstitial = false
    private var isLoadingRewarded = false

    init {
        loadInterstitial()
        loadRewarded()
    }

    // =========================================================
    // INTERSTITIAL
    // =========================================================

    private fun loadInterstitial() {

        if (isLoadingInterstitial || interstitialAd != null) {
            return
        }

        isLoadingInterstitial = true

        Log.d(TAG, "Loading interstitial ad...")

        val request = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            request,
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(ad: InterstitialAd) {

                    isLoadingInterstitial = false
                    interstitialAd = ad

                    Log.d(TAG, "INTERSTITIAL LOADED SUCCESSFULLY")

                    ad.fullScreenContentCallback =
                        object : FullScreenContentCallback() {

                            override fun onAdDismissedFullScreenContent() {

                                Log.d(
                                    TAG,
                                    "Interstitial dismissed"
                                )

                                interstitialAd = null
                                loadInterstitial()
                            }

                            override fun onAdFailedToShowFullScreenContent(
                                adError: AdError
                            ) {

                                Log.e(
                                    TAG,
                                    "Interstitial show failed: ${adError.message}"
                                )

                                interstitialAd = null
                                loadInterstitial()
                            }
                        }
                }

                override fun onAdFailedToLoad(
                    error: LoadAdError
                ) {

                    isLoadingInterstitial = false
                    interstitialAd = null

                    Log.e(
                        TAG,
                        "INTERSTITIAL LOAD FAILED: " +
                                "code=${error.code}, " +
                                "message=${error.message}"
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

            Log.d(
                TAG,
                "Interstitial not ready. Loading again."
            )

            loadInterstitial()
            onFinished()
            return
        }

        interstitialAd = null

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {

                    Log.d(
                        TAG,
                        "Interstitial closed"
                    )

                    loadInterstitial()
                    onFinished()
                }

                override fun onAdFailedToShowFullScreenContent(
                    adError: AdError
                ) {

                    Log.e(
                        TAG,
                        "Interstitial show error: ${adError.message}"
                    )

                    loadInterstitial()
                    onFinished()
                }
            }

        Log.d(
            TAG,
            "Showing INTERSTITIAL"
        )

        ad.show(activity)
    }

    // =========================================================
    // REWARDED
    // =========================================================

    private fun loadRewarded() {

        if (isLoadingRewarded || rewardedAd != null) {
            return
        }

        isLoadingRewarded = true

        Log.d(TAG, "Loading rewarded ad...")

        val request = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            request,
            object : RewardedAdLoadCallback() {

                override fun onAdLoaded(
                    ad: RewardedAd
                ) {

                    isLoadingRewarded = false
                    rewardedAd = ad

                    Log.d(
                        TAG,
                        "REWARDED LOADED SUCCESSFULLY"
                    )
                }

                override fun onAdFailedToLoad(
                    error: LoadAdError
                ) {

                    isLoadingRewarded = false
                    rewardedAd = null

                    Log.e(
                        TAG,
                        "REWARDED LOAD FAILED: " +
                                "code=${error.code}, " +
                                "message=${error.message}"
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

            Log.d(
                TAG,
                "Rewarded not ready. Loading again."
            )

            loadRewarded()
            return
        }

        rewardedAd = null

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {

                    Log.d(
                        TAG,
                        "Rewarded closed"
                    )

                    loadRewarded()
                }

                override fun onAdFailedToShowFullScreenContent(
                    adError: AdError
                ) {

                    Log.e(
                        TAG,
                        "Rewarded show error: ${adError.message}"
                    )

                    loadRewarded()
                }
            }

        Log.d(
            TAG,
            "Showing REWARDED"
        )

        ad.show(activity) {

            Log.d(
                TAG,
                "REWARDED COMPLETED"
            )

            onReward()
        }
    }
}
