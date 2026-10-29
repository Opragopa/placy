package com.example.placy

import kotlinx.coroutines.CancellationException
import io.ktor.client.HttpClient
import io.ktor.client.request.put
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class NextcloudClient(
    private val client: HttpClient,
    private val baseUrl: String = BuildConfig.SERVER_URL,
    private val user: String = BuildConfig.USERNAME,
) {

    suspend fun uploadImage(imageData: ByteArray, filename: String): Boolean {
        val url = "$baseUrl/remote.php/dav/files/$user/$filename.jpg"

        try {
            val response: HttpResponse = client.put(url) {
                setBody(imageData)
                contentType(ContentType.Image.JPEG)
            }

            return response.status.isSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun downloadImage(remotePath: String): ByteArray? {
        val cleanPath = remotePath.trimStart('/')
        val url = "$baseUrl/remote.php/dav/files/$user/$cleanPath"

        return try {
            val response: HttpResponse = client.get(url)

            if (response.status.isSuccess()) {
                response.readRawBytes()
            } else {
                println("Download failed: ${response.status}")
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}