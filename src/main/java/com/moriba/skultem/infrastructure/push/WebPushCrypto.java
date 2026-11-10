package com.moriba.skultem.infrastructure.push;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// Web Push without a library: message encryption per RFC 8291 (aes128gcm, RFC 8188) and the VAPID signature
// (RFC 8292) the push services require. Only the JDK's own crypto is used.
public final class WebPushCrypto {

    private static final int RECORD_SIZE = 4096;
    private static final SecureRandom RANDOM = new SecureRandom();

    private WebPushCrypto() {
    }

    // ---- Encryption (RFC 8291) ----

    /** Encrypts a message for one device, with a fresh sender key and salt. */
    public static byte[] encrypt(byte[] plaintext, String uaPublicB64, String authSecretB64) throws GeneralSecurityException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return encrypt(plaintext, b64(uaPublicB64), b64(authSecretB64), gen.generateKeyPair(), salt);
    }

    /** The same, with the sender key and salt given - for tests against the RFC's worked example. */
    static byte[] encrypt(byte[] plaintext, byte[] uaPublic, byte[] authSecret, KeyPair sender, byte[] salt)
            throws GeneralSecurityException {
        byte[] asPublic = rawPublicKey((ECPublicKey) sender.getPublic());

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(sender.getPrivate());
        ka.doPhase(publicKey(uaPublic), true);
        byte[] ecdhSecret = ka.generateSecret();

        // IKM = HKDF(salt = auth secret, ikm = ECDH secret, info = "WebPush: info" 0x00 ua_public as_public, 32)
        byte[] keyInfo = concat("WebPush: info".getBytes(StandardCharsets.US_ASCII), new byte[] { 0 }, uaPublic, asPublic);
        byte[] ikm = hkdf(authSecret, ecdhSecret, keyInfo, 32);

        byte[] cek = hkdf(salt, ikm, concat("Content-Encoding: aes128gcm".getBytes(StandardCharsets.US_ASCII), new byte[] { 0 }), 16);
        byte[] nonce = hkdf(salt, ikm, concat("Content-Encoding: nonce".getBytes(StandardCharsets.US_ASCII), new byte[] { 0 }), 12);

        // One record: the message, then the 0x02 "last record" delimiter.
        byte[] padded = concat(plaintext, new byte[] { 2 });
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] ciphertext = cipher.doFinal(padded);

        // Header: salt (16) | record size (4) | key id length (1) | key id = sender public key (65)
        ByteBuffer header = ByteBuffer.allocate(16 + 4 + 1 + asPublic.length);
        header.put(salt).putInt(RECORD_SIZE).put((byte) asPublic.length).put(asPublic);
        return concat(header.array(), ciphertext);
    }

    // ---- VAPID (RFC 8292) ----

    /** The Authorization header value for a push to {@code endpoint}, signed with the server's VAPID key. */
    public static String vapidAuthorization(String endpoint, String subject, String publicKeyB64, String privateKeyB64)
            throws GeneralSecurityException {
        java.net.URI uri = java.net.URI.create(endpoint);
        String audience = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
        long exp = java.time.Instant.now().getEpochSecond() + 12 * 3600;

        String header = b64url("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        String claims = b64url(("{\"aud\":\"" + audience + "\",\"exp\":" + exp + ",\"sub\":\"" + subject + "\"}")
                .getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + claims;

        Signature signer = Signature.getInstance("SHA256withECDSAinP1363Format");
        signer.initSign(privateKey(b64(privateKeyB64)));
        signer.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        String jwt = signingInput + "." + b64url(signer.sign());

        return "vapid t=" + jwt + ", k=" + publicKeyB64;
    }

    /** A new VAPID key pair as base64url {public (65-byte point), private (32 bytes)} - to set up a server once. */
    public static String[] generateVapidKeys() throws GeneralSecurityException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
        KeyPair pair = gen.generateKeyPair();
        byte[] d = unsigned32(((ECPrivateKey) pair.getPrivate()).getS());
        return new String[] { b64url(rawPublicKey((ECPublicKey) pair.getPublic())), b64url(d) };
    }

    public static void main(String[] args) throws Exception {
        String[] keys = generateVapidKeys();
        System.out.println("PUSH_VAPID_PUBLIC_KEY=" + keys[0]);
        System.out.println("PUSH_VAPID_PRIVATE_KEY=" + keys[1]);
    }

    // ---- Helpers ----

    static byte[] hkdf(byte[] salt, byte[] ikm, byte[] info, int length) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        byte[] okm = mac.doFinal(concat(info, new byte[] { 1 }));
        return Arrays.copyOf(okm, length);
    }

    private static ECParameterSpec p256() throws GeneralSecurityException {
        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec("secp256r1"));
        return params.getParameterSpec(ECParameterSpec.class);
    }

    static ECPublicKey publicKey(byte[] raw) throws GeneralSecurityException {
        if (raw.length != 65 || raw[0] != 4) {
            throw new GeneralSecurityException("Not an uncompressed P-256 public key");
        }
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(raw, 1, 33));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(raw, 33, 65));
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(x, y), p256()));
    }

    static ECPrivateKey privateKey(byte[] d) throws GeneralSecurityException {
        return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new ECPrivateKeySpec(new BigInteger(1, d), p256()));
    }

    static byte[] rawPublicKey(ECPublicKey key) {
        return concat(new byte[] { 4 }, unsigned32(key.getW().getAffineX()), unsigned32(key.getW().getAffineY()));
    }

    private static byte[] unsigned32(BigInteger n) {
        byte[] b = n.toByteArray();
        if (b.length == 32) return b;
        byte[] out = new byte[32];
        if (b.length > 32) System.arraycopy(b, b.length - 32, out, 0, 32);
        else System.arraycopy(b, 0, out, 32 - b.length, b.length);
        return out;
    }

    static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) out.writeBytes(p);
        return out.toByteArray();
    }

    static byte[] b64(String s) {
        return Base64.getUrlDecoder().decode(s.replace('+', '-').replace('/', '_').replace("=", ""));
    }

    static String b64url(byte[] b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
