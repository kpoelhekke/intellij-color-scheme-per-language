package dev.appelflap.editorschemebylanguage.model

data class SchemeRule(
    var targetKind: RuleTargetKind = RuleTargetKind.LANGUAGE,
    var targetId: String = "",
    var targetDisplayName: String = "",
    var schemeName: String = "",
) {
    fun targetKey(): String = "${targetKind.name}:$targetId"
}
