package dev.appelflap.editorschemebylanguage.settings

import dev.appelflap.editorschemebylanguage.EditorSchemeByLanguageBundle
import dev.appelflap.editorschemebylanguage.model.RuleTargetKind
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorSchemeSettingsPanelValidationTest {
    @Test
    fun `duplicate targets are invalid`() {
        val result = EditorSchemeSettingsPanel.validateRules(
            rules = listOf(
                SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", "Default"),
                SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", "Default"),
            ),
            installedSchemeNames = listOf("Default"),
        )

        assertFalse(result.isValid)
    }

    @Test
    fun `missing scheme references are invalid`() {
        val result = EditorSchemeSettingsPanel.validateRules(
            rules = listOf(
                SchemeRule(RuleTargetKind.FILE_TYPE, "JAVA", "Java", "Missing Scheme"),
            ),
            installedSchemeNames = listOf("Default"),
        )

        assertFalse(result.isValid)
        assertEquals(
            EditorSchemeByLanguageBundle.message("settings.validation.missing.scheme"),
            result.message,
        )
    }

    @Test
    fun `unique targets with installed schemes are valid`() {
        val result = EditorSchemeSettingsPanel.validateRules(
            rules = listOf(
                SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", "Default"),
                SchemeRule(RuleTargetKind.FILE_TYPE, "JAVA", "Java", "Default"),
            ),
            installedSchemeNames = listOf("Default"),
        )

        assertTrue(result.isValid)
    }
}
