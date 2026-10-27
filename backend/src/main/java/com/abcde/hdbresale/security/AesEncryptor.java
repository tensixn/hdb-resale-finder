package com.abcde.hdbresale.security;

import com.abcde.hdbresale.config.AppProperties;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
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
        // TODO (Lab 4): implement get
        throw new UnsupportedOperationException("TODO: AesEncryptor.get");
    }

    public String encrypt(String plain) {
        // TODO (Lab 4): implement encrypt
        throw new UnsupportedOperationException("TODO: AesEncryptor.encrypt");
    }

    public String decrypt(String encoded) {
        // TODO (Lab 4): implement decrypt
        throw new UnsupportedOperationException("TODO: AesEncryptor.decrypt");
    }
}
