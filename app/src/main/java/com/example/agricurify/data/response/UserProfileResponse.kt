package com.example.agricurify.data.response

data class UserProfileResponse(
    val message: String,
    val data: UserData?
)

data class UserData(
    val id: Int,
    val name: String,
    val email: String,
    val image: String
)
