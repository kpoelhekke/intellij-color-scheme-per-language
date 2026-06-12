package dev.appelflap.colorschemeperlanguage.settings

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.intellij.ui.table.JBTable
import dev.appelflap.colorschemeperlanguage.ColorSchemePerLanguageBundle
import dev.appelflap.colorschemeperlanguage.model.SchemeRule
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.DefaultCellEditor
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellEditor

class ColorSchemeSettingsPanel {
    private val enabledCheckBox = JCheckBox(ColorSchemePerLanguageBundle.message("settings.enable"))
    private val tableModel = RulesTableModel()
    private val table = JBTable(tableModel)
    private val rootPanel = JPanel(BorderLayout())

    init {
        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        table.emptyText.text = ColorSchemePerLanguageBundle.message("settings.rules.title")
        table.columnModel.getColumn(SCHEME_COLUMN).cellEditor = createSchemeEditor()
        table.columnModel.getColumn(SCHEME_COLUMN).cellRenderer = SchemeCellRenderer()

        val tablePanel = ToolbarDecorator.createDecorator(table)
            .setAddAction {
                val chooser = RuleTargetChooserDialog(existingTargetKeys = tableModel.targetKeys())
                if (chooser.showAndGet()) {
                    chooser.selectedTarget?.let(::addRuleForTarget)
                }
            }
            .setRemoveAction {
                val selectedRow = table.selectedRow
                if (selectedRow >= 0) {
                    cancelActiveTableEdit()
                    tableModel.removeRule(table.convertRowIndexToModel(selectedRow))
                }
            }
            .createPanel()

        rootPanel.add(enabledCheckBox, BorderLayout.NORTH)
        rootPanel.add(tablePanel, BorderLayout.CENTER)
    }

    fun component(): JPanel = rootPanel

    fun setState(enabled: Boolean, rules: List<SchemeRule>) {
        cancelActiveTableEdit()
        enabledCheckBox.isSelected = enabled
        tableModel.setRules(rules)
    }

    fun isEnabledSelected(): Boolean = enabledCheckBox.isSelected

    fun rulesSnapshot(): List<SchemeRule> =
        tableModel.rules()

    fun rulesSnapshotWithActiveEditorValue(): List<SchemeRule> {
        val rules = rulesSnapshot()
        if (!table.isEditing || table.editingColumn != SCHEME_COLUMN) {
            return rules
        }

        val editingRow = table.editingRow
        if (editingRow < 0) {
            return rules
        }

        val modelRow = table.convertRowIndexToModel(editingRow)
        if (modelRow !in rules.indices) {
            return rules
        }

        return rules.mapIndexed { index, rule ->
            if (index == modelRow) {
                rule.copy(schemeName = table.cellEditor?.cellEditorValue?.toString().orEmpty())
            } else {
                rule
            }
        }
    }

    fun commitAndRules(): List<SchemeRule> {
        check(commitActiveTableEdit()) { "Active table editor rejected the current value" }
        return rulesSnapshot()
    }

    fun validationResult(): ValidationResult {
        if (!commitActiveTableEdit()) {
            return ValidationResult(
                isValid = false,
                message = ColorSchemePerLanguageBundle.message("settings.validation.missing.scheme"),
            )
        }

        return validateRules(rulesSnapshot(), installedSchemeNames())
    }

    fun addRuleForTarget(target: RuleTargetChooserDialog.RuleTarget): Boolean {
        cancelActiveTableEdit()
        if (target.targetKey() in tableModel.targetKeys()) {
            return false
        }

        tableModel.addRule(
            SchemeRule(
                targetId = target.id,
                targetDisplayName = target.displayName,
                schemeName = currentGlobalSchemeName(),
            ),
        )
        return true
    }

    private fun createSchemeEditor(): TableCellEditor {
        val comboBox = JComboBox(installedSchemeNames().toTypedArray())
        comboBox.renderer = textListCellRenderer("") { displaySchemeName(it) }
        return DefaultCellEditor(comboBox)
    }

    private fun installedSchemeNames(): List<String> =
        EditorColorsManager.getInstance().allSchemes.map { it.name }

    private class SchemeCellRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int,
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            text = displaySchemeName(value?.toString())
            return component
        }
    }

    private fun currentGlobalSchemeName(): String =
        EditorColorsManager.getInstance().globalScheme.name

    private fun commitActiveTableEdit(): Boolean {
        if (!table.isEditing) {
            return true
        }

        return table.cellEditor?.stopCellEditing() ?: true
    }

    private fun cancelActiveTableEdit() {
        if (table.isEditing) {
            table.cellEditor?.cancelCellEditing()
        }
    }

    private class RulesTableModel : AbstractTableModel() {
        private val rules = mutableListOf<SchemeRule>()

        override fun getRowCount(): Int = rules.size

        override fun getColumnCount(): Int = 2

        override fun getColumnName(column: Int): String =
            when (column) {
                TARGET_COLUMN -> ColorSchemePerLanguageBundle.message("settings.column.target")
                SCHEME_COLUMN -> ColorSchemePerLanguageBundle.message("settings.column.scheme")
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

        fun targetKeys(): Set<String> = rules.mapTo(mutableSetOf()) { it.targetKey() }

        fun rules(): List<SchemeRule> = rules.map { it.copy() }
    }

    data class ValidationResult(
        val isValid: Boolean,
        val message: String? = null,
    )

    companion object {
        private const val TARGET_COLUMN = 0
        private const val SCHEME_COLUMN = 1

        // Internal name prefix the platform gives to user-editable copies of bundled schemes;
        // stripped so the dropdown matches the native Color Scheme selector.
        private const val EDITABLE_COPY_PREFIX = "_@user_"

        private fun displaySchemeName(name: String?): String =
            name?.removePrefix(EDITABLE_COPY_PREFIX).orEmpty()

        fun validateRules(
            rules: List<SchemeRule>,
            installedSchemeNames: List<String>,
        ): ValidationResult {
            if (rules.map { it.targetKey() }.distinct().size != rules.size) {
                return ValidationResult(
                    isValid = false,
                    message = ColorSchemePerLanguageBundle.message("settings.validation.duplicate"),
                )
            }

            val installed = installedSchemeNames.toSet()
            if (rules.any { it.schemeName !in installed }) {
                return ValidationResult(
                    isValid = false,
                    message = ColorSchemePerLanguageBundle.message("settings.validation.missing.scheme"),
                )
            }

            return ValidationResult(isValid = true)
        }
    }
}
