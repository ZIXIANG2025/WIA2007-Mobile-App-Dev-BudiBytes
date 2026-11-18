package com.um.rainguard.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

/**
 * Table 4 of 5: verification_logs.  Module 4 (Verification and Response Management).
 *
 * One row for EVERY admin action on a report (verify, reject, assign, progress, resolve,
 * AI override). This doubles as the report's history timeline (UI: report "history" list,
 * "Analytics & Activity") and as the audit trail.
 *
 * Rule: REJECT requires non-empty remarks. RESOLVE requires resolution remarks.
 */
@Entity(
        tableName = "verification_logs",
        foreignKeys = {@ForeignKey(
                entity = Report.class,
                parentColumns = "reportId",
                childColumns = "reportId",
                onDelete = ForeignKey.CASCADE
        )},
        indices = {@Index("reportId"), @Index("actorId")}
)
public class VerificationLog {

    @PrimaryKey
    @NonNull
    public String logId = UUID.randomUUID().toString();

    @NonNull
    public String reportId = "";                   // -> reports.reportId

    @NonNull
    public String actorId = "";                    // admin userId (-> users.userId)

    @NonNull
    public ReviewAction action = ReviewAction.VERIFY;

    @Nullable
    public ReportStatus fromStatus;
    @Nullable
    public ReportStatus toStatus;

    @NonNull
    public String remarks = "";

    @Nullable
    public String team;                            // for ASSIGN

    public long createdAt = System.currentTimeMillis();
}
