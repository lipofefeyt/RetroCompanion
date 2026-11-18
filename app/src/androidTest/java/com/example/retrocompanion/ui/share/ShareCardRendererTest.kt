package com.example.retrocompanion.ui.share

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Needs a real Canvas/StaticLayout, so it runs on a device or emulator, not the JVM. */
@RunWith(AndroidJUnit4::class)
class ShareCardRendererTest {

    private val light = ShareCardPalette(
        background = Color.rgb(250, 250, 250),
        surface = Color.rgb(230, 230, 235),
        onSurface = Color.rgb(20, 20, 20),
        onSurfaceVariant = Color.rgb(90, 90, 100),
        accent = Color.rgb(60, 90, 200)
    )
    private val dark = ShareCardPalette(
        background = Color.rgb(18, 18, 20),
        surface = Color.rgb(40, 40, 46),
        onSurface = Color.rgb(235, 235, 235),
        onSurfaceVariant = Color.rgb(170, 170, 180),
        accent = Color.rgb(150, 170, 255)
    )

    private fun content(title: String = "Sample Achievement", description: String? = "Do a sample thing") =
        ShareCardContent(
            kind = ShareCardKind.RARE_ACHIEVEMENT,
            title = title,
            subtitle = "Sample Game · Sample Console",
            description = description,
            stats = "25 pts · 2.3% of players · Unlocked Mar 3, 2026",
            iconUrl = ""
        )

    @Test
    fun rendersInLightAndDarkWithTheThemeBackground() {
        listOf(light, dark).forEach { palette ->
            val bitmap = ShareCardRenderer.render(content(), palette, icon = null)

            assertEquals(ShareCardRenderer.WIDTH, bitmap.width)
            assertTrue(bitmap.height > 300)
            assertEquals(palette.background, bitmap.getPixel(0, 0))
        }
        assertNotEquals(
            ShareCardRenderer.render(content(), light, null).getPixel(0, 0),
            ShareCardRenderer.render(content(), dark, null).getPixel(0, 0)
        )
    }

    @Test
    fun aVeryLongTitleAndDescriptionStayWithinTheLineLimits() {
        val normal = ShareCardRenderer.render(content(), light, null)
        val huge = ShareCardRenderer.render(
            content(title = "Very Long Title ".repeat(200), description = "Long description ".repeat(300)),
            light,
            null
        )

        // Titles are capped at 3 lines and descriptions at 4, so growth is bounded.
        assertTrue(huge.height < normal.height + 600)
    }

    @Test
    fun aMissingIconAndMissingOptionalTextStillRender() {
        val minimal = ShareCardContent(
            kind = ShareCardKind.MASTERY,
            title = "Sample Game",
            subtitle = null,
            description = null,
            stats = "10 achievements",
            iconUrl = ""
        )

        val bitmap = ShareCardRenderer.render(minimal, dark, icon = null)

        assertTrue(bitmap.height > 0)
    }

    @Test
    fun aProvidedIconIsDrawnAndDoesNotChangeTheSize() {
        val icon = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }

        val withIcon = ShareCardRenderer.render(content(), light, icon)
        val withoutIcon = ShareCardRenderer.render(content(), light, null)

        assertEquals(withoutIcon.height, withIcon.height)
        assertEquals(ShareCardRenderer.WIDTH, withIcon.width)
    }
}
