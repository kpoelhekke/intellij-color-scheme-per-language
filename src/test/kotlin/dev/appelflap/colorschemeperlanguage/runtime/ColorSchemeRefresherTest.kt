package dev.appelflap.colorschemeperlanguage.runtime

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.appelflap.colorschemeperlanguage.model.SchemeRule
import dev.appelflap.colorschemeperlanguage.platform.ColorSchemePlatform
import dev.appelflap.colorschemeperlanguage.platform.PlatformEditorContext
import org.junit.Assert.assertEquals

class ColorSchemeRefresherTest : BasePlatformTestCase() {
    fun testRefreshEditorsAppliesResolvedSchemeToEveryEditor() {
        val firstEditor = myFixture.configureByText("First.kt", "fun main() = Unit")
            .let { myFixture.editor }
        val language = myFixture.file.language
        val secondEditor = myFixture.configureByText("Second.kt", "fun main() = Unit")
            .let { myFixture.editor }
        val manager = EditorColorsManager.getInstance()
        val defaultScheme = manager.globalScheme
        val resolvedScheme = manager.allSchemes.first { it.name != defaultScheme.name }
        val platform = FakeColorSchemePlatform(
            context = PlatformEditorContext(language = language),
            defaultScheme = defaultScheme,
            installedSchemes = listOf(defaultScheme, resolvedScheme),
        )
        val handler = ColorSchemeRefreshHandler(
            ColorSchemeApplier(
                platform = platform,
                enabled = { true },
                rules = {
                    listOf(
                        SchemeRule(
                            targetId = language.id,
                            targetDisplayName = language.displayName,
                            schemeName = resolvedScheme.name,
                        ),
                    )
                },
            ),
        )

        assertEquals(2, handler.refreshEditors(listOf(firstEditor, secondEditor)))

        assertEquals(listOf(firstEditor, secondEditor), platform.appliedEditors)
        assertEquals(
            listOf(resolvedScheme.name, resolvedScheme.name),
            platform.appliedSchemes.map { it.name },
        )
    }

    fun testRefreshEditorsSkipsEditorsWithoutContext() {
        myFixture.configureByText("Example.kt", "fun main() = Unit")
        val manager = EditorColorsManager.getInstance()
        val platform = FakeColorSchemePlatform(
            context = null,
            defaultScheme = manager.globalScheme,
            installedSchemes = manager.allSchemes.toList(),
        )
        val handler = ColorSchemeRefreshHandler(
            ColorSchemeApplier(
                platform = platform,
                enabled = { true },
                rules = { emptyList() },
            ),
        )

        assertEquals(0, handler.refreshEditors(listOf(myFixture.editor)))

        assertEquals(0, platform.appliedEditors.size)
    }

    private class FakeColorSchemePlatform(
        private val context: PlatformEditorContext?,
        private val defaultScheme: EditorColorsScheme,
        private val installedSchemes: List<EditorColorsScheme>,
    ) : ColorSchemePlatform {
        val appliedEditors = mutableListOf<Editor>()
        val appliedSchemes = mutableListOf<EditorColorsScheme>()

        override fun contextFor(editor: Editor): PlatformEditorContext? = context

        override fun currentGlobalScheme(): EditorColorsScheme = defaultScheme

        override fun findScheme(name: String): EditorColorsScheme? =
            installedSchemes.firstOrNull { it.name == name }

        override fun installedSchemes(): List<EditorColorsScheme> = installedSchemes

        override fun applySchemeToEditor(editor: Editor, scheme: EditorColorsScheme): Boolean {
            appliedEditors += editor
            appliedSchemes += scheme
            return true
        }
    }
}
