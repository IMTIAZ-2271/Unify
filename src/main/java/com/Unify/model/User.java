package com.Unify.model;

import java.time.LocalDateTime;

public class User {
    // Add this inside User.java
    private boolean isAdmin;
    // Add the getter and setter
    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }
    private int id;
    private String username;
    private String email;
    private String passwordHash;
    private String displayName;
    private String bio;
    private byte[] profilePicture;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    public User() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String v) {
        this.username = v;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        this.email = v;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String v) {
        this.passwordHash = v;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : username;
    }

    public void setDisplayName(String v) {
        this.displayName = v;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String v) {
        this.bio = v;
    }

    public byte[] getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(byte[] v) {
        this.profilePicture = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        this.createdAt = v;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime v) {
        this.lastLogin = v;
    }

    @Override
    public String toString() {
        return getDisplayName() + " (@" + username + ")";
    }
}
