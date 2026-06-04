package com.codelevel.module.certificate.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CertificateCodeTest {

    @Test
    void shouldGenerateNonNullCode() {
        CertificateCode code = CertificateCode.generate();

        assertNotNull(code);
        assertNotNull(code.value());
    }

    @Test
    void shouldGenerateUniqueCodesEachTime() {
        CertificateCode code1 = CertificateCode.generate();
        CertificateCode code2 = CertificateCode.generate();

        assertNotEquals(code1.value(), code2.value());
    }

    @Test
    void shouldFormatDisplayWithCertPrefix() {
        CertificateCode code = CertificateCode.generate();

        assertTrue(code.display().startsWith("CERT-"));
    }

    @Test
    void shouldFormatDisplayWithEightCharsAfterPrefix() {
        CertificateCode code = CertificateCode.generate();
        String suffix = code.display().substring(5);

        assertEquals(8, suffix.length());
    }

    @Test
    void shouldFormatDisplayInUppercase() {
        CertificateCode code = CertificateCode.generate();
        String suffix = code.display().substring(5);

        assertEquals(suffix.toUpperCase(), suffix);
    }

    @Test
    void shouldFormatDisplayWithTotalLengthOfThirteen() {
        CertificateCode code = CertificateCode.generate();

        assertEquals(13, code.display().length());
    }

    @Test
    void shouldThrowWhenConstructedWithNull() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new CertificateCode(null));

        assertNotNull(ex.getMessage());
    }

    @Test
    void shouldPreserveUUIDValue() {
        UUID uuid = UUID.randomUUID();
        CertificateCode code = new CertificateCode(uuid);

        assertEquals(uuid, code.value());
    }

    @Test
    void shouldProduceSameDisplayForSameUUID() {
        UUID uuid = UUID.randomUUID();
        CertificateCode c1 = new CertificateCode(uuid);
        CertificateCode c2 = new CertificateCode(uuid);

        assertEquals(c1.display(), c2.display());
    }

    @Test
    void shouldImplementRecordEqualityByValue() {
        UUID uuid = UUID.randomUUID();
        CertificateCode c1 = new CertificateCode(uuid);
        CertificateCode c2 = new CertificateCode(uuid);

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenDifferentUUID() {
        CertificateCode c1 = new CertificateCode(UUID.randomUUID());
        CertificateCode c2 = new CertificateCode(UUID.randomUUID());

        assertNotEquals(c1, c2);
    }
}
