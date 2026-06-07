package dev.appelflap.editorschemebylanguage.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import dev.appelflap.editorschemebylanguage.EditorSchemeByLanguageBundle
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
            settingsPanel.rulesSnapshotWithActiveEditorValue() != settings.rules
    }

    override fun apply() {
        val settingsPanel = panel ?: return
        val validation = settingsPanel.validationResult()
        if (!validation.isValid) {
            throw ConfigurationException(validation.message)
        }

        val settings = EditorSchemeSettingsState.getInstance()
        settings.update(settingsPanel.isEnabledSelected(), settingsPanel.commitAndRules())
    }

    override fun reset() {
        val settings = EditorSchemeSettingsState.getInstance()
        panel?.setState(settings.enabled, settings.rules)
    }

    override fun disposeUIResources() {
        panel = null
    }
}
