package dev.appelflap.colorschemeperlanguage.matching

import dev.appelflap.colorschemeperlanguage.model.ColorSchemeContext
import dev.appelflap.colorschemeperlanguage.model.SchemeRef
import dev.appelflap.colorschemeperlanguage.model.SchemeRule
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorSchemeMatcherTest {
    private val defaultScheme = SchemeRef("Default")
    private val installedSchemes = listOf(defaultScheme, SchemeRef("Kotlin Scheme"), SchemeRef("Text Scheme"))

    @Test
    fun `language rule selects its scheme`() {
        val selectedScheme = ColorSchemeMatcher.resolve(
            enabled = true,
            rules = listOf(
                SchemeRule("java", "Java", "Text Scheme"),
                SchemeRule("kotlin", "Kotlin", "Kotlin Scheme"),
            ),
            context = ColorSchemeContext(
                languageId = "kotlin",
                languageDisplayName = "Kotlin",
            ),
            installedSchemes = installedSchemes,
            defaultScheme = defaultScheme,
        )

        assertEquals(SchemeRef("Kotlin Scheme"), selectedScheme)
    }

    @Test
    fun `default scheme is used when disabled`() {
        val selectedScheme = ColorSchemeMatcher.resolve(
            enabled = false,
            rules = listOf(
                SchemeRule("kotlin", "Kotlin", "Kotlin Scheme"),
            ),
            context = ColorSchemeContext(
                languageId = "kotlin",
                languageDisplayName = "Kotlin",
            ),
            installedSchemes = installedSchemes,
            defaultScheme = defaultScheme,
        )

        assertEquals(defaultScheme, selectedScheme)
    }

    @Test
    fun `default scheme is used when no rule matches the language`() {
        val selectedScheme = ColorSchemeMatcher.resolve(
            enabled = true,
            rules = listOf(
                SchemeRule("java", "Java", "Kotlin Scheme"),
            ),
            context = ColorSchemeContext(
                languageId = "kotlin",
                languageDisplayName = "Kotlin",
            ),
            installedSchemes = installedSchemes,
            defaultScheme = defaultScheme,
        )

        assertEquals(defaultScheme, selectedScheme)
    }

    @Test
    fun `default scheme is used when the context has no language`() {
        val selectedScheme = ColorSchemeMatcher.resolve(
            enabled = true,
            rules = listOf(
                SchemeRule("kotlin", "Kotlin", "Kotlin Scheme"),
            ),
            context = ColorSchemeContext(),
            installedSchemes = installedSchemes,
            defaultScheme = defaultScheme,
        )

        assertEquals(defaultScheme, selectedScheme)
    }

    @Test
    fun `default scheme is used when mapped scheme is missing`() {
        val selectedScheme = ColorSchemeMatcher.resolve(
            enabled = true,
            rules = listOf(
                SchemeRule("kotlin", "Kotlin", "Missing Scheme"),
            ),
            context = ColorSchemeContext(
                languageId = "kotlin",
                languageDisplayName = "Kotlin",
            ),
            installedSchemes = installedSchemes,
            defaultScheme = defaultScheme,
        )

        assertEquals(defaultScheme, selectedScheme)
    }
}
