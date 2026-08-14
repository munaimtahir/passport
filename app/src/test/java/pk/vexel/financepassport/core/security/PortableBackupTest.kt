package pk.vexel.financepassport.core.security

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class PortableBackupTest {
    @Test fun backupRoundTripPreservesPayload() {
        val payload = "database + encrypted document manifest".toByteArray()
        val crypto = PortableBackupCrypto(iterations = 100_000)
        assertArrayEquals(payload, crypto.decrypt(crypto.encrypt(payload, "correct horse".toCharArray()), "correct horse".toCharArray()))
    }

    @Test(expected = Exception::class)
    fun wrongPasswordFailsAuthentication() {
        val crypto = PortableBackupCrypto(iterations = 100_000)
        crypto.decrypt(crypto.encrypt(byteArrayOf(1, 2, 3), "correct horse".toCharArray()), "wrong horse".toCharArray())
    }

    @Test(expected = Exception::class)
    fun tamperingFailsAuthentication() {
        val crypto = PortableBackupCrypto(iterations = 100_000)
        val backup = crypto.encrypt(byteArrayOf(1, 2, 3), "correct horse".toCharArray()).bytes.copyOf()
        backup[backup.lastIndex] = (backup.last() + 1).toByte()
        crypto.decrypt(BackupEnvelope(backup), "correct horse".toCharArray())
    }
}
