package com.example.sellerhelperebay.domain.model

/**
 * The clothing-focused MVP field set. [ebayAspectName] is the exact aspect name eBay
 * expects in `product.aspects`; null means the field maps to a top-level draft field
 * (title/description/condition) instead of an aspect.
 */
enum class FieldKey(val displayName: String, val ebayAspectName: String?) {
    TITLE("Title", null),
    DESCRIPTION("Description", null),
    CONDITION("Condition", null),
    BRAND("Brand", "Brand"),
    SIZE("Size", "Size"),
    SIZE_TYPE("Size Type", "Size Type"),
    COLOR("Color", "Color"),
    STYLE("Style", "Style"),
    MATERIAL("Material", "Material"),
    DEPARTMENT("Department", "Department"),
    TYPE("Type", "Type"),
    PATTERN("Pattern", "Pattern"),
    FIT("Fit", "Fit"),
    SLEEVE_LENGTH("Sleeve Length", "Sleeve Length"),
    COUNTRY_OF_MANUFACTURE("Country of Manufacture", "Country/Region of Manufacture");

    companion object {
        fun fromNameOrNull(name: String): FieldKey? = entries.firstOrNull { it.name == name }
    }
}
