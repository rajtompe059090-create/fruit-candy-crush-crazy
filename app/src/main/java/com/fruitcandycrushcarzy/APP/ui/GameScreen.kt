package com.fruitcandycrushcarzy.APP.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "game_scores"
)

class ScoreRepository(private val context: Context) {

    private val HIGH_SCORE_KEY =
        intPreferencesKey("high_score")

    private val SOUND_KEY =
        booleanPreferencesKey("sound_enabled")

    private val MUSIC_KEY =
        booleanPreferencesKey("music_enabled")

    private val VIBRATION_KEY =
        booleanPreferencesKey("vibration_enabled")

    private val HAS_RATED_KEY =
        booleanPreferencesKey("has_rated")

    private val GAMES_PLAYED_KEY =
        intPreferencesKey("games_played")

    private val WALLET_BALANCE_KEY =
        intPreferencesKey("wallet_balance")

    private val TRANSACTIONS_KEY =
        stringPreferencesKey("transactions")

    val highScoreFlow: Flow<Int> =
        context.dataStore.data.map {
            it[HIGH_SCORE_KEY] ?: 0
        }

    val soundEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map {
            it[SOUND_KEY] ?: true
        }

    val musicEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map {
            it[MUSIC_KEY] ?: true
        }

    val vibrationEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map {
            it[VIBRATION_KEY] ?: true
        }

    val hasRatedFlow: Flow<Boolean> =
        context.dataStore.data.map {
            it[HAS_RATED_KEY] ?: false
        }

    val gamesPlayedFlow: Flow<Int> =
        context.dataStore.data.map {
            it[GAMES_PLAYED_KEY] ?: 0
        }

    val walletBalanceFlow: Flow<Int> =
        context.dataStore.data.map {
            it[WALLET_BALANCE_KEY] ?: 0
        }

    val transactionsFlow: Flow<String> =
        context.dataStore.data.map {
            it[TRANSACTIONS_KEY] ?: ""
        }

    suspend fun updateHighScore(score: Int) {
        context.dataStore.edit {
            val current = it[HIGH_SCORE_KEY] ?: 0

            if (score > current) {
                it[HIGH_SCORE_KEY] = score
            }
        }
    }

    suspend fun incrementGamesPlayed() {
        context.dataStore.edit {
            val current = it[GAMES_PLAYED_KEY] ?: 0
            it[GAMES_PLAYED_KEY] = current + 1
        }
    }

    suspend fun addEarning(amount: Int) {

        context.dataStore.edit {

            val currentBalance =
                it[WALLET_BALANCE_KEY] ?: 0

            it[WALLET_BALANCE_KEY] =
                currentBalance + amount

            val oldTransactions =
                it[TRANSACTIONS_KEY] ?: ""

            val newTransaction =
                "+₹$amount|Level Reward|${System.currentTimeMillis()}"

            it[TRANSACTIONS_KEY] =
                if (oldTransactions.isEmpty()) {
                    newTransaction
                } else {
                    "$newTransaction\n$oldTransactions"
                }
        }
    }

    suspend fun setHasRated(rated: Boolean) {
        context.dataStore.edit {
            it[HAS_RATED_KEY] = rated
        }
    }

    suspend fun toggleSound(enabled: Boolean) {
        context.dataStore.edit {
            it[SOUND_KEY] = enabled
        }
    }

    suspend fun toggleMusic(enabled: Boolean) {
        context.dataStore.edit {
            it[MUSIC_KEY] = enabled
        }
    }

    suspend fun toggleVibration(enabled: Boolean) {
        context.dataStore.edit {
            it[VIBRATION_KEY] = enabled
        }
    }
}
