package com.um.rainguard.utils;

import android.content.Context;
import android.content.Intent;

import com.um.rainguard.ui.admin.AllReportsActivity;
import com.um.rainguard.ui.admin.AnalyticsActivity;
import com.um.rainguard.ui.admin.MaintenanceTasksActivity;
import com.um.rainguard.ui.dashboard.CampusHomeActivity;
import com.um.rainguard.ui.dashboard.DashboardActivity;
import com.um.rainguard.ui.hazardmap.HazardMapActivity;
import com.um.rainguard.ui.myreports.MyReportsActivity;
import com.um.rainguard.ui.notifications.NotificationsActivity;
import com.um.rainguard.ui.report.ReportSubmissionActivity;
import com.um.rainguard.ui.reportdetails.ReportDetailsActivity;
import com.um.rainguard.ui.riskoverview.RiskOverviewActivity;
import com.um.rainguard.ui.settings.SettingsActivity;

import java.util.Arrays;
import java.util.List;

public class NavigationHelper {

    public static Class<?> getScreenClass(String page) {
        if (page == null) return DashboardActivity.class;
        switch (page) {
            case "dashboard": return DashboardActivity.class;
            case "home": return CampusHomeActivity.class;
            case "map": return HazardMapActivity.class;
            case "report": return ReportSubmissionActivity.class;
            case "reports": return MyReportsActivity.class;
            case "all-reports": return AllReportsActivity.class;
            case "risk": return RiskOverviewActivity.class;
            case "tasks": return MaintenanceTasksActivity.class;
            case "analytics": return AnalyticsActivity.class;
            case "notifications": return NotificationsActivity.class;
            case "settings": return SettingsActivity.class;
            case "report-details": return ReportDetailsActivity.class;
            default: return DashboardActivity.class;
        }
    }

    public static List<String[]> getNavigationItems(boolean isAdmin) {
        if (isAdmin) {
            return Arrays.asList(
                    new String[]{"dashboard", "Admin Dashboard"},
                    new String[]{"all-reports", "All Reports"},
                    new String[]{"tasks", "Maintenance Tasks"},
                    new String[]{"home", "Campus Home"},
                    new String[]{"analytics", "Analytics & Activity"},
                    new String[]{"settings", "Profile & Settings"});
        }
        return Arrays.asList(
                new String[]{"dashboard", "Home"},
                new String[]{"map", "Hazard Map"},
                new String[]{"report", "Report"},
                new String[]{"reports", "My Reports"},
                new String[]{"risk", "Risk Overview"},
                new String[]{"notifications", "Notifications"},
                new String[]{"settings", "Profile & Settings"});
    }

    public static String getPageTitle(String page, boolean isAdmin) {
        if ("report-details".equals(page)) return "Report details";
        for (String[] item : getNavigationItems(isAdmin)) {
            if (item[0].equals(page)) return item[1];
        }
        return "Campus safety";
    }

    public static String getNavGlyph(String page) {
        if ("map".equals(page)) return "🗺";
        if ("report".equals(page)) return "＋";
        if ("reports".equals(page) || "all-reports".equals(page)) return "📋";
        if ("tasks".equals(page)) return "⚒";
        if ("risk".equals(page)) return "⚡";
        if ("notifications".equals(page)) return "🔔";
        if ("settings".equals(page)) return "⚙";
        if ("more".equals(page)) return "≡";
        return "🏠";
    }

    public static void navigateTo(Context context, String role, String email, String targetPage, String reportId, String originPage) {
        Class<?> targetClass = getScreenClass(targetPage);
        Intent intent = new Intent(context, targetClass);
        intent.putExtra(Constants.EXTRA_ROLE, role);
        intent.putExtra(Constants.EXTRA_EMAIL, email);
        intent.putExtra(Constants.EXTRA_PAGE, targetPage);
        intent.putExtra(Constants.EXTRA_RETURN_PAGE, originPage);
        if (reportId != null) {
            intent.putExtra(Constants.EXTRA_REPORT_ID, reportId);
        }
        context.startActivity(intent);
    }
}
