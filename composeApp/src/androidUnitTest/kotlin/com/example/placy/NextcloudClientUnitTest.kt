package com.example.placy

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NextcloudClientUnitTest {
    @Test
    fun testUploadImageSuccess() = runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    assertEquals("PUT", request.method.value)
                    assertTrue(request.url.toString().contains("test.jpg"))
                    respond("", HttpStatusCode.Created)
                }
            }
        }
        val nextcloudClient = NextcloudClient(client)
        val imageData = byteArrayOf(0x1, 0x2, 0x3)

        val result = nextcloudClient.uploadImage(imageData, "test")

        assertTrue(result)
    }

    @Test
    fun testUploadImageFailure() = runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond("", HttpStatusCode.BadRequest)
                }
            }
        }
        val nextcloudClient = NextcloudClient(client)
        val imageData = byteArrayOf(0x1, 0x2, 0x3)

        val result = nextcloudClient.uploadImage(imageData, "test")

        assertFalse(result)
    }

    @Test
    fun testUploadImageException() = runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    throw Exception("Upload failed")
                }
            }
        }
        val nextcloudClient = NextcloudClient(client)
        val imageData = byteArrayOf(0x1, 0x2, 0x3)

        val result = nextcloudClient.uploadImage(imageData, "test")

        assertFalse(result)
    }

    @Test
    fun testDownloadImageSuccess() = runTest {
        val expectedData = byteArrayOf(0x4, 0x5, 0x6)
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    assertEquals("GET", request.method.value)
                    respond(
                        content = expectedData,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Image.JPEG.toString())
                    )
                }
            }
        }
        val nextcloudClient = NextcloudClient(client)

        val result = nextcloudClient.downloadImage("test.jpg")

        assertEquals(expectedData.size, result?.size)
        assertTrue(expectedData.contentEquals(result ?: byteArrayOf()))
    }

    @Test
    fun testDownloadImageFailure() = runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond("", HttpStatusCode.NotFound)
                }
            }
        }
        val nextcloudClient = NextcloudClient(client)

        val result = nextcloudClient.downloadImage("nonexistent.jpg")

        assertNull(result)
    }

    @Test
    fun testDownloadImageException() = runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    throw Exception("Download failed")
                }
            }
        }
        val nextcloudClient = NextcloudClient(client)

        val result = nextcloudClient.downloadImage("test.jpg")

        assertNull(result)
    }

    @Test
    fun testDownloadImagePathCleaning() = runTest {
        var capturedUrl = ""
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    capturedUrl = request.url.toString()
                    respond(
                        content = "test".toByteArray(),
                        status = HttpStatusCode.OK
                    )
                }
            }
        }

        val nextcloudClient = NextcloudClient(client)

        nextcloudClient.downloadImage(remotePath = "/test.jpg")

        println("Captured URL: $capturedUrl")
        assertTrue(capturedUrl.contains("/remote.php/dav/files/"))
        assertFalse(capturedUrl.contains("/remote.php/dav/files//"))

    }
}