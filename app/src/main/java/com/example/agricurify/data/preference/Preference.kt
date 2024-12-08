package com.example.agricurify.data.preference

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.agricurify.data.response.LoginModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")
class Preference private constructor(private val dataStore: DataStore<Preferences>){
    suspend fun logout() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun saveToken(userModel: LoginModel) {
        try {
            dataStore.edit { preferences ->
                preferences[TOKEN_KEY] = userModel.token
            }
        } catch (e: IOException) {
            Log.e("UserPreference", "Error saving token: ${e.message}")
        }
    }

    fun getToken(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[TOKEN_KEY] ?: ""
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: Preference? = null

        private val TOKEN_KEY = stringPreferencesKey("token")

        fun getInstance(dataStore: DataStore<Preferences>): Preference {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Preference(dataStore).also { INSTANCE = it }
            }
        }
    }
}