package com.example.placy

/** Publication flow shared by the camera and deterministic tests. */
internal class PhotoPublisher(
    private val upload: suspend (ByteArray, String) -> Boolean,
    private val save: suspend (GeoMark) -> Boolean,
) {
    suspend fun publish(bytes: ByteArray, photoId: String, latitude: Double, longitude: Double): PublishResult {
        if (!upload(bytes, photoId)) return PublishResult.UploadFailed
        return if (save(GeoMark(photoId, latitude, longitude))) PublishResult.Success else PublishResult.MetadataFailed
    }
}

internal enum class PublishResult { Success, UploadFailed, MetadataFailed }
