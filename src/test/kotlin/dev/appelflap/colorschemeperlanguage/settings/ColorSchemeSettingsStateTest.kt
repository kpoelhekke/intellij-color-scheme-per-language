package dev.appelflap.colorschemeperlanguage.settings

import com.intellij.util.xmlb.XmlSerializer
import dev.appelflap.colorschemeperlanguage.model.SchemeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorSchemeSettingsStateTest {
    @Test
    fun `settings state saves and reloads enabled flag and canonical rule entries`() {
        val original = ColorSchemeSettingsState()
        original.update(
            enabled = false,
            rules = listOf(
                SchemeRule("kotlin", "Kotlin", "Kotlin Scheme"),
                SchemeRule("java", "Java", "Text Scheme"),
            ),
        )

        val reloaded = ColorSchemeSettingsState()
        reloaded.loadState(original.state)

        assertFalse(reloaded.enabled)
        assertEquals(
            listOf(
                SchemeRule("kotlin", "Kotlin", "Kotlin Scheme"),
                SchemeRule("java", "Java", "Text Scheme"),
            ),
            reloaded.rules,
        )
        assertEquals("kotlin", reloaded.rules[0].targetKey())
        assertEquals("java", reloaded.rules[1].targetKey())
    }

    @Test
    fun `loadState defensively copies incoming mutable state`() {
        val sourceRule = SchemeRule("kotlin", "Kotlin", "Kotlin Scheme")
        val sourceState = ColorSchemeSettingsState().apply {
            enabled = false
            rules = mutableListOf(sourceRule)
        }

        val loaded = ColorSchemeSettingsState()
        loaded.loadState(sourceState)

        sourceState.enabled = true
        sourceState.rules.clear()
        sourceState.rules.add(SchemeRule("java", "Java", "Text Scheme"))
        sourceRule.schemeName = "Mutated Scheme"

        assertFalse(loaded.enabled)
        assertEquals(listOf(SchemeRule("kotlin", "Kotlin", "Kotlin Scheme")), loaded.rules)
        assertTrue(loaded.rules[0] !== sourceRule)
    }

    @Test
    fun `settings state survives xml serialization round trip`() {
        val original = ColorSchemeSettingsState()
        original.update(
            enabled = false,
            rules = listOf(
                SchemeRule("kotlin", "Kotlin", "Kotlin Scheme"),
                SchemeRule("java", "Java", "Text Scheme"),
            ),
        )

        val element = XmlSerializer.serialize(original.state)
        val reloaded = XmlSerializer.deserialize(element, ColorSchemeSettingsState::class.java)

        assertFalse(reloaded.enabled)
        assertEquals(original.rules, reloaded.rules)
    }
}
