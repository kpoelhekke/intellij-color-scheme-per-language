package dev.appelflap.colorschemeperlanguage.runtime

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import dev.appelflap.colorschemeperlanguage.model.SchemeRule
import dev.appelflap.colorschemeperlanguage.platform.ColorSchemePlatform
import dev.appelflap.colorschemeperlanguage.platform.IntellijColorSchemePlatform
import dev.appelflap.colorschemeperlanguage.settings.ColorSchemeSettingsState

class ColorSchemeSelectionListener : FileEditorManagerListener {
    override fun selectionChanged(event: FileEditorManagerEvent) {
        val textEditor = event.newEditor as? TextEditor ?: return
        val settings = ColorSchemeSettingsState.getInstance()
        val handler = ColorSchemeSelectionHandler(
            platform = IntellijColorSchemePlatform(event.manager.project),
            enabled = { settings.enabled },
            rules = { settings.rules.map { it.copy() } },
        )

        handler.applyForEditor(textEditor.editor)
    }
}

class ColorSchemeSelectionHandler(
    private val platform: ColorSchemePlatform,
    private val enabled: () -> Boolean,
    private val rules: () -> List<SchemeRule>,
) {
    private val applier = ColorSchemeApplier(platform, enabled, rules)

    fun applyForEditor(editor: Editor): Boolean =
        applier.applyForEditor(editor)
}
