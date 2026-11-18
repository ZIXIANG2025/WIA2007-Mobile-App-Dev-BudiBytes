package com.um.rainguard.data.repository;

import com.um.rainguard.data.model.AiStatus;
import com.um.rainguard.data.model.AiTriage;
import com.um.rainguard.database.RainGuardDatabase;
import com.um.rainguard.data.model.Report;
import com.um.rainguard.data.model.ReportCategory;
import com.um.rainguard.data.model.ReportStatus;
import com.um.rainguard.data.model.ReviewAction;
import com.um.rainguard.data.model.Severity;
import com.um.rainguard.data.model.TaskStatus;
import com.um.rainguard.data.model.User;
import com.um.rainguard.data.model.UserRole;
import com.um.rainguard.data.model.VerificationLog;
import com.um.rainguard.utils.UIUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * First-run demo content, written into the real tables — no JSON, no preferences.
 *
 * <p>Five reports, three accounts, their AI triage rows and the admin actions that produced their
 * current state. Ids are derived from fixed strings so the seed is reproducible on every install.
 */
public final class DemoData {

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60L * MINUTE;
    private static final long DAY = 24L * HOUR;

    private DemoData() {
    }

    public static void seedIfEmpty(RainGuardDatabase database) {
        if (database.reportDao().count() > 0) return;

        database.runInTransaction(() -> {
            long now = System.currentTimeMillis();

            seedUsers(database);
            seedReports(database, now);
        });
    }

    private static void seedUsers(RainGuardDatabase database) {
        upsertUser(database, "student@um.edu.my", "Aina", UserRole.STUDENT);
        upsertUser(database, "community@um.edu.my", "Campus community", UserRole.STAFF);
        upsertUser(database, "admin@um.edu.my", "Operations admin", UserRole.ADMIN);
    }

    private static void upsertUser(RainGuardDatabase database, String email, String displayName, UserRole role) {
        User user = User.create(email, displayName, role);
        user.userId = ReportRepository.userIdForEmail(email);
        database.userDao().upsert(user);
    }

    private static void seedReports(RainGuardDatabase database, long now) {
        List<Report> reports = new ArrayList<>();
        List<VerificationLog> logs = new ArrayList<>();

        // 1 — in progress, team already on site
        Report engineering = report("demo-1",
                "student@um.edu.my", "Faculty of Engineering, Block A", "Faculty buildings",
                ReportCategory.CLOGGED_DRAIN, Severity.HIGH, ReportStatus.IN_PROGRESS,
                "Clogged observed after rainfall. Please inspect the drainage channel and nearby walkway.",
                now - 2 * HOUR, 14, 0);
        engineering.assignedTeam = "JPPHB Drainage Team";
        engineering.instructions = "Inspect the box culvert and clear fallen leaves.";
        engineering.taskStatus = TaskStatus.INSPECTION_IN_PROGRESS;
        reports.add(engineering);
        logs.add(log(engineering, "admin@um.edu.my", ReviewAction.VERIFY,
                ReportStatus.PENDING_MODERATOR, ReportStatus.VERIFIED,
                "Report verified by maintenance administrator.", null, now - 100 * MINUTE));
        logs.add(log(engineering, "admin@um.edu.my", ReviewAction.ASSIGN,
                ReportStatus.VERIFIED, ReportStatus.IN_PROGRESS,
                "Maintenance assigned to JPPHB Drainage Team. Instructions: Inspect the box culvert and clear fallen leaves.",
                "JPPHB Drainage Team", now - 90 * MINUTE));
        logs.add(log(engineering, "admin@um.edu.my", ReviewAction.UPDATE_PROGRESS,
                ReportStatus.IN_PROGRESS, ReportStatus.IN_PROGRESS,
                "Inspection in Progress: Team on site, clearing the culvert inlet.",
                "JPPHB Drainage Team", now - 40 * MINUTE));

        // 2 — verified, waiting for an assignee
        Report busStop = report("demo-2",
                "community@um.edu.my", "KK8 walkway to bus stop", "KK8",
                ReportCategory.STAGNANT_WATER, Severity.HIGH, ReportStatus.VERIFIED,
                "Stagnant water observed along the walkway after rainfall.", now - 5 * HOUR, 9, 1);
        reports.add(busStop);
        logs.add(log(busStop, "admin@um.edu.my", ReviewAction.VERIFY,
                ReportStatus.PENDING_MODERATOR, ReportStatus.VERIFIED,
                "Report verified. Awaiting a maintenance team.", null, now - 4 * HOUR));

        // 3 — in the review queue
        Report library = report("demo-3",
                "student@um.edu.my", "Main Library back entrance", "Central campus",
                ReportCategory.OVERFLOW, Severity.MEDIUM, ReportStatus.PENDING_MODERATOR,
                "Overflowing drain observed near the back entrance.", now - DAY, 4, 0);
        reports.add(library);

        // 4 — resolved, with the full audit trail
        Report rimba = report("demo-4",
                "community@um.edu.my", "Rimba Ilmu car park", "Central campus",
                ReportCategory.SEDIMENT, Severity.LOW, ReportStatus.RESOLVED,
                "Sediment was building up in the drainage channel.", now - 3 * DAY, 6, 5);
        rimba.assignedTeam = "Campus Facilities Team";
        rimba.instructions = "Remove the sediment and restore drainage flow.";
        rimba.taskStatus = TaskStatus.COMPLETED;
        rimba.resolutionNotes = "Sediment removed and drainage flow restored.";
        rimba.resolvedAt = now - 2 * DAY;
        reports.add(rimba);
        logs.add(log(rimba, "admin@um.edu.my", ReviewAction.VERIFY,
                ReportStatus.PENDING_MODERATOR, ReportStatus.VERIFIED,
                "Report verified by maintenance administrator.", null, now - 70 * HOUR));
        logs.add(log(rimba, "admin@um.edu.my", ReviewAction.ASSIGN,
                ReportStatus.VERIFIED, ReportStatus.IN_PROGRESS,
                "Maintenance assigned to Campus Facilities Team. Instructions: Remove the sediment and restore drainage flow.",
                "Campus Facilities Team", now - 68 * HOUR));
        logs.add(log(rimba, "admin@um.edu.my", ReviewAction.RESOLVE,
                ReportStatus.IN_PROGRESS, ReportStatus.RESOLVED,
                "Task completed; report resolved. Sediment removed and drainage flow restored.",
                "Campus Facilities Team", now - 2 * DAY));

        // 5 — in the review queue, at a landmark
        Report dtc = report("demo-5",
                "community@um.edu.my", "Dewan Tunku Canselor side drain", "Central campus",
                ReportCategory.CLOGGED_DRAIN, Severity.MEDIUM, ReportStatus.PENDING_MODERATOR,
                "Litter is restricting water flow in the side drain.", now - 3 * DAY, 3, 0);
        reports.add(dtc);

        for (Report report : reports) {
            database.reportDao().upsert(report);
            database.aiTriageDao().upsert(triage(report, now));
        }
        for (VerificationLog entry : logs) {
            database.verificationLogDao().insert(entry);
        }
    }

