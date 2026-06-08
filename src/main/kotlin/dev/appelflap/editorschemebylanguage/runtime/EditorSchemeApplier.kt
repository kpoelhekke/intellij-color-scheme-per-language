package dev.appelflap.editorschemebylanguage.runtime

import com.intellij.openapi.editor.Editor
import dev.appelflap.editorschemebylanguage.matching.EditorSchemeMatcher
import dev.appelflap.editorschemebylanguage.model.EditorSchemeContext
import dev.appelflap.editorschemebylanguage.model.SchemeRef
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import dev.appelflap.editorschemebylanguage.platform.EditorSchemePlatform
import dev.appelflap.editorschemebylanguage.platform.PlatformEditorContext

class EditorSchemeApplier(
    private val platform: EditorSchemePlatform,
    private val enabled: () -> Boolean,
    private val rules: () -> List<SchemeRule>,
) {
    fun applyForEditor(editor: Editor, context: PlatformEditorContext? = null): Boolean {
        val platformContext = context ?: platform.contextFor(editor) ?: return false
        val defaultScheme = platform.currentGlobalScheme()
        val selectedScheme = EditorSchemeMatcher.resolve(
            enabled = enabled(),
            rules = rules(),
            context = platformContext.toEditorSchemeContext(),
            installedSchemes = platform.installedSchemes().map { SchemeRef(it.name) },
            defaultScheme = SchemeRef(defaultScheme.name),
        )

        val scheme = platform.findScheme(selectedScheme.name) ?: defaultScheme
        return platform.applySchemeToEditor(editor, scheme)
    }

    private fun PlatformEditorContext.toEditorSchemeContext(): EditorSchemeContext =
        EditorSchemeContext(
            languageId = language?.id,
            languageDisplayName = language?.displayName,
            fileTypeId = fileType?.name,
            fileTypeDisplayName = fileType?.displayName,
        )
}
