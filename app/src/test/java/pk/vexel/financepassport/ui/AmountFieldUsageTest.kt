package pk.vexel.financepassport.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountFieldUsageTest {

    /**
     * Guards against the numeric-keyboard regression Sprint 17 fixed (docs/09_UI_DESIGN_SYSTEM.md
     * requires it on every amount field): every PkrMoneyInput-driven field should go through the
     * shared AmountField composable, which is the only place still allowed to write the raw
     * `PkrMoneyInput.groupedInput(it)?.let { x -> y = x } }` inline pattern this scans for.
     */
    @Test
    fun everyPkrMoneyInputFieldGoesThroughSharedAmountField() {
        val source = findSourceFile("app/src/main/java/pk/vexel/financepassport/ui/PassportApp.kt")
        val text = source.readText()
        val rawInlinePattern = Regex("""PkrMoneyInput\.groupedInput\(it\)\?\.let\s*\{\s*\w+\s*->\s*\w+\s*=\s*\w+\s*\}""")
        val matches = rawInlinePattern.findAll(text).map { it.value }.toList()
        assertTrue(
            "Found ${matches.size} raw PkrMoneyInput inline handler(s) outside the shared AmountField " +
                "composable: $matches -- route these through AmountField(...) instead so the numeric " +
                "keyboard requirement can't silently regress.",
            matches.isEmpty(),
        )
    }

    private fun findSourceFile(relativePath: String): File {
        val startDir = System.getProperty("user.dir") ?: "."
        var dir: File? = File(startDir)
        repeat(8) {
            val candidate = File(dir, relativePath)
            if (candidate.exists()) return candidate
            dir = dir?.parentFile
        }
        error("Could not locate $relativePath starting from $startDir")
    }
}
