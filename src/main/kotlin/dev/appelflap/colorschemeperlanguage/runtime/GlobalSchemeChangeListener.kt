package dev.appelflap.colorschemeperlanguage.runtime

import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsScheme
import dev.appelflap.colorschemeperlanguage.settings.ColorSchemeSettingsState

/**
 * Re-applies per-language schemes to open editors when the global/main scheme changes.
 *
 * Override editors take their font from the main theme (see FontFollowingColorScheme). Switching the
 * main theme or its font does not, on its own, re-render those editors with the new font, so we re-apply
 * to force a repaint. Re-applying sets a per-editor scheme, which never changes the global scheme, so
 * this cannot loop.
 *
 * This fires for every theme/scheme change in the IDE, so skip the refresh when the plugin has nothing
 * to maintain: with no rules every editor already shows the (newly changed) global scheme, and when
 * disabled the revert is handled by the settings-update path instead.
 */
class GlobalSchemeChangeListener : EditorColorsListener {
    override fun globalSchemeChange(scheme: EditorColorsScheme?) {
        val settings = ColorSchemeSettingsState.getInstance()
        if (!settings.enabled || settings.rules.isEmpty()) return
        ColorSchemeRefresher.scheduleRefresh()
    }
}
