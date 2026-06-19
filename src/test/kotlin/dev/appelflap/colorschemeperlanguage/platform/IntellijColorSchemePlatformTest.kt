package dev.appelflap.colorschemeperlanguage.platform

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.impl.DelegateColorScheme
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class IntellijColorSchemePlatformTest : BasePlatformTestCase() {
    fun testApplySchemeToDisposedEditorDoesNotThrow() {
        val editorFactory = EditorFactory.getInstance()
        val document = editorFactory.createDocument("fun main() = Unit")
        val editor = editorFactory.createEditor(document, project) as EditorEx
        editorFactory.releaseEditor(editor)
        assertTrue("Editor should be disposed after release", editor.isDisposed)

        val platform = IntellijColorSchemePlatform(project)
        val scheme = EditorColorsManager.getInstance().globalScheme

        // Selection events can be delivered after the editor was already disposed.
        // Setting the colors scheme then crashes deep in EditorImpl.reinitSettings,
        // so the platform must skip disposed editors instead of mutating them.
        val applied = platform.applySchemeToEditor(editor, scheme)

        assertFalse("Disposed editor must not be touched", applied)
    }

    fun testGlobalSchemeIsAppliedRawWithoutWrapping() {
        withEditor { editor, platform ->
            val global = EditorColorsManager.getInstance().globalScheme

            assertTrue(platform.applySchemeToEditor(editor, global))

            assertFalse(
                "the main scheme already reports the right font, so it must not be wrapped",
                editor.appliedScheme() is FontFollowingColorScheme,
            )
        }
    }

    fun testOverrideSchemeIsWrappedToFollowMainThemeFont() {
        withEditor { editor, platform ->
            val global = EditorColorsManager.getInstance().globalScheme
            val override = (global.clone() as EditorColorsScheme).apply { name = "Per-Language Override" }

            assertTrue(platform.applySchemeToEditor(editor, override))

            assertTrue(
                "a scheme that differs from the main scheme must be wrapped",
                editor.appliedScheme() is FontFollowingColorScheme,
            )
        }
    }

    // EditorImpl wraps any assigned scheme in its own MyColorSchemeDelegate, so the scheme we set is one
    // delegate level down. Unwrap it to see what the platform actually applied.
    private fun EditorEx.appliedScheme(): EditorColorsScheme =
        (colorsScheme as? DelegateColorScheme)?.delegate ?: colorsScheme

    private fun withEditor(block: (EditorEx, IntellijColorSchemePlatform) -> Unit) {
        val editorFactory = EditorFactory.getInstance()
        val document = editorFactory.createDocument("fun main() = Unit")
        val editor = editorFactory.createEditor(document, project) as EditorEx
        try {
            block(editor, IntellijColorSchemePlatform(project))
        } finally {
            editorFactory.releaseEditor(editor)
        }
    }
}
