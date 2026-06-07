package dev.appelflap.editorschemebylanguage.platform

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager

class IntellijEditorSchemePlatform(
    private val project: Project,
) : EditorSchemePlatform {
    override fun contextFor(editor: Editor): PlatformEditorContext? {
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document)

        return PlatformEditorContext(
            language = psiFile?.language,
            fileType = virtualFile?.fileType,
        )
    }

    override fun currentGlobalScheme(): EditorColorsScheme =
        EditorColorsManager.getInstance().globalScheme

    override fun findScheme(name: String): EditorColorsScheme? =
        EditorColorsManager.getInstance().getScheme(name)

    override fun installedSchemes(): List<EditorColorsScheme> =
        EditorColorsManager.getInstance().allSchemes.toList()

    override fun applySchemeToEditor(editor: Editor, scheme: EditorColorsScheme): Boolean {
        val editorEx = editor as? EditorEx ?: return false
        val application = ApplicationManager.getApplication()

        if (application.isDispatchThread) {
            editorEx.colorsScheme = scheme
            editorEx.component.repaint()
        } else {
            application.invokeLater {
                editorEx.colorsScheme = scheme
                editorEx.component.repaint()
            }
        }

        return true
    }
}
