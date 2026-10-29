package com.example.placy

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhotoPublisherTest {
    @Test fun successfulPublishPreservesImageIdAndCoordinates() = runTest {
        var uploaded: ByteArray? = null
        var uploadedId = ""
        var saved: GeoMark? = null
        val publisher = PhotoPublisher({ bytes, id -> uploaded = bytes; uploadedId = id; true }, { saved = it; true })
        val bytes = byteArrayOf(0, 1, -1)
        assertEquals(PublishResult.Success, publisher.publish(bytes, "photo-1", 59.9, 30.3))
        assertContentEquals(bytes, uploaded)
        assertEquals("photo-1", uploadedId)
        assertEquals(GeoMark("photo-1", 59.9, 30.3), saved)
    }
    @Test fun uploadFailureDoesNotSaveMetadata() = runTest {
        var saved = false
        val publisher = PhotoPublisher({ _, _ -> false }, { saved = true; true })
        assertEquals(PublishResult.UploadFailed, publisher.publish(byteArrayOf(1), "a", 1.0, 2.0))
        assertFalse(saved)
    }
    @Test fun metadataFailureIsNotReportedAsSuccess() = runTest {
        var uploaded = false
        val publisher = PhotoPublisher({ _, _ -> uploaded = true; true }, { false })
        assertEquals(PublishResult.MetadataFailed, publisher.publish(byteArrayOf(1), "a", 1.0, 2.0))
        assertTrue(uploaded)
    }
    @Test fun uploadFinishesBeforeMetadataIsSaved() = runTest {
        val events = mutableListOf<String>()
        val publisher = PhotoPublisher({ _, _ -> events += "upload"; true }, { events += "save"; true })
        publisher.publish(byteArrayOf(1), "a", 1.0, 2.0)
        assertEquals(listOf("upload", "save"), events)
    }
    @Test fun distinctSubmissionsKeepTheirOwnIdentifiers() = runTest {
        val marks = mutableListOf<GeoMark>()
        val publisher = PhotoPublisher({ _, _ -> true }, { marks += it; true })
        publisher.publish(byteArrayOf(1), "a", 1.0, 2.0)
        publisher.publish(byteArrayOf(2), "b", 3.0, 4.0)
        assertEquals(listOf(GeoMark("a", 1.0, 2.0), GeoMark("b", 3.0, 4.0)), marks)
    }
    @Test fun uploadExceptionPropagatesAndSkipsSave() = runTest {
        var saved = false
        val publisher = PhotoPublisher({ _, _ -> throw IllegalStateException("upload") }, { saved = true; true })
        assertFailsWith<IllegalStateException> { publisher.publish(byteArrayOf(1), "a", 1.0, 2.0) }
        assertFalse(saved)
    }
    @Test fun metadataExceptionPropagatesAfterUpload() = runTest {
        var uploaded = false
        val publisher = PhotoPublisher({ _, _ -> uploaded = true; true }, { throw IllegalStateException("save") })
        assertFailsWith<IllegalStateException> { publisher.publish(byteArrayOf(1), "a", 1.0, 2.0) }
        assertTrue(uploaded)
    }
}
