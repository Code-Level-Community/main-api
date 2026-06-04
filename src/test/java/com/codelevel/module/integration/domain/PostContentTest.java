package com.codelevel.module.integration.domain;

import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PostContentTest {

    @Test
    void shouldCreateWithValidContent() {
        PostContent content = new PostContent("Parabéns ao nosso top contributor do mês!");
        assertEquals("Parabéns ao nosso top contributor do mês!", content.value());
    }

    @Test
    void shouldThrowWhenContentIsNull() {
        assertThrows(BusinessRuleException.class, () -> new PostContent(null));
    }

    @Test
    void shouldThrowWhenContentIsBlank() {
        assertThrows(BusinessRuleException.class, () -> new PostContent("   "));
    }

    @Test
    void shouldThrowWhenContentIsEmpty() {
        assertThrows(BusinessRuleException.class, () -> new PostContent(""));
    }

    @Test
    void shouldAcceptContentWithExactlyMaxLength() {
        String content = "a".repeat(2200);
        assertDoesNotThrow(() -> new PostContent(content));
    }

    @Test
    void shouldThrowWhenContentExceedsMaxLength() {
        String content = "a".repeat(2201);
        assertThrows(BusinessRuleException.class, () -> new PostContent(content));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "🎉 Parabéns ao nosso top contributor!",
            "Novo curso disponível: Quarkus com GraalVM",
            "Atualização da plataforma: agora com suporte a LinkedIn"
    })
    void shouldAcceptValidContents(String value) {
        assertDoesNotThrow(() -> new PostContent(value));
    }
}