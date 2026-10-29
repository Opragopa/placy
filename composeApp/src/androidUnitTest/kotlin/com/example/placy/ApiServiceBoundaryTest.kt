package com.example.placy

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApiServiceBoundaryTest {
    private fun client(body: String, status: HttpStatusCode = HttpStatusCode.OK) = HttpClient(MockEngine) {
        engine {
            addHandler { respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())) }
        }
        install(ContentNegotiation) { json() }
    }

    @Test fun multipleMarksPreserveCoordinatesAndOrder() = runTest {
        val http = client("""[{"photoUUID":"a","latitude":-90.0,"longitude":-180.0},{"photoUUID":"b","latitude":90.0,"longitude":180.0}]""")
        try {
            assertEquals(listOf(GeoMark("a", -90.0, -180.0), GeoMark("b", 90.0, 180.0)), ApiService(http).getAllMarks())
        } finally { http.close() }
    }

    @Test fun malformedJsonReturnsEmptyList() = runTest {
        val http = client("not-json")
        try { assertTrue(ApiService(http).getAllMarks().isEmpty()) } finally { http.close() }
    }

    @Test fun missingRequiredCoordinateReturnsEmptyList() = runTest {
        val http = client("""[{"photoUUID":"a","latitude":1.0}]""")
        try { assertTrue(ApiService(http).getAllMarks().isEmpty()) } finally { http.close() }
    }

    @Test fun saveAcceptsLowerSuccessBoundary() = runTest {
        val http = client("", HttpStatusCode.OK)
        try { assertTrue(ApiService(http).saveMark(GeoMark("a", 1.0, 2.0))) } finally { http.close() }
    }

    @Test fun saveRejectsRedirectBoundary() = runTest {
        val http = client("", HttpStatusCode.MultipleChoices)
        try { assertFalse(ApiService(http).saveMark(GeoMark("a", 1.0, 2.0))) } finally { http.close() }
    }

    @Test fun saveRejectsServerFailure() = runTest {
        val http = client("", HttpStatusCode.InternalServerError)
        try { assertFalse(ApiService(http).saveMark(GeoMark("a", 1.0, 2.0))) } finally { http.close() }
    }
}
