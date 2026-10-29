package com.example.placy

import android.Manifest
import android.view.KeyEvent
import android.view.MotionEvent
import android.os.SystemClock
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.pinch
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import java.net.URL
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
    @get:Rule val permissions = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA
    )

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

    private fun saveScreenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(requireNotNull(instrumentation.targetContext.getExternalFilesDir(null)), "e2e-screenshots")
        check(directory.isDirectory || directory.mkdirs())
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            FileOutputStream(File(directory, "$name.png")).use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
            }
        } finally { bitmap.recycle() }
    }

    // E2E-08 / F_Post_03: selecting the map marker opens a card.
    @Test fun markerOpensPublicationCard() = isolatedLaunch { openFixtureCard(); saveScreenshot("card") }

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

    private fun cameraSnapshot(): String =
        compose.onNodeWithTag("map-screen").fetchSemanticsNode().config[MapCameraSnapshotKey]

    // E2E-03 / F_Map_02: dragging changes the visible map position.
    @Test fun dragMovesTheMap() = isolatedLaunch {
        val before = cameraSnapshot()
        compose.onNodeWithTag("map-screen").performTouchInput {
            swipe(Offset(center.x, center.y), Offset(center.x + 100f, center.y + 40f), 500)
        }
        compose.waitUntil(timeoutMillis = 10000) { cameraSnapshot() != before }
    }

    // E2E-04 / F_Map_03: spreading two fingers changes the map zoom.
    @Test fun pinchChangesTheMapScale() = isolatedLaunch {
        val before = cameraSnapshot()
        compose.onNodeWithTag("map-screen").performTouchInput {
            pinch(Offset(center.x - 30f, center.y), Offset(center.x - 120f, center.y),
                Offset(center.x + 30f, center.y), Offset(center.x + 120f, center.y), 600)
        }
        compose.waitUntil(timeoutMillis = 10000) { cameraSnapshot() != before }
    }

    // E2E-10 / F_Post_05: a tap outside the card dismisses it.
    @Test fun tappingOutsideClosesCard() = isolatedLaunch {
        openFixtureCard()
        val now = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 10f, 120f, 0)
        val up = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, 10f, 120f, 0)
        try {
            InstrumentationRegistry.getInstrumentation().sendPointerSync(down)
            InstrumentationRegistry.getInstrumentation().sendPointerSync(up)
        } finally { down.recycle(); up.recycle() }
        compose.onNodeWithTag("post-card").assertDoesNotExist()
        compose.onNodeWithTag("map-screen").assertIsDisplayed()
    }

    // E2E-13 / F_View_03: closing the photo retains camera position and zoom.
    @Test fun closingPhotoPreservesMapCamera() = isolatedLaunch {
        compose.waitUntil(timeoutMillis = 15000) {
            compose.onAllNodes(androidx.compose.ui.test.hasContentDescription("Карта, публикаций: 1"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        val before = cameraSnapshot()
        openFixtureCard()
        compose.onNodeWithContentDescription("Фотография публикации").performClick()
        compose.onNodeWithTag("fullscreen-photo").assertIsDisplayed().performClick()
        compose.onNodeWithTag("map-screen").assertIsDisplayed()
        kotlin.test.assertEquals(before, cameraSnapshot())
    }

    private fun screenshotRedBounds(): IntArray? {
        val bmp = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            var left = bmp.width; var top = bmp.height; var right = -1; var bottom = -1
            for (y in 0 until bmp.height step 3) for (x in 0 until bmp.width step 3) {
                val px = bmp.getPixel(x, y)
                val red = android.graphics.Color.red(px)
                val green = android.graphics.Color.green(px)
                val blue = android.graphics.Color.blue(px)
                if (red > 180 && green < 70 && blue < 70) {
                    left = minOf(left, x); right = maxOf(right, x)
                    top = minOf(top, y); bottom = maxOf(bottom, y)
                }
            }
            return if (right >= left && bottom >= top) intArrayOf(left, top, right, bottom) else null
        } finally { bmp.recycle() }
    }

    // E2E-06 / F_Map_05: a red pin is visible near the known map coordinate.
    @Test fun publishedMarkerIsRedAndVisible() = isolatedLaunch {
        compose.waitUntil(timeoutMillis = 15000) {
            compose.onAllNodes(androidx.compose.ui.test.hasContentDescription("Карта, публикаций: 1"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        var bounds: IntArray? = null
        compose.waitUntil(timeoutMillis = 15000) { bounds = screenshotRedBounds(); bounds != null }
        val map = compose.onNodeWithTag("map-screen").fetchSemanticsNode().boundsInRoot
        val b = requireNotNull(bounds)
        assertTrue(b[0] <= map.center.x && b[2] >= map.center.x)
        assertTrue(b[1] < map.center.y && b[3] <= map.center.y + 8f)
    }

    // F_View_01: the fixture is a 2:1 image, and its rendered rectangle must retain that ratio.
    @Test fun fullscreenPhotoRetainsItsAspectRatio() = isolatedLaunch {
        openFixtureCard()
        compose.onNodeWithContentDescription("Фотография публикации").performClick()
        compose.onNodeWithTag("fullscreen-photo").assertIsDisplayed()
        var bounds: IntArray? = null
        compose.waitUntil(timeoutMillis = 15000) {
            bounds = screenshotRedBounds()
            val b = bounds
            b != null && b[2] - b[0] > 100 && b[3] - b[1] > 50
        }
        val b = requireNotNull(bounds)
        val ratio = (b[2] - b[0]).toDouble() / (b[3] - b[1]).toDouble()
        assertTrue(ratio in 1.85..2.15, "Photo aspect ratio was $ratio")
        saveScreenshot("fullscreen")
    }

    // F_Auth_01: the map and publication can be viewed without entering credentials.
    @Test fun guestCanViewMapAndPublication() = isolatedLaunch {
        compose.onNodeWithTag("map-screen").assertIsDisplayed()
        openFixtureCard()
        compose.onNodeWithContentDescription("Фотография публикации").assertIsDisplayed()
    }

    // E2E-14 / F_Post_07: cancelling the actual capture flow creates no server-side writes.
    @Test fun cancellingCameraReturnsWithoutPublishing() = isolatedLaunch {
        val before = URL("http://10.0.2.2:18080/test-events").readText().toInt()
        compose.onNodeWithContentDescription("Камера").performClick()
        compose.waitUntil(timeoutMillis = 20000) {
            compose.onAllNodes(androidx.compose.ui.test.hasTestTag("map-screen")).fetchSemanticsNodes().isEmpty()
        }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitUntil(timeoutMillis = 15000) {
            compose.onAllNodes(androidx.compose.ui.test.hasTestTag("map-screen")).fetchSemanticsNodes().isNotEmpty()
        }
        kotlin.test.assertEquals(before, URL("http://10.0.2.2:18080/test-events").readText().toInt())
    }

    // E2E-01 / F_Map_01: application starts on the map.
    @Test fun mapIsDisplayedAtLaunch() = isolatedLaunch {
        compose.onNodeWithTag("map-screen").assertIsDisplayed()
        saveScreenshot("map")
    }

    // A second launch in the same process must not reinitialize MapKit with a new key.
    @Test fun reopeningActivityDoesNotCrashMap() {
        isolatedLaunch { compose.onNodeWithTag("map-screen").assertIsDisplayed() }
        isolatedLaunch { compose.onNodeWithTag("map-screen").assertIsDisplayed() }
    }

    // E2E-07 / F_Map_06: the camera control is visible from the map.
    @Test fun cameraControlIsDisplayedOnMap() = isolatedLaunch {
        compose.onNodeWithContentDescription("Камера").assertIsDisplayed()
    }
}
