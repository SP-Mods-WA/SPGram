package com.spmods.spgram

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object ThemePreference {
    private val IS_DARK = booleanPreferencesKey("is_dark")

    fun isDarkFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[IS_DARK] ?: true }

    suspend fun save(context: Context, isDark: Boolean) {
        context.dataStore.edit { prefs -> prefs[IS_DARK] = isDark }
    }
}
