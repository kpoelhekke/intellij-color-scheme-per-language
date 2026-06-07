package dev.appelflap.editorschemebylanguage.settings

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.table.JBTable
import dev.appelflap.editorschemebylanguage.model.RuleTargetKind
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import org.junit.Assert.assertEquals
import java.awt.Container
import javax.swing.DefaultCellEditor
import javax.swing.JTextField

class EditorSchemeSettingsPanelBehaviorTest : BasePlatformTestCase() {
    fun testRulesSnapshotDoesNotCommitActiveTableEditor() {
        val colorManager = EditorColorsManager.getInstance()
        val originalScheme = colorManager.globalScheme
        val editedScheme = colorManager.allSchemes.first { it.name != originalScheme.name }
        val panel = EditorSchemeSettingsPanel()
        panel.setState(
            enabled = true,
            rules = listOf(
                SchemeRule(
                    targetKind = RuleTargetKind.LANGUAGE,
                    targetId = "kotlin",
                    targetDisplayName = "Kotlin",
                    schemeName = originalScheme.name,
                ),
            ),
        )
        val table = findTable(panel.component())
        table.columnModel.getColumn(SCHEME_COLUMN).cellEditor = DefaultCellEditor(JTextField())

        table.editCellAt(0, SCHEME_COLUMN)
        (table.editorComponent as JTextField).text = editedScheme.name

        assertEquals(originalScheme.name, panel.rulesSnapshot().single().schemeName)
        assertEquals(editedScheme.name, panel.commitAndRules().single().schemeName)
    }

    private fun findTable(container: Container): JBTable {
        container.components.forEach { component ->
            if (component is JBTable) {
                return component
            }
            if (component is Container) {
                val table = runCatching { findTable(component) }.getOrNull()
                if (table != null) {
                    return table
                }
            }
        }

        error("Settings rules table was not found")
    }

    private companion object {
        private const val SCHEME_COLUMN = 1
    }
}
