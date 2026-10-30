package com.example.placy

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeoMarkValidationTest {
    private fun mark(lat: Double, lon: Double) = GeoMark("id", lat, lon)
    @Test fun equatorAndPrimeMeridianAreValid() = assertTrue(mark(0.0, 0.0).hasValidCoordinates())
    @Test fun northPoleIsValid() = assertTrue(mark(90.0, 0.0).hasValidCoordinates())
    @Test fun southPoleIsValid() = assertTrue(mark(-90.0, 0.0).hasValidCoordinates())
    @Test fun easternAntimeridianIsValid() = assertTrue(mark(0.0, 180.0).hasValidCoordinates())
    @Test fun westernAntimeridianIsValid() = assertTrue(mark(0.0, -180.0).hasValidCoordinates())
    @Test fun latitudeAboveNorthPoleIsRejected() = assertFalse(mark(90.00001, 0.0).hasValidCoordinates())
    @Test fun latitudeBelowSouthPoleIsRejected() = assertFalse(mark(-90.00001, 0.0).hasValidCoordinates())
    @Test fun longitudeEastOfRangeIsRejected() = assertFalse(mark(0.0, 180.00001).hasValidCoordinates())
    @Test fun longitudeWestOfRangeIsRejected() = assertFalse(mark(0.0, -180.00001).hasValidCoordinates())
    @Test fun nanLatitudeIsRejected() = assertFalse(mark(Double.NaN, 0.0).hasValidCoordinates())
    @Test fun nanLongitudeIsRejected() = assertFalse(mark(0.0, Double.NaN).hasValidCoordinates())
    @Test fun positiveInfiniteLatitudeIsRejected() = assertFalse(mark(Double.POSITIVE_INFINITY, 0.0).hasValidCoordinates())
    @Test fun negativeInfiniteLongitudeIsRejected() = assertFalse(mark(0.0, Double.NEGATIVE_INFINITY).hasValidCoordinates())
    @Test fun invalidCoordinatesAreNotSubmitted() = runTest {
        var calls = 0
        val http = HttpClient(MockEngine { calls++; respond("", HttpStatusCode.Created) })
        try {
            assertFalse(ApiService(http).saveMark(mark(91.0, 0.0)))
            assertEquals(0, calls)
        } finally { http.close() }
    }
    @Test fun invalidServerCoordinatesAreExcludedWithoutDiscardingValidMarks() = runTest {
        val http = HttpClient(MockEngine {
            respond("""[{"photoUUID":"ok","latitude":90.0,"longitude":180.0},{"photoUUID":"bad","latitude":91.0,"longitude":0.0}]""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json() } }
        try { assertEquals(listOf(GeoMark("ok", 90.0, 180.0)), ApiService(http).getAllMarks()) }
        finally { http.close() }
    }
}
