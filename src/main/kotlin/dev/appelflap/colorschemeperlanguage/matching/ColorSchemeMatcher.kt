package dev.appelflap.colorschemeperlanguage.matching

import dev.appelflap.colorschemeperlanguage.model.ColorSchemeContext
import dev.appelflap.colorschemeperlanguage.model.SchemeRef
import dev.appelflap.colorschemeperlanguage.model.SchemeRule

object ColorSchemeMatcher {
    fun resolve(
        enabled: Boolean,
        rules: List<SchemeRule>,
        context: ColorSchemeContext,
        installedSchemes: List<SchemeRef>,
        defaultScheme: SchemeRef,
    ): SchemeRef {
        if (!enabled) {
            return defaultScheme
        }

        val matchingRule = findLanguageRule(rules, context)
            ?: return defaultScheme

        return installedSchemes.find { it.name == matchingRule.schemeName } ?: defaultScheme
    }

    private fun findLanguageRule(
        rules: List<SchemeRule>,
        context: ColorSchemeContext,
    ): SchemeRule? {
        val languageId = context.languageId ?: return null
        return rules.firstOrNull { it.targetId == languageId }
    }
}
