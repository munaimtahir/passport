package pk.vexel.financepassport.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class EncryptedBytes(val nonce: ByteArray, val ciphertext: ByteArray)

class KeystoreCryptoService(private val alias: String = "vexel_passport_master_v1") {
    private val key: SecretKey by lazy { loadOrCreateKey() }

    fun encrypt(plaintext: ByteArray, associatedData: ByteArray? = null): EncryptedBytes {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        associatedData?.let(cipher::updateAAD)
        return EncryptedBytes(cipher.iv, cipher.doFinal(plaintext))
    }

    fun decrypt(encrypted: EncryptedBytes, associatedData: ByteArray? = null): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, encrypted.nonce))
        associatedData?.let(cipher::updateAAD)
        return cipher.doFinal(encrypted.ciphertext)
    }

    private fun loadOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build())
        }.generateKey()
    }
}
