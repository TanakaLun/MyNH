package io.tl.mynhentai.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Stores the API key encrypted with an AES-GCM key held by the Android Keystore.
 *
 * The ciphertext lives in a separate file so it never ends up in the app's own JSON backup
 * (which serialises the whole settings SharedPreferences) and it is excluded from Android
 * auto backup by the backup rules.
 */
class SecretStore(private val context: Context) {

    private val secretFile: File
        get() = File(context.filesDir, FILE_NAME)

    @Volatile
    private var cached: String? = null

    @Volatile
    private var cacheLoaded = false

    fun saveApiKey(apiKey: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val cipherText = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) +
            SEPARATOR +
            Base64.encodeToString(cipherText, Base64.NO_WRAP)
        secretFile.writeText(encoded)
        cached = apiKey
        cacheLoaded = true
    }

    fun readApiKey(): String? {
        if (cacheLoaded) return cached
        val decoded = decryptFromDisk()
        cached = decoded
        cacheLoaded = true
        return decoded
    }

    fun clear() {
        secretFile.delete()
        cached = null
        cacheLoaded = true
    }

    private fun decryptFromDisk(): String? {
        val file = secretFile
        if (!file.exists()) return null
        return runCatching {
            val parts = file.readText().split(SEPARATOR)
            if (parts.size != 2) return null
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                javax.crypto.spec.GCMParameterSpec(GCM_TAG_BITS, iv)
            )
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val FILE_NAME = "account_secret.dat"
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "mynhentai_account_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val SEPARATOR = ":"
    }
}
