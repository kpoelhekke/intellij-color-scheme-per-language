package dev.appelflap.editorschemebylanguage.settings

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.ProjectManager
import dev.appelflap.editorschemebylanguage.EditorSchemeByLanguageBundle
import dev.appelflap.editorschemebylanguage.platform.IntellijEditorSchemePlatform
import dev.appelflap.editorschemebylanguage.runtime.EditorSchemeSelectionHandler
import javax.swing.JComponent

class EditorSchemeConfigurable : Configurable {
    private var panel: EditorSchemeSettingsPanel? = null

    override fun getDisplayName(): String =
        EditorSchemeByLanguageBundle.message("settings.display.name")

    override fun createComponent(): JComponent {
        val settingsPanel = EditorSchemeSettingsPanel()
        panel = settingsPanel
        reset()
        return settingsPanel.component()
    }

    override fun isModified(): Boolean {
        val settings = EditorSchemeSettingsState.getInstance()
        val settingsPanel = panel ?: return false

        return settingsPanel.isEnabledSelected() != settings.enabled ||
            settingsPanel.rulesSnapshot() != settings.rules
    }

    override fun apply() {
        val settingsPanel = panel ?: return
        val validation = settingsPanel.validationResult()
        if (!validation.isValid) {
            throw ConfigurationException(validation.message)
        }

        val settings = EditorSchemeSettingsState.getInstance()
        settings.update(settingsPanel.isEnabledSelected(), settingsPanel.commitAndRules())
        applyToSelectedEditor(settings)
    }

    override fun reset() {
        val settings = EditorSchemeSettingsState.getInstance()
        panel?.setState(settings.enabled, settings.rules)
    }

    override fun disposeUIResources() {
        panel = null
    }

    private fun applyToSelectedEditor(settings: EditorSchemeSettingsState) {
        ProjectManager.getInstance().openProjects.forEach { project ->
            val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return@forEach
            val handler = EditorSchemeSelectionHandler(
                platform = IntellijEditorSchemePlatform(project),
                enabled = { settings.enabled },
                rules = { settings.rules.map { it.copy() } },
            )

            handler.applyForEditor(editor)
        }
    }
}
