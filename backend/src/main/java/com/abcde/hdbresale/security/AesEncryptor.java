package com.abcde.hdbresale.security;

import com.abcde.hdbresale.config.AppProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** AES-256-GCM encryption for sensitive columns (NFR-6). Output is base64(iv || ciphertext+tag). */
@Component
public class AesEncryptor {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static volatile AesEncryptor instance;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public AesEncryptor(AppProperties properties) {
        byte[] raw = Base64.getDecoder().decode(properties.security().encryptionKey());
        if (raw.length != 32) {
            throw new IllegalStateException("app.security.encryption-key must be a base64 encoded 32-byte key");
        }
        this.key = new SecretKeySpec(raw, "AES");
        instance = this;
    }

    /** JPA converters are created by Hibernate, so they reach the configured encryptor through this. */
    static AesEncryptor get() {
        if (instance == null) {
            throw new IllegalStateException("AesEncryptor not initialised");
        }
        return instance;
    }

    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] all = Base64.getDecoder().decode(encoded);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_BYTES));
            return new String(cipher.doFinal(all, IV_BYTES, all.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }
}
