package pk.vexel.financepassport.core.model

/** Strict whole-PKR input rules shared by every monetary form. */
object PkrMoneyInput {
    fun parseRupees(input: String, allowZero: Boolean = true): Long {
        val value = input.trim()
        require(value.isNotEmpty()) { "Enter an amount in whole Pakistani rupees" }
        val validGrouping = value.matches(Regex("[0-9]{1,3}(,[0-9]{3})+"))
        require(value.all { it.isDigit() || it == ',' } && (',' !in value || validGrouping)) {
            "Use whole rupees with optional three-digit commas"
        }
        val digits = value.replace(",", "")
        val rupees = digits.toLongOrNull() ?: error("Amount is too large")
        require(allowZero || rupees > 0) { "Amount must be greater than zero" }
        return rupees
    }

    fun toMinorUnits(input: String, allowZero: Boolean = true): Long =
        Math.multiplyExact(parseRupees(input, allowZero), 100L)

    /** Formats an edit value while the user types; invalid edits are rejected by the caller. */
    fun groupedInput(input: String): String? {
        if (input.isEmpty()) return ""
        if (!input.all { it.isDigit() || it == ',' }) return null
        val digits = input.replace(",", "")
        if (digits.isEmpty()) return ""
        return formatRupees(digits.toLongOrNull() ?: return null)
    }

    fun formatRupees(rupees: Long): String = "%,d".format(java.util.Locale.US, rupees)

    fun formatMinorUnits(minorUnits: Long): String {
        val negative = minorUnits < 0
        val absolute = if (negative) Math.negateExact(minorUnits) else minorUnits
        val rupees = absolute / 100
        val paisa = absolute % 100
        val value = if (paisa == 0L) formatRupees(rupees) else "%s.%02d".format(java.util.Locale.US, formatRupees(rupees), paisa)
        return (if (negative) "-" else "") + "PKR " + value
    }
}
