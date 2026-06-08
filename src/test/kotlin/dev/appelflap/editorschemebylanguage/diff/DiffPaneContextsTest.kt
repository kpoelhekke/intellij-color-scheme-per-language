package dev.appelflap.editorschemebylanguage.diff

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.appelflap.editorschemebylanguage.platform.PlatformEditorContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class DiffPaneContextsTest : BasePlatformTestCase() {
    fun testSideBySideUsesMatchingContentForEachEditorIndex() {
        val left = PlatformEditorContext(
            language = null,
            fileType = FileTypeManager.getInstance().getFileTypeByExtension("txt"),
        )
        val right = PlatformEditorContext(
            language = null,
            fileType = FileTypeManager.getInstance().getFileTypeByExtension("kt"),
        )

        val contexts = DiffPaneContexts.forSideBySide(
            contentContexts = listOf(left, right),
            editorCount = 2,
        )

        assertEquals(left, contexts[0])
        assertEquals(right, contexts[1])
    }

    fun testUnifiedPrefersRightContentThenLeftContent() {
        val left = PlatformEditorContext(
            language = null,
            fileType = FileTypeManager.getInstance().getFileTypeByExtension("txt"),
        )
        val right = PlatformEditorContext(
            language = null,
            fileType = FileTypeManager.getInstance().getFileTypeByExtension("kt"),
        )

        val contexts = DiffPaneContexts.forUnified(
            contentContexts = listOf(left, right),
            editorCount = 1,
        )

        assertEquals(right, contexts.single())
    }

    fun testUnifiedFallsBackToLeftContentWhenRightIsUnavailable() {
        val left = PlatformEditorContext(
            language = null,
            fileType = FileTypeManager.getInstance().getFileTypeByExtension("kt"),
        )

        val contexts = DiffPaneContexts.forUnified(
            contentContexts = listOf(left, null),
            editorCount = 1,
        )

        assertEquals(left, contexts.single())
    }

    fun testUnsupportedEditorWithoutContentGetsNullContext() {
        val contexts = DiffPaneContexts.forSideBySide(
            contentContexts = emptyList(),
            editorCount = 1,
        )

        assertNull(contexts.single())
    }
}
