package com.example.healthcast.model

data class RegisterRequest(
    val uid: String,
    val email: String,
    val fullName: String
)