package com.example.agricurify.data.di

import android.content.Context
import com.example.agricurify.data.remote.ApiConfig
import com.example.agricurify.data.repository.MainRepository

object Injection {
    fun provideMainRepository(context: Context): MainRepository {
        val apiService = ApiConfig.getWeather()
        val apiServiceDetection = ApiConfig.imageClassification()
        return MainRepository.getInstance(apiService, context, apiServiceDetection)
    }
}