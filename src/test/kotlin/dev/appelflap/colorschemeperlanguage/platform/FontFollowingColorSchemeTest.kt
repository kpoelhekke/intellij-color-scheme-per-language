package dev.appelflap.colorschemeperlanguage.platform

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FontFollowingColorSchemeTest : BasePlatformTestCase() {
    private fun ownFontScheme(size: Int): EditorColorsScheme =
        (EditorColorsManager.getInstance().globalScheme.clone() as EditorColorsScheme).apply {
            editorFontSize = size
            // Precondition: an explicit size means the scheme carries its own font.
            assertFalse("scheme with explicit size should not use the app font", isUseAppFontPreferencesInEditor)
        }

    private fun appFontScheme(): EditorColorsScheme =
        (EditorColorsManager.getInstance().globalScheme.clone() as EditorColorsScheme).apply {
            setUseAppFontPreferencesInEditor()
            assertTrue("scheme should use the app font", isUseAppFontPreferencesInEditor)
        }

    fun testOverrideWithOwnFontKeepsItsFontSize() {
        val override = ownFontScheme(17)
        val mainTheme = ownFontScheme(21)

        val scheme = FontFollowingColorScheme(override) { mainTheme }

        assertEquals(17, scheme.editorFontSize)
    }

    fun testOverrideUsingAppFontFollowsMainThemeOwnFont() {
        val override = appFontScheme()
        val mainTheme = ownFontScheme(21)

        val scheme = FontFollowingColorScheme(override) { mainTheme }

        assertEquals("override using the app font must follow the main theme", 21, scheme.editorFontSize)
    }

    fun testBothUsingAppFontFallBackToOverrideAppFont() {
        val override = appFontScheme()
        val mainTheme = appFontScheme()

        val scheme = FontFollowingColorScheme(override) { mainTheme }

        // Both fall back to the app default font, which the override scheme already reports.
        assertEquals(override.editorFontSize, scheme.editorFontSize)
    }

    fun testFontSetterDoesNotMutateTheWrappedScheme() {
        val override = appFontScheme()
        val mainTheme = ownFontScheme(21)
        val scheme = FontFollowingColorScheme(override) { mainTheme }

        scheme.editorFontSize = 99

        assertTrue(
            "setter must not flip the shared scheme onto an explicit font",
            override.isUseAppFontPreferencesInEditor,
        )
    }

    fun testMainThemeFontIsReadLive() {
        val override = appFontScheme()
        var mainTheme = ownFontScheme(21)
        val scheme = FontFollowingColorScheme(override) { mainTheme }
        assertEquals(21, scheme.editorFontSize)

        mainTheme = ownFontScheme(28)

        assertEquals("font must follow the main theme as it changes", 28, scheme.editorFontSize)
    }
}
