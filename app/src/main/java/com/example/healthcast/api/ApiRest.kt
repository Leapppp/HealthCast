package com.example.healthcast.api

import com.example.healthcast.model.CommentRequest
import com.example.healthcast.model.LikeRequest
import com.example.healthcast.model.RegisterRequest
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiRest {
    @POST("register")
    fun register(@Body registerRequest: RegisterRequest): Call<JsonObject>

    @GET("posts")
    fun getStory(): Call<JsonObject?>

    @GET("users/{id}")
    fun getUser(
        @Path("id") id: String?,
    ): Call<JsonObject?>

    @Multipart
    @POST("posts")
    fun savePost(
        @Part("uid") uid: RequestBody?,
        @Part("title") title: RequestBody?,
        @Part("description") description: RequestBody?,
        @Part image: MultipartBody.Part?
    ): Call<JsonObject?>

    @Multipart
    @PUT("posts/{id}")
    fun editPost(
        @Path("id") id: String?,
        @Part("uid") uid: RequestBody?,
        @Part("title") title: RequestBody?,
        @Part("description") description: RequestBody?,
        @Part image: MultipartBody.Part?
    ): Call<JsonObject?>

    @DELETE("posts/{id}")
    fun deletePost(
        @Path("id") id: String?,
    ): Call<JsonObject?>

    @GET("posts/{id}/comments")
    fun getComment(
        @Path("id") id: String?,
    ): Call<JsonObject?>

    @POST("posts/{id}/comments")
    fun comment(
        @Path("id") id: String?,
        @Body commentRequest: CommentRequest
    ): Call<JsonObject>

    @PUT("posts/{id}/like")
    fun like(
        @Path("id") id: String?,
        @Body likeRequest: LikeRequest
    ): Call<JsonObject>

    @PUT("posts/{id}/dislike")
    fun dislike(
        @Path("id") id: String?,
        @Body likeRequest: LikeRequest
    ): Call<JsonObject>


    @GET("weather")
    fun getWheater(
        @Query("lat") lat: String?,
        @Query("lon") lon: String?,
        @Query("appid") appid: String = "2e97e113c860da365130a0a61d0cb7f1",
    ): Call<JsonObject?>

}
