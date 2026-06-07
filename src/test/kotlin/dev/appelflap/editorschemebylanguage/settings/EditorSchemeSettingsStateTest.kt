package dev.appelflap.editorschemebylanguage.settings

import dev.appelflap.editorschemebylanguage.model.RuleTargetKind
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EditorSchemeSettingsStateTest {
    @Test
    fun `settings state saves and reloads enabled flag and canonical rule entries`() {
        val original = EditorSchemeSettingsState()
        original.update(
            enabled = false,
            rules = listOf(
                SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", "Kotlin Scheme"),
                SchemeRule(RuleTargetKind.FILE_TYPE, "PLAIN_TEXT", "Text", "Text Scheme"),
            ),
        )

        val reloaded = EditorSchemeSettingsState()
        reloaded.loadState(original.state)

        assertFalse(reloaded.enabled)
        assertEquals(
            listOf(
                SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", "Kotlin Scheme"),
                SchemeRule(RuleTargetKind.FILE_TYPE, "PLAIN_TEXT", "Text", "Text Scheme"),
            ),
            reloaded.rules,
        )
        assertEquals("LANGUAGE:kotlin", reloaded.rules[0].targetKey())
        assertEquals("FILE_TYPE:PLAIN_TEXT", reloaded.rules[1].targetKey())
    }
}
