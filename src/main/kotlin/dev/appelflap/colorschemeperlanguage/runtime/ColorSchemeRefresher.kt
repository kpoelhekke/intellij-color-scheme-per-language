package dev.appelflap.colorschemeperlanguage.runtime

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.ProjectManager
import dev.appelflap.colorschemeperlanguage.platform.IntellijColorSchemePlatform
import dev.appelflap.colorschemeperlanguage.settings.ColorSchemeSettingsState

object ColorSchemeRefresher {
    // invokeLater (rather than running inline) keeps this safe when triggered from
    // loadState() during service initialization and from non-EDT settings-sync imports.
    fun scheduleRefresh() {
        val application = ApplicationManager.getApplication() ?: return
        application.invokeLater { refreshOpenEditors() }
    }

    private fun refreshOpenEditors() {
        val settings = ColorSchemeSettingsState.getInstance()
        ProjectManager.getInstance().openProjects.forEach { project ->
            val handler = ColorSchemeRefreshHandler(
                ColorSchemeApplier(
                    platform = IntellijColorSchemePlatform(project),
                    enabled = { settings.enabled },
                    rules = { settings.rules.map { it.copy() } },
                ),
            )
            val editors = FileEditorManager.getInstance(project).allEditors
                .filterIsInstance<TextEditor>()
                .map { it.editor }
            handler.refreshEditors(editors)
        }
    }
}

class ColorSchemeRefreshHandler(
    private val applier: ColorSchemeApplier,
) {
    fun refreshEditors(editors: List<Editor>): Int =
        editors.count { applier.applyForEditor(it) }
}
