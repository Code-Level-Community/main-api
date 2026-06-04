package com.codelevel.module.identity.domain;

import com.codelevel.shared.exception.BusinessRuleException;

import java.util.UUID;

public class User {

    private UUID publicId;
    private Username username;
    private String fullName;
    private EmailAddress emailAddress;
    private Password password;

    public User() {
        this.publicId = UUID.randomUUID();
    }

    public User(String name, String email, String password) {
        setFullName(name);
        if (this.username == null) this.username = new Username(fullName.split(" ")[0]+hashCode());
        this.emailAddress = new EmailAddress(email);
        this.password = new Password(password);
    }

    public User(UUID publicId, String name, String email, String password) {
        this.publicId = publicId;
        this.username = new Username(name);
        this.emailAddress = new EmailAddress(email);
        this.password = new Password(password);
    }

    public User(UUID publicId, String username, String fullName, String email, String password) {
        this.publicId = publicId;
        this.username = new Username(username);
        this.fullName = fullName;
        this.emailAddress = new EmailAddress(email);
        this.password = new Password(password);
    }

    public boolean matchPass(String pass, PasswordHasher hasher) {
        return hasher.verify(pass, this.password.value());
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public String getUsername() {
        return username.value();
    }

    public void setUsername(String name) {
        this.username = new Username(name);
    }

    public String getEmailAddress() {
        return emailAddress.value();
    }

    public void setEmailAddress(String email) {
        this.emailAddress = new EmailAddress(email);
    }

    public String getPassword() {
        return password.value();
    }

    public void setPassword(String password) {
        this.password = new Password(password);
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            throw new BusinessRuleException("Full name cannot be null or empty");
        }
        this.fullName = fullName;
    }

    @Override
    public String toString() {
        return "User{" +
            "publicId=" + publicId.toString() +
            ", username=" + username.value() +
            ", fullName='" + fullName + '\'' +
            ", email=" + emailAddress.value() +
            ", password=" + password.value() +
            '}';
    }
}
