package dev.appelflap.editorschemebylanguage.matching

import dev.appelflap.editorschemebylanguage.model.EditorSchemeContext
import dev.appelflap.editorschemebylanguage.model.RuleTargetKind
import dev.appelflap.editorschemebylanguage.model.SchemeRef
import dev.appelflap.editorschemebylanguage.model.SchemeRule

object EditorSchemeMatcher {
    fun resolve(
        enabled: Boolean,
        rules: List<SchemeRule>,
        context: EditorSchemeContext,
        installedSchemes: List<SchemeRef>,
        defaultScheme: SchemeRef,
    ): SchemeRef {
        if (!enabled) {
            return defaultScheme
        }

        val matchingRule = findLanguageRule(rules, context)
            ?: findFileTypeRule(rules, context)
            ?: return defaultScheme

        return installedSchemes.find { it.name == matchingRule.schemeName } ?: defaultScheme
    }

    private fun findLanguageRule(
        rules: List<SchemeRule>,
        context: EditorSchemeContext,
    ): SchemeRule? {
        val languageId = context.languageId ?: return null
        return rules.firstOrNull {
            it.targetKind == RuleTargetKind.LANGUAGE && it.targetId == languageId
        }
    }

    private fun findFileTypeRule(
        rules: List<SchemeRule>,
        context: EditorSchemeContext,
    ): SchemeRule? {
        val fileTypeId = context.fileTypeId ?: return null
        return rules.firstOrNull {
            it.targetKind == RuleTargetKind.FILE_TYPE && it.targetId == fileTypeId
        }
    }
}
