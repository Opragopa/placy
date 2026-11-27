package com.example.placy

import org.junit.Test
import kotlin.test.assertEquals

class MapPresentationTest {
    @Test fun usesSmallMarkerBelowFirstBoundary() {
        assertEquals(0.3f, publicationMarkerScale(7.99f))
    }

    @Test fun usesMediumMarkerAtFirstBoundary() {
        assertEquals(0.4f, publicationMarkerScale(8f))
        assertEquals(0.4f, publicationMarkerScale(12.99f))
    }

    @Test fun usesLargeMarkerAtSecondBoundary() {
        assertEquals(0.5f, publicationMarkerScale(13f))
    }
}
