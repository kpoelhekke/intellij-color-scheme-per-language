package dev.appelflap.editorschemebylanguage.runtime

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.appelflap.editorschemebylanguage.model.RuleTargetKind
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import dev.appelflap.editorschemebylanguage.platform.EditorSchemePlatform
import dev.appelflap.editorschemebylanguage.platform.PlatformEditorContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue

class EditorSchemeApplierTest : BasePlatformTestCase() {
    fun testApplyForEditorUsesExplicitContextBeforePlatformContext() {
        myFixture.configureByText("Example.kt", "fun main() = Unit")
        val manager = EditorColorsManager.getInstance()
        val defaultScheme = manager.globalScheme
        val resolvedScheme = manager.allSchemes.first { it.name != defaultScheme.name }
        val explicitContext = PlatformEditorContext(
            language = myFixture.file.language,
            fileType = myFixture.file.fileType,
        )
        val platform = FakeEditorSchemePlatform(
            context = null,
            defaultScheme = defaultScheme,
            installedSchemes = listOf(defaultScheme, resolvedScheme),
        )
        val applier = EditorSchemeApplier(
            platform = platform,
            enabled = { true },
            rules = {
                listOf(
                    SchemeRule(
                        targetKind = RuleTargetKind.LANGUAGE,
                        targetId = explicitContext.language!!.id,
                        targetDisplayName = explicitContext.language.displayName,
                        schemeName = resolvedScheme.name,
                    ),
                )
            },
        )

        assertTrue(applier.applyForEditor(myFixture.editor, explicitContext))

        assertSame(myFixture.editor, platform.appliedEditor)
        assertEquals(resolvedScheme.name, platform.appliedScheme?.name)
    }

    fun testApplyForEditorReturnsFalseWhenNoContextExists() {
        myFixture.configureByText("Example.kt", "fun main() = Unit")
        val manager = EditorColorsManager.getInstance()
        val platform = FakeEditorSchemePlatform(
            context = null,
            defaultScheme = manager.globalScheme,
            installedSchemes = manager.allSchemes.toList(),
        )
        val applier = EditorSchemeApplier(
            platform = platform,
            enabled = { true },
            rules = { emptyList() },
        )

        assertFalse(applier.applyForEditor(myFixture.editor))

        assertEquals(0, platform.applyCount)
    }

    private class FakeEditorSchemePlatform(
        private val context: PlatformEditorContext?,
        private val defaultScheme: EditorColorsScheme,
        private val installedSchemes: List<EditorColorsScheme>,
    ) : EditorSchemePlatform {
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
