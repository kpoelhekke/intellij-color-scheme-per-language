package dev.appelflap.colorschemeperlanguage.diff

import com.intellij.lang.Language
import com.intellij.lexer.EmptyLexer
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.editor.highlighter.HighlighterClient
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.Color

class SchemeAwareHighlighterTest : BasePlatformTestCase() {
    private object TestLanguage : Language("ColorSchemePerLanguageTest")

    private companion object {
        val KEY = TextAttributesKey.createTextAttributesKey("COLOR_SCHEME_PER_LANGUAGE_TEST_KEY")
        val MAPPED_TOKEN = IElementType("MAPPED_TOKEN", TestLanguage)
        val UNMAPPED_TOKEN = IElementType("UNMAPPED_TOKEN", TestLanguage)
    }

    override fun setUp() {
        super.setUp()
        val highlighter = object : SyntaxHighlighterBase() {
            override fun getHighlightingLexer(): Lexer = EmptyLexer()

            override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
                if (tokenType == MAPPED_TOKEN) arrayOf(KEY) else TextAttributesKey.EMPTY_ARRAY
        }
        val factory = object : SyntaxHighlighterFactory() {
            override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
                highlighter
        }
        SyntaxHighlighterFactory.getLanguageFactory().addExplicitExtension(TestLanguage, factory, testRootDisposable)
    }

    // Mimics the unified diff highlighter: attributes frozen from another scheme. One token over [0, 1); the
    // iterator must reach its end, or an editor laying out with this highlighter spins forever on the EDT.
    private class FrozenHighlighter(
        private val attributes: TextAttributes,
        private val token: IElementType?,
    ) : EditorHighlighter {
        override fun createIterator(startOffset: Int): HighlighterIterator = object : HighlighterIterator {
            private var atEnd = startOffset >= 1

            override fun getTextAttributes(): TextAttributes = attributes

            override fun getStart(): Int = 0

            override fun getEnd(): Int = 1

            override fun getTokenType(): IElementType? = token

            override fun advance() {
                atEnd = true
            }

            override fun retreat() {
                atEnd = false
            }

            override fun atEnd(): Boolean = atEnd

            override fun getDocument(): Document = error("unused")
        }

        override fun setEditor(editor: HighlighterClient) = Unit
    }

    private fun schemeWith(color: Color): EditorColorsScheme =
        (EditorColorsManager.getInstance().globalScheme.clone() as EditorColorsScheme).apply {
            setAttributes(KEY, TextAttributes(color, null, null, null, 0))
        }

    private val frozen = TextAttributes(Color.RED, null, null, null, 0)

    private fun wrapped(token: IElementType?, scheme: EditorColorsScheme) =
        SchemeAwareHighlighter(FrozenHighlighter(frozen, token), scheme, project, null)

    fun testResolvesTokenColorsAgainstTheEditorScheme() {
        val highlighter = wrapped(MAPPED_TOKEN, schemeWith(Color.BLUE))

        assertEquals(Color.BLUE, highlighter.createIterator(0).textAttributes.foregroundColor)
    }

    fun testFollowsSchemeChanges() {
        val highlighter = wrapped(MAPPED_TOKEN, schemeWith(Color.BLUE))
        assertEquals(Color.BLUE, highlighter.createIterator(0).textAttributes.foregroundColor)

        highlighter.setColorScheme(schemeWith(Color.GREEN))

        assertEquals(Color.GREEN, highlighter.createIterator(0).textAttributes.foregroundColor)
    }

    fun testKeepsFrozenAttributesForTokensWithoutKeys() {
        assertSame(frozen, wrapped(UNMAPPED_TOKEN, schemeWith(Color.BLUE)).createIterator(0).textAttributes)
        assertSame(frozen, wrapped(null, schemeWith(Color.BLUE)).createIterator(0).textAttributes)
    }

    fun testInstallWrapsEachNewHighlighterButLeavesEmptyOnesAlone() {
        val factory = EditorFactory.getInstance()
        val editor = factory.createViewer(factory.createDocument("x")) as EditorEx
        try {
            // A fresh editor starts with an empty highlighter; re-attaching it would log an error and fail the test.
            SchemeAwareHighlighter.install(editor, project, null, testRootDisposable)
            assertFalse(editor.highlighter is SchemeAwareHighlighter)

            editor.highlighter = FrozenHighlighter(frozen, MAPPED_TOKEN)

            assertTrue(editor.highlighter is SchemeAwareHighlighter)
        } finally {
            factory.releaseEditor(editor)
        }
    }
}
