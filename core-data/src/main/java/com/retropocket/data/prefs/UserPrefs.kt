package com.retropocket.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "retropocket_prefs")

class UserPrefs(private val context: Context) {
    private val romTreeUriKey = stringPreferencesKey("rom_tree_uri")

    val romTreeUri: Flow<String?> = context.dataStore.data.map { it[romTreeUriKey] }

    suspend fun setRomTreeUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) prefs.remove(romTreeUriKey) else prefs[romTreeUriKey] = uri
        }
    }
}
