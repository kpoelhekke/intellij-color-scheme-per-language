package dev.appelflap.colorschemeperlanguage.model

data class SchemeRule(
    var targetId: String = "",
    var targetDisplayName: String = "",
    var schemeName: String = "",
) {
    fun targetKey(): String = targetId
}
