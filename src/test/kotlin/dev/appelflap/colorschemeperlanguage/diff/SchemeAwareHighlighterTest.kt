package dev.appelflap.colorschemeperlanguage.diff

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.editor.highlighter.HighlighterClient
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.Color

class SchemeAwareHighlighterTest : BasePlatformTestCase() {
    private val key = TextAttributesKey.createTextAttributesKey("COLOR_SCHEME_PER_LANGUAGE_TEST_KEY")

    // Mimics the unified diff highlighter: attributes frozen from another scheme, keys kept alongside.
    private class FrozenHighlighter(
        private val attributes: TextAttributes,
        private val keys: Array<TextAttributesKey>,
    ) : EditorHighlighter {
        override fun createIterator(startOffset: Int): HighlighterIterator = object : HighlighterIterator {
            override fun getTextAttributes(): TextAttributes = attributes

            override fun getTextAttributesKeys(): Array<TextAttributesKey> = keys

            override fun getStart(): Int = 0

            override fun getEnd(): Int = 1

            override fun getTokenType(): IElementType? = null

            override fun advance() = Unit

            override fun retreat() = Unit

            override fun atEnd(): Boolean = false

            override fun getDocument(): Document = error("unused")
        }

        override fun setEditor(editor: HighlighterClient) = Unit
    }

    private fun schemeWith(color: Color): EditorColorsScheme =
        (EditorColorsManager.getInstance().globalScheme.clone() as EditorColorsScheme).apply {
            setAttributes(key, TextAttributes(color, null, null, null, 0))
        }

    private val frozen = TextAttributes(Color.RED, null, null, null, 0)

    fun testResolvesTokenColorsAgainstTheEditorScheme() {
        val highlighter = SchemeAwareHighlighter(FrozenHighlighter(frozen, arrayOf(key)), schemeWith(Color.BLUE))

        assertEquals(Color.BLUE, highlighter.createIterator(0).textAttributes.foregroundColor)
    }

    fun testFollowsSchemeChanges() {
        val highlighter = SchemeAwareHighlighter(FrozenHighlighter(frozen, arrayOf(key)), schemeWith(Color.BLUE))
        highlighter.createIterator(0).textAttributes

        highlighter.setColorScheme(schemeWith(Color.GREEN))

        assertEquals(Color.GREEN, highlighter.createIterator(0).textAttributes.foregroundColor)
    }

    fun testKeepsFrozenAttributesForTokensWithoutKeys() {
        val highlighter = SchemeAwareHighlighter(FrozenHighlighter(frozen, emptyArray()), schemeWith(Color.BLUE))

        assertSame(frozen, highlighter.createIterator(0).textAttributes)
    }
}
