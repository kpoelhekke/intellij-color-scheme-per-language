package dev.appelflap.colorschemeperlanguage.runtime

import com.intellij.openapi.editor.Editor
import dev.appelflap.colorschemeperlanguage.matching.ColorSchemeMatcher
import dev.appelflap.colorschemeperlanguage.model.ColorSchemeContext
import dev.appelflap.colorschemeperlanguage.model.SchemeRef
import dev.appelflap.colorschemeperlanguage.model.SchemeRule
import dev.appelflap.colorschemeperlanguage.platform.ColorSchemePlatform
import dev.appelflap.colorschemeperlanguage.platform.PlatformEditorContext

class ColorSchemeApplier(
    private val platform: ColorSchemePlatform,
    private val enabled: () -> Boolean,
    private val rules: () -> List<SchemeRule>,
) {
    fun applyForEditor(editor: Editor, context: PlatformEditorContext? = null): Boolean {
        val platformContext = context ?: platform.contextFor(editor) ?: return false
        val defaultScheme = platform.currentGlobalScheme()
        val selectedScheme = ColorSchemeMatcher.resolve(
            enabled = enabled(),
            rules = rules(),
            context = platformContext.toColorSchemeContext(),
            installedSchemes = platform.installedSchemes().map { SchemeRef(it.name) },
            defaultScheme = SchemeRef(defaultScheme.name),
        )

        val scheme = platform.findScheme(selectedScheme.name) ?: defaultScheme
        return platform.applySchemeToEditor(editor, scheme)
    }

    private fun PlatformEditorContext.toColorSchemeContext(): ColorSchemeContext =
        ColorSchemeContext(
            languageId = language?.id,
            languageDisplayName = language?.displayName,
        )
}
