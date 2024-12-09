package com.example.agricurify.data.response

import com.google.gson.annotations.SerializedName

data class ChangePasswordResponse(
    @SerializedName("message")
    val message: String
)
