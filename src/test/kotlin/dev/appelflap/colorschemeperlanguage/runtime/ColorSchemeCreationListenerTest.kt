package dev.appelflap.colorschemeperlanguage.runtime

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.appelflap.colorschemeperlanguage.platform.ColorSchemePlatform
import dev.appelflap.colorschemeperlanguage.platform.PlatformEditorContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class ColorSchemeCreationListenerTest : BasePlatformTestCase() {
    fun testCreatedEditorIsAppliedWhenContextExists() {
        myFixture.configureByText("Example.kt", "fun main() = Unit")
        val manager = EditorColorsManager.getInstance()
        val platform = FakeColorSchemePlatform(
            context = PlatformEditorContext(myFixture.file.language),
            defaultScheme = manager.globalScheme,
            installedSchemes = manager.allSchemes.toList(),
        )
        val handler = ColorSchemeCreationHandler(
            applier = ColorSchemeApplier(platform, enabled = { true }, rules = { emptyList() }),
        )

        assertTrue(handler.applyForCreatedEditor(myFixture.editor))
    }

    fun testCreatedEditorIsIgnoredWhenContextIsUnavailable() {
        myFixture.configureByText("Example.kt", "fun main() = Unit")
        val manager = EditorColorsManager.getInstance()
        val platform = FakeColorSchemePlatform(
            context = null,
            defaultScheme = manager.globalScheme,
            installedSchemes = manager.allSchemes.toList(),
        )
        val handler = ColorSchemeCreationHandler(
            applier = ColorSchemeApplier(platform, enabled = { true }, rules = { emptyList() }),
        )

        assertFalse(handler.applyForCreatedEditor(myFixture.editor))
    }

    private class FakeColorSchemePlatform(
        private val context: PlatformEditorContext?,
        private val defaultScheme: EditorColorsScheme,
        private val installedSchemes: List<EditorColorsScheme>,
    ) : ColorSchemePlatform {
        override fun contextFor(editor: Editor): PlatformEditorContext? = context

        override fun currentGlobalScheme(): EditorColorsScheme = defaultScheme

        override fun findScheme(name: String): EditorColorsScheme? =
            installedSchemes.firstOrNull { it.name == name }

        override fun installedSchemes(): List<EditorColorsScheme> = installedSchemes

        override fun applySchemeToEditor(editor: Editor, scheme: EditorColorsScheme): Boolean = true
    }
}
