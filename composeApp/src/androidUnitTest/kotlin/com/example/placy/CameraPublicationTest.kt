package com.example.placy

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class CameraPublicationTest {
    @Test fun forwardsImageBytesAndReportsSuccess() = runTest {
        val photo = byteArrayOf(7, 8, 9)
        val error = publishCapturedPhoto(
            load = { photo },
            publish = { bytes ->
                assertContentEquals(photo, bytes)
                PublishResult.Success
            }
        )
        assertNull(error)
    }

    @Test fun showsTheMessageForEachRejectedPublication() = runTest {
        val expected = mapOf(
            PublishResult.EmptyImage to "Фотография не содержит данных",
            PublishResult.InvalidPhotoId to "Некорректный идентификатор фотографии",
            PublishResult.InvalidCoordinates to "Некорректные координаты",
            PublishResult.UploadFailed to "Не удалось загрузить фотографию",
            PublishResult.MetadataFailed to "Не удалось сохранить публикацию",
        )
        for ((result, message) in expected) {
            assertEquals(message, publishCapturedPhoto({ byteArrayOf(1) }, { result }), result.name)
        }
    }

    @Test fun doesNotPublishWhenReadingTheImageFails() = runTest {
        var attempted = false
        val error = publishCapturedPhoto(
            load = { throw IllegalStateException("unreadable") },
            publish = { attempted = true; PublishResult.Success }
        )
        assertEquals("Не удалось обработать фотографию", error)
        assertFalse(attempted)
    }

    @Test fun reportsUnexpectedPublicationFailure() = runTest {
        assertEquals(
            "Не удалось обработать фотографию",
            publishCapturedPhoto({ byteArrayOf(1) }, { throw IllegalStateException("offline") })
        )
    }

    @Test fun propagatesCancellationWithoutPresentingAnError() = runTest {
        val error = assertFailsWith<CancellationException> {
            publishCapturedPhoto({ byteArrayOf(1) }, { throw CancellationException("cancelled") })
        }
        assertEquals("cancelled", error.message)
    }
}
