package com.example.agricurify.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.http.HttpException
import androidx.core.app.ActivityCompat
import com.example.agricurify.BuildConfig
import com.example.agricurify.data.remote.ApiService
import com.example.agricurify.data.response.ModelResponse
import com.example.agricurify.data.response.WeatherResponse
import com.example.agricurify.utils.await
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

class MainRepository private constructor(
    private val apiService: ApiService,
    private val context: Context,
    private val apiServiceDetection: ApiService,
) {
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    suspend fun getWeatherData(): WeatherResponse {
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

            return apiService.getWeatherData(
                lat = latitude,
                lon = longitude,
                appid = BuildConfig.API_KEY,
            )
        } ?: throw Exception("Location not available")
    }

    suspend fun getAppleDetection(file: File): ModelResponse {
        @Suppress("UNREACHABLE_CODE")
        return try {
            val requestImageFile = file.asRequestBody("image/jpeg".toMediaType())
            val multipartBody = MultipartBody.Part.createFormData(
                "file",
                file.name,
                requestImageFile
            )
            return apiServiceDetection.uploadAppleImage(
                file = multipartBody
            )
        } catch (@SuppressLint("NewApi") e: HttpException){
            throw e
        } catch (e: IOException) {
            throw e
        }


    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: MainRepository? = null
        fun getInstance(apiService: ApiService, context: Context, apiServiceDetection: ApiService) =
            instance ?: synchronized(this) {
                instance ?: MainRepository(apiService, context, apiServiceDetection )
            }.also { instance = it }
    }
}