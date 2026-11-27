package com.example.placy

import kotlinx.coroutines.CancellationException

/** Returns the message displayed by the camera, or null after a successful publication. */
internal suspend fun publishCapturedPhoto(
    load: suspend () -> ByteArray,
    publish: suspend (ByteArray) -> PublishResult,
): String? = try {
    when (publish(load())) {
        PublishResult.Success -> null
        PublishResult.EmptyImage -> "Фотография не содержит данных"
        PublishResult.InvalidPhotoId -> "Некорректный идентификатор фотографии"
        PublishResult.InvalidCoordinates -> "Некорректные координаты"
        PublishResult.UploadFailed -> "Не удалось загрузить фотографию"
        PublishResult.MetadataFailed -> "Не удалось сохранить публикацию"
    }
} catch (error: CancellationException) {
    throw error
} catch (_: Exception) {
    "Не удалось обработать фотографию"
}
