package dev.appelflap.editorschemebylanguage.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import dev.appelflap.editorschemebylanguage.model.SchemeRule

@Service(Service.Level.APP)
@State(
    name = "EditorSchemeByLanguageSettings",
    storages = [Storage("editorSchemeByLanguage.xml")],
)
class EditorSchemeSettingsState : PersistentStateComponent<EditorSchemeSettingsState> {
    var enabled: Boolean = true
    var rules: MutableList<SchemeRule> = mutableListOf()

    override fun getState(): EditorSchemeSettingsState = this

    override fun loadState(state: EditorSchemeSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun update(enabled: Boolean, rules: List<SchemeRule>) {
        this.enabled = enabled
        this.rules = rules.map { it.copy() }.toMutableList()
    }

    companion object {
        fun getInstance(): EditorSchemeSettingsState =
            ApplicationManager.getApplication().getService(EditorSchemeSettingsState::class.java)
    }
}
