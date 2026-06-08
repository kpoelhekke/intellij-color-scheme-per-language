package dev.appelflap.editorschemebylanguage.merge

import com.intellij.diff.merge.MergeContext
import com.intellij.diff.merge.MergeRequest
import com.intellij.diff.merge.MergeTool

class MergeEditorSchemeTool : MergeTool {
    override fun canShow(context: MergeContext, request: MergeRequest): Boolean =
        false

    override fun createComponent(context: MergeContext, request: MergeRequest): MergeTool.MergeViewer {
        error("MergeEditorSchemeTool is a public API registration probe and must not create viewers.")
    }
}
