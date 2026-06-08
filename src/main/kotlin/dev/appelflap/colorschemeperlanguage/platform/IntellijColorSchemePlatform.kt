package dev.appelflap.colorschemeperlanguage.platform

import com.intellij.diff.contents.DiffContent
import com.intellij.diff.contents.DocumentContent
import com.intellij.diff.contents.FileContent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager

class IntellijColorSchemePlatform(
    private val project: Project,
) : ColorSchemePlatform {
    override fun contextFor(editor: Editor): PlatformEditorContext? {
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document)

        return PlatformEditorContext(
            language = psiFile?.language,
        )
    }

    fun contextForDiffContent(content: DiffContent): PlatformEditorContext? {
        val virtualFile = when (content) {
            is FileContent -> content.file
            is DocumentContent -> content.highlightFile ?: FileDocumentManager.getInstance().getFile(content.document)
            else -> null
        }
        val language = virtualFile?.let { PsiManager.getInstance(project).findFile(it) }?.language
            ?: return null

        return PlatformEditorContext(language = language)
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
