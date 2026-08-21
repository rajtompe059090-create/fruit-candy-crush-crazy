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

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "game_scores"
)

data class Transaction(
    val id: Long,
    val type: String,
    val amount: Int,
    val description: String,
    val timestamp: Long
)

class ScoreRepository(
    private val context: Context
) {

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

    private val HIGHEST_LEVEL_KEY =
        intPreferencesKey("highest_level")


    // ==============================
    // HIGH SCORE
    // ==============================

    val highScoreFlow: Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[HIGH_SCORE_KEY] ?: 0
        }


    // ==============================
    // WALLET
    // ==============================

    val walletBalanceFlow: Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[WALLET_BALANCE_KEY] ?: 0
        }


    // ==============================
    // HIGHEST UNLOCKED LEVEL
    // ==============================

    val highestLevelFlow: Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[HIGHEST_LEVEL_KEY] ?: 1
        }


    // ==============================
    // SOUND
    // ==============================

    val soundEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[SOUND_KEY] ?: true
        }


    // ==============================
    // MUSIC
    // ==============================

    val musicEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[MUSIC_KEY] ?: true
        }


    // ==============================
    // VIBRATION
    // ==============================

    val vibrationEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[VIBRATION_KEY] ?: true
        }


    // ==============================
    // RATING
    // ==============================

    val hasRatedFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[HAS_RATED_KEY] ?: false
        }


    // ==============================
    // GAMES PLAYED
    // ==============================

    val gamesPlayedFlow: Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[GAMES_PLAYED_KEY] ?: 0
        }


    // ==============================
    // TRANSACTIONS
    // ==============================

    val transactionsFlow: Flow<List<Transaction>> =
        context.dataStore.data.map { preferences ->

            val json =
                preferences[TRANSACTIONS_KEY] ?: "[]"

            parseTransactions(json)
        }


    // ==============================
    // UPDATE HIGH SCORE
    // ==============================

    suspend fun updateHighScore(score: Int) {

        context.dataStore.edit { preferences ->

            val old =
                preferences[HIGH_SCORE_KEY] ?: 0

            if (score > old) {
                preferences[HIGH_SCORE_KEY] = score
            }
        }
    }


    // ==============================
    // GAME PLAYED
    // ==============================

    suspend fun incrementGamesPlayed() {

        context.dataStore.edit { preferences ->

            val current =
                preferences[GAMES_PLAYED_KEY] ?: 0

            preferences[GAMES_PLAYED_KEY] =
                current + 1
        }
    }


    // ==============================
    // ADD EARNING
    // ==============================

    suspend fun addEarning(amount: Int) {

        if (amount <= 0) return

        context.dataStore.edit { preferences ->

            val currentBalance =
                preferences[WALLET_BALANCE_KEY] ?: 0

            preferences[WALLET_BALANCE_KEY] =
                currentBalance + amount

            val oldJson =
                preferences[TRANSACTIONS_KEY] ?: "[]"

            val transactions =
                parseTransactions(oldJson).toMutableList()

            val now =
                System.currentTimeMillis()

            transactions.add(
                0,
                Transaction(
                    id = now,
                    type = "EARNING",
                    amount = amount,
                    description = "Level completed",
                    timestamp = now
                )
            )

            preferences[TRANSACTIONS_KEY] =
                transactionsToJson(
                    transactions.take(100)
                )
        }
    }


    // ==============================
    // UNLOCK LEVEL
    // ==============================

    suspend fun unlockLevel(level: Int) {

        if (level < 1) return

        context.dataStore.edit { preferences ->

            val current =
                preferences[HIGHEST_LEVEL_KEY] ?: 1

            if (level > current) {
                preferences[HIGHEST_LEVEL_KEY] = level
            }
        }
    }


    // ==============================
    // GET HIGHEST LEVEL
    // ==============================

    suspend fun setHighestLevel(level: Int) {

        if (level < 1) return

        context.dataStore.edit { preferences ->

            preferences[HIGHEST_LEVEL_KEY] = level
        }
    }


    // ==============================
    // RATING
    // ==============================

    suspend fun setHasRated(rated: Boolean) {

        context.dataStore.edit { preferences ->
            preferences[HAS_RATED_KEY] = rated
        }
    }


    // ==============================
    // SOUND
    // ==============================

    suspend fun toggleSound(enabled: Boolean) {

        context.dataStore.edit { preferences ->
            preferences[SOUND_KEY] = enabled
        }
    }


    // ==============================
    // MUSIC
    // ==============================

    suspend fun toggleMusic(enabled: Boolean) {

        context.dataStore.edit { preferences ->
            preferences[MUSIC_KEY] = enabled
        }
    }


    // ==============================
    // VIBRATION
    // ==============================

    suspend fun toggleVibration(enabled: Boolean) {

        context.dataStore.edit { preferences ->
            preferences[VIBRATION_KEY] = enabled
        }
    }


    // ==============================
    // PARSE TRANSACTIONS
    // ==============================

    private fun parseTransactions(
        jsonString: String
    ): List<Transaction> {

        val result =
            mutableListOf<Transaction>()

        try {

            val array =
                JSONArray(jsonString)

            for (i in 0 until array.length()) {

                val obj =
                    array.getJSONObject(i)

                result.add(
                    Transaction(
                        id =
                            obj.optLong("id"),

                        type =
                            obj.optString(
                                "type",
                                "EARNING"
                            ),

                        amount =
                            obj.optInt(
                                "amount",
                                0
                            ),

                        description =
                            obj.optString(
                                "description",
                                "Earning"
                            ),

                        timestamp =
                            obj.optLong("timestamp")
                    )
                )
            }

        } catch (_: Exception) {
            // Invalid JSON hone par empty list
        }

        return result
    }


    // ==============================
    // TRANSACTIONS TO JSON
    // ==============================

    private fun transactionsToJson(
        transactions: List<Transaction>
    ): String {

        val array =
            JSONArray()

        transactions.forEach { transaction ->

            val obj =
                JSONObject()

            obj.put(
                "id",
                transaction.id
            )

            obj.put(
                "type",
                transaction.type
            )

            obj.put(
                "amount",
                transaction.amount
            )

            obj.put(
                "description",
                transaction.description
            )

            obj.put(
                "timestamp",
                transaction.timestamp
            )

            array.put(obj)
        }

        return array.toString()
    }
}
