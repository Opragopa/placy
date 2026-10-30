package com.example.placy

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhotoInputTest {
    private fun publisher() = PhotoPublisher({ _, _ -> true }, { true })
    @Test fun emptyPhotoIsRejectedBeforeUpload() = runTest {
        var called = false
        val service = PhotoPublisher({ _, _ -> called = true; true }, { true })
        assertEquals(PublishResult.EmptyImage, service.publish(byteArrayOf(), "a", 1.0, 2.0))
        assertFalse(called)
    }
    @Test fun emptyPhotoIdIsRejected() = runTest {
        assertEquals(PublishResult.InvalidPhotoId, publisher().publish(byteArrayOf(1), "", 1.0, 2.0))
    }
    @Test fun whitespaceOnlyPhotoIdIsRejected() = runTest {
        assertEquals(PublishResult.InvalidPhotoId, publisher().publish(byteArrayOf(1), "  ", 1.0, 2.0))
    }
    @Test fun forwardSlashInPhotoIdIsRejected() = runTest {
        assertEquals(PublishResult.InvalidPhotoId, publisher().publish(byteArrayOf(1), "a/b", 1.0, 2.0))
    }
    @Test fun backslashInPhotoIdIsRejected() = runTest {
        assertEquals(PublishResult.InvalidPhotoId, publisher().publish(byteArrayOf(1), "a\\b", 1.0, 2.0))
    }
    @Test fun questionMarkInPhotoIdIsRejected() = runTest {
        assertEquals(PublishResult.InvalidPhotoId, publisher().publish(byteArrayOf(1), "a?b", 1.0, 2.0))
    }
    @Test fun fragmentMarkerInPhotoIdIsRejected() = runTest {
        assertEquals(PublishResult.InvalidPhotoId, publisher().publish(byteArrayOf(1), "a#b", 1.0, 2.0))
    }
    @Test fun controlCharacterInPhotoIdIsRejected() = runTest {
        assertEquals(PublishResult.InvalidPhotoId, publisher().publish(byteArrayOf(1), "a\nb", 1.0, 2.0))
    }
    @Test fun validUuidPhotoIdIsAccepted() = runTest {
        assertEquals(PublishResult.Success, publisher().publish(byteArrayOf(1), "79312fd3-dc3c-4839-bab9-0ad9903bd808", 1.0, 2.0))
    }
    @Test fun invalidIdDoesNotReachUploadOrMetadata() = runTest {
        var calls = 0
        val service = PhotoPublisher({ _, _ -> calls++; true }, { calls++; true })
        assertEquals(PublishResult.InvalidPhotoId, service.publish(byteArrayOf(1), "../x", 1.0, 2.0))
        assertTrue(calls == 0)
    }
}
