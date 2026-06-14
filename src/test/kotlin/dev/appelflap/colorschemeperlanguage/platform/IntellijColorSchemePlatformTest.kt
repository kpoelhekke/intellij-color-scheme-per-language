package dev.appelflap.colorschemeperlanguage.platform

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
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
}
