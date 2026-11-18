package com.um.rainguard.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * What the PUBLIC map / hazard list may see. Not a table: Room fills it from a SELECT
 * of these columns only (see ReportDao).
 *
 * NO reporterId, NO email, NO matric number, NO phone. (Privacy rules 1 and 2)
 * Module 2 (Campus Hazard Map) must only ever use this class, never Report.
 */
public class HazardPublic {
    @NonNull
    public String reportId = "";
    @NonNull
    public ReportCategory category = ReportCategory.OTHER;
    @NonNull
    public Severity severity = Severity.MEDIUM;
    @NonNull
    public String description = "";
    @Nullable
    public String photoUri;
    public double latitude;
    public double longitude;
    @NonNull
    public String locationName = "";
    @NonNull
    public String zone = "";
    @NonNull
    public ReportStatus status = ReportStatus.VERIFIED;
    public long createdAt;
    public int stillThereCount;
    public int clearedCount;
}
