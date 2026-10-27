package com.abcde.hdbresale.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Encrypts a String column transparently on write and decrypts it on read (NFR-6). */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : AesEncryptor.get().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AesEncryptor.get().decrypt(dbData);
    }
}
