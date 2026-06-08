package dev.appelflap.colorschemeperlanguage.runtime

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import dev.appelflap.colorschemeperlanguage.platform.IntellijColorSchemePlatform
import dev.appelflap.colorschemeperlanguage.settings.ColorSchemeSettingsState

class ColorSchemeCreationListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        val project = event.editor.project ?: return
        val settings = ColorSchemeSettingsState.getInstance()
        val applier = ColorSchemeApplier(
            platform = IntellijColorSchemePlatform(project),
            enabled = { settings.enabled },
            rules = { settings.rules.map { it.copy() } },
        )

        ColorSchemeCreationHandler(applier).applyForCreatedEditor(event.editor)
    }
}

class ColorSchemeCreationHandler(
    private val applier: ColorSchemeApplier,
) {
    fun applyForCreatedEditor(editor: Editor): Boolean =
        applier.applyForEditor(editor)
}
