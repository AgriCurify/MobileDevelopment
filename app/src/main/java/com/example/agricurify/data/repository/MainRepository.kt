package com.example.agricurify.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.http.HttpException
import androidx.core.app.ActivityCompat
import androidx.lifecycle.liveData
import com.example.agricurify.BuildConfig
import com.example.agricurify.data.preference.Preference
import com.example.agricurify.data.remote.ApiService
import com.example.agricurify.data.response.LoginModel
import com.example.agricurify.data.response.LoginRequest
import com.example.agricurify.data.response.ModelResponse
import com.example.agricurify.data.response.RegisterRequest
import com.example.agricurify.data.response.WeatherResponse
import com.example.agricurify.utils.ResultState
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
    private val apiAuthentication: ApiService,
    private val userPreference: Preference
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

    suspend fun getGrapeDetection(file: File): ModelResponse {
        @Suppress("UNREACHABLE_CODE")
        return try {
            val requestImageFile = file.asRequestBody("image/jpeg".toMediaType())
            val multipartBody = MultipartBody.Part.createFormData(
                "file",
                file.name,
                requestImageFile
            )
            return apiServiceDetection.uploadGrapeImage(
                file = multipartBody
            )
        } catch (@SuppressLint("NewApi") e: HttpException){
            throw e
        } catch (e: IOException) {
            throw e
        }
    }

    suspend fun getTomatoDetection(file: File): ModelResponse {
        @Suppress("UNREACHABLE_CODE")
        return try {
            val requestImageFile = file.asRequestBody("image/jpeg".toMediaType())
            val multipartBody = MultipartBody.Part.createFormData(
                "file",
                file.name,
                requestImageFile
            )
            return apiServiceDetection.uploadTomatoImage(
                file = multipartBody
            )
        } catch (@SuppressLint("NewApi") e: HttpException){
            throw e
        } catch (e: IOException) {
            throw e
        }
    }

    suspend fun register (name: String, email: String, password: String) = liveData {
        emit(ResultState.Loading)
        try {
            val request = RegisterRequest(name, email, password)
            val response = apiAuthentication.register(request)
            emit(ResultState.Success(response))
        } catch (e: Exception) {
            emit(ResultState.Error(e.message.toString()))
        }

    }

    suspend fun login (email: String, password: String) = liveData {
        emit(ResultState.Loading)
        try {
            val request = LoginRequest(email, password)
            val response = apiAuthentication.login(request)
            val loginModel = LoginModel(
                token = response.token
            )
            userPreference.saveToken(loginModel)
            emit(ResultState.Success(response))
        } catch (e: Exception) {
            emit(ResultState.Error(e.message.toString()))
        }

    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: MainRepository? = null
        fun getInstance(apiService: ApiService, context: Context, apiServiceDetection: ApiService, apiAuthentication: ApiService, pref: Preference) =
            instance ?: synchronized(this) {
                instance ?: MainRepository(apiService, context, apiServiceDetection, apiAuthentication, pref )
            }.also { instance = it }
    }
}