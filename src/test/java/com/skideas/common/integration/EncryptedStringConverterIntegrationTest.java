package com.skideas.common.integration;

import com.skideas.common.converter.EncryptedStringConverter;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(EncryptedStringConverter.class)
@TestPropertySource(properties = "skideas.encryption.key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
class EncryptedStringConverterIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void roundTrip_encryptsAndDecryptsCorrectly() {
        var entity = new SecretEntity("my secret value");
        var saved = em.persistFlushFind(entity);

        assertThat(saved.getSecret()).isEqualTo("my secret value");
    }

    @Test
    void databaseColumn_storesCiphertext_notPlaintext() {
        var entity = new SecretEntity("sensitive data");
        em.persistAndFlush(entity);
        em.clear();

        String raw = jdbc.queryForObject(
            "SELECT secret FROM secret_entity WHERE id = ?", String.class, entity.getId());

        assertThat(raw).isNotEqualTo("sensitive data");
        assertThat(raw).isNotBlank();
    }

    @Test
    void eachEncryptionCall_producesDifferentCiphertext() {
        var e1 = new SecretEntity("same plaintext");
        var e2 = new SecretEntity("same plaintext");
        em.persistAndFlush(e1);
        em.persistAndFlush(e2);
        em.clear();

        String raw1 = jdbc.queryForObject(
            "SELECT secret FROM secret_entity WHERE id = ?", String.class, e1.getId());
        String raw2 = jdbc.queryForObject(
            "SELECT secret FROM secret_entity WHERE id = ?", String.class, e2.getId());

        assertThat(raw1).isNotEqualTo(raw2);
    }

    @Test
    void nullInput_returnsNull() {
        var entity = new SecretEntity(null);
        var saved = em.persistFlushFind(entity);

        assertThat(saved.getSecret()).isNull();
    }

    @Test
    void thousandEncryptions_allUnique() {
        var converter = new EncryptedStringConverter("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        Set<String> ciphertexts = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ciphertexts.add(converter.convertToDatabaseColumn("constant plaintext"));
        }
        assertThat(ciphertexts).hasSize(1000);
    }

    @Test
    void blankInput_returnsNull() {
        var converter = new EncryptedStringConverter("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        assertThat(converter.convertToDatabaseColumn("   ")).isNull();
        assertThat(converter.convertToEntityAttribute("   ")).isNull();
    }

    @Test
    void invalidKeyLength_throwsIllegalArgumentException() {
        String shortKey = Base64.getEncoder().encodeToString("tooshort".getBytes());
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> new EncryptedStringConverter(shortKey))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("32-byte");
    }

    @Test
    void tamperedCiphertext_throwsIllegalStateException() {
        var converter = new EncryptedStringConverter("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> converter.convertToEntityAttribute("dGhpcyBpcyBub3QgdmFsaWQgY2lwaGVydGV4dA=="))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Decryption failed");
    }

    // ── minimal test entity ────────────────────────────────────────────────────

    @Entity
    @Table(name = "secret_entity")
    static class SecretEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Convert(converter = EncryptedStringConverter.class)
        @Column(name = "secret")
        private String secret;

        SecretEntity() {}

        SecretEntity(String secret) { this.secret = secret; }

        Long getId() { return id; }
        String getSecret() { return secret; }
    }
}
