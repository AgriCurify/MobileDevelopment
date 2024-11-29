package com.example.agricurify.data.di

import android.content.Context
import com.example.agricurify.data.remote.ApiConfig
import com.example.agricurify.data.repository.MainRepository
import com.google.android.gms.location.LocationServices

object Injection {
    fun provideMainRepository(context: Context): MainRepository {
        val apiService = ApiConfig.getWeather()
        return MainRepository.getInstance(apiService, context)
    }
}