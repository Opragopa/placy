package com.example.placy

import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.platform.app.InstrumentationRegistry
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

    private fun openFixtureCard() {
        compose.waitUntil(timeoutMillis = 15000) {
            compose.onAllNodes(androidx.compose.ui.test.hasContentDescription("Карта, публикаций: 1"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("map-screen").performTouchInput {
            click(Offset(center.x, center.y - 5f))
        }
        compose.onNodeWithTag("post-card").assertIsDisplayed()
    }

    // E2E-08 / F_Post_03: selecting the map marker opens a card.
    @Test fun markerOpensPublicationCard() = isolatedLaunch { openFixtureCard() }

    // E2E-09 / F_Post_04: selecting the photo opens the fullscreen viewer.
    @Test fun cardPhotoOpensFullscreen() = isolatedLaunch {
        openFixtureCard()
        compose.onNodeWithContentDescription("Фотография публикации").performClick()
        compose.onNodeWithTag("fullscreen-photo").assertIsDisplayed()
    }

    // E2E-11 / F_Post_06: Android back dismisses the card.
    @Test fun systemBackClosesCard() = isolatedLaunch {
        openFixtureCard()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithTag("post-card").assertDoesNotExist()
        compose.onNodeWithTag("map-screen").assertIsDisplayed()
    }

    // E2E-12 / F_View_02: tapping the fullscreen photo returns to the map.
    @Test fun fullscreenTapReturnsToMap() = isolatedLaunch {
        openFixtureCard()
        compose.onNodeWithContentDescription("Фотография публикации").performClick()
        compose.onNodeWithTag("fullscreen-photo").assertIsDisplayed().performClick()
        compose.onNodeWithTag("fullscreen-photo").assertDoesNotExist()
        compose.onNodeWithTag("post-card").assertDoesNotExist()
        compose.onNodeWithTag("map-screen").assertIsDisplayed()
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
