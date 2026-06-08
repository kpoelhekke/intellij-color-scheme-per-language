package dev.appelflap.colorschemeperlanguage.diff

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.appelflap.colorschemeperlanguage.platform.PlatformEditorContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class DiffPaneContextsTest : BasePlatformTestCase() {
    fun testSideBySideUsesMatchingContentForEachEditorIndex() {
        val left = PlatformEditorContext(language = PlainTextLanguage.INSTANCE)
        val right = PlatformEditorContext(language = Language.ANY)

        val contexts = DiffPaneContexts.forSideBySide(
            contentContexts = listOf(left, right),
            editorCount = 2,
        )

        assertEquals(left, contexts[0])
        assertEquals(right, contexts[1])
    }

    fun testUnifiedPrefersRightContentThenLeftContent() {
        val left = PlatformEditorContext(language = PlainTextLanguage.INSTANCE)
        val right = PlatformEditorContext(language = Language.ANY)

        val contexts = DiffPaneContexts.forUnified(
            contentContexts = listOf(left, right),
            editorCount = 1,
        )

        assertEquals(right, contexts.single())
    }

    fun testUnifiedFallsBackToLeftContentWhenRightIsUnavailable() {
        val left = PlatformEditorContext(language = PlainTextLanguage.INSTANCE)

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
