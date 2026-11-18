package com.um.rainguard.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;

import com.um.rainguard.data.model.ReportCategory;
import com.um.rainguard.data.model.ReportStatus;
import com.um.rainguard.data.model.ReviewAction;
import com.um.rainguard.data.model.Severity;
import com.um.rainguard.data.model.TaskStatus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class UIUtils {

    private static final TimeZone CAMPUS_ZONE = TimeZone.getTimeZone("Asia/Kuala_Lumpur");

    public static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    // --- colours -----------------------------------------------------------
    // The renderers read colours from the labels above, so both the typed enum
    // and the display string keep working.

    public static int riskColor(String risk) {
        if ("High".equals(risk)) return Constants.DANGER;
        if ("Medium".equals(risk)) return Constants.WARNING;
        return Constants.SUCCESS;
    }

    public static int riskColor(Severity severity) {
        return riskColor(riskLabel(severity));
    }

    public static int statusColor(String status) {
        if ("Resolved".equals(status) || "Completed".equals(status)) return Constants.SUCCESS;
        if ("Rejected".equals(status)) return Constants.DANGER;
        if ("Submitted".equals(status)) return Constants.MUTED;
        return Constants.NAVY;
    }

    public static int statusBgColor(String status) {
        if ("In progress".equals(status)) return Color.rgb(254, 243, 199);
        if ("Resolved".equals(status) || "Completed".equals(status)) return Color.rgb(240, 253, 244);
        if ("Rejected".equals(status)) return Color.rgb(254, 226, 226);
        return Color.rgb(239, 246, 255);
    }

    // --- enum -> display label --------------------------------------------
    // These are the "UI wording" strings of the specification: they are the only
    // place a status, severity, category or task state becomes a String.

    public static String statusLabel(ReportStatus status) {
        if (status == null) return "Submitted";
        switch (status) {
            case PENDING_AI:
                return "Submitted";
            case PENDING_MODERATOR:
                return "Under Review";
            case VERIFIED:
                return "Verified";
            case IN_PROGRESS:
                return "In progress";
            case RESOLVED:
                return "Resolved";
            case REJECTED:
                return "Rejected";
            default:
                return "Submitted";
        }
    }

    public static String riskLabel(Severity severity) {
        if (severity == null) return "Medium";
        switch (severity) {
            case HIGH:
                return "High";
            case LOW:
                return "Low";
            default:
                return "Medium";
        }
    }

    public static String categoryLabel(ReportCategory category) {
        if (category == null) return "Other";
        switch (category) {
            case CLOGGED_DRAIN:
                return "Clogged";
            case STAGNANT_WATER:
                return "Stagnant water";
            case OVERFLOW:
                return "Overflow";
            case SEDIMENT:
                return "Sediment";
            case FLOODED_ROAD:
                return "Flooded road";
            case FALLEN_TREE:
                return "Fallen tree";
            default:
                return "Other";
        }
    }

    public static String taskStatusLabel(TaskStatus status) {
        if (status == null) return "Awaiting Assignment";
        switch (status) {
            case ASSIGNED:
                return "Assigned";
            case INSPECTION_IN_PROGRESS:
                return "Inspection in Progress";
            case REPAIR_IN_PROGRESS:
                return "Repair in Progress";
            case COMPLETED:
                return "Completed";
            default:
                return "Awaiting Assignment";
        }
    }

    /** Spinner label back to the enum — the form shows words, the table stores enums. */
    public static ReportCategory categoryFromLabel(String label) {
        if (label == null) return ReportCategory.OTHER;
        switch (label) {
            case "Clogged":
                return ReportCategory.CLOGGED_DRAIN;
            case "Stagnant water":
                return ReportCategory.STAGNANT_WATER;
            case "Overflow":
                return ReportCategory.OVERFLOW;
            case "Sediment":
                return ReportCategory.SEDIMENT;
            case "Flooded road":
                return ReportCategory.FLOODED_ROAD;
            case "Fallen tree":
                return ReportCategory.FALLEN_TREE;
            default:
                return ReportCategory.OTHER;
        }
    }

    public static Severity severityFromLabel(String label) {
        if ("High".equals(label)) return Severity.HIGH;
        if ("Low".equals(label)) return Severity.LOW;
        return Severity.MEDIUM;
    }

    public static TaskStatus taskStatusFromLabel(String label) {
        if ("Assigned".equals(label)) return TaskStatus.ASSIGNED;
        if ("Inspection in Progress".equals(label)) return TaskStatus.INSPECTION_IN_PROGRESS;
        if ("Repair in Progress".equals(label)) return TaskStatus.REPAIR_IN_PROGRESS;
        if ("Completed".equals(label)) return TaskStatus.COMPLETED;
        return TaskStatus.AWAITING_ASSIGNMENT;
    }

    public static String actionLabel(ReviewAction action) {
        if (action == null) return "Update";
        switch (action) {
            case VERIFY:
                return "Verified";
            case REJECT:
                return "Rejected";
            case ASSIGN:
                return "Assigned";
            case UPDATE_PROGRESS:
                return "Progress update";
            case RESOLVE:
                return "Resolved";
            case OVERRIDE_AI:
                return "AI override";
            default:
                return "Update";
        }
    }

    // --- display strings for typed columns ---------------------------------

    /** Short human-readable id derived from the UUID, e.g. "RG-3F2A". */
    public static String reportCode(String reportId) {
        if (reportId == null || reportId.isEmpty()) return "RG-0000";
        String compact = reportId.replace("-", "");
        String tail = compact.length() >= 4 ? compact.substring(0, 4) : compact;
        return "RG-" + tail.toUpperCase(Locale.ROOT);
    }

    /** "2026-11-18 23:20" in campus time. */
    public static String formatStamp(long epochMillis) {
        return formatter("yyyy-MM-dd HH:mm").format(new Date(epochMillis));
    }

    /** "2026-11-18" in campus time. */
    public static String formatDay(long epochMillis) {
        return formatter("yyyy-MM-dd").format(new Date(epochMillis));
    }

    /** "Just now", "12m ago", "3h ago", "2d ago". */
    public static String relativeTime(long epochMillis) {
        long minutes = Math.max(0L, (System.currentTimeMillis() - epochMillis) / 60_000L);
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        return (hours / 24) + "d ago";
    }

    /** Nearest known campus zone for a free-text place. */
    public static String zoneForPlace(String place) {
        String value = place == null ? "" : place;
        if (value.contains("KK8")) return "KK8";
        if (value.contains("Engineering") || value.contains("Faculty")) return "Faculty buildings";
        return "Central campus";
    }

    private static SimpleDateFormat formatter(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ROOT);
        format.setTimeZone(CAMPUS_ZONE);
        return format;
    }

    // --- drawables ---------------------------------------------------------

    public static StateListDrawable createButtonDrawable(Context context, int normalColor, int hoverColor, int cornerRadiusDp) {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(normalColor);
        normal.setCornerRadius(dp(context, cornerRadiusDp));

        GradientDrawable hover = new GradientDrawable();
        hover.setColor(hoverColor);
        hover.setCornerRadius(dp(context, cornerRadiusDp));

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, hover);
        states.addState(new int[]{android.R.attr.state_hovered}, hover);
        states.addState(new int[]{android.R.attr.state_focused}, hover);
        states.addState(new int[]{}, normal);
        return states;
    }

    public static StateListDrawable createBorderButtonDrawable(Context context, int normalBg, int hoverBg, int strokeColor, int cornerRadiusDp) {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(normalBg);
        normal.setCornerRadius(dp(context, cornerRadiusDp));
        normal.setStroke(dp(context, 1), strokeColor);

        GradientDrawable hover = new GradientDrawable();
        hover.setColor(hoverBg);
        hover.setCornerRadius(dp(context, cornerRadiusDp));
        hover.setStroke(dp(context, 1), strokeColor);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, hover);
        states.addState(new int[]{android.R.attr.state_hovered}, hover);
        states.addState(new int[]{android.R.attr.state_focused}, hover);
        states.addState(new int[]{}, normal);
        return states;
    }

    public static String timestamp() {
        return formatter("yyyy-MM-dd HH:mm").format(new Date());
    }
}
