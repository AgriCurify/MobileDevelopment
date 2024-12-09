package com.example.agricurify.data.response

import com.google.gson.annotations.SerializedName

data class ChangePasswordRequest(
    @SerializedName("oldPassword")
    val oldPassword: String,

    @SerializedName("newPassword")
    val newPassword: String,

    @SerializedName("confirmPassword")
    val confirmPassword: String
)
