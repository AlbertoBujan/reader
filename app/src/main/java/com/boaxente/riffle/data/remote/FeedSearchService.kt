package com.boaxente.riffle.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface FeedSearchService {
    @GET("api/v1/search")
    suspend fun search(@Query("url") url: String): List<FeedSearchDto>
}

data class FeedSearchDto(
    @SerializedName("url") val url: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("site_url") val siteUrl: String?,
    @SerializedName("self_url") val selfUrl: String?,
    @SerializedName("favicon") val favicon: String?,
    @SerializedName("version") val version: String?
)
