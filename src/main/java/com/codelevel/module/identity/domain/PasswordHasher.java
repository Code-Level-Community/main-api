// language: java
package com.codelevel.module.identity.domain;

public interface PasswordHasher {
    String hash(String raw);
    boolean verify(String raw, String hashed);
}
