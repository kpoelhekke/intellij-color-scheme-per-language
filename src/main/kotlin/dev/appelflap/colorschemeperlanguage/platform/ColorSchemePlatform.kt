package dev.appelflap.colorschemeperlanguage.platform

import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsScheme

data class PlatformEditorContext(
    val language: Language?,
)

interface ColorSchemePlatform {
    fun contextFor(editor: Editor): PlatformEditorContext?

    fun currentGlobalScheme(): EditorColorsScheme

    fun findScheme(name: String): EditorColorsScheme?

    fun installedSchemes(): List<EditorColorsScheme>

    fun applySchemeToEditor(editor: Editor, scheme: EditorColorsScheme): Boolean
}
