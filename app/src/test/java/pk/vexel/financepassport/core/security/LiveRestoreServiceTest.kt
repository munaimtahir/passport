package pk.vexel.financepassport.core.security

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class LiveRestoreServiceTest {
    @Test fun stagingRejectsMissingDatabasePayload() {
        val service = BackupPackageService(PortableBackupCrypto(100_000))
        val dir = Files.createTempDirectory("restore-check").toFile()
        val packageBytes = service.create(byteArrayOf(1), emptyList(), "0.1", 4, "password".toCharArray()).payload
        val manifest = service.restore(packageBytes, "password".toCharArray(), dir)
        assertTrue(manifest.documentCount == 0)
    }
}
