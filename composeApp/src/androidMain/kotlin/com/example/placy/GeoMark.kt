package com.example.placy

import kotlinx.serialization.Serializable

@Serializable
data class GeoMark(
    val photoUUID: String,
    val latitude: Double,
    val longitude: Double
)
/** Latitude and longitude accepted by the map must be finite geographic coordinates. */
internal fun GeoMark.hasValidCoordinates(): Boolean =
    latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0
