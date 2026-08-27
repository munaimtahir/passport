package pk.vexel.financepassport.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PinRecord(val salt: ByteArray, val digest: ByteArray, val iterations: Int = 120_000)

object PinVerifier {
    fun create(pin: CharArray): PinRecord {
        require(pin.size >= 4) { "PIN must contain at least four digits" }
        require(pin.all(Char::isDigit)) { "PIN must contain digits only" }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        return PinRecord(salt, derive(pin, salt, 120_000))
    }

    fun verify(pin: CharArray, record: PinRecord): Boolean = MessageDigest.isEqual(record.digest, derive(pin, record.salt, record.iterations))

    private fun derive(pin: CharArray, salt: ByteArray, iterations: Int): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(pin, salt, iterations, 256)).encoded
}
