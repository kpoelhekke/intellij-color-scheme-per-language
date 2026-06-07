package dev.appelflap.editorschemebylanguage.settings

import com.intellij.util.xmlb.XmlSerializer
import dev.appelflap.editorschemebylanguage.model.RuleTargetKind
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `loadState defensively copies incoming mutable state`() {
        val sourceRule = SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", "Kotlin Scheme")
        val sourceState = EditorSchemeSettingsState().apply {
            enabled = false
            rules = mutableListOf(sourceRule)
        }

        val loaded = EditorSchemeSettingsState()
        loaded.loadState(sourceState)

        sourceState.enabled = true
        sourceState.rules.clear()
        sourceState.rules.add(SchemeRule(RuleTargetKind.FILE_TYPE, "PLAIN_TEXT", "Text", "Text Scheme"))
        sourceRule.schemeName = "Mutated Scheme"

        assertFalse(loaded.enabled)
        assertEquals(listOf(SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", "Kotlin Scheme")), loaded.rules)
        assertTrue(loaded.rules[0] !== sourceRule)
    }

    @Test
    fun `settings state survives xml serialization round trip`() {
        val original = EditorSchemeSettingsState()
        original.update(
            enabled = false,
            rules = listOf(
                SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", "Kotlin Scheme"),
                SchemeRule(RuleTargetKind.FILE_TYPE, "PLAIN_TEXT", "Text", "Text Scheme"),
            ),
        )

        val element = XmlSerializer.serialize(original.state)
        val reloaded = XmlSerializer.deserialize(element, EditorSchemeSettingsState::class.java)

        assertFalse(reloaded.enabled)
        assertEquals(original.rules, reloaded.rules)
    }
}
