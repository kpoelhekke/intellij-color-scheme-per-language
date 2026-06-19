package dev.appelflap.colorschemeperlanguage.platform

import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.colors.FontPreferences
import com.intellij.openapi.editor.colors.impl.DelegateColorScheme
import dev.appelflap.colorschemeperlanguage.matching.EditorFontSource
import dev.appelflap.colorschemeperlanguage.matching.EditorFontSourceResolver
import java.awt.Font

/**
 * Wraps a per-language override scheme so its colors apply while the editor font follows the cascade
 * override scheme -> default/main scheme -> app default. See [EditorFontSourceResolver].
 *
 * Colors and everything non-font delegate to the override scheme unchanged. Only the editor font is
 * redirected, and only when the override scheme uses the app default font while the main scheme defines
 * its own (otherwise the override scheme already reports the right font). [globalScheme] is read live so
 * the font keeps following the main scheme as it changes.
 *
 * Font setters are intentionally no-ops: the wrapped scheme is the shared installed scheme object used by
 * the Settings UI and every other editor, so a stray `setEditorFontSize` must not mutate it.
 */
class FontFollowingColorScheme(
    override: EditorColorsScheme,
    private val globalScheme: () -> EditorColorsScheme,
) : DelegateColorScheme(override) {
    private fun fontSource(): EditorColorsScheme {
        val global = globalScheme()
        return when (
            EditorFontSourceResolver.choose(
                overrideUsesAppFont = delegate.isUseAppFontPreferencesInEditor,
                defaultUsesAppFont = global.isUseAppFontPreferencesInEditor,
            )
        ) {
            EditorFontSource.OVERRIDE -> delegate
            EditorFontSource.DEFAULT -> global
        }
    }

    override fun getEditorFontName(): String = fontSource().editorFontName

    override fun getEditorFontSize(): Int = fontSource().editorFontSize

    override fun getEditorFontSize2D(): Float = fontSource().editorFontSize2D

    override fun getFontPreferences(): FontPreferences = fontSource().fontPreferences

    override fun getLineSpacing(): Float = fontSource().lineSpacing

    override fun getFont(key: EditorFontType?): Font = fontSource().getFont(key)

    override fun setEditorFontName(fontName: String?) = Unit

    override fun setEditorFontSize(fontSize: Int) = Unit

    override fun setEditorFontSize(fontSize: Float) = Unit

    override fun setFontPreferences(preferences: FontPreferences) = Unit

    override fun setLineSpacing(lineSpacing: Float) = Unit
}
