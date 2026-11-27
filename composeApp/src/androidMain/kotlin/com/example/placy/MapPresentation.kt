package com.example.placy

internal fun publicationMarkerScale(zoom: Float): Float = when {
    zoom < 8f -> 0.3f
    zoom < 13f -> 0.4f
    else -> 0.5f
}
