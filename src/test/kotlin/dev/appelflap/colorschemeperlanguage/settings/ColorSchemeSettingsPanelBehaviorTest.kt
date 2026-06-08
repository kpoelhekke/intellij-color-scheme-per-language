package dev.appelflap.colorschemeperlanguage.settings

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.table.JBTable
import dev.appelflap.colorschemeperlanguage.model.SchemeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.awt.Container
import javax.swing.DefaultCellEditor
import javax.swing.JComponent
import javax.swing.JTextField

class ColorSchemeSettingsPanelBehaviorTest : BasePlatformTestCase() {
    fun testRulesSnapshotDoesNotCommitActiveTableEditor() {
        val colorManager = EditorColorsManager.getInstance()
        val originalScheme = colorManager.globalScheme
        val editedScheme = colorManager.allSchemes.first { it.name != originalScheme.name }
        val panel = ColorSchemeSettingsPanel()
        panel.setState(
            enabled = true,
            rules = listOf(
                SchemeRule(
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

    fun testConfigurableIsModifiedSeesActiveEditedSchemeBeforeCommit() {
        val colorManager = EditorColorsManager.getInstance()
        val originalScheme = colorManager.globalScheme
        val editedScheme = colorManager.allSchemes.first { it.name != originalScheme.name }
        val settings = ColorSchemeSettingsState.getInstance()
        val previousEnabled = settings.enabled
        val previousRules = settings.rules.map { it.copy() }
        val configurable = ColorSchemeConfigurable()

        try {
            settings.update(
                enabled = true,
                rules = listOf(
                    SchemeRule(
                        targetId = "kotlin",
                        targetDisplayName = "Kotlin",
                        schemeName = originalScheme.name,
                    ),
                ),
            )
            val component = configurable.createComponent()
            val table = findTable(component)
            table.columnModel.getColumn(SCHEME_COLUMN).cellEditor = DefaultCellEditor(JTextField())

            table.editCellAt(0, SCHEME_COLUMN)
            (table.editorComponent as JTextField).text = editedScheme.name

            assertTrue(configurable.isModified)
            assertEquals(originalScheme.name, settings.rules.single().schemeName)
        } finally {
            configurable.disposeUIResources()
            settings.update(previousEnabled, previousRules)
        }
    }

    fun testRulesSnapshotWithActiveEditorValueOverlaysUncommittedScheme() {
        val colorManager = EditorColorsManager.getInstance()
        val originalScheme = colorManager.globalScheme
        val editedScheme = colorManager.allSchemes.first { it.name != originalScheme.name }
        val panel = ColorSchemeSettingsPanel()
        panel.setState(
            enabled = true,
            rules = listOf(
                SchemeRule(
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

        assertEquals(editedScheme.name, panel.rulesSnapshotWithActiveEditorValue().single().schemeName)
        assertEquals(originalScheme.name, panel.rulesSnapshot().single().schemeName)
    }

    fun testSetStateCancelsActiveEditorBeforeReplacingRules() {
        val colorManager = EditorColorsManager.getInstance()
        val originalScheme = colorManager.globalScheme
        val editedScheme = colorManager.allSchemes.first { it.name != originalScheme.name }
        val panel = ColorSchemeSettingsPanel()
        panel.setState(
            enabled = true,
            rules = listOf(
                SchemeRule(
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

        panel.setState(
            enabled = true,
            rules = listOf(
                SchemeRule(
                    targetId = "kotlin",
                    targetDisplayName = "Kotlin",
                    schemeName = originalScheme.name,
                ),
            ),
        )

        table.cellEditor?.stopCellEditing()

        assertFalse(table.isEditing)
        assertEquals(originalScheme.name, panel.rulesSnapshot().single().schemeName)
    }

    fun testDuplicateTargetAddIsRejectedImmediately() {
        val panel = ColorSchemeSettingsPanel()
        panel.setState(
            enabled = true,
            rules = listOf(
                SchemeRule(
                    targetId = "kotlin",
                    targetDisplayName = "Kotlin",
                    schemeName = "Default",
                ),
            ),
        )

        val added = panel.addRuleForTarget(
            RuleTargetChooserDialog.RuleTarget(
                id = "kotlin",
                displayName = "Kotlin",
            ),
        )

        assertFalse(added)
        assertEquals(1, panel.rulesSnapshot().size)
    }

    fun testUniqueTargetAddIsAccepted() {
        val panel = ColorSchemeSettingsPanel()
        panel.setState(
            enabled = true,
            rules = listOf(
                SchemeRule(
                    targetId = "kotlin",
                    targetDisplayName = "Kotlin",
                    schemeName = "Default",
                ),
            ),
        )

        val added = panel.addRuleForTarget(
            RuleTargetChooserDialog.RuleTarget(
                id = "java",
                displayName = "Java",
            ),
        )

        assertTrue(added)
        assertEquals(2, panel.rulesSnapshot().size)
    }

    private fun findTable(container: JComponent): JBTable =
        findTable(container as Container)

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
