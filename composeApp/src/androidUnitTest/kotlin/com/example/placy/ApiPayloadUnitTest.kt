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

/** Different payload and transport failure classes used by the map and publication flows. */
class ApiPayloadUnitTest {
    private fun client(payload: String, status: HttpStatusCode = HttpStatusCode.OK) = HttpClient(MockEngine {
        respond(payload, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

    @Test fun unknownServerFieldsDoNotDiscardValidMark() = runTest {
        val http = client("""[{"photoUUID":"a","latitude":1.0,"longitude":2.0,"extra":123}]""")
        try { assertEquals(listOf(GeoMark("a",1.0,2.0)), ApiService(http).getAllMarks()) } finally { http.close() }
    }
    @Test fun missingPhotoIdentifierRejectsPayload() = runTest {
        val http = client("""[{"latitude":1.0,"longitude":2.0}]""")
        try { assertTrue(ApiService(http).getAllMarks().isEmpty()) } finally { http.close() }
    }
    @Test fun nullLatitudeRejectsPayload() = runTest {
        val http = client("""[{"photoUUID":"a","latitude":null,"longitude":2.0}]""")
        try { assertTrue(ApiService(http).getAllMarks().isEmpty()) } finally { http.close() }
    }
    @Test fun objectInsteadOfArrayRejectsPayload() = runTest {
        val http = client("""{"photoUUID":"a","latitude":1.0,"longitude":2.0}""")
        try { assertTrue(ApiService(http).getAllMarks().isEmpty()) } finally { http.close() }
    }
    @Test fun mixedValidAndInvalidMarksDoNotProducePartialList() = runTest {
        val http = client("""[{"photoUUID":"a","latitude":1.0,"longitude":2.0},{"photoUUID":"b","latitude":3.0}]""")
        try { assertTrue(ApiService(http).getAllMarks().isEmpty()) } finally { http.close() }
    }
    @Test fun unicodePhotoIdIsPreserved() = runTest {
        val http = client("""[{"photoUUID":"фото-1","latitude":0.0,"longitude":0.0}]""")
        try { assertEquals("фото-1", ApiService(http).getAllMarks().single().photoUUID) } finally { http.close() }
    }
    @Test fun zeroCoordinatesArePreserved() = runTest {
        val http = client("""[{"photoUUID":"a","latitude":0.0,"longitude":0.0}]""")
        try { assertEquals(GeoMark("a",0.0,0.0), ApiService(http).getAllMarks().single()) } finally { http.close() }
    }
    @Test fun serviceUnavailableRejectsSave() = runTest {
        val http = client("",HttpStatusCode.ServiceUnavailable)
        try { assertFalse(ApiService(http).saveMark(GeoMark("a",0.0,0.0))) } finally { http.close() }
    }
    @Test fun unauthorizedSaveIsRejected() = runTest {
        val http = client("",HttpStatusCode.Unauthorized)
        try { assertFalse(ApiService(http).saveMark(GeoMark("a",0.0,0.0))) } finally { http.close() }
    }
}
