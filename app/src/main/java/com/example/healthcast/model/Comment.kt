package com.example.healthcast.model

data class Comment(
    val id: String,
    val likes: List<String>,
    val dislikes: List<String>,
    val user: User,
    val comment: String,
    val created_at: String
)
