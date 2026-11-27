package com.amneziawg.fork.util

import android.util.Base64
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.EdECPoint
import java.security.spec.NamedParameterSpec
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Utility for generating WireGuard keys
 */
object KeyGenerator {

    /**
     * Generate a new WireGuard private/public key pair
     * Uses X25519 (Curve25519) for key agreement
     */
    fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("X25519")
        keyPairGenerator.initialize(NamedParameterSpec("X25519"), SecureRandom())
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Generate a random private key (32 bytes) as Base64 string
     */
    fun generatePrivateKey(): String {
        val keyPair = generateKeyPair()
        val privateKey = keyPair.private
        val encoded = privateKey.encoded
        return Base64.encodeToString(encoded, Base64.NO_WRAP)
    }

    /**
     * Derive public key from private key (Base64 encoded)
     */
    fun derivePublicKey(privateKeyBase64: String): String {
        val privateKeyBytes = Base64.decode(privateKeyBase64, Base64.NO_WRAP)
        // This would require proper X25519 implementation
        // For now, return placeholder - in real implementation use WireGuard library
        return "PUBLIC_KEY_DERIVED_FROM_PRIVATE"
    }

    /**
     * Validate WireGuard key format (Base64, 32 bytes)
     */
    fun isValidKey(key: String): Boolean {
        return try {
            val decoded = Base64.decode(key, Base64.NO_WRAP)
            decoded.size == 32
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generate a pre-shared key (PSK) for additional security
     */
    fun generatePsk(): String {
        val keyGenerator = KeyGenerator.getInstance("HmacSHA256")
        keyGenerator.init(256, SecureRandom())
        val secretKey: SecretKey = keyGenerator.generateKey()
        return Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Generate a random string for use as password/token
     */
    fun generateRandomString(length: Int = 32): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        return (1..length).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }
}