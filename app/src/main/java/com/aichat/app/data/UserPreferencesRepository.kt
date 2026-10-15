package com.aichat.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {

    private val ROUTINE_CONTEXT_KEY = stringPreferencesKey("routine_context")
    private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")

    val routineContext: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[ROUTINE_CONTEXT_KEY] ?: ""
        }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] ?: false
        }

    suspend fun saveRoutineContext(contextText: String) {
        context.dataStore.edit { preferences ->
            preferences[ROUTINE_CONTEXT_KEY] = contextText
        }
    }

    suspend fun completeOnboarding() {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = true
        }
    }
}
