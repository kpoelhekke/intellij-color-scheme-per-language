package dev.appelflap.editorschemebylanguage.diff

import dev.appelflap.editorschemebylanguage.platform.PlatformEditorContext

object DiffPaneContexts {
    fun forSideBySide(
        contentContexts: List<PlatformEditorContext?>,
        editorCount: Int,
    ): List<PlatformEditorContext?> =
        List(editorCount) { index ->
            contentContexts.getOrNull(index)
        }

    fun forUnified(
        contentContexts: List<PlatformEditorContext?>,
        editorCount: Int,
    ): List<PlatformEditorContext?> {
        val preferred = contentContexts.getOrNull(1) ?: contentContexts.getOrNull(0)
        return List(editorCount) { preferred }
    }
}
