package com.example.placy

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClientContractUnitTest {
    @Test fun saveSendsCoordinatesAsJson() = runTest {
        var body = ""
        val http = HttpClient(MockEngine { request ->
            body = request.body.toByteArray().decodeToString()
            respond("", HttpStatusCode.Created)
        }) { install(ContentNegotiation) { json() } }
        try {
            assertTrue(ApiService(http).saveMark(GeoMark("photo-1", -12.5, 180.0)))
            val parsed = Json.parseToJsonElement(body).jsonObject
            assertEquals("photo-1", parsed.getValue("photoUUID").jsonPrimitive.content)
            assertEquals("-12.5", parsed.getValue("latitude").jsonPrimitive.content)
            assertEquals("180.0", parsed.getValue("longitude").jsonPrimitive.content)
        } finally { http.close() }
    }

    @Test fun saveUsesPostAndJsonContentType() = runTest {
        var method = ""
        var contentType: ContentType? = null
        val http = HttpClient(MockEngine { request ->
            method = request.method.value
            contentType = request.body.contentType
            respond("", HttpStatusCode.Created)
        }) { install(ContentNegotiation) { json() } }
        try {
            assertTrue(ApiService(http).saveMark(GeoMark("photo-1", 0.0, 0.0)))
            assertEquals("POST", method)
            assertEquals(ContentType.Application.Json, contentType?.withoutParameters())
        } finally { http.close() }
    }

    @Test fun getUsesConfiguredEndpoint() = runTest {
        var url = ""
        val http = HttpClient(MockEngine { request ->
            url = request.url.toString()
            respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json() } }
        try {
            assertTrue(ApiService(http, "https://example.invalid").getAllMarks().isEmpty())
            assertEquals("https://example.invalid/geomarks", url)
        } finally { http.close() }
    }

    @Test fun emptyDownloadResponseReturnsEmptyBytes() = runTest {
        val http = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        try {
            val bytes = NextcloudClient(http).downloadImage("empty.jpg")
            assertNotNull(bytes)
            assertTrue(bytes.isEmpty())
        } finally { http.close() }
    }

    @Test fun downloadRemovesAllLeadingSlashes() = runTest {
        var path = ""
        val http = HttpClient(MockEngine { request ->
            path = request.url.encodedPath
            respond("ok", HttpStatusCode.OK)
        })
        try {
            assertNotNull(NextcloudClient(http, "https://example.invalid", "alice").downloadImage("///dir/a.jpg"))
            assertEquals("/remote.php/dav/files/alice/dir/a.jpg", path)
        } finally { http.close() }
    }

    @Test fun uploadUsesJpegAndPreservesBytes() = runTest {
        var method = ""
        var body = byteArrayOf()
        var type: ContentType? = null
        val http = HttpClient(MockEngine { request ->
            method = request.method.value
            body = request.body.toByteArray()
            type = request.body.contentType
            respond("", HttpStatusCode.Created)
        })
        try {
            val expected = byteArrayOf(0, 1, -1)
            assertTrue(NextcloudClient(http).uploadImage(expected, "photo"))
            assertEquals("PUT", method)
            assertEquals(ContentType.Image.JPEG, type)
            assertContentEquals(expected, body)
        } finally { http.close() }
    }

    @Test fun uploadRejectsUnauthorizedResponse() = runTest {
        val http = HttpClient(MockEngine { respond("", HttpStatusCode.Unauthorized) })
        try { assertFalse(NextcloudClient(http).uploadImage(byteArrayOf(1), "photo")) } finally { http.close() }
    }

    @Test fun downloadRejectsUnauthorizedResponse() = runTest {
        val http = HttpClient(MockEngine { respond("", HttpStatusCode.Unauthorized) })
        try { assertNull(NextcloudClient(http).downloadImage("photo.jpg")) } finally { http.close() }
    }
}
