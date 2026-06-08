package dev.appelflap.colorschemeperlanguage.runtime

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.appelflap.colorschemeperlanguage.model.SchemeRule
import dev.appelflap.colorschemeperlanguage.platform.ColorSchemePlatform
import dev.appelflap.colorschemeperlanguage.platform.PlatformEditorContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue

class ColorSchemeSelectionListenerTest : BasePlatformTestCase() {
    fun testApplyForEditorAppliesResolvedSchemeToActiveEditor() {
        myFixture.configureByText("Example.kt", "fun main() = Unit")
        val manager = EditorColorsManager.getInstance()
        val defaultScheme = manager.globalScheme
        val resolvedScheme = manager.allSchemes.first { it.name != defaultScheme.name }
        val editor = myFixture.editor
        val platform = FakeColorSchemePlatform(
            context = PlatformEditorContext(
                language = myFixture.file.language,
            ),
            defaultScheme = defaultScheme,
            installedSchemes = listOf(defaultScheme, resolvedScheme),
        )
        val handler = ColorSchemeSelectionHandler(
            platform = platform,
            enabled = { true },
            rules = {
                listOf(
                    SchemeRule(
                        targetId = myFixture.file.language.id,
                        targetDisplayName = myFixture.file.language.displayName,
                        schemeName = resolvedScheme.name,
                    ),
                )
            },
        )

        assertTrue(handler.applyForEditor(editor))

        assertSame(editor, platform.appliedEditor)
        assertEquals(resolvedScheme.name, platform.appliedScheme?.name)
    }

    fun testApplyForEditorReturnsFalseAndDoesNotApplyWhenContextIsUnavailable() {
        myFixture.configureByText("Example.kt", "fun main() = Unit")
        val manager = EditorColorsManager.getInstance()
        val platform = FakeColorSchemePlatform(
            context = null,
            defaultScheme = manager.globalScheme,
            installedSchemes = manager.allSchemes.toList(),
        )
        val handler = ColorSchemeSelectionHandler(
            platform = platform,
            enabled = { true },
            rules = { emptyList() },
        )

        assertFalse(handler.applyForEditor(myFixture.editor))

        assertEquals(0, platform.applyCount)
    }

    private class FakeColorSchemePlatform(
        private val context: PlatformEditorContext?,
        private val defaultScheme: EditorColorsScheme,
        private val installedSchemes: List<EditorColorsScheme>,
    ) : ColorSchemePlatform {
        var appliedEditor: Editor? = null
            private set
        var appliedScheme: EditorColorsScheme? = null
            private set
        var applyCount = 0
            private set

        override fun contextFor(editor: Editor): PlatformEditorContext? = context

        override fun currentGlobalScheme(): EditorColorsScheme = defaultScheme

        override fun findScheme(name: String): EditorColorsScheme? =
            installedSchemes.firstOrNull { it.name == name }

        override fun installedSchemes(): List<EditorColorsScheme> = installedSchemes

        override fun applySchemeToEditor(editor: Editor, scheme: EditorColorsScheme): Boolean {
            applyCount += 1
            appliedEditor = editor
            appliedScheme = scheme
            return true
        }
    }
}
