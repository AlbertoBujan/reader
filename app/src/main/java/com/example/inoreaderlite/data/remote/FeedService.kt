package com.example.inoreaderlite.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

interface FeedService {
    @GET
    suspend fun fetchFeed(@Url url: String): ResponseBody
}
