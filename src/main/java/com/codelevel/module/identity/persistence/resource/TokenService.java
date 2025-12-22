package com.codelevel.module.identity.persistence.resource;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    public String generateAccessToken(String username, String roles) {
        return Jwt.issuer(issuer)
                .upn(username)
                .groups(new HashSet<>(Arrays.asList(roles.split(","))))
                .expiresIn(Duration.ofMinutes(15))
                .sign();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

}
