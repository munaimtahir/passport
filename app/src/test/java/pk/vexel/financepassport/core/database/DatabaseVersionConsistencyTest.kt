package pk.vexel.financepassport.core.database

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards against the exact bug class this project has already hit twice (the 8->9 and 9->10
 * schema boundaries): the encrypted-backup manifest's schemaVersion silently drifting from the
 * real Room schema version because it was written as a separate hardcoded integer literal.
 *
 * AppDatabase.DATABASE_VERSION is now the single source of truth, referenced both by the
 * @Database annotation and by every backup-manifest call site in FinanceRepository — this test
 * scans that source file to make sure nobody reintroduces a second, independent literal there.
 */
class DatabaseVersionConsistencyTest {

    private fun financeRepositorySource(): String {
        val candidates = listOf(
            "app/src/main/java/pk/vexel/financepassport/core/database/FinanceRepository.kt",
            "src/main/java/pk/vexel/financepassport/core/database/FinanceRepository.kt",
        )
        val file = candidates.map { File(it) }.firstOrNull { it.exists() }
            ?: error("Could not locate FinanceRepository.kt from working dir ${File(".").absolutePath}")
        return file.readText()
    }

    @Test
    fun backupManifestSchemaVersionUsesTheSharedConstantNotAHardcodedLiteral() {
        val source = financeRepositorySource()
        val occurrences = Regex("BuildConfig\\.VERSION_NAME,\\s*(\\S+),\\s*password").findAll(source).map { it.groupValues[1] }.toList()
        assertTrue("Expected both backup-manifest call sites (create + createStreaming) to be found", occurrences.size == 2)
        occurrences.forEach { token ->
            assertEquals(
                "Backup manifest schemaVersion must reference the shared DATABASE_VERSION constant, not a separate literal",
                "DATABASE_VERSION",
                token,
            )
        }
    }

    @Test
    fun databaseVersionConstantIsWhatWasClaimedForIncomeSources() {
        assertEquals(11, DATABASE_VERSION)
    }
}
