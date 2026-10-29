package com.example.placy

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Real HTTP transport against a local controlled server; no production services or MapKit. */
class HttpClientIntegrationTest {
    private lateinit var server: MockWebServer
    private lateinit var http: HttpClient
    private lateinit var api: ApiService
    private lateinit var photos: NextcloudClient

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        http = HttpClient(CIO) { install(ContentNegotiation) { json() } }
        val url = server.url("/").toString().removeSuffix("/")
        api = ApiService(http, url)
        photos = NextcloudClient(http, url, "test-user")
    }

    @After fun tearDown() { http.close(); server.shutdown() }

    private fun json(body: String, code: Int = 200) = MockResponse()
        .setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private fun request() = assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))

    // INT-01, F_Map_04: transport/deserialization only; visible-area filtering is absent.
    @Test fun loadsMarksOverHttp() = runTest {
        server.enqueue(json("""[{"photoUUID":"photo-a","latitude":59.9,"longitude":30.3}]"""))
        assertEquals(listOf(GeoMark("photo-a", 59.9, 30.3)), api.getAllMarks())
        val sent = request()
        assertEquals("GET", sent.method)
        assertEquals("/geomarks", sent.path)
    }

    // INT-05, F_Post_08: verify the two HTTP client contracts and shared photo identifier.
    // This does not exercise CameraView or establish end-to-end publication.
    @Test fun imageAndMetadataUseSameIdentifier() = runTest {
        server.enqueue(MockResponse().setResponseCode(201))
        server.enqueue(MockResponse().setResponseCode(201))
        val bytes = byteArrayOf(1, 2, 3, 4)
        assertTrue(photos.uploadImage(bytes, "photo-a"))
        assertTrue(api.saveMark(GeoMark("photo-a", 59.9, 30.3)))
        val upload = request()
        assertEquals("PUT", upload.method)
        assertEquals("/remote.php/dav/files/test-user/photo-a.jpg", upload.path)
        assertContentEquals(bytes, upload.body.readByteArray())
        val metadata = request()
        assertEquals("POST", metadata.method)
        assertEquals("/geomarks", metadata.path)
        val body = metadata.body.readUtf8()
        assertTrue(body.contains("\"photoUUID\":\"photo-a\""))
        assertTrue(body.contains("59.9"))
        assertTrue(body.contains("30.3"))
    }

    // F_Post_04: the image client can retrieve the exact bytes needed by the viewer;
    // rendering and the missing publication card are not covered.
    @Test fun downloadsPhotoOverHttp() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(okio.Buffer().write(byteArrayOf(1, 2, 3))))
        assertContentEquals(byteArrayOf(1, 2, 3), photos.downloadImage("/photo-a.jpg"))
        assertEquals("/remote.php/dav/files/test-user/photo-a.jpg", request().path)
    }

    @Test fun uploadFailureIsReportedByClient() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        assertFalse(photos.uploadImage(byteArrayOf(1), "photo-a"))
        assertEquals("PUT", request().method)
    }

    @Test fun missingPhotoIsReportedByClient() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        assertEquals(null, photos.downloadImage("missing.jpg"))
        assertEquals("GET", request().method)
    }

    @Test fun malformedMarksResponseIsReportedAsEmptyByCurrentClient() = runTest {
        server.enqueue(json("invalid"))
        assertTrue(api.getAllMarks().isEmpty())
        assertEquals("/geomarks", request().path)
    }
}
