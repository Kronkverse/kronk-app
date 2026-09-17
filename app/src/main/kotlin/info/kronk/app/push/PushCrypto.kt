package info.kronk.app.push

import android.content.Context
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// RFC 8291 Web Push encryption keys + decrypt.
//
// The push subscription payload sent to Kronk contains:
//   - `endpoint`  — the URL Kronk POSTs encrypted messages to
//     (UnifiedPush distributor URL, or fcm.googleapis.com/fcm/send/…)
//   - `keys.p256dh` — our public ECDH key, base64url, uncompressed
//     P-256 format (65 bytes starting with 0x04)
//   - `keys.auth`   — a 16-byte shared secret we generate
//
// The Rails webpush gem uses these to derive per-message symmetric
// keys (via HKDF-SHA256) and encrypt payloads (AES-128-GCM). We do
// the reverse on receive.
//
// Ports from :mastodon/PushSubscriptionManager.java lines 220-390.
// Keys persist in SharedPreferences ("push") for the app lifetime;
// re-generated on sign-out.

object PushCrypto {

    private const val PREFS = "push"
    private const val KEY_PRIVATE = "privateKey"
    private const val KEY_PUBLIC = "publicKey"
    private const val KEY_AUTH = "authSecret"
    private const val EC_CURVE = "secp256r1"

    // Uncompressed P-256 public-key SPKI header. Concatenated with
    // the raw 64-byte (x‖y) coordinates it forms a valid X.509
    // SubjectPublicKeyInfo an Android KeyFactory can decode.
    private val P256_SPKI_HEADER = byteArrayOf(
        0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2a, 0x86.toByte(), 0x48, 0xce.toByte(), 0x3d, 0x02,
        0x01, 0x06, 0x08, 0x2a, 0x86.toByte(), 0x48, 0xce.toByte(), 0x3d, 0x03, 0x01, 0x07, 0x03,
        0x42, 0x00,
    )

    data class Keys(
        val privateKey: PrivateKey,
        val publicKey: ECPublicKey,
        // Uncompressed P-256 encoding: 0x04 ‖ x ‖ y, 65 bytes.
        // This is the `keys.p256dh` value the subscription payload
        // carries.
        val publicKeyRaw: ByteArray,
        // 16 random bytes; Kronk uses this as the HMAC input in the
        // per-message key derivation. Sent as `keys.auth`.
        val authSecret: ByteArray,
    )

