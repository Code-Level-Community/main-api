package com.codelevel.module.identity.domain;

import java.util.UUID;

public class User {

    private UUID publicId;
    private Username username;
    private String email;
    private Password password;

    public User(String name, String email, String password) {
        this.username = new Username(name);
        this.email = email;
        this.password = new Password(password);
    }

    public User(UUID publicId, String name, String email, String password) {
        this.publicId = publicId;
        this.username = new Username(name);
        this.email = email;
        this.password = new Password(password);
    }

    public boolean matchPass(String pass) {
        return password.matches(pass);
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password.value();
    }

    public void setPassword(String password) {
        this.password = new Password(password);
    }
}
