package com.um.rainguard.data.model;

/** UI "Campus User" = STUDENT / STAFF / LECTURER. UI "Admin / Maintenance Team" = ADMIN. */
public enum UserRole {
    STUDENT, STAFF, LECTURER, ADMIN;

    public boolean isAdmin() {
        return this == ADMIN;
    }
}