    fun ensureKeys(context: Context): Keys {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val privB64 = prefs.getString(KEY_PRIVATE, null)
        val pubB64 = prefs.getString(KEY_PUBLIC, null)
        val authB64 = prefs.getString(KEY_AUTH, null)
        if (privB64 != null && pubB64 != null && authB64 != null) {
            val priv = KeyFactory.getInstance("EC")
                .generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privB64, Base64.NO_WRAP)))
            val pub = KeyFactory.getInstance("EC")
                .generatePublic(X509EncodedKeySpec(Base64.decode(pubB64, Base64.NO_WRAP)))
                    as ECPublicKey
            val auth = Base64.decode(authB64, Base64.NO_WRAP)
            return Keys(priv, pub, encodeRawPublic(pub), auth)
        }
        val gen = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec(EC_CURVE))
        }
        val pair: KeyPair = gen.generateKeyPair()
        val priv = pair.private
        val pub = pair.public as ECPublicKey
        val auth = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_PRIVATE, Base64.encodeToString(priv.encoded, Base64.NO_WRAP))
            .putString(KEY_PUBLIC, Base64.encodeToString(pub.encoded, Base64.NO_WRAP))
            .putString(KEY_AUTH, Base64.encodeToString(auth, Base64.NO_WRAP))
            .apply()
        return Keys(priv, pub, encodeRawPublic(pub), auth)
    }

    fun clearKeys(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_PRIVATE)
            .remove(KEY_PUBLIC)
            .remove(KEY_AUTH)
            .apply()
    }

    // Serialize an ECPublicKey into the uncompressed 65-byte
    // 0x04 ‖ x ‖ y format Web Push expects.
    fun encodeRawPublic(key: ECPublicKey): ByteArray {
        val point = key.w
        val x = normalizeCoordinate(point.affineX)
        val y = normalizeCoordinate(point.affineY)
        return byteArrayOf(0x04) + x + y
    }

    private fun normalizeCoordinate(c: BigInteger): ByteArray {
        val raw = c.toByteArray()
        return when {
            raw.size == 32 -> raw
            raw.size == 33 && raw[0] == 0.toByte() -> raw.copyOfRange(1, 33)
            raw.size < 32 -> ByteArray(32 - raw.size) + raw
            else -> raw.copyOfRange(raw.size - 32, raw.size)
        }
    }

    // Deserialize a raw 65-byte uncompressed P-256 point into an
    // ECPublicKey. Used to import the ephemeral server public key
    // included in each Web Push message.
    fun decodeRawPublic(raw: ByteArray): PublicKey {
        require(raw.size == 65 && raw[0] == 0x04.toByte()) {
            "invalid raw P-256 public key: ${raw.size} bytes"
        }
        val spki = P256_SPKI_HEADER + raw
        return KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(spki))
    }

    // Decrypt an aes128gcm-format Web Push payload (RFC 8188 +
    // RFC 8291). This is what Rails' `webpush` gem emits.
    //
    // Body layout:
    //   [ salt(16) | rs(4) | idlen(1) | keyid(idlen) | ciphertext... ]
    // For Web Push aes128gcm, keyid holds the server's ephemeral
    // public key (65 bytes uncompressed).
    fun decrypt(keys: Keys, ciphertext: ByteArray): ByteArray {
        require(ciphertext.size > 21) { "push payload too small" }
        val salt = ciphertext.copyOfRange(0, 16)
        val idlen = ciphertext[20].toInt() and 0xFF
        require(idlen == 65) { "expected keyid = 65-byte server pubkey, got $idlen" }
        val serverPubRaw = ciphertext.copyOfRange(21, 21 + idlen)
        val encrypted = ciphertext.copyOfRange(21 + idlen, ciphertext.size)

        val serverPub = decodeRawPublic(serverPubRaw)
        val shared = ecdh(keys.privateKey, serverPub)

        // Two-step HKDF per RFC 8291 §3.4:
        //   PRK_key = HKDF(auth, ecdh_secret, "WebPush: info\0" || uaPubkey || serverPubkey, 32)
        //   IKM     = HKDF(PRK_key, salt, "Content-Encoding: aes128gcm\0", 16)
        //   NONCE   = HKDF(PRK_key, salt, "Content-Encoding: nonce\0", 12)
        val infoRfc = buildWebPushInfo(keys.publicKeyRaw, serverPubRaw)
        val prkKey = hkdf(keys.authSecret, shared, infoRfc, 32)
        val ikm = hkdf(salt, prkKey, "Content-Encoding: aes128gcm\u0000".toByteArray(Charsets.US_ASCII), 16)
        val nonce = hkdf(salt, prkKey, "Content-Encoding: nonce\u0000".toByteArray(Charsets.US_ASCII), 12)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(ikm, "AES"), GCMParameterSpec(128, nonce))
        val padded = cipher.doFinal(encrypted)

        // aes128gcm content trailing pad marker: last non-padding
        // byte carries the pad flag; strip trailing 0x02 (last
        // record) or 0x01 (interior) then 0x00 padding.
        var end = padded.size
        while (end > 0 && padded[end - 1] == 0.toByte()) end--
        require(end > 0) { "empty push payload after decrypt" }
        val flag = padded[end - 1].toInt() and 0xFF
        require(flag == 0x01 || flag == 0x02) { "unexpected pad marker $flag" }
        return padded.copyOfRange(0, end - 1)
    }

    private fun ecdh(priv: PrivateKey, pub: PublicKey): ByteArray {
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(priv)
        ka.doPhase(pub, true)
        return ka.generateSecret()
    }

    // The Web Push info string: "WebPush: info\0" || uaPubkey || serverPubkey.
    private fun buildWebPushInfo(uaPub: ByteArray, serverPub: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write("WebPush: info".toByteArray(Charsets.US_ASCII))
        out.write(0)
        out.write(uaPub)
        out.write(serverPub)
        return out.toByteArray()
    }

    // HKDF (RFC 5869) with SHA-256 — extract + expand, single-block.
    // The webpush gem only needs `L <= 32` bytes here so a single
    // expand iteration suffices.
    private fun hkdf(salt: ByteArray, ikm: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = mac.doFinal(ikm)
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(info)
        mac.update(0x01)
        val t = mac.doFinal()
        require(length <= t.size) { "hkdf length $length > HMAC size ${t.size}" }
        return t.copyOfRange(0, length)
    }

    fun MessageDigest.also(update: (MessageDigest) -> Unit): MessageDigest {
        update(this); return this
    }
}
