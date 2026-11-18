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
 * Table 5 of 5: alerts.  Module 5 (Alerts and Safety Information).
 * (UI: Notifications page / panel, "heavy rainfall warning", precautions list)
 *
 * PUBLIC data: safe for every user to read. Contains no personal data.
 * level == EMERGENCY  => the app must also fire a push notification.
 */
@Entity(tableName = "alerts", indices = {@Index("isActive"), @Index("level")})
public class Alert {

    @PrimaryKey
    @NonNull
    public String alertId = UUID.randomUUID().toString();

    @NonNull
    public String title = "";

    @NonNull
    public String message = "";

    @NonNull
    public AlertLevel level = AlertLevel.INFO;

    /** Comma-separated zones from CampusZones.ALL. Empty = whole campus. */
    @NonNull
    public String affectedAreas = "";

    /** One precaution per line, e.g. "Avoid basement car parks\nDo not walk through flowing water". */
    @NonNull
    public String precautions = "";

    @NonNull
    public AlertSource source = AlertSource.MANUAL;

    @Nullable
    public String relatedReportId;                 // e.g. "Crew dispatched to RG-1042"
    @Nullable
    public String createdBy;                       // admin userId; null for METMALAYSIA / SYSTEM

    public long createdAt = System.currentTimeMillis();

    @Nullable
    public Long expiresAt;

    public boolean isActive = true;

    public List<String> areas() {
        return splitClean(affectedAreas, ",");
    }

    public List<String> precautionList() {
        return splitClean(precautions, "\n");
    }

    private static List<String> splitClean(String s, String sep) {
        List<String> out = new ArrayList<>();
        for (String part : s.split(sep)) {
            if (!part.trim().isEmpty()) out.add(part.trim());
        }
        return out;
    }
}
