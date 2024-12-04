package com.example.agricurify.data.remote

import com.example.agricurify.data.response.ModelResponse
import com.example.agricurify.data.response.WeatherResponse
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {

    @GET("forecast")
    suspend fun getWeatherData(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") appid: String,
    ): WeatherResponse

    @Multipart
    @POST("predict/grape")
    suspend fun uploadAppleImage(
        @Part file :MultipartBody.Part
    ) : ModelResponse
}