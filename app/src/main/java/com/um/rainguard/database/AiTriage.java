package com.um.rainguard.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Table 3 of 5: ai_triage.  Module 3 (AI-Assisted Report Triage). One row per report.
 * (UI: "AI-assisted review", "AI Incident Brief", duplicate-report suggestions)
 *
 * The AI only SUGGESTS. It never verifies, assigns or resolves a report: an admin decides.
 * If the AI fails or times out we still save a row (status = FAILED / TIMEOUT) and the
 * report moves on to PENDING_MODERATOR so nothing gets stuck.
 *
 * PRIVACY: this table is the "AI log". It must NOT contain raw personal data
 * (no email, matric no, phone, reporter name).
 */
@Entity(
        tableName = "ai_triage",
        foreignKeys = {@ForeignKey(
                entity = Report.class,
                parentColumns = "reportId",
                childColumns = "reportId",
                onDelete = ForeignKey.CASCADE
        )},
        indices = {@Index(value = {"reportId"}, unique = true)}
)
public class AiTriage {

    @PrimaryKey
    @NonNull
    public String triageId = UUID.randomUUID().toString();

    @NonNull
    public String reportId = "";                   // -> reports.reportId

    @NonNull
    public AiStatus status = AiStatus.SUCCESS;     // SUCCESS / FAILED / TIMEOUT

    @Nullable
    public String errorMessage;                    // only when FAILED / TIMEOUT
    @Nullable
    public Long latencyMs;
    @Nullable
    public String modelName;                       // what was used, for the AI usage log

    // --- AI output (all null when status != SUCCESS) ---
    @Nullable
    public ReportCategory suggestedCategory;
    @Nullable
    public Severity suggestedSeverity;
    @Nullable
    public Float confidence;                       // 0.0 .. 1.0
    @Nullable
    public String summary;                         // UI: "Generate Summary"
    @Nullable
    public String suggestedAction;                 // UI: "Suggested next action"
    @Nullable
    public String safetyAdviceGenerated;           // shown to the student right after submitting

    /** Comma-separated reportIds that look like duplicates. UI: "Duplicate-report suggestions". */
    @NonNull
    public String possibleDuplicateIds = "";

    // --- moderator override (function: recordModeratorOverride) ---
    @Nullable
    public String overriddenBy;                    // admin userId
    @Nullable
    public Severity overrideSeverity;
    @Nullable
    public String overrideReason;
    @Nullable
    public Long overriddenAt;

    public long createdAt = System.currentTimeMillis();

    public List<String> duplicates() {
        List<String> out = new ArrayList<>();
        for (String s : possibleDuplicateIds.split(",")) {
            if (!s.trim().isEmpty()) out.add(s.trim());
        }
        return out;
    }
}
