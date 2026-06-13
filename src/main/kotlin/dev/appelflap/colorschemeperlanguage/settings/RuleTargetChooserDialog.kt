package dev.appelflap.colorschemeperlanguage.settings

import com.intellij.lang.Language
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SearchTextField
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import dev.appelflap.colorschemeperlanguage.ColorSchemePerLanguageBundle
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class RuleTargetChooserDialog(
    existingTargetKeys: Set<String> = emptySet(),
) : DialogWrapper(false) {
    private val allTargets = loadTargets().filterNot { it.targetKey() in existingTargetKeys }
    private val listModel = DefaultListModel<RuleTarget>()
    private val targetList = JBList(listModel)
    private val searchField = SearchTextField()

    var selectedTarget: RuleTarget? = null
        private set

    init {
        title = ColorSchemePerLanguageBundle.message("dialog.target.title")
        targetList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        targetList.cellRenderer = RuleTargetListCellRenderer()
        targetList.addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(event: MouseEvent) {
                    if (event.clickCount == 2 && targetList.selectedValue != null) {
                        doOKAction()
                    }
                }
            },
        )

        searchField.textEditor.emptyText.text = ColorSchemePerLanguageBundle.message("dialog.target.search")
        searchField.textEditor.document.addDocumentListener(
            object : DocumentListener {
                override fun insertUpdate(event: DocumentEvent) = filterTargets()
                override fun removeUpdate(event: DocumentEvent) = filterTargets()
                override fun changedUpdate(event: DocumentEvent) = filterTargets()
            },
        )

        filterTargets()
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(JBUI.scale(0), JBUI.scale(8)))
        panel.preferredSize = JBUI.size(420, 360)
        panel.add(searchField, BorderLayout.NORTH)
        panel.add(JBScrollPane(targetList), BorderLayout.CENTER)
        return panel
    }

    override fun doOKAction() {
        selectedTarget = targetList.selectedValue ?: return
        super.doOKAction()
    }

    private fun filterTargets() {
        val query = searchField.text.trim().lowercase()
        listModel.clear()
        allTargets
            .filter { target ->
                query.isEmpty() ||
                    target.displayName.lowercase().contains(query) ||
                    target.id.lowercase().contains(query)
            }
            .forEach(listModel::addElement)

        if (listModel.size() > 0) {
            targetList.selectedIndex = 0
        }
    }

    data class RuleTarget(
        val id: String,
        val displayName: String,
    ) {
        fun targetKey(): String = id

        override fun toString(): String = displayName
    }

    companion object {
        private fun loadTargets(): List<RuleTarget> =
            Language.getRegisteredLanguages()
                // Language.ANY is a placeholder with a blank id and display name; it would render as " ()".
                .filterNot { it === Language.ANY }
                .filter { it.id.isNotBlank() && it.displayName.isNotBlank() }
                .map { language ->
                    RuleTarget(
                        id = language.id,
                        displayName = language.displayName,
                    )
                }
                .distinctBy { it.id }
                .sortedBy { it.displayName.lowercase() }
    }
}

private class RuleTargetListCellRenderer : ColoredListCellRenderer<RuleTargetChooserDialog.RuleTarget>() {
    override fun customizeCellRenderer(
        list: JList<out RuleTargetChooserDialog.RuleTarget>,
        value: RuleTargetChooserDialog.RuleTarget?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean,
    ) {
        if (value == null) {
            return
        }

        append(value.displayName)
        append(" (${value.id})", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    }
}
