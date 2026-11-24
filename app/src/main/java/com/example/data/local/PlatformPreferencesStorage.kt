package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.UserPreferenceDoc
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.platformDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nightcoffee_platform_prefs"
)

class PlatformPreferencesStorage(private val context: Context) {

    private val compareIdsKey = stringPreferencesKey("compare_shop_ids_csv")
    private val optNewOpeningsKey = booleanPreferencesKey("opt_new_openings")
    private val optOffersKey = booleanPreferencesKey("opt_offers")
    private val optSavedShopsKey = booleanPreferencesKey("opt_saved_shops")
    private val optReservationsKey = booleanPreferencesKey("opt_reservations")
    private val optEventsKey = booleanPreferencesKey("opt_events")
    private val optAnnouncementsKey = booleanPreferencesKey("opt_announcements")
    private val fcmTokenKey = stringPreferencesKey("fcm_token")

    val compareShopIds: Flow<List<String>> = safePrefsFlow().map { prefs ->
        prefs[compareIdsKey]
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(3)
    }

    val localPreferences: Flow<UserPreferenceDoc> = safePrefsFlow().map { prefs ->
        UserPreferenceDoc(
            fcmToken = prefs[fcmTokenKey].orEmpty(),
            optInNewOpenings = prefs[optNewOpeningsKey] ?: false,
            optInOffers = prefs[optOffersKey] ?: false,
            optInSavedShops = prefs[optSavedShopsKey] ?: true,
            optInReservations = prefs[optReservationsKey] ?: true,
            optInEvents = prefs[optEventsKey] ?: false,
            optInAnnouncements = prefs[optAnnouncementsKey] ?: true,
            compareShopIds = prefs[compareIdsKey]
                .orEmpty()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(3)
        )
    }

    suspend fun saveCompareIds(ids: List<String>) {
        runCatching {
            context.platformDataStore.edit { prefs ->
                prefs[compareIdsKey] = ids.distinct().take(3).joinToString(",")
            }
        }
    }

    suspend fun saveNotificationPrefs(doc: UserPreferenceDoc) {
        runCatching {
            context.platformDataStore.edit { prefs ->
                prefs[optNewOpeningsKey] = doc.optInNewOpenings
                prefs[optOffersKey] = doc.optInOffers
                prefs[optSavedShopsKey] = doc.optInSavedShops
                prefs[optReservationsKey] = doc.optInReservations
                prefs[optEventsKey] = doc.optInEvents
                prefs[optAnnouncementsKey] = doc.optInAnnouncements
                if (doc.fcmToken.isNotBlank()) {
                    prefs[fcmTokenKey] = doc.fcmToken
                }
                prefs[compareIdsKey] = doc.compareShopIds.distinct().take(3).joinToString(",")
            }
        }
    }

    suspend fun saveFcmToken(token: String) {
        runCatching {
            context.platformDataStore.edit { prefs ->
                prefs[fcmTokenKey] = token
            }
        }
    }

    suspend fun canSubmitCrowdReport(shopId: String, cooldownMs: Long, nowMillis: Long): Boolean {
        val key = longPreferencesKey("crowd_cooldown_$shopId")
        val lastMillis = safePrefsFlow().first()[key] ?: 0L
        return (nowMillis - lastMillis) >= cooldownMs
    }

    suspend fun recordCrowdReportSubmitted(shopId: String, nowMillis: Long) {
        val key = longPreferencesKey("crowd_cooldown_$shopId")
        runCatching {
            context.platformDataStore.edit { prefs ->
                prefs[key] = nowMillis
            }
        }
    }

    private fun safePrefsFlow(): Flow<Preferences> = context.platformDataStore.data
        .catch { error ->
            if (error is IOException) {
                Log.w("PlatformPrefs", "Recovered DataStore error", error)
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
}
