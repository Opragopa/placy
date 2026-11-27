package com.example.placy

import kotlinx.coroutines.CancellationException
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
            val response = client.get("$baseUrl/geomarks")
            if (!response.status.isSuccess()) return emptyList()
            response.body<List<GeoMark>>().filter { it.hasValidCoordinates() }
        } catch (e: CancellationException) {
            throw e
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}