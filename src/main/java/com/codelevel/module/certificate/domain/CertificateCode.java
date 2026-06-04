package com.codelevel.module.certificate.domain;

import java.util.UUID;

public record CertificateCode(UUID value) {

    public CertificateCode {
        if (value == null) throw new IllegalArgumentException("Certificate code cannot be null");
    }

    public static CertificateCode generate() {
        return new CertificateCode(UUID.randomUUID());
    }

    /** Returns a human-readable display format, e.g. "CERT-A1B2C3D4" */
    public String display() {
        return "CERT-" + value.toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
