package dev.appelflap.colorschemeperlanguage.matching

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorFontSourceResolverTest {
    @Test
    fun `override scheme with its own font wins`() {
        assertEquals(
            EditorFontSource.OVERRIDE,
            EditorFontSourceResolver.choose(overrideUsesAppFont = false, defaultUsesAppFont = false),
        )
    }

    @Test
    fun `override scheme with its own font wins even when default uses the app font`() {
        assertEquals(
            EditorFontSource.OVERRIDE,
            EditorFontSourceResolver.choose(overrideUsesAppFont = false, defaultUsesAppFont = true),
        )
    }

    @Test
    fun `default scheme is used when override uses the app font but default has its own`() {
        assertEquals(
            EditorFontSource.DEFAULT,
            EditorFontSourceResolver.choose(overrideUsesAppFont = true, defaultUsesAppFont = false),
        )
    }

    @Test
    fun `override scheme is used when both fall back to the app font`() {
        assertEquals(
            EditorFontSource.OVERRIDE,
            EditorFontSourceResolver.choose(overrideUsesAppFont = true, defaultUsesAppFont = true),
        )
    }
}
