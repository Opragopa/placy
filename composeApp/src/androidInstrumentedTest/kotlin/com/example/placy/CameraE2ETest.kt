package com.example.placy

import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** System-level test: no Compose test rule or test-controlled recomposer is installed. */
class CameraE2ETest {
    @get:Rule val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CAMERA
    )

    @Test fun cancellingCameraReturnsWithoutPublishing() {
        assertTrue(BuildConfig.API_URL.startsWith("http://10.0.2.2:"))
        assertTrue(BuildConfig.SERVER_URL.startsWith("http://10.0.2.2:"))
        assertTrue(BuildConfig.MAPKIT_API_KEY.isNotBlank())
        val before = URL("http://10.0.2.2:18080/test-events").readText().toInt()
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        ActivityScenario.launch(MainActivity::class.java).use {
            val camera = requireNotNull(device.wait(Until.findObject(By.desc("Камера")), 15000)) {
                "Map camera button must be available"
            }
            camera.click()
            assertTrue("Camera screen did not open", device.wait(Until.hasObject(By.desc("Камера готова")), 20000))
            device.pressBack()
            assertTrue("Map did not return after cancellation", device.wait(Until.hasObject(By.desc("Камера")), 15000))
            assertEquals("Cancelling must not write to the server", before,
                URL("http://10.0.2.2:18080/test-events").readText().toInt())
        }
    }
}
