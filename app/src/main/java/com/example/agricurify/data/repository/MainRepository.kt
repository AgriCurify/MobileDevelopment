package com.example.agricurify.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.example.agricurify.data.remote.ApiService
import com.example.agricurify.data.response.WeatherResponse
import com.example.agricurify.utils.await
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainRepository private constructor(
    private val apiService: ApiService,
    private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    suspend fun getWeatherData(): WeatherResponse {
        // Check permission
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Location permission not granted")
        }

        val location = fusedLocationClient.lastLocation.await()
        location?.let {
            val latitude = it.latitude
            val longitude = it.longitude

            // Call OpenWeather API
            return apiService.getWeatherData(
                lat = latitude,
                lon = longitude,
                appid = "a1d91f746849c525b26c14cf44cd7a6f",
            )
        } ?: throw Exception("Location not available")
    }



    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: MainRepository? = null
        fun getInstance(apiService: ApiService, context: Context) =
            instance ?: synchronized(this) {
                instance ?: MainRepository(apiService, context )
            }.also { instance = it }
    }
}