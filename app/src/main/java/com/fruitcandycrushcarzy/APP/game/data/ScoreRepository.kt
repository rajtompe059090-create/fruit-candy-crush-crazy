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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_scores")

data class Transaction(
    val title: String,
    val amount: Int,
    val type: String,
    val date: String
)

class ScoreRepository(private val context: Context) {

    private val HIGH_SCORE_KEY = intPreferencesKey("high_score")
    private val SOUND_KEY = booleanPreferencesKey("sound_enabled")
    private val MUSIC_KEY = booleanPreferencesKey("music_enabled")
    private val VIBRATION_KEY = booleanPreferencesKey("vibration_enabled")
    private val HAS_RATED_KEY = booleanPreferencesKey("has_rated")
    private val GAMES_PLAYED_KEY = intPreferencesKey("games_played")

    private val WALLET_BALANCE_KEY = intPreferencesKey("wallet_balance")

    private val TRANSACTIONS_KEY =
        stringPreferencesKey("wallet_transactions")

    val highScoreFlow: Flow<Int> = context.dataStore.data
        .map { it[HIGH_SCORE_KEY] ?: 0 }

    val soundEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { it[SOUND_KEY] ?: true }

    val musicEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { it[MUSIC_KEY] ?: true }

    val vibrationEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { it[VIBRATION_KEY] ?: true }

    val hasRatedFlow: Flow<Boolean> = context.dataStore.data
        .map { it[HAS_RATED_KEY] ?: false }

    val gamesPlayedFlow: Flow<Int> = context.dataStore.data
        .map { it[GAMES_PLAYED_KEY] ?: 0 }

    val walletBalanceFlow: Flow<Int> = context.dataStore.data
        .map { it[WALLET_BALANCE_KEY] ?: 0 }

    val transactionsFlow: Flow<List<Transaction>> =
        context.dataStore.data.map { preferences ->

            val json = preferences[TRANSACTIONS_KEY] ?: "[]"

            try {
                val array = JSONArray(json)

                List(array.length()) { index ->
                    val obj = array.getJSONObject(index)

                    Transaction(
                        title = obj.optString("title"),
                        amount = obj.optInt("amount"),
                        type = obj.optString("type"),
                        date = obj.optString("date")
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun updateHighScore(score: Int) {
        context.dataStore.edit { preferences ->
            val currentHighScore =
                preferences[HIGH_SCORE_KEY] ?: 0

            if (score > currentHighScore) {
                preferences[HIGH_SCORE_KEY] = score
            }
        }
    }

    suspend fun incrementGamesPlayed() {
        context.dataStore.edit { preferences ->
            val current =
                preferences[GAMES_PLAYED_KEY] ?: 0

            preferences[GAMES_PLAYED_KEY] =
                current + 1
        }
    }

    suspend fun addEarning(amount: Int) {

        context.dataStore.edit { preferences ->

            val currentBalance =
                preferences[WALLET_BALANCE_KEY] ?: 0

            preferences[WALLET_BALANCE_KEY] =
                currentBalance + amount

            val currentJson =
                preferences[TRANSACTIONS_KEY] ?: "[]"

            val array = JSONArray(currentJson)

            val transaction =
                JSONObject().apply {
                    put(
                        "title",
                        "Level Completed"
                    )

                    put(
                        "amount",
                        amount
                    )

                    put(
                        "type",
                        "CREDIT"
                    )

                    put(
                        "date",
                        SimpleDateFormat(
                            "dd MMM yyyy, hh:mm a",
                            Locale.getDefault()
                        ).format(Date())
                    )
                }

            array.put(transaction)

            preferences[TRANSACTIONS_KEY] =
                array.toString()
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
