package com.example.placy

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

class ApiPayloadUnitTest {
    private fun client(payload: String, status: HttpStatusCode = HttpStatusCode.OK) = HttpClient(MockEngine {
        respond(payload, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

    @Test fun failedGetDoesNotTreatErrorBodyAsMarks() = runTest {
        val http = client("""[{"photoUUID":"a","latitude":1.0,"longitude":2.0}]""", HttpStatusCode.ServiceUnavailable)
        try { assertTrue(ApiService(http).getAllMarks().isEmpty()) } finally { http.close() }
    }
    @Test fun cancellationPropagatesFromGet() = runTest {
        val http = HttpClient(MockEngine { throw kotlinx.coroutines.CancellationException("cancelled") })
        try { kotlin.test.assertFailsWith<kotlinx.coroutines.CancellationException> { ApiService(http).getAllMarks() } } finally { http.close() }
    }
}
