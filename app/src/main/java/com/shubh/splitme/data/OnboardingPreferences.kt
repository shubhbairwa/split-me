package com.shubh.splitme.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPrefs by preferencesDataStore(name = "app_prefs")
private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

/** Remembers on this device whether the intro slides have been shown. */
class OnboardingPreferences(private val context: Context) {
    val isDone: Flow<Boolean> = context.appPrefs.data.map { it[ONBOARDING_DONE] ?: false }

    suspend fun markDone() {
        context.appPrefs.edit { it[ONBOARDING_DONE] = true }
    }
}
