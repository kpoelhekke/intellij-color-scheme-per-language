package dev.appelflap.editorschemebylanguage.diff

import com.intellij.diff.DiffContext
import com.intellij.diff.DiffExtension
import com.intellij.diff.EditorDiffViewer
import com.intellij.diff.FrameDiffTool
import com.intellij.diff.requests.ContentDiffRequest
import com.intellij.diff.requests.DiffRequest
import dev.appelflap.editorschemebylanguage.platform.IntellijEditorSchemePlatform
import dev.appelflap.editorschemebylanguage.runtime.EditorSchemeApplier
import dev.appelflap.editorschemebylanguage.settings.EditorSchemeSettingsState

class DiffEditorSchemeExtension : DiffExtension() {
    override fun onViewerCreated(
        viewer: FrameDiffTool.DiffViewer,
        context: DiffContext,
        request: DiffRequest,
    ) {
        if (viewer !is EditorDiffViewer) return
        val contentRequest = request as? ContentDiffRequest ?: return
        val project = context.project ?: return
        val settings = EditorSchemeSettingsState.getInstance()
        val platform = IntellijEditorSchemePlatform(project)
        val applier = EditorSchemeApplier(
            platform = platform,
            enabled = { settings.enabled },
            rules = { settings.rules.map { it.copy() } },
        )
        val editors = viewer.editors
        val contentContexts = contentRequest.contents.map { platform.contextForDiffContent(it) }
        val contexts = if (editors.size == 1 && contentContexts.size >= 2) {
            DiffPaneContexts.forUnified(contentContexts, editors.size)
        } else {
            DiffPaneContexts.forSideBySide(contentContexts, editors.size)
        }

        editors.forEachIndexed { index, editor ->
            applier.applyForEditor(editor, contexts.getOrNull(index))
        }
    }
}
