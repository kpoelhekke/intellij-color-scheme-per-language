package dev.appelflap.colorschemeperlanguage.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import dev.appelflap.colorschemeperlanguage.ColorSchemePerLanguageBundle
import javax.swing.JComponent

class ColorSchemeConfigurable : Configurable {
    private var panel: ColorSchemeSettingsPanel? = null

    override fun getDisplayName(): String =
        ColorSchemePerLanguageBundle.message("settings.display.name")

    override fun createComponent(): JComponent {
        val settingsPanel = ColorSchemeSettingsPanel()
        panel = settingsPanel
        reset()
        return settingsPanel.component()
    }

    override fun isModified(): Boolean {
        val settings = ColorSchemeSettingsState.getInstance()
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

        val settings = ColorSchemeSettingsState.getInstance()
        settings.update(settingsPanel.isEnabledSelected(), settingsPanel.commitAndRules())
    }

    override fun reset() {
        val settings = ColorSchemeSettingsState.getInstance()
        panel?.setState(settings.enabled, settings.rules)
    }

    override fun disposeUIResources() {
        panel = null
    }
}
