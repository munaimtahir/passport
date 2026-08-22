package pk.vexel.financepassport.core.model

import java.text.NumberFormat
import java.util.Locale

/** Whole-PKR input rules shared by every monetary form. */
object MoneyInput {
    private val grouped = Regex("[0-9]{1,3}(,[0-9]{3})*")

    fun parseRupees(text: String, allowZero: Boolean = true): Long {
        val value = text.trim()
        require(value.isNotEmpty()) { "Enter an amount" }
        require(!value.contains('.') && !value.contains('-') && value.all { it.isDigit() || it == ',' }) {
            "Enter whole Pakistani rupees only"
        }
        require(!value.contains(',') || grouped.matches(value)) { "Use commas only as three-digit separators" }
        val rupees = value.replace(",", "").toLongOrNull() ?: error("Amount is too large")
        require(allowZero || rupees > 0) { "Amount must be greater than zero" }
        return rupees
    }

    fun toMinorUnits(text: String, allowZero: Boolean = true): Long =
        Math.multiplyExact(parseRupees(text, allowZero), 100L)

    fun formatMinorUnits(minorUnits: Long): String {
        require(minorUnits >= 0) { "Use signed formatting for derived negative values" }
        val rupees = minorUnits / 100
        val paisa = minorUnits % 100
        val groupedRupees = NumberFormat.getIntegerInstance(Locale.US).format(rupees)
        return if (paisa == 0L) "PKR $groupedRupees" else "PKR $groupedRupees.${paisa.toString().padStart(2, '0')}"
    }

    fun formatSignedMinorUnits(minorUnits: Long): String =
        if (minorUnits < 0) "-PKR ${formatMinorUnits(Math.negateExact(minorUnits)).removePrefix("PKR ")}"
        else formatMinorUnits(minorUnits)
}
