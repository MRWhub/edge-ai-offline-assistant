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

import javax.inject.Inject
import javax.inject.Singleton

import dagger.hilt.android.qualifiers.ApplicationContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val ROUTINE_CONTEXT_KEY = stringPreferencesKey("routine_context")
    private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
    private val LAST_MODEL_NAME_KEY = stringPreferencesKey("last_model_name")

    val routineContext: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[ROUTINE_CONTEXT_KEY] ?: ""
        }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] ?: false
        }
        
    val lastModelName: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_MODEL_NAME_KEY]
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
    
    suspend fun saveLastModelName(modelName: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_MODEL_NAME_KEY] = modelName
        }
    }
}
