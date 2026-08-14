package pk.vexel.financepassport.core.security

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupEnvelope(val bytes: ByteArray)

class PortableBackupCrypto(private val iterations: Int = 180_000) {
    fun encrypt(payload: ByteArray, password: CharArray): BackupEnvelope {
        require(password.size >= 8) { "Backup password must contain at least 8 characters" }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val nonce = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, derive(password, salt), GCMParameterSpec(128, nonce))
        cipher.updateAAD(HEADER)
        val ciphertext = cipher.doFinal(payload)
        return BackupEnvelope(ByteArrayOutputStream().apply { write(HEADER); write(ByteBuffer.allocate(4).putInt(iterations).array()); write(salt); write(nonce); write(ciphertext) }.toByteArray())
    }

    fun decrypt(envelope: BackupEnvelope, password: CharArray): ByteArray {
        val input = ByteArrayInputStream(envelope.bytes)
        val header = ByteArray(HEADER.size); require(input.read(header) == HEADER.size && header.contentEquals(HEADER)) { "Unsupported or corrupt backup" }
        val iterationBytes = ByteArray(4); require(input.read(iterationBytes) == 4) { "Corrupt backup metadata" }
        val storedIterations = ByteBuffer.wrap(iterationBytes).int
        require(storedIterations in 100_000..1_000_000) { "Invalid backup work factor" }
        val salt = ByteArray(16); val nonce = ByteArray(12)
        require(input.read(salt) == salt.size && input.read(nonce) == nonce.size) { "Corrupt backup metadata" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, derive(password, salt, storedIterations), GCMParameterSpec(128, nonce))
        cipher.updateAAD(HEADER)
        return cipher.doFinal(input.readBytes())
    }

    private fun derive(password: CharArray, salt: ByteArray, work: Int = iterations): SecretKeySpec {
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(password, salt, work, 256)).encoded
        return SecretKeySpec(key, "AES")
    }

    private companion object { val HEADER = "VEXEL-BACKUP-1".toByteArray(Charsets.US_ASCII) }
}
