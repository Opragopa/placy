package com.example.placy.tests.publication

import com.example.placy.ApiService
import com.example.placy.GeoMark

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class PublicationApiFailureTest {
    private fun client(payload: String, status: HttpStatusCode = HttpStatusCode.OK) = HttpClient(MockEngine {
        respond(payload, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

    @Test fun serviceUnavailableRejectsSave() = runTest {
        val http = client("",HttpStatusCode.ServiceUnavailable)
        try { assertFalse(ApiService(http).saveMark(GeoMark("a",0.0,0.0))) } finally { http.close() }
    }
}