    private static Report report(String seed, String reporterEmail, String locationName, String zone,
                                 ReportCategory category, Severity severity, ReportStatus status,
                                 String description, long createdAt, int stillThere, int cleared) {
        Report report = new Report();
        report.reportId = demoId(seed);
        report.reporterId = ReportRepository.userIdForEmail(reporterEmail);
        report.locationName = locationName;
        report.zone = zone;
        report.category = category;
        report.severity = severity;
        report.description = description;
        report.status = status;
        report.createdAt = createdAt;
        report.updatedAt = createdAt;
        report.stillThereCount = stillThere;
        report.clearedCount = cleared;
        return report;
    }

    private static AiTriage triage(Report report, long now) {
        AiTriage triage = new AiTriage();
        triage.triageId = demoId("triage-" + report.reportId);
        triage.reportId = report.reportId;
        triage.status = AiStatus.SUCCESS;
        triage.modelName = "demo-rule-based";
        triage.latencyMs = 12L;
        triage.confidence = 0.72f;
        triage.suggestedCategory = report.category;
        triage.suggestedSeverity = report.severity;
        triage.summary = UIUtils.categoryLabel(report.category) + " reported at " + report.locationName
                + ". " + report.description + " The affected area may create a slipping hazard and "
                + "disrupt pedestrian movement. An on-site inspection is recommended. This is a demo "
                + "rule-based summary; administrator review is required.";
        triage.suggestedAction = "Inspect the location, confirm the hazard, then assign a maintenance team.";
        triage.safetyAdviceGenerated = "Avoid walking or driving through standing water, and report any change in the hazard.";
        triage.createdAt = now;
        return triage;
    }

    private static VerificationLog log(Report report, String actorEmail, ReviewAction action,
                                       ReportStatus fromStatus, ReportStatus toStatus,
                                       String remarks, String team, long createdAt) {
        VerificationLog entry = new VerificationLog();
        entry.logId = demoId("log-" + report.reportId + "-" + action + "-" + createdAt);
        entry.reportId = report.reportId;
        entry.actorId = ReportRepository.userIdForEmail(actorEmail);
        entry.action = action;
        entry.fromStatus = fromStatus;
        entry.toStatus = toStatus;
        entry.remarks = remarks;
        entry.team = team;
        entry.createdAt = createdAt;
        return entry;
    }

    private static String demoId(String seed) {
        return UUID.nameUUIDFromBytes(("rainguard:demo:" + seed).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
