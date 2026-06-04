package com.codelevel.shared.lib.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.io.IOException;

@Provider
public class SecurityHeadersFilter implements ContainerResponseFilter {

    @ConfigProperty(name = "app.environment", defaultValue = "development")
    String environment;

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {

        // Content Security Policy
        String csp = isProduction()
                ? "default-src 'self'; " +
                "script-src 'self' https://cdnjs.cloudflare.com; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: https:; " +
                "font-src 'self' data:; " +
                "connect-src 'self' https://api.codelevel.com.br;"
                : "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdnjs.cloudflare.com; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: https:; " +
                "font-src 'self' data:; " +
                "connect-src 'self' http://localhost:* ws://localhost:*;";

        responseContext.getHeaders().add("Content-Security-Policy", csp);

        // Additional security headers
        responseContext.getHeaders().add("X-Content-Type-Options", "nosniff");
        responseContext.getHeaders().add("X-Frame-Options", "DENY");
        responseContext.getHeaders().add("X-XSS-Protection", "1; mode=block");
        responseContext.getHeaders().add("Referrer-Policy", "strict-origin-when-cross-origin");

        // HSTS (production only)
        if (isProduction()) {
            responseContext.getHeaders().add(
                    "Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains"
            );
        }
    }

    private boolean isProduction() {
        return "production".equalsIgnoreCase(environment);
    }
}