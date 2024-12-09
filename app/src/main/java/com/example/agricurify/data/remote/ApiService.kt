package com.example.agricurify.data.remote

import com.example.agricurify.data.response.ChangePasswordRequest
import com.example.agricurify.data.response.ChangePasswordResponse
import com.example.agricurify.data.response.LoginRequest
import com.example.agricurify.data.response.LoginResponse
import com.example.agricurify.data.response.LogoutResponse
import com.example.agricurify.data.response.ModelResponse
import com.example.agricurify.data.response.ProfileImageResponse
import com.example.agricurify.data.response.RegisterRequest
import com.example.agricurify.data.response.RegsiterResponse
import com.example.agricurify.data.response.UserProfileResponse
import com.example.agricurify.data.response.WeatherResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
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
    @POST("predict/apple")
    suspend fun uploadAppleImage(
        @Part file :MultipartBody.Part
    ) : ModelResponse

    @Multipart
    @POST("predict/grape")
    suspend fun uploadGrapeImage(
        @Part file :MultipartBody.Part
    ) : ModelResponse

    @Multipart
    @POST("predict/tomato")
    suspend fun uploadTomatoImage(
        @Part file :MultipartBody.Part
    ) : ModelResponse

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): RegsiterResponse

    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("users")
    suspend fun getProfile(@Header("Authorization") token: String): UserProfileResponse

    @POST("logout")
    suspend fun logout(@Header("Authorization") token: String): LogoutResponse

    @Multipart
    @PUT("/users/updateProfile")
    suspend fun updateProfileImage(
        @Header("Authorization") token: String,
        @Part profileImage: MultipartBody.Part
    ): Response<ProfileImageResponse>

    @PUT("users")
    suspend fun updateUserData(
        @Header("Authorization") token: String,
        @Body updateRequest: Map<String, String>
    ): Response<UserProfileResponse>

    @PUT("users/changePassword")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<ChangePasswordResponse>
}
