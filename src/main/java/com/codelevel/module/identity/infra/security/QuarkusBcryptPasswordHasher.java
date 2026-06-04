package com.codelevel.module.identity.infra.security;

import com.codelevel.module.identity.domain.PasswordHasher;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class QuarkusBcryptPasswordHasher implements PasswordHasher {

    private static final int COST = 12;

    @Override
    public String hash(String raw) {
        return BcryptUtil.bcryptHash(raw, COST);
    }

    @Override
    public boolean verify(String raw, String hashed) {
        return BcryptUtil.matches(raw, hashed);
    }

}
