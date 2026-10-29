package com.example.placy

/** Publication flow shared by the camera and deterministic tests. */
internal class PhotoPublisher(
    private val upload: suspend (ByteArray, String) -> Boolean,
    private val save: suspend (GeoMark) -> Boolean,
) {
    suspend fun publish(bytes: ByteArray, photoId: String, latitude: Double, longitude: Double): PublishResult {
        if (bytes.isEmpty()) return PublishResult.EmptyImage
        if (photoId.isBlank() || photoId.any { it in "/\\?#" || it.isISOControl() }) return PublishResult.InvalidPhotoId
        val mark = GeoMark(photoId, latitude, longitude)
        if (!mark.hasValidCoordinates()) return PublishResult.InvalidCoordinates
        if (!upload(bytes, photoId)) return PublishResult.UploadFailed
        return if (save(mark)) PublishResult.Success else PublishResult.MetadataFailed
    }
}

internal enum class PublishResult { Success, EmptyImage, InvalidPhotoId, InvalidCoordinates, UploadFailed, MetadataFailed }
