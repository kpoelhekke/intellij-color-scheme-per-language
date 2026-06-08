package dev.appelflap.colorschemeperlanguage.settings

import dev.appelflap.colorschemeperlanguage.ColorSchemePerLanguageBundle
import dev.appelflap.colorschemeperlanguage.model.SchemeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorSchemeSettingsPanelValidationTest {
    @Test
    fun `duplicate languages are invalid`() {
        val result = ColorSchemeSettingsPanel.validateRules(
            rules = listOf(
                SchemeRule("kotlin", "Kotlin", "Default"),
                SchemeRule("kotlin", "Kotlin", "Default"),
            ),
            installedSchemeNames = listOf("Default"),
        )

        assertFalse(result.isValid)
    }

    @Test
    fun `missing scheme references are invalid`() {
        val result = ColorSchemeSettingsPanel.validateRules(
            rules = listOf(
                SchemeRule("java", "Java", "Missing Scheme"),
            ),
            installedSchemeNames = listOf("Default"),
        )

        assertFalse(result.isValid)
        assertEquals(
            ColorSchemePerLanguageBundle.message("settings.validation.missing.scheme"),
            result.message,
        )
    }

    @Test
    fun `unique languages with installed schemes are valid`() {
        val result = ColorSchemeSettingsPanel.validateRules(
            rules = listOf(
                SchemeRule("kotlin", "Kotlin", "Default"),
                SchemeRule("java", "Java", "Default"),
            ),
            installedSchemeNames = listOf("Default"),
        )

        assertTrue(result.isValid)
    }
}
