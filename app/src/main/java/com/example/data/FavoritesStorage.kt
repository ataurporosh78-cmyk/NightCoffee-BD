package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

private val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nightcoffee_favorites"
)

interface FavoritesStorage {
    val favoriteIds: Flow<Set<String>>
    suspend fun toggleFavorite(shopId: String)
}

class DataStoreFavoritesStorage(
    private val context: Context
) : FavoritesStorage {

    private val favoriteIdsKey = stringSetPreferencesKey("favorite_shop_ids")

    override val favoriteIds: Flow<Set<String>> = context.favoritesDataStore.data
        .map { preferences -> preferences[favoriteIdsKey] ?: emptySet() }

    override suspend fun toggleFavorite(shopId: String) {
        context.favoritesDataStore.edit { preferences ->
            val current = preferences[favoriteIdsKey] ?: emptySet()
            preferences[favoriteIdsKey] = if (shopId in current) {
                current - shopId
            } else {
                current + shopId
            }
        }
    }
}

class InMemoryFavoritesStorage(
    initialFavorites: Set<String> = emptySet()
) : FavoritesStorage {
    private val _favoriteIds = MutableStateFlow(initialFavorites)
    override val favoriteIds: Flow<Set<String>> = _favoriteIds.asStateFlow()

    override suspend fun toggleFavorite(shopId: String) {
        _favoriteIds.update { current ->
            if (shopId in current) current - shopId else current + shopId
        }
    }
}
