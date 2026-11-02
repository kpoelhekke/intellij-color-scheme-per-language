package dev.appelflap.colorschemeperlanguage.diff

import com.intellij.diff.FrameDiffTool
import com.intellij.diff.tools.fragmented.UnifiedDiffViewer
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.openapi.editor.markup.TextAttributes
import java.util.concurrent.ConcurrentHashMap

/**
 * The unified diff viewer builds its syntax highlighting from highlighters created with the *global* scheme and
 * copies their resolved colors into a highlighter that ignores [setColorScheme]. So a per-language scheme set on
 * that editor keeps the global scheme's token colors on top of its own background. This wrapper re-resolves each
 * token's colors from its attribute keys against the editor's scheme, the same way lexer highlighters do.
 */
class SchemeAwareHighlighter(
    private val delegate: EditorHighlighter,
    private var scheme: EditorColorsScheme,
) : EditorHighlighter by delegate {
    private val resolved = ConcurrentHashMap<List<TextAttributesKey>, TextAttributes>()

    override fun setColorScheme(scheme: EditorColorsScheme) {
        this.scheme = scheme
        resolved.clear()
        delegate.setColorScheme(scheme)
    }

    override fun createIterator(startOffset: Int): HighlighterIterator {
        val iterator = delegate.createIterator(startOffset)
        return object : HighlighterIterator by iterator {
            override fun getTextAttributesKeys(): Array<TextAttributesKey> = iterator.textAttributesKeys

            override fun getTextAttributes(): TextAttributes {
                val keys = iterator.textAttributesKeys
                if (keys.isEmpty()) return iterator.textAttributes
                return resolved.getOrPut(keys.asList()) { merge(keys) }
            }
        }
    }

    // Mirrors the platform's LayeredTextAttributes.create, which is internal API.
    private fun merge(keys: Array<TextAttributesKey>): TextAttributes =
        keys.fold(TextAttributes()) { result, key ->
            scheme.getAttributes(key)?.let { TextAttributes.merge(result, it) } ?: result
        }

    companion object {
        fun installIfUnified(viewer: FrameDiffTool.DiffViewer, editor: EditorEx) {
            if (viewer !is UnifiedDiffViewer) return
            wrap(editor)
            // The viewer replaces its highlighter on every re-diff, so wrap each new one.
            editor.addPropertyChangeListener { event ->
                if (event.propertyName == EditorEx.PROP_HIGHLIGHTER) wrap(editor)
            }
        }

        private fun wrap(editor: EditorEx) {
            val highlighter = editor.highlighter
            if (highlighter !is SchemeAwareHighlighter) {
                editor.highlighter = SchemeAwareHighlighter(highlighter, editor.colorsScheme)
            }
        }
    }
}
