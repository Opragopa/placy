package com.example.placy

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json

class ApiService(
    private val client: HttpClient,
    private val baseUrl: String = BuildConfig.API_URL,
) {

    suspend fun getAllMarks(): List<GeoMark> {
        return try {
            client.get("$baseUrl/geomarks").body<List<GeoMark>>().filter { it.hasValidCoordinates() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun saveMark(mark: GeoMark): Boolean {
        if (!mark.hasValidCoordinates()) return false
        return try {
            val response = client.post("$baseUrl/geomarks") {
                contentType(ContentType.Application.Json)
                setBody(mark)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}