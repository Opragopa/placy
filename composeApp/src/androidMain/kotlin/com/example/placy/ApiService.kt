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
    private val baseUrl: String = "http://188.242.76.219:57492",
) {

    suspend fun getAllMarks(): List<GeoMark> {
        return try {
            client.get("$baseUrl/geomarks").body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun saveMark(mark: GeoMark): Boolean {
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