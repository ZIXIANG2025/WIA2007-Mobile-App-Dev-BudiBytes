package com.um.rainguard.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Table 1 of 5: users.  (UI: sign-in, Profile & Settings, notification preferences)
 *
 * PRIVATE data. Never put User fields (email, matricNo) into any public/map model.
 * Passwords are NOT stored here. Authentication is handled by the auth provider
 * (demo login or Firebase Auth); we only keep the profile + preferences.
 *
 * Fields are public so Room and every module can use them directly:
 *   User u = User.create("student@um.edu.my", "Ali", UserRole.STUDENT);
 */
@Entity(tableName = "users", indices = {@Index(value = {"email"}, unique = true)})
public class User {

    @PrimaryKey
    @NonNull
    public String userId = UUID.randomUUID().toString();   // UUID (or Firebase uid later)

    @NonNull
    public String email = "";                               // e.g. student@um.edu.my (PRIVATE)

    @NonNull
    public String displayName = "";

    @NonNull
    public UserRole role = UserRole.STUDENT;

    @Nullable
    public String matricNo;                                 // optional, PRIVATE

    // Preferences (UI: Settings > "Notifications & zones")
    public boolean notifyHeavyRain = true;
    public boolean notifyFlashFlood = true;
    public boolean weeklySummary = false;

    /** Comma-separated zone names, e.g. "KK8,Faculty buildings". Values come from CampusZones.ALL. */
    @NonNull
    public String subscribedZones = "KK8,Faculty buildings";

    public long createdAt = System.currentTimeMillis();

    /** Convenience factory. Room itself uses the public no-arg constructor. */
    public static User create(String email, String displayName, UserRole role) {
        User u = new User();
        u.email = email;
        u.displayName = displayName;
        u.role = role;
        return u;
    }

    public List<String> zones() {
        List<String> out = new ArrayList<>();
        for (String z : subscribedZones.split(",")) {
            if (!z.trim().isEmpty()) out.add(z.trim());
        }
        return out;
    }
}
