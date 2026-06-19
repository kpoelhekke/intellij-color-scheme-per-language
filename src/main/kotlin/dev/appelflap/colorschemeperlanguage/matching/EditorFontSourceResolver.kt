package dev.appelflap.colorschemeperlanguage.matching

/**
 * Which scheme an editor should take its font from when a per-language override scheme is applied.
 *
 * IntelliJ schemes carry a single "use the application default font" flag per editor font
 * (`EditorColorsScheme.isUseAppFontPreferencesInEditor`). When that flag is on, the scheme reports
 * the global `Settings | Editor | Font` values; when off, it reports its own font.
 */
enum class EditorFontSource {
    /** Take the font from the per-language override scheme (its own font, or the app default it carries). */
    OVERRIDE,

    /** Take the font from the default/main color scheme. */
    DEFAULT,
}

object EditorFontSourceResolver {
    /**
     * Resolves the desired font cascade:
     * 1. override scheme defines its own font -> use the override scheme,
     * 2. otherwise the default scheme defines its own font -> use the default scheme,
     * 3. otherwise both fall back to the app default font, which the override scheme already reports.
     */
    fun choose(overrideUsesAppFont: Boolean, defaultUsesAppFont: Boolean): EditorFontSource =
        when {
            !overrideUsesAppFont -> EditorFontSource.OVERRIDE
            !defaultUsesAppFont -> EditorFontSource.DEFAULT
            else -> EditorFontSource.OVERRIDE
        }
}
