package dev.appelflap.editorschemebylanguage.runtime

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import dev.appelflap.editorschemebylanguage.matching.EditorSchemeMatcher
import dev.appelflap.editorschemebylanguage.model.EditorSchemeContext
import dev.appelflap.editorschemebylanguage.model.SchemeRef
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import dev.appelflap.editorschemebylanguage.platform.EditorSchemePlatform
import dev.appelflap.editorschemebylanguage.platform.IntellijEditorSchemePlatform
import dev.appelflap.editorschemebylanguage.platform.PlatformEditorContext
import dev.appelflap.editorschemebylanguage.settings.EditorSchemeSettingsState

class EditorSchemeSelectionListener : FileEditorManagerListener {
    override fun selectionChanged(event: FileEditorManagerEvent) {
        val textEditor = event.newEditor as? TextEditor ?: return
        val settings = EditorSchemeSettingsState.getInstance()
        val handler = EditorSchemeSelectionHandler(
            platform = IntellijEditorSchemePlatform(event.manager.project),
            enabled = { settings.enabled },
            rules = { settings.rules.map { it.copy() } },
        )

        handler.applyForEditor(textEditor.editor)
    }
}

class EditorSchemeSelectionHandler(
    private val platform: EditorSchemePlatform,
    private val enabled: () -> Boolean,
    private val rules: () -> List<SchemeRule>,
) {
    fun applyForEditor(editor: Editor): Boolean {
        val platformContext = platform.contextFor(editor) ?: return false
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
