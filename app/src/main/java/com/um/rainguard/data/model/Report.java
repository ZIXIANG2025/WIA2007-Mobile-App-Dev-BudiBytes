package com.um.rainguard.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

/**
 * Table 2 of 5: reports.  The central table; modules 1, 2, 3 and 4 all use it.
 * (UI: Report Issue, My Reports, Hazard Map, All Reports, Maintenance Tasks)
 *
 * PRIVATE entity (contains reporterId). For the public map use {@link HazardPublic} instead.
 */
@Entity(
        tableName = "reports",
        indices = {@Index("reporterId"), @Index("status"), @Index("zone")}
)
public class Report {

    @PrimaryKey
    @NonNull
    public String reportId = UUID.randomUUID().toString();

    @NonNull
    public String reporterId = "";                 // -> users.userId   (PRIVATE)

    // --- what the reporter submitted (Module 1) ---
    @NonNull
    public ReportCategory category = ReportCategory.OTHER;

    /** Reporter's guess; an admin may change it (VERIFY / OVERRIDE_AI). */
    @NonNull
    public Severity severity = Severity.MEDIUM;

    @NonNull
    public String description = "";                // min 10 chars (UI rule)

    @Nullable
    public String photoUri;                        // local uri now, cloud URL later

    // --- location (Module 2). Keep BOTH so we can start with a picture map and upgrade later ---
    public double latitude;
    public double longitude;

    @NonNull
    public String locationName = "";               // e.g. "KK8 walkway to bus stop"

    @NonNull
    public String zone = "";                       // one of CampusZones.ALL

    @Nullable
    public Float pinX;                             // optional 0..100 for the mock campus picture map
    @Nullable
    public Float pinY;

    // --- workflow ---
    @NonNull
    public ReportStatus status = ReportStatus.PENDING_AI;

    public long createdAt = System.currentTimeMillis();
    public long updatedAt = System.currentTimeMillis();

    // --- community confirmation (UI: "Still There" / "Cleared"). Never resolves a report by itself ---
    public int stillThereCount = 0;
    public int clearedCount = 0;

    // --- maintenance response (Module 4). All null until an admin assigns a team ---
    @Nullable
    public String assignedTeam;                    // one of MaintenanceTeams.ALL
    @Nullable
    public String instructions;
    @Nullable
    public TaskStatus taskStatus;
    @Nullable
    public String resolutionNotes;
    @Nullable
    public String completionPhotoUri;
    @Nullable
    public Long resolvedAt;

    /** Copy only the public columns. The result never contains reporterId. */
    public HazardPublic toPublic() {
        HazardPublic h = new HazardPublic();
        h.reportId = reportId;
        h.category = category;
        h.severity = severity;
        h.description = description;
        h.photoUri = photoUri;
        h.latitude = latitude;
        h.longitude = longitude;
        h.locationName = locationName;
        h.zone = zone;
        h.status = status;
        h.createdAt = createdAt;
        h.stillThereCount = stillThereCount;
        h.clearedCount = clearedCount;
        return h;
    }
}
