package com.example.agricurify.data.di

import android.content.Context
import com.example.agricurify.data.preference.Preference
import com.example.agricurify.data.preference.dataStore
import com.example.agricurify.data.remote.ApiConfig
import com.example.agricurify.data.repository.MainRepository

object Injection {
    fun provideMainRepository(context: Context): MainRepository {
        val apiService = ApiConfig.getWeather()
        val apiServiceDetection = ApiConfig.imageClassification()
        val apiAuthentication = ApiConfig.authentication()
        val pref = Preference.getInstance(context.dataStore)
        return MainRepository.getInstance(apiService, context, apiServiceDetection, apiAuthentication, pref)
    }

    fun userPreference(context: Context): Preference {
        return Preference.getInstance(context.dataStore)
    }
}