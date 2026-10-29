package com.example.placy

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

/** Component integration checks; these are not counted as full end-to-end scenarios. */
class PostCardUiTest {
    @get:Rule val compose = createComposeRule()
    private val loader by lazy { ImageLoader.Builder(ApplicationProvider.getApplicationContext<Context>()).build() }

    @Test fun cardShowsImageAndOpensPhoto() {
        var clicks = 0
        compose.setContent {
            PostCard("http://10.0.2.2:18080/photo.jpg", loader, { clicks++ }, {})
        }
        compose.onNodeWithTag("post-card").assertIsDisplayed()
        compose.onNodeWithContentDescription("Фотография публикации").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun photoOpensAndDismissesFullscreenViewer() {
        var fullscreen by mutableStateOf(false)
        compose.setContent {
            if (fullscreen) {
                SimpleFullScreenImageViewer("http://10.0.2.2:18080/photo.jpg", { fullscreen = false }, loader)
            } else {
                PostCard("http://10.0.2.2:18080/photo.jpg", loader, { fullscreen = true }, {})
            }
        }
        compose.onNodeWithContentDescription("Фотография публикации").performClick()
        compose.onNodeWithTag("fullscreen-photo").assertIsDisplayed().performClick()
        compose.onNodeWithTag("fullscreen-photo").assertDoesNotExist()
        compose.onNodeWithTag("post-card").assertIsDisplayed()
    }
}
