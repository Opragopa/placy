// ApiServiceTest.kt
package com.example.placy

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.contentnegotiation.ContentNegotiationConfig
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiServiceUnitTest {
    private fun createMockHttpClient(responseData: String, statusCode: HttpStatusCode): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(
                        content = responseData,
                        status = statusCode,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    @Test
    fun testGetAllMarksSuccess() = runTest {
        val mockResponse = """[{"photoUUID":"123","latitude":55.0,"longitude":37.0}]"""
        val client = createMockHttpClient(mockResponse, HttpStatusCode.OK)
        val apiService = ApiService(client)

        val result = apiService.getAllMarks()

        assertEquals(1, result.size)
        assertEquals("123", result[0].photoUUID)
        assertEquals(55.0, result[0].latitude, 0.001)
        assertEquals(37.0, result[0].longitude, 0.001)
    }

    @Test
    fun testGetAllMarksEmpty() = runTest {
        val client = createMockHttpClient("[]", HttpStatusCode.OK)
        val apiService = ApiService(client)

        val result = apiService.getAllMarks()

        assertTrue(result.isEmpty())
    }

    @Test
    fun testGetAllMarksException() = runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    throw Exception("Network error")
                }
            }
        }
        val apiService = ApiService(client)

        val result = apiService.getAllMarks()

        assertTrue(result.isEmpty())
    }

    @Test
    fun testSaveMarkSuccess() = runTest {
        val client = createMockHttpClient("", HttpStatusCode.Created)
        val apiService = ApiService(client)
        val mark = GeoMark("456", 56.0, 38.0)

        val result = apiService.saveMark(mark)

        assertTrue(result)
    }

    @Test
    fun testSaveMarkFailure() = runTest {
        val client = createMockHttpClient("", HttpStatusCode.BadRequest)
        val apiService = ApiService(client)
        val mark = GeoMark("789", 57.0, 39.0)

        val result = apiService.saveMark(mark)

        assertTrue(!result)
    }

    @Test
    fun testSaveMarkException() = runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    throw Exception("Network error")
                }
            }
        }
        val apiService = ApiService(client)
        val mark = GeoMark("999", 58.0, 40.0)

        val result = apiService.saveMark(mark)

        assertTrue(!result)
    }
}