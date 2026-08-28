package pk.vexel.financepassport.core.model

/** Stable utility taxonomy shared by persistence compatibility, entry, filters and ledger provenance. */
enum class UtilityCategory(val id: String, val label: String) {
    ELECTRICITY("electricity", "Electricity"),
    GAS("gas", "Gas"),
    WATER("water", "Water"),
    INTERNET("internet", "Internet"),
    MOBILE_TELEPHONE("mobile_telephone", "Mobile / Telephone"),
    SUBSCRIPTION_SERVICE("subscription_service", "Subscription / Service"),
    OTHER("other", "Other");

    companion object {
        val selectable: List<UtilityCategory> = entries

        /** Maps all known legacy display values without rewriting or losing stored records. */
        fun fromStored(value: String?): UtilityCategory = when (value?.trim()?.lowercase()) {
            "electricity", "power" -> ELECTRICITY
            "gas" -> GAS
            "water" -> WATER
            "internet", "broadband" -> INTERNET
            "telephone", "mobile", "phone", "mobile / telephone", "mobile/telephone" -> MOBILE_TELEPHONE
            "subscription", "service", "subscription / service", "subscription/service" -> SUBSCRIPTION_SERVICE
            else -> OTHER
        }

        fun canonicalLabel(value: String?): String = fromStored(value).label
    }
}
