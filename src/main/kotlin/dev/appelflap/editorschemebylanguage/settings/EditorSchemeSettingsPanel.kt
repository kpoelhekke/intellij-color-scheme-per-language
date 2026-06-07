package dev.appelflap.editorschemebylanguage.settings

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import dev.appelflap.editorschemebylanguage.EditorSchemeByLanguageBundle
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import java.awt.BorderLayout
import javax.swing.DefaultCellEditor
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableCellEditor

class EditorSchemeSettingsPanel {
    private val enabledCheckBox = JCheckBox(EditorSchemeByLanguageBundle.message("settings.enable"))
    private val tableModel = RulesTableModel()
    private val table = JBTable(tableModel)
    private val rootPanel = JPanel(BorderLayout())

    init {
        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        table.emptyText.text = EditorSchemeByLanguageBundle.message("settings.rules.title")
        table.columnModel.getColumn(SCHEME_COLUMN).cellEditor = createSchemeEditor()

        val tablePanel = ToolbarDecorator.createDecorator(table)
            .setAddAction {
                val chooser = RuleTargetChooserDialog()
                if (chooser.showAndGet()) {
                    chooser.selectedTarget?.let { target ->
                        tableModel.addRule(
                            SchemeRule(
                                targetKind = target.kind,
                                targetId = target.id,
                                targetDisplayName = target.displayName,
                                schemeName = currentGlobalSchemeName(),
                            ),
                        )
                    }
                }
            }
            .setRemoveAction {
                val selectedRow = table.selectedRow
                if (selectedRow >= 0) {
                    tableModel.removeRule(table.convertRowIndexToModel(selectedRow))
                }
            }
            .createPanel()

        rootPanel.add(enabledCheckBox, BorderLayout.NORTH)
        rootPanel.add(tablePanel, BorderLayout.CENTER)
    }

    fun component(): JPanel = rootPanel

    fun setState(enabled: Boolean, rules: List<SchemeRule>) {
        enabledCheckBox.isSelected = enabled
        tableModel.setRules(rules)
    }

    fun isEnabledSelected(): Boolean = enabledCheckBox.isSelected

    fun rulesSnapshot(): List<SchemeRule> =
        tableModel.rules()

    fun commitAndRules(): List<SchemeRule> {
        commitActiveTableEdit()
        return rulesSnapshot()
    }

    fun validationResult(): ValidationResult =
        validateRules(commitAndRules(), installedSchemeNames())

    private fun createSchemeEditor(): TableCellEditor =
        DefaultCellEditor(JComboBox(installedSchemeNames().toTypedArray()))

    private fun installedSchemeNames(): List<String> =
        EditorColorsManager.getInstance().allSchemes.map { it.name }

    private fun currentGlobalSchemeName(): String =
        EditorColorsManager.getInstance().globalScheme.name

    private fun commitActiveTableEdit() {
        if (table.isEditing) {
            table.cellEditor?.stopCellEditing()
        }
    }

    private class RulesTableModel : AbstractTableModel() {
        private val rules = mutableListOf<SchemeRule>()

        override fun getRowCount(): Int = rules.size

        override fun getColumnCount(): Int = 2

        override fun getColumnName(column: Int): String =
            when (column) {
                TARGET_COLUMN -> EditorSchemeByLanguageBundle.message("settings.column.target")
                SCHEME_COLUMN -> EditorSchemeByLanguageBundle.message("settings.column.scheme")
                else -> super.getColumnName(column)
            }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean =
            columnIndex == SCHEME_COLUMN

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any =
            when (columnIndex) {
                TARGET_COLUMN -> rules[rowIndex].targetDisplayName
                SCHEME_COLUMN -> rules[rowIndex].schemeName
                else -> ""
            }

        override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
            if (columnIndex == SCHEME_COLUMN) {
                rules[rowIndex].schemeName = value?.toString().orEmpty()
                fireTableCellUpdated(rowIndex, columnIndex)
            }
        }

        fun setRules(newRules: List<SchemeRule>) {
            rules.clear()
            rules.addAll(newRules.map { it.copy() })
            fireTableDataChanged()
        }

        fun addRule(rule: SchemeRule) {
            rules.add(rule.copy())
            fireTableRowsInserted(rules.lastIndex, rules.lastIndex)
        }

        fun removeRule(index: Int) {
            rules.removeAt(index)
            fireTableRowsDeleted(index, index)
        }

        fun rules(): List<SchemeRule> = rules.map { it.copy() }
    }

    data class ValidationResult(
        val isValid: Boolean,
        val message: String? = null,
    )

    companion object {
        private const val TARGET_COLUMN = 0
        private const val SCHEME_COLUMN = 1

        fun validateRules(
            rules: List<SchemeRule>,
            installedSchemeNames: List<String>,
        ): ValidationResult {
            if (rules.map { it.targetKey() }.distinct().size != rules.size) {
                return ValidationResult(
                    isValid = false,
                    message = EditorSchemeByLanguageBundle.message("settings.validation.duplicate"),
                )
            }

            val installed = installedSchemeNames.toSet()
            if (rules.any { it.schemeName !in installed }) {
                return ValidationResult(
                    isValid = false,
                    message = EditorSchemeByLanguageBundle.message("settings.validation.missing.scheme"),
                )
            }

            return ValidationResult(isValid = true)
        }
    }
}
