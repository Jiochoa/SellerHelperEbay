package com.example.sellerhelperebay.data.ebay

data class CategoryOption(val label: String, val id: String)

/**
 * Small hardcoded clothing leaf-category set for the MVP. IDs are EBAY_US leaf
 * categories; if eBay rejects one during testing, re-check it with the Taxonomy API's
 * getCategorySuggestions. The dynamic Taxonomy-driven flow replaces this later.
 */
object CategoryMap {
    val options: List<CategoryOption> = listOf(
        CategoryOption("Men > T-Shirts", "15687"),
        CategoryOption("Men > Casual Shirts", "57990"),
        CategoryOption("Men > Dress Shirts", "57991"),
        CategoryOption("Men > Sweaters", "11484"),
        CategoryOption("Men > Coats & Jackets", "57988"),
        CategoryOption("Men > Jeans", "11483"),
        CategoryOption("Women > Tops & Blouses", "53159"),
        CategoryOption("Women > Dresses", "63861"),
        CategoryOption("Women > Sweaters", "63866"),
        CategoryOption("Women > Coats & Jackets", "63862"),
        CategoryOption("Women > Jeans", "11554")
    )

    /** Best-effort default based on what the analysis found. */
    fun suggest(department: String?, type: String?): CategoryOption {
        val dept = department?.lowercase().orEmpty()
        val t = type?.lowercase().orEmpty()
        val prefix = if (dept.startsWith("wom") || dept.startsWith("girl")) "Women" else "Men"
        val match = options.filter { it.label.startsWith(prefix) }.firstOrNull { option ->
            val leaf = option.label.substringAfter("> ").lowercase()
            t.isNotEmpty() && (leaf.contains(t) || t.contains(leaf.trimEnd('s')))
        }
        return match
            ?: options.first { it.label.startsWith(prefix) } // department default: T-Shirts/Tops
    }
}
