package dev.appelflap.editorschemebylanguage.runtime

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.appelflap.editorschemebylanguage.platform.EditorSchemePlatform
import dev.appelflap.editorschemebylanguage.platform.PlatformEditorContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class EditorSchemeCreationListenerTest : BasePlatformTestCase() {
    fun testCreatedEditorIsAppliedWhenContextExists() {
        myFixture.configureByText("Example.kt", "fun main() = Unit")
        val manager = EditorColorsManager.getInstance()
        val platform = FakeEditorSchemePlatform(
            context = PlatformEditorContext(myFixture.file.language, myFixture.file.fileType),
            defaultScheme = manager.globalScheme,
            installedSchemes = manager.allSchemes.toList(),
        )
        val handler = EditorSchemeCreationHandler(
            applier = EditorSchemeApplier(platform, enabled = { true }, rules = { emptyList() }),
        )

        assertTrue(handler.applyForCreatedEditor(myFixture.editor))
    }

    fun testCreatedEditorIsIgnoredWhenContextIsUnavailable() {
        myFixture.configureByText("Example.kt", "fun main() = Unit")
        val manager = EditorColorsManager.getInstance()
        val platform = FakeEditorSchemePlatform(
            context = null,
            defaultScheme = manager.globalScheme,
            installedSchemes = manager.allSchemes.toList(),
        )
        val handler = EditorSchemeCreationHandler(
            applier = EditorSchemeApplier(platform, enabled = { true }, rules = { emptyList() }),
        )

        assertFalse(handler.applyForCreatedEditor(myFixture.editor))
    }

    private class FakeEditorSchemePlatform(
        private val context: PlatformEditorContext?,
        private val defaultScheme: EditorColorsScheme,
        private val installedSchemes: List<EditorColorsScheme>,
    ) : EditorSchemePlatform {
        override fun contextFor(editor: Editor): PlatformEditorContext? = context

        override fun currentGlobalScheme(): EditorColorsScheme = defaultScheme

        override fun findScheme(name: String): EditorColorsScheme? =
            installedSchemes.firstOrNull { it.name == name }

        override fun installedSchemes(): List<EditorColorsScheme> = installedSchemes

        override fun applySchemeToEditor(editor: Editor, scheme: EditorColorsScheme): Boolean = true
    }
}
