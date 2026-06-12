package dev.appelflap.colorschemeperlanguage.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import dev.appelflap.colorschemeperlanguage.model.SchemeRule
import dev.appelflap.colorschemeperlanguage.runtime.ColorSchemeRefresher

@Service(Service.Level.APP)
@State(
    name = "ColorSchemePerLanguageSettings",
    storages = [Storage("colorSchemePerLanguage.xml")],
)
class ColorSchemeSettingsState : PersistentStateComponent<ColorSchemeSettingsState> {
    var enabled: Boolean = true

    // var + MutableList is required by IntelliJ's XML state serializer (reflective population)
    // and by loadState reassigning the field.
    @Suppress("DoubleMutabilityForCollection")
    var rules: MutableList<SchemeRule> = mutableListOf()

    override fun getState(): ColorSchemeSettingsState = this

    override fun loadState(state: ColorSchemeSettingsState) {
        update(state.enabled, state.rules)
    }

    fun update(enabled: Boolean, rules: List<SchemeRule>) {
        this.enabled = enabled
        this.rules = rules.map { it.copy() }.toMutableList()
        ColorSchemeRefresher.scheduleRefresh()
    }

    companion object {
        fun getInstance(): ColorSchemeSettingsState =
            ApplicationManager.getApplication().getService(ColorSchemeSettingsState::class.java)
    }
}
