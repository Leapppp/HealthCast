package com.example.healthcast.model

data class Story(
    val id: String,
    val likes: List<String>,
    val dislikes: List<String>,
    val user: User,
    val image_url: String,
    val title: String,
    val description: String,
    val created_at: String
)
