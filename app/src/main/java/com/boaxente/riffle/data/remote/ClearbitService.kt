package com.boaxente.riffle.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface ClearbitService {
    @GET("v1/companies/suggest")
    suspend fun suggestCompanies(@Query("query") query: String): List<ClearbitSuggestion>
}

data class ClearbitSuggestion(
    val name: String,
    val domain: String,
    val logo: String?
)
