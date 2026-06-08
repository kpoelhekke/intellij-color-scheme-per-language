package dev.appelflap.editorschemebylanguage.runtime

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import dev.appelflap.editorschemebylanguage.platform.EditorSchemePlatform
import dev.appelflap.editorschemebylanguage.platform.IntellijEditorSchemePlatform
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
    private val applier = EditorSchemeApplier(platform, enabled, rules)

    fun applyForEditor(editor: Editor): Boolean =
        applier.applyForEditor(editor)
}
