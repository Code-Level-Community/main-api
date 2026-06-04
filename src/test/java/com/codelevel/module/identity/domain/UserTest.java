package com.codelevel.module.identity.domain;

import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private static final PasswordHasher PLAIN_HASHER = new PasswordHasher() {
        @Override public String hash(String raw) { return raw; }
        @Override public boolean verify(String raw, String hashed) { return raw.equals(hashed); }
    };

    @Test
    void defaultConstructorShouldGeneratePublicId() {
        var user = new User();
        assertNotNull(user.getPublicId());
    }

    @Test
    void defaultConstructorShouldGenerateDifferentPublicIdEachTime() {
        assertNotEquals(new User().getPublicId(), new User().getPublicId());
    }

    @Test
    void fullConstructorShouldStoreAllFields() {
        var id = UUID.randomUUID();
        var user = new User(id, "lucas", "Lucas Silva", "lucas@example.com", "Secret123!");

        assertEquals(id, user.getPublicId());
        assertEquals("lucas", user.getUsername());
        assertEquals("Lucas Silva", user.getFullName());
        assertEquals("lucas@example.com", user.getEmailAddress());
        assertEquals("Secret123!", user.getPassword());
    }

    @Test
    void nameEmailPasswordConstructorShouldSetFullName() {
        var user = new User("Lucas Silva", "lucas@example.com", "Secret123!");
        assertEquals("Lucas Silva", user.getFullName());
    }

    @Test
    void nameEmailPasswordConstructorShouldDeriveUsernameFromFirstWord() {
        var user = new User("Lucas Silva", "lucas@example.com", "Secret123!");
        assertTrue(user.getUsername().startsWith("Lucas"));
    }

    @Test
    void publicIdNameEmailPasswordConstructorShouldSetPublicId() {
        var id = UUID.randomUUID();
        var user = new User(id, "lucas", "lucas@example.com", "Secret123!");
        assertEquals(id, user.getPublicId());
    }

    @Test
    void matchPassShouldReturnTrueWhenPasswordMatches() {
        var user = new User(UUID.randomUUID(), "lucas", "Lucas Silva", "lucas@example.com", "Secret123!");
        user.setPassword(PLAIN_HASHER.hash("Secret123!"));

        assertTrue(user.matchPass("Secret123!", PLAIN_HASHER));
    }

    @Test
    void matchPassShouldReturnFalseWhenPasswordDoesNotMatch() {
        var user = new User(UUID.randomUUID(), "lucas", "Lucas Silva", "lucas@example.com", "Secret123!");
        user.setPassword(PLAIN_HASHER.hash("Secret123!"));

        assertFalse(user.matchPass("wrongpass", PLAIN_HASHER));
    }

    @Test
    void setUsernameShouldValidateNewValue() {
        var user = new User(UUID.randomUUID(), "lucas", "Lucas Silva", "lucas@example.com", "Secret123!");
        user.setUsername("newname");
        assertEquals("newname", user.getUsername());
    }

    @Test
    void setUsernameShouldThrowWhenInvalid() {
        var user = new User(UUID.randomUUID(), "lucas", "Lucas Silva", "lucas@example.com", "Secret123!");
        assertThrows(BusinessRuleException.class, () -> user.setUsername("ab"));
    }

    @Test
    void setEmailAddressShouldValidateNewValue() {
        var user = new User(UUID.randomUUID(), "lucas", "Lucas Silva", "lucas@example.com", "Secret123!");
        user.setEmailAddress("new@example.com");
        assertEquals("new@example.com", user.getEmailAddress());
    }

    @Test
    void setEmailAddressShouldThrowWhenInvalid() {
        var user = new User(UUID.randomUUID(), "lucas", "Lucas Silva", "lucas@example.com", "Secret123!");
        assertThrows(BusinessRuleException.class, () -> user.setEmailAddress("not-an-email"));
    }

    @Test
    void setPasswordShouldValidateNewValue() {
        var user = new User(UUID.randomUUID(), "lucas", "Lucas Silva", "lucas@example.com", "Secret123!");
        user.setPassword("Newpass123!");
        assertEquals("Newpass123!", user.getPassword());
    }

    @Test
    void setPasswordShouldThrowWhenTooShort() {
        var user = new User(UUID.randomUUID(), "lucas", "Lucas Silva", "lucas@example.com", "Secret123!");
        assertThrows(BusinessRuleException.class, () -> user.setPassword("abc"));
    }

    @Test
    void setFullNameShouldThrowWhenNull() {
        var user = new User(UUID.randomUUID(), "lucas", "Lucas Silva", "lucas@example.com", "Secret123!");
        var ex = assertThrows(BusinessRuleException.class, () -> user.setFullName(null));
        assertEquals("Full name cannot be null or empty", ex.getMessage());
    }

    @Test
    void setFullNameShouldThrowWhenEmpty() {
        var user = new User(UUID.randomUUID(), "lucas", "Lucas Silva", "lucas@example.com", "Secret123!");
        var ex = assertThrows(BusinessRuleException.class, () -> user.setFullName(""));
        assertEquals("Full name cannot be null or empty", ex.getMessage());
    }

    @Test
    void toStringShouldContainPublicId() {
        var id = UUID.randomUUID();
        var user = new User(id, "lucas", "Lucas Silva", "lucas@example.com", "Secret123!");
        assertTrue(user.toString().contains(id.toString()));
    }
}
