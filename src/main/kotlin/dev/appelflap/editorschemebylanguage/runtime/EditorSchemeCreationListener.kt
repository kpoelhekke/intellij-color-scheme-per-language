package dev.appelflap.editorschemebylanguage.runtime

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import dev.appelflap.editorschemebylanguage.platform.IntellijEditorSchemePlatform
import dev.appelflap.editorschemebylanguage.settings.EditorSchemeSettingsState

class EditorSchemeCreationListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        val project = event.editor.project ?: return
        val settings = EditorSchemeSettingsState.getInstance()
        val applier = EditorSchemeApplier(
            platform = IntellijEditorSchemePlatform(project),
            enabled = { settings.enabled },
            rules = { settings.rules.map { it.copy() } },
        )

        EditorSchemeCreationHandler(applier).applyForCreatedEditor(event.editor)
    }
}

class EditorSchemeCreationHandler(
    private val applier: EditorSchemeApplier,
) {
    fun applyForCreatedEditor(editor: Editor): Boolean =
        applier.applyForEditor(editor)
}
