package com.example.sellerhelperebay.data.ebay

data class ConditionOption(val label: String, val enumValue: String)

/** Maps the human-readable condition labels used in the form to eBay condition enums. */
object ConditionMap {
    val options: List<ConditionOption> = listOf(
        ConditionOption("New with tags", "NEW"),
        ConditionOption("New without tags", "NEW_OTHER"),
        ConditionOption("Used - Excellent", "USED_EXCELLENT"),
        ConditionOption("Used - Good", "USED_GOOD"),
        ConditionOption("Used - Fair", "USED_ACCEPTABLE")
    )

    fun enumForLabel(label: String?): String? =
        options.firstOrNull { it.label.equals(label?.trim(), ignoreCase = true) }?.enumValue
}
