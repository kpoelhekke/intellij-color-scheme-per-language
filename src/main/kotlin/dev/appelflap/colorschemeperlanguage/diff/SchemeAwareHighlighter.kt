package dev.appelflap.colorschemeperlanguage.diff

import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EmptyEditorHighlighter
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.tree.IElementType
import java.util.concurrent.ConcurrentHashMap

/**
 * The unified diff viewer builds its syntax highlighting from highlighters created with the *global* scheme and
 * copies their resolved colors into a highlighter that ignores [setColorScheme]. So a per-language scheme set on
 * that editor keeps the global scheme's token colors on top of its own background. This wrapper re-resolves each
 * token's colors against the editor's scheme, looking up the token's attribute keys from the language's syntax
 * highlighter (the iterator's own key accessor is experimental API).
 *
 * shortcut: the delegate gets no setText/document events (Kotlin `by` skips Java default methods); fine for the
 * unified diff highlighter, which ignores both. Forward them explicitly if this ever wraps other highlighters.
 */
class SchemeAwareHighlighter(
    private val delegate: EditorHighlighter,
    private var scheme: EditorColorsScheme,
    private val project: Project?,
    private val fileLanguage: Language?,
) : EditorHighlighter by delegate {
    private val keysByToken = ConcurrentHashMap<IElementType, Array<TextAttributesKey>>()
    private val resolved = ConcurrentHashMap<IElementType, TextAttributes>()

    override fun setColorScheme(scheme: EditorColorsScheme) {
        this.scheme = scheme
        resolved.clear()
        delegate.setColorScheme(scheme)
    }

    override fun createIterator(startOffset: Int): HighlighterIterator {
        val iterator = delegate.createIterator(startOffset)
        return object : HighlighterIterator by iterator {
            override fun getTextAttributes(): TextAttributes = resolve(iterator.tokenType) ?: iterator.textAttributes
        }
    }

    private fun resolve(token: IElementType?): TextAttributes? {
        if (token == null) return null
        val keys = keysByToken.getOrPut(token) { keysFor(token) }
        if (keys.isEmpty()) return null
        return resolved.getOrPut(token) { merge(keys) }
    }

    // The diffed file's highlighter knows dialect-specific keys; the token's own language covers embedded tokens.
    private fun keysFor(token: IElementType): Array<TextAttributesKey> =
        listOfNotNull(fileLanguage, token.language.takeIf { it != Language.ANY })
            .asSequence()
            .map { SyntaxHighlighterFactory.getSyntaxHighlighter(it, project, null).getTokenHighlights(token) }
            .firstOrNull { it.isNotEmpty() }
            ?: TextAttributesKey.EMPTY_ARRAY

    // Mirrors the platform's LayeredTextAttributes.create, which is internal API.
    private fun merge(keys: Array<TextAttributesKey>): TextAttributes =
        keys.fold(TextAttributes()) { result, key ->
            scheme.getAttributes(key)?.let { TextAttributes.merge(result, it) } ?: result
        }

    companion object {
        fun install(editor: EditorEx, project: Project?, fileLanguage: Language?, parentDisposable: Disposable) {
            wrap(editor, project, fileLanguage)
            // The viewer replaces its highlighter on every re-diff, so wrap each new one. Wrapping inside the event
            // makes the editor reinit twice, but deferring it would paint a frame in the global scheme's colors.
            editor.addPropertyChangeListener(
                { event -> if (event.propertyName == EditorEx.PROP_HIGHLIGHTER) wrap(editor, project, fileLanguage) },
                parentDisposable,
            )
        }

        private fun wrap(editor: EditorEx, project: Project?, fileLanguage: Language?) {
            val highlighter = editor.highlighter
            // Empty highlighters already follow the scheme, and re-attaching one to the editor logs an error.
            if (highlighter is SchemeAwareHighlighter || highlighter is EmptyEditorHighlighter) return
            editor.highlighter = SchemeAwareHighlighter(highlighter, editor.colorsScheme, project, fileLanguage)
        }
    }
}
