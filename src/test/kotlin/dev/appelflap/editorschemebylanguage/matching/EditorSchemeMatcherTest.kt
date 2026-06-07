package dev.appelflap.editorschemebylanguage.matching

import dev.appelflap.editorschemebylanguage.model.EditorSchemeContext
import dev.appelflap.editorschemebylanguage.model.RuleTargetKind
import dev.appelflap.editorschemebylanguage.model.SchemeRef
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorSchemeMatcherTest {
    private val defaultScheme = SchemeRef("Default")
    private val installedSchemes = listOf(defaultScheme, SchemeRef("Kotlin Scheme"), SchemeRef("Text Scheme"))

    @Test
    fun `language rule wins over file type rule`() {
        val selectedScheme = EditorSchemeMatcher.resolve(
            enabled = true,
            rules = listOf(
                SchemeRule(RuleTargetKind.FILE_TYPE, "PLAIN_TEXT", "Text", "Text Scheme"),
                SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", "Kotlin Scheme"),
            ),
            context = EditorSchemeContext(
                languageId = "kotlin",
                languageDisplayName = "Kotlin",
                fileTypeId = "PLAIN_TEXT",
                fileTypeDisplayName = "Text",
            ),
            installedSchemes = installedSchemes,
            defaultScheme = defaultScheme,
        )

        assertEquals(SchemeRef("Kotlin Scheme"), selectedScheme)
    }

    @Test
    fun `file type rule is used when language rule is absent`() {
        val selectedScheme = EditorSchemeMatcher.resolve(
            enabled = true,
            rules = listOf(
                SchemeRule(RuleTargetKind.FILE_TYPE, "PLAIN_TEXT", "Text", "Text Scheme"),
            ),
            context = EditorSchemeContext(
                languageId = "kotlin",
                languageDisplayName = "Kotlin",
                fileTypeId = "PLAIN_TEXT",
                fileTypeDisplayName = "Text",
            ),
            installedSchemes = installedSchemes,
            defaultScheme = defaultScheme,
        )

        assertEquals(SchemeRef("Text Scheme"), selectedScheme)
    }

    @Test
    fun `default scheme is used when disabled`() {
        val selectedScheme = EditorSchemeMatcher.resolve(
            enabled = false,
            rules = listOf(
                SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", "Kotlin Scheme"),
            ),
            context = EditorSchemeContext(
                languageId = "kotlin",
                languageDisplayName = "Kotlin",
                fileTypeId = null,
                fileTypeDisplayName = null,
            ),
            installedSchemes = installedSchemes,
            defaultScheme = defaultScheme,
        )

        assertEquals(defaultScheme, selectedScheme)
    }

    @Test
    fun `default scheme is used when enabled rules do not match context`() {
        val selectedScheme = EditorSchemeMatcher.resolve(
            enabled = true,
            rules = listOf(
                SchemeRule(RuleTargetKind.LANGUAGE, "java", "Java", "Kotlin Scheme"),
                SchemeRule(RuleTargetKind.FILE_TYPE, "XML", "XML", "Text Scheme"),
            ),
            context = EditorSchemeContext(
                languageId = "kotlin",
                languageDisplayName = "Kotlin",
                fileTypeId = "PLAIN_TEXT",
                fileTypeDisplayName = "Text",
            ),
            installedSchemes = installedSchemes,
            defaultScheme = defaultScheme,
        )

        assertEquals(defaultScheme, selectedScheme)
    }

    @Test
    fun `default scheme is used when mapped scheme is missing`() {
        val selectedScheme = EditorSchemeMatcher.resolve(
            enabled = true,
            rules = listOf(
                SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", "Missing Scheme"),
            ),
            context = EditorSchemeContext(
                languageId = "kotlin",
                languageDisplayName = "Kotlin",
                fileTypeId = null,
                fileTypeDisplayName = null,
            ),
            installedSchemes = installedSchemes,
            defaultScheme = defaultScheme,
        )

        assertEquals(defaultScheme, selectedScheme)
    }
}
