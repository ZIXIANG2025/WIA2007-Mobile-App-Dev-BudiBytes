package com.um.rainguard.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The single data layer the screens talk to. It owns no data of its own: every field comes from
 * the Room tables in {@link RainGuardDatabase}, and every write goes through a DAO inside a
 * transaction.
 *
 * <p>Two ways to read:
 * <ul>
 *   <li>{@link #observeReports()} — a {@link LiveData} that re-emits whenever the reports table
 *       changes. Lists and dashboards observe it and re-render.</li>
 *   <li>{@link #getReports()} / {@link #findReport(String)} — a synchronous snapshot of the same
 *       data, kept up to date by the observer above. Safe to call on the UI thread because it never
 *       touches SQLite.</li>
 * </ul>
 *
 * <p>The observer is registered in the constructor, so the repository must be created on the main
 * thread — it always is, from {@code BaseActivity.onCreate}.
 */
public class ReportRepository {

    private static volatile ReportRepository instance;

    private final RainGuardDatabase database;
    private final MutableLiveData<List<Report>> reportsLive = new MutableLiveData<>(Collections.emptyList());
    private volatile List<Report> cachedReports = Collections.emptyList();

    private ReportRepository(Context context) {
        Context appContext = context.getApplicationContext();
        database = RainGuardDatabase.getInstance(appContext);

        // Keeps the synchronous snapshot warm and fans every table change out to the screens.
        database.reportDao().observeAll().observeForever(list -> {
            cachedReports = list == null ? Collections.emptyList() : new ArrayList<>(list);
            reportsLive.setValue(cachedReports);
        });

        // First launch: five demo reports, three demo users, their audit trail and AI rows.
        RainGuardDatabase.databaseExecutor.execute(() -> DemoData.seedIfEmpty(database));
    }

    public static ReportRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (ReportRepository.class) {
                if (instance == null) {
                    instance = new ReportRepository(context);
                }
            }
        }
        return instance;
    }

    // --- identity ----------------------------------------------------------

    /**
     * Stable id for an account, used as {@code users.userId} and {@code reports.reporterId}.
     *
     * <p>The demo sign-in only collects an e-mail, so the id is derived from it. When real login
     * lands this becomes the authentication provider's uid and nothing else has to change.
     */
    public static String userIdForEmail(String email) {
        String normalised = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return UUID.nameUUIDFromBytes(("rainguard:" + normalised).getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** Insert the signed-in account if the users table does not have it yet. */
    public void ensureUser(String email, String role) {
        if (email == null) return;
        String normalised = email.trim().toLowerCase(Locale.ROOT);
        String userId = userIdForEmail(normalised);
        UserRole userRole = "admin".equals(role) ? UserRole.ADMIN : UserRole.STUDENT;

        RainGuardDatabase.databaseExecutor.execute(() -> {
            User existing = database.userDao().getById(userId);
            if (existing == null) {
                User user = User.create(normalised, displayNameFor(normalised), userRole);
                user.userId = userId;
                database.userDao().upsert(user);
            } else if (existing.role != userRole) {
                existing.role = userRole;
                database.userDao().upsert(existing);
            }
        });
    }

    private static String displayNameFor(String email) {
        int at = email.indexOf('@');
        String local = at > 0 ? email.substring(0, at) : email;
        if (local.isEmpty()) return "Campus user";
        return Character.toUpperCase(local.charAt(0)) + local.substring(1);
    }

    // --- reading -----------------------------------------------------------

    public LiveData<List<Report>> observeReports() {
        return reportsLive;
    }

    public LiveData<Report> observeReport(String reportId) {
        return database.reportDao().observeById(reportId);
    }

    public LiveData<List<VerificationLog>> observeTimeline(String reportId) {
        return database.verificationLogDao().observeForReport(reportId);
    }

    public LiveData<List<VerificationLog>> observeActivity(int limit) {
        return database.verificationLogDao().observeRecent(limit);
    }

    public LiveData<AiTriage> observeTriage(String reportId) {
        return database.aiTriageDao().observeByReportId(reportId);
    }

    public LiveData<User> observeUser(String userId) {
        return database.userDao().observeById(userId);
    }

    /** Newest first, as the DAO returns it. Never null. */
    public List<Report> getReports() {
        return cachedReports;
    }

    public Report findReport(String reportId) {
        if (reportId == null) return null;
        for (Report report : cachedReports) {
            if (reportId.equals(report.reportId)) return report;
        }
        return null;
    }

    /** Same place and category, still open — the "confirm the existing report" path. */
    public Report findSimilar(String locationName, ReportCategory category) {
        for (Report report : cachedReports) {
            if (report.status == ReportStatus.RESOLVED || report.status == ReportStatus.REJECTED) continue;
            if (report.locationName.equalsIgnoreCase(locationName) && report.category == category) {
                return report;
            }
        }
        return null;
    }

    /** Reports at the same place and category that an admin may want to merge — never the report itself. */
    public List<Report> findDuplicates(Report report) {
        List<Report> duplicates = new ArrayList<>();
        if (report == null) return duplicates;
        for (Report other : cachedReports) {
            if (other.reportId.equals(report.reportId)) continue;
            if (other.status == ReportStatus.REJECTED) continue;
            if (other.locationName.equalsIgnoreCase(report.locationName) && other.category == report.category) {
                duplicates.add(other);
            }
        }
        return duplicates;
    }

    // --- filtering and counting -------------------------------------------

    public List<Report> visibleReports(boolean management, String userId, String query,
                                       String status, String category, String risk, String day) {
        List<Report> result = new ArrayList<>();
        String term = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();

        for (Report report : cachedReports) {
            if (!management && (userId == null || !userId.equals(report.reporterId))) continue;
            if (status != null && !"All".equals(status) && !UIUtils.statusLabel(report.status).equals(status)) continue;
            if (category != null && !"All".equals(category) && !UIUtils.categoryLabel(report.category).equals(category)) continue;
            if (risk != null && !"All".equals(risk) && !UIUtils.riskLabel(report.severity).equals(risk)) continue;
            if (day != null && !day.isEmpty() && !UIUtils.formatDay(report.createdAt).startsWith(day)) continue;

            String content = (UIUtils.reportCode(report.reportId) + " " + report.locationName + " "
                    + report.description).toLowerCase(Locale.ROOT);
            if (content.contains(term)) result.add(report);
        }
        return result;
    }

    public int countAll() {
        return cachedReports.size();
    }

    public int openCount() {
        int total = 0;
        for (Report report : cachedReports) {
            if (report.status == ReportStatus.PENDING_AI
                    || report.status == ReportStatus.PENDING_MODERATOR
                    || report.status == ReportStatus.VERIFIED) {
                total++;
            }
        }
        return total;
    }

    public int countStatus(String statusLabel) {
        int total = 0;
        for (Report report : cachedReports) {
            if (UIUtils.statusLabel(report.status).equals(statusLabel)) total++;
        }
        return total;
    }

    public int countHighRisk() {
        int total = 0;
        for (Report report : cachedReports) {
            if (report.severity == Severity.HIGH
                    && report.status != ReportStatus.RESOLVED
                    && report.status != ReportStatus.REJECTED) {
                total++;
            }
        }
        return total;
    }

    public int countTasksCompleted() {
        int total = 0;
        for (Report report : cachedReports) {
            if (report.taskStatus == TaskStatus.COMPLETED) total++;
        }
        return total;
    }

    public int countTasksAssigned() {
        int total = 0;
        for (Report report : cachedReports) {
            if (report.taskStatus != null) total++;
        }
        return total;
    }

    // --- writes ------------------------------------------------------------

    public void addStillThere(String reportId) {
        long now = System.currentTimeMillis();
        write(() -> {
            database.reportDao().addStillThere(reportId);
            database.reportDao().touch(reportId, now);
        });
    }

    public void addCleared(String reportId) {
        long now = System.currentTimeMillis();
        write(() -> {
            database.reportDao().addCleared(reportId);
            database.reportDao().touch(reportId, now);
        });
    }

    /**
     * Module 1 + Module 3 in one transaction: the report is stored as PENDING_AI, the demo triage
     * row is written, and the report moves to PENDING_MODERATOR — so it never sits waiting for an
     * AI that is not there.
     */
    public void createReport(String reporterId, String locationName, String zone,
                             ReportCategory category, Severity severity,
                             String description, String photoUri) {
        long now = System.currentTimeMillis();

        Report report = new Report();
        report.reporterId = reporterId;
        report.locationName = locationName;
        report.zone = zone;
        report.category = category;
        report.severity = severity;
        report.description = description;
        report.photoUri = photoUri;
        report.status = ReportStatus.PENDING_AI;
        report.createdAt = now;
        report.updatedAt = now;

        write(() -> {
            database.reportDao().upsert(report);
            database.aiTriageDao().upsert(buildTriage(report, now));
            database.reportDao().updateStatus(report.reportId, ReportStatus.PENDING_MODERATOR, now);
        });
    }

    public void verify(String reportId, String actorId) {
        long now = System.currentTimeMillis();
        write(() -> {
            database.reportDao().updateStatus(reportId, ReportStatus.VERIFIED, now);
            database.verificationLogDao().insert(log(reportId, actorId, ReviewAction.VERIFY,
                    ReportStatus.PENDING_MODERATOR, ReportStatus.VERIFIED,
                    "Report verified by maintenance administrator.", null, now));
        });
    }

    public void reject(String reportId, String actorId, String remarks) {
        long now = System.currentTimeMillis();
        write(() -> {
            database.reportDao().updateStatus(reportId, ReportStatus.REJECTED, now);
            database.verificationLogDao().insert(log(reportId, actorId, ReviewAction.REJECT,
                    ReportStatus.PENDING_MODERATOR, ReportStatus.REJECTED, remarks, null, now));
        });
    }

    public void assign(String reportId, String actorId, String team, String instructions) {
        long now = System.currentTimeMillis();
        write(() -> {
            Report report = database.reportDao().getById(reportId);
            if (report == null) return;
            report.assignedTeam = team;
            report.instructions = instructions;
            report.taskStatus = TaskStatus.ASSIGNED;
            report.status = ReportStatus.IN_PROGRESS;
            report.updatedAt = now;
            database.reportDao().update(report);
            database.verificationLogDao().insert(log(reportId, actorId, ReviewAction.ASSIGN,
                    ReportStatus.VERIFIED, ReportStatus.IN_PROGRESS,
                    "Maintenance assigned to " + team + ". Instructions: " + instructions, team, now));
        });
    }

    public void updateProgress(String reportId, String actorId, TaskStatus taskStatus, String notes) {
        long now = System.currentTimeMillis();
        write(() -> {
            Report report = database.reportDao().getById(reportId);
            if (report == null) return;
            report.taskStatus = taskStatus;
            report.updatedAt = now;
            database.reportDao().update(report);
            database.verificationLogDao().insert(log(reportId, actorId, ReviewAction.UPDATE_PROGRESS,
                    report.status, report.status,
                    UIUtils.taskStatusLabel(taskStatus) + ": " + notes, report.assignedTeam, now));
        });
    }

    public void resolve(String reportId, String actorId, String notes) {
        long now = System.currentTimeMillis();
        write(() -> {
            Report report = database.reportDao().getById(reportId);
            if (report == null) return;
            report.status = ReportStatus.RESOLVED;
            report.taskStatus = TaskStatus.COMPLETED;
            report.resolutionNotes = notes;
            report.resolvedAt = now;
            report.updatedAt = now;
            database.reportDao().update(report);
            database.verificationLogDao().insert(log(reportId, actorId, ReviewAction.RESOLVE,
                    ReportStatus.IN_PROGRESS, ReportStatus.RESOLVED,
                    "Task completed; report resolved. " + notes, report.assignedTeam, now));
        });
    }

    public void attachCompletionPhoto(String reportId, String actorId, String photoUri) {
        long now = System.currentTimeMillis();
        write(() -> {
            Report report = database.reportDao().getById(reportId);
            if (report == null) return;
            report.completionPhotoUri = photoUri;
            report.updatedAt = now;
            database.reportDao().update(report);
            database.verificationLogDao().insert(log(reportId, actorId, ReviewAction.UPDATE_PROGRESS,
                    report.status, report.status,
                    "Inspection / completion photo attached.", report.assignedTeam, now));
        });
    }

    /** Rebuild the demo AI row for one report (the "Generate Summary" button). */
    public void generateSummary(String reportId) {
        long now = System.currentTimeMillis();
        write(() -> {
            Report report = database.reportDao().getById(reportId);
            if (report == null) return;
            database.aiTriageDao().upsert(buildTriage(report, now));
        });
    }

    public void savePreferences(String userId, boolean notifyHeavyRain, boolean notifyFlashFlood,
                                boolean weeklySummary, List<String> zones) {
        write(() -> {
            User user = database.userDao().getById(userId);
            if (user == null) return;
            user.notifyHeavyRain = notifyHeavyRain;
            user.notifyFlashFlood = notifyFlashFlood;
            user.weeklySummary = weeklySummary;
            user.subscribedZones = String.join(",", zones);
            database.userDao().upsert(user);
        });
    }

    // --- plumbing ----------------------------------------------------------

    /** Every write runs off the UI thread, and every multi-table write runs in one transaction. */
    private void write(Runnable body) {
        RainGuardDatabase.databaseExecutor.execute(() -> database.runInTransaction(body));
    }

    private VerificationLog log(String reportId, String actorId, ReviewAction action,
                                ReportStatus fromStatus, ReportStatus toStatus,
                                String remarks, String team, long now) {
        VerificationLog entry = new VerificationLog();
        entry.reportId = reportId;
        entry.actorId = actorId == null ? "" : actorId;
        entry.action = action;
        entry.fromStatus = fromStatus;
        entry.toStatus = toStatus;
        entry.remarks = remarks == null ? "" : remarks;
        entry.team = team;
        entry.createdAt = now;
        return entry;
    }

    private AiTriage buildTriage(Report report, long now) {
        AiTriage triage = new AiTriage();
        triage.reportId = report.reportId;
        triage.status = AiStatus.SUCCESS;
        triage.modelName = "demo-rule-based";
        triage.latencyMs = 12L;
        triage.confidence = 0.72f;
        triage.suggestedCategory = report.category;
        triage.suggestedSeverity = report.severity;
        triage.summary = summaryFor(report);
        triage.suggestedAction = "Inspect the location, confirm the hazard, then assign a maintenance team.";
        triage.safetyAdviceGenerated = "Avoid walking or driving through standing water, and report any change in the hazard.";
        triage.possibleDuplicateIds = joinDuplicates(report);
        triage.createdAt = now;
        return triage;
    }

    private String summaryFor(Report report) {
        return UIUtils.categoryLabel(report.category) + " reported at " + report.locationName + ". "
                + report.description + " The affected area may create a slipping hazard and disrupt "
                + "pedestrian movement. An on-site inspection is recommended. This is a demo "
                + "rule-based summary; administrator review is required.";
    }

    private String joinDuplicates(Report report) {
        List<String> ids = new ArrayList<>();
        for (Report other : findDuplicates(report)) {
            ids.add(other.reportId);
        }
        return String.join(",", ids);
    }
}
