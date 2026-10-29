package com.example.placy

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

/** Launches the real app. The emulator must reach an isolated fixture server at 10.0.2.2. */
class MapE2ETest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun isolatedLaunch(block: () -> Unit) {
        // Fail before launching the app if its metadata endpoint points at a real service.
        assertTrue(BuildConfig.API_URL.startsWith("http://10.0.2.2:"), "Configure API_URL to an isolated emulator fixture server")
        assertTrue(BuildConfig.SERVER_URL.startsWith("http://10.0.2.2:"), "Configure SERVER_URL to an isolated emulator fixture server")
        assertTrue(BuildConfig.MAPKIT_API_KEY.isNotBlank(), "A test MapKit key is required")
        ActivityScenario.launch(MainActivity::class.java).use { block() }
    }

    // E2E-01 / F_Map_01: application starts on the map.
    @Test fun mapIsDisplayedAtLaunch() = isolatedLaunch {
        compose.onNodeWithTag("map-screen").assertIsDisplayed()
    }

    // E2E-07 / F_Map_06: the camera control is visible from the map.
    @Test fun cameraControlIsDisplayedOnMap() = isolatedLaunch {
        compose.onNodeWithContentDescription("Камера").assertIsDisplayed()
    }
}
