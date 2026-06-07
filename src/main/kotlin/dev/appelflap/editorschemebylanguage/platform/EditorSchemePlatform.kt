package dev.appelflap.editorschemebylanguage.platform

import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.fileTypes.FileType

data class PlatformEditorContext(
    val language: Language?,
    val fileType: FileType?,
)

interface EditorSchemePlatform {
    fun contextFor(editor: Editor): PlatformEditorContext?

    fun currentGlobalScheme(): EditorColorsScheme

    fun findScheme(name: String): EditorColorsScheme?

    fun installedSchemes(): List<EditorColorsScheme>

    fun applySchemeToEditor(editor: Editor, scheme: EditorColorsScheme): Boolean
}
