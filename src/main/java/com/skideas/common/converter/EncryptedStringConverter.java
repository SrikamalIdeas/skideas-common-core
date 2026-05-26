package com.skideas.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA attribute converter that transparently encrypts/decrypts String fields using AES-256-GCM.
 *
 * <p>Usage on an entity field:
 * <pre>{@code
 *   @Convert(converter = EncryptedStringConverter.class)
 *   private String sensitiveData;
 * }</pre>
 *
 * <p>Requires {@code skideas.encryption.key} property set to a Base64-encoded 32-byte AES key.
 * Set via environment variable {@code SKIDEAS_ENCRYPTION_KEY}.
 *
 * <p>Security properties:
 * <ul>
 *   <li>AES-256-GCM with a unique 12-byte IV per encryption call</li>
 *   <li>IV is prepended to ciphertext before Base64 encoding</li>
 *   <li>Plaintext and key material are never logged</li>
 * </ul>
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(EncryptedStringConverter.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptedStringConverter(@Value("${skideas.encryption.key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                "SKIDEAS_ENCRYPTION_KEY must be a Base64-encoded 32-byte key; got " + keyBytes.length + " bytes");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        log.debug("EncryptedStringConverter initialised");
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

            byte[] ivPlusCipher = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, ivPlusCipher, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, ivPlusCipher, IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(ivPlusCipher);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        try {
            byte[] ivPlusCipher = Base64.getDecoder().decode(stored);
            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[ivPlusCipher.length - IV_LENGTH];
            System.arraycopy(ivPlusCipher, 0, iv, 0, IV_LENGTH);
            System.arraycopy(ivPlusCipher, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext));
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }
}
