package com.um.rainguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.widget.PopupWindow;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int NAVY = Color.rgb(7, 89, 133);
    private static final int INK = Color.rgb(23, 43, 58);
    private static final int MUTED = Color.rgb(102, 121, 134);
    private static final int BG = Color.rgb(245, 248, 250);
    private static final int WHITE = Color.WHITE;
    private static final int BORDER = Color.rgb(217, 227, 233);
    private static final int WARNING = Color.rgb(182, 106, 8);
    private static final int DANGER = Color.rgb(180, 35, 24);
    private static final int SUCCESS = Color.rgb(21, 115, 71);
    private static final String PREFERENCES = "rainguard-demo-session";
    private static final String DATA_PREFERENCES = "rainguard-demo-data";
    private static final String EXTRA_ROLE = "screen_role";
    private static final String EXTRA_EMAIL = "screen_email";
    private static final String EXTRA_PAGE = "screen_page";
    private static final String EXTRA_REPORT_ID = "screen_report_id";
    private static final String EXTRA_RETURN_PAGE = "screen_return_page";
    private static final int REQUEST_REPORT_PHOTO = 1001;
    private static final int REQUEST_COMPLETION_PHOTO = 1002;

    private LinearLayout screenHost;
    private LinearLayout bottomNavigation;
    private TextView workspaceBreadcrumb;
    private TextView workspaceTitle;
    private ScrollView pageScroll;
    private SharedPreferences preferences;
    private SharedPreferences dataPreferences;
    private final List<ReportItem> reports = new ArrayList<>();
    private String role;
    private String userEmail;
    private String currentPage = "dashboard";
    private String returnPage = "dashboard";
    private String selectedReportId;
    private Uri selectedPhoto;
    private TextView photoLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        setContentView(screenLayout());

        View rootContent = findViewById(android.R.id.content);
        if (rootContent != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootContent, (v, insets) -> {
                Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }

        screenHost = findViewById(R.id.page_content);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        workspaceBreadcrumb = findViewById(R.id.workspace_breadcrumb);
        workspaceTitle = findViewById(R.id.workspace_title);
        pageScroll = findViewById(R.id.page_scroll);
        preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        dataPreferences = getSharedPreferences(DATA_PREFERENCES, MODE_PRIVATE);
        loadReports();
        role = getIntent().getStringExtra(EXTRA_ROLE);
        userEmail = getIntent().getStringExtra(EXTRA_EMAIL);
        if (role == null && preferences.getBoolean("remember", false)) {
            String savedRole = preferences.getString("role", null);
            String savedEmail = preferences.getString("email", null);
            if (("admin".equals(savedRole) || "student".equals(savedRole))
                    && savedEmail != null
                    && android.util.Patterns.EMAIL_ADDRESS.matcher(savedEmail).matches()) {
                role = savedRole;
                userEmail = savedEmail;
            }
        }
        if (getClass() == MainActivity.class && role != null && userEmail != null) {
            launchScreen("dashboard", null, "dashboard");
            finish();
            return;
        }
        if (role != null && userEmail != null) {
            currentPage = getIntent().getStringExtra(EXTRA_PAGE);
            if (currentPage == null) currentPage = initialPage();
            selectedReportId = getIntent().getStringExtra(EXTRA_REPORT_ID);
            returnPage = getIntent().getStringExtra(EXTRA_RETURN_PAGE);
            if (returnPage == null) returnPage = "dashboard";
            renderWorkspace();
        } else {
            renderSignIn();
        }
    }

    protected int screenLayout() {
        return R.layout.screen_sign_in;
    }

    protected String initialPage() {
        return "dashboard";
    }

    private void seedReports() {
        reports.clear();
        reports.add(new ReportItem("RG-1042", "Faculty of Engineering, Block A", "Clogged",
                "Clogged drain · leaves", "In progress", "High", "2h ago", 14, 0,
                "Clogged observed after rainfall. Please inspect the drainage channel and nearby walkway.",
                "student@um.edu.my", "2026-10-24", "JPPHB Drainage Team",
                "Inspect the box culvert and clear fallen leaves.", "Inspection in Progress"));
        reports.add(new ReportItem("RG-1039", "KK8 walkway to bus stop", "Stagnant water",
                "Stagnant water", "Verified", "High", "5h ago", 9, 1,
                "Stagnant water observed along the walkway after rainfall.", "community@um.edu.my",
                "2026-10-23", null, null, null));
        reports.add(new ReportItem("RG-1031", "Main Library back entrance", "Overflow",
                "Overflowing drain", "Submitted", "Medium", "1d ago", 4, 0,
                "Overflowing drain observed near the back entrance.", "student@um.edu.my",
                "2026-10-22", null, null, null));
        reports.add(new ReportItem("RG-1020", "Rimba Ilmu car park", "Sediment",
                "Sediment build-up", "Resolved", "Low", "3d ago", 6, 5,
                "Sediment was building up in the drainage channel.", "community@um.edu.my",
                "2026-10-21", "Campus Facilities Team",
                "Sediment removed and drainage flow restored.", "Completed"));
        reports.add(new ReportItem("RG-1015", "Dewan Tunku Canselor side drain", "Clogged",
                "Clogged drain · litter", "Submitted", "Medium", "3d ago", 3, 0,
                "Litter is restricting water flow in the side drain.", "community@um.edu.my",
                "2026-10-20", null, null, null));
        for (ReportItem item : reports) item.addHistory(item.createdAt, "Report " + item.id + " received.");
        reports.get(3).resolution = "Sediment removed and drainage flow restored.";
        reports.get(3).resolvedAt = "2026-10-23 11:30";
    }

    private void loadReports() {
        String saved = dataPreferences.getString("reports", null);
        if (saved == null) {
            seedReports();
            persistReports();
            return;
        }
        try {
            JSONArray data = new JSONArray(saved);
            List<ReportItem> loaded = new ArrayList<>();
            for (int i = 0; i < data.length(); i++) {
                JSONObject object = data.getJSONObject(i);
                ReportItem item = new ReportItem(
                        object.getString("id"),
                        object.getString("place"),
                        object.getString("category"),
                        object.getString("type"),
                        object.getString("status"),
                        object.getString("risk"),
                        object.optString("time", ""),
                        object.optInt("votes", 0),
                        object.optInt("cleared", 0),
                        object.optString("description", ""),
                        object.optString("owner", ""),
                        object.optString("createdAt", ""),
                        object.optString("team", null),
                        object.optString("instructions", null),
                        object.optString("taskStatus", null));
                item.summary = object.optString("summary", null);
                item.resolution = object.optString("resolution", null);
                item.resolvedAt = object.optString("resolvedAt", null);
                item.photoUri = object.optString("photoUri", null);
                item.completionPhotoUri = object.optString("completionPhotoUri", null);
                JSONArray history = object.optJSONArray("history");
                if (history != null) {
                    for (int j = 0; j < history.length(); j++) {
                        JSONObject entry = history.getJSONObject(j);
                        item.history.add(new HistoryEntry(
                                entry.optString("date", ""), entry.optString("text", "")));
                    }
                }
                loaded.add(item);
            }
            reports.clear();
            reports.addAll(loaded);
        } catch (JSONException exception) {
            android.util.Log.w("UMRainGuard", "Saved demo reports were invalid; restoring sample reports.",
                    exception);
            seedReports();
            persistReports();
        }
    }

    private void persistReports() {
        JSONArray data = new JSONArray();
        try {
            for (ReportItem item : reports) {
                JSONObject object = new JSONObject();
                object.put("id", item.id);
                object.put("place", item.place);
                object.put("category", item.category);
                object.put("type", item.type);
                object.put("status", item.status);
                object.put("risk", item.risk);
                object.put("time", item.time);
                object.put("votes", item.votes);
                object.put("cleared", item.cleared);
                object.put("description", item.description);
                object.put("owner", item.owner);
                object.put("createdAt", item.createdAt);
                object.put("team", item.team);
                object.put("instructions", item.instructions);
                object.put("taskStatus", item.taskStatus);
                object.put("summary", item.summary);
                object.put("resolution", item.resolution);
                object.put("resolvedAt", item.resolvedAt);
                object.put("photoUri", item.photoUri);
                object.put("completionPhotoUri", item.completionPhotoUri);
                JSONArray history = new JSONArray();
                for (HistoryEntry entry : item.history) {
                    JSONObject event = new JSONObject();
                    event.put("date", entry.date);
                    event.put("text", entry.text);
                    history.put(event);
                }
                object.put("history", history);
                data.put(object);
            }
            dataPreferences.edit().putString("reports", data.toString()).apply();
        } catch (JSONException exception) {
            android.util.Log.e("UMRainGuard", "Could not save demo reports.", exception);
            showToast("Unable to save demo report changes.");
        }
    }

    private void renderSignIn() {
        role = null;
        userEmail = null;
        View signIn = getWindow().getDecorView();

        LinearLayout roleSelection = signIn.findViewById(R.id.role_selection);
        LinearLayout loginForm = signIn.findViewById(R.id.login_form);
        EditText email = signIn.findViewById(R.id.email_input);
        EditText password = signIn.findViewById(R.id.password_input);
        TextView roleTitle = signIn.findViewById(R.id.login_role_title);
        TextView error = signIn.findViewById(R.id.login_error);

        View.OnClickListener chooseRole = view -> {
            role = view.getId() == R.id.choose_admin ? "admin" : "student";
            roleSelection.setVisibility(View.GONE);
            loginForm.setVisibility(View.VISIBLE);
            roleTitle.setText(role.equals("admin")
                    ? "Admin / Maintenance Team"
                    : "UM Campus User");
            email.setText(role.equals("admin") ? "admin@um.edu.my" : "student@um.edu.my");
            password.setText("");
            error.setVisibility(View.GONE);
        };
        signIn.findViewById(R.id.choose_student).setOnClickListener(chooseRole);
        signIn.findViewById(R.id.choose_admin).setOnClickListener(chooseRole);
        signIn.findViewById(R.id.back_to_roles).setOnClickListener(view -> {
            role = null;
            roleSelection.setVisibility(View.VISIBLE);
            loginForm.setVisibility(View.GONE);
            error.setVisibility(View.GONE);
            password.setText("");
        });
        signIn.findViewById(R.id.sign_in).setOnClickListener(view -> {
            String enteredEmail = email.getText().toString().trim().toLowerCase(Locale.ROOT);
            String enteredPassword = password.getText().toString();
            if (role == null) {
                showLoginError(error, "Choose a demo role first.");
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(enteredEmail).matches()) {
                showLoginError(error, "Enter a valid email address.");
            } else if (!enteredPassword.equals("RainGuard123!")) {
                showLoginError(error, "Incorrect demo password. Use RainGuard123!");
            } else {
                userEmail = enteredEmail;
                boolean remember = ((CheckBox) signIn.findViewById(R.id.remember_me)).isChecked();
                preferences.edit()
                        .putBoolean("remember", remember)
                        .putString("role", remember ? role : null)
                        .putString("email", remember ? userEmail : null)
                        .apply();
                navigate("dashboard");
            }
        });
    }

    private void showLoginError(TextView view, String message) {
        view.setText(message);
        view.setVisibility(View.VISIBLE);
    }

    private void renderWorkspace() {
        if (role == null || userEmail == null) {
            renderSignIn();
            return;
        }
        bottomNavigation.setVisibility(View.VISIBLE);
        screenHost.removeAllViews();
        workspaceBreadcrumb.setText("Universiti Malaya / "
                + (isAdmin() ? "Operations" : "Campus safety"));
        workspaceTitle.setText(pageTitle(currentPage));
        findViewById(R.id.workspace_menu).setOnClickListener(view -> showNavigationMenu());
        findViewById(R.id.workspace_notifications).setOnClickListener(
                view -> navigate("notifications"));
        findViewById(R.id.workspace_profile).setOnClickListener(view -> showProfileBubble(view));

        TextView roleBadge = findViewById(R.id.workspace_profile);
        if (roleBadge != null) {
            roleBadge.setText(isAdmin() ? "🛡 Admin" : "🛡 Campus User");
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setColor(isAdmin() ? Color.rgb(254, 243, 199) : Color.rgb(239, 246, 255));
            badgeBg.setCornerRadius(dp(12));
            badgeBg.setStroke(dp(1), isAdmin() ? Color.rgb(253, 230, 138) : Color.rgb(191, 219, 254));
            roleBadge.setBackground(badgeBg);
            roleBadge.setTextColor(isAdmin() ? Color.rgb(180, 83, 9) : Color.rgb(29, 78, 216));
        }

        TextView mockBadge = findViewById(R.id.mock_data_badge);
        if (mockBadge != null) {
            GradientDrawable mockBg = new GradientDrawable();
            mockBg.setColor(Color.rgb(255, 247, 237));
            mockBg.setCornerRadius(dp(12));
            mockBg.setStroke(dp(1), Color.rgb(254, 215, 170));
            mockBadge.setBackground(mockBg);
        }

        TextView notifIcon = findViewById(R.id.workspace_notifications);
        if (notifIcon != null) {
            GradientDrawable notifBg = new GradientDrawable();
            notifBg.setColor(WHITE);
            notifBg.setCornerRadius(dp(8));
            notifBg.setStroke(dp(1), Color.rgb(226, 232, 240));
            notifIcon.setBackground(notifBg);
        }

        screenHost.setOrientation(LinearLayout.VERTICAL);
        screenHost.setPadding(dp(16), dp(4), dp(16), dp(24));
        renderPage(screenHost);
        pageScroll.post(() -> pageScroll.scrollTo(0, 0));
        renderBottomNavigation();
    }

    private void renderPage(LinearLayout page) {
        if (currentPage.equals("report-details")) {
            ReportItem selected = findReport(selectedReportId);
            if (selected == null) {
                navigate(returnPage);
            } else {
                renderReportDetails(page, selected);
            }
        } else if (currentPage.equals("dashboard") || currentPage.equals("home")) {
            if (isAdmin() && currentPage.equals("dashboard")) {
                renderAdminDashboard(page);
            } else {
                renderDashboard(page);
            }
        } else if (currentPage.equals("map")) {
            renderMap(page);
        } else if (currentPage.equals("report")) {
            renderReportForm(page);
        } else if (currentPage.equals("reports") || currentPage.equals("all-reports")) {
            renderReportList(page, currentPage.equals("all-reports"));
        } else if (currentPage.equals("risk")) {
            renderRisk(page);
        } else if (currentPage.equals("tasks")) {
            renderTasks(page);
        } else if (currentPage.equals("analytics")) {
            renderAnalytics(page);
        } else if (currentPage.equals("notifications")) {
            renderNotifications(page);
        } else if (currentPage.equals("settings")) {
            renderSettings(page);
        } else {
            renderDashboard(page);
        }
    }

    private void renderDashboard(LinearLayout page) {
        // Hero Card
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(18), dp(20), dp(24));
        GradientDrawable heroBg = new GradientDrawable();
        heroBg.setColor(Color.rgb(27, 73, 101));
        heroBg.setCornerRadius(dp(16));
        hero.setBackground(heroBg);

        LinearLayout heroTop = new LinearLayout(this);
        heroTop.setOrientation(LinearLayout.HORIZONTAL);
        heroTop.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout heroTitleBox = new LinearLayout(this);
        heroTitleBox.setOrientation(LinearLayout.VERTICAL);
        addText(heroTitleBox, isAdmin() ? "Campus operations overview" : "Selamat pagi, UM community",
                13, Color.rgb(165, 243, 252), false);
        addText(heroTitleBox, "UM RainGuard", 24, WHITE, true);
        heroTop.addView(heroTitleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout heroIcons = new LinearLayout(this);
        heroIcons.setOrientation(LinearLayout.HORIZONTAL);

        TextView settingsBtn = text("⚙", 16, WHITE, false);
        settingsBtn.setGravity(Gravity.CENTER);
        GradientDrawable iconBg1 = new GradientDrawable();
        iconBg1.setColor(Color.parseColor("#33FFFFFF"));
        iconBg1.setCornerRadius(dp(18));
        settingsBtn.setBackground(iconBg1);
        settingsBtn.setOnClickListener(v -> navigate("settings"));

        TextView bellBtn = text("🔔", 16, WHITE, false);
        bellBtn.setGravity(Gravity.CENTER);
        GradientDrawable iconBg2 = new GradientDrawable();
        iconBg2.setColor(Color.parseColor("#33FFFFFF"));
        iconBg2.setCornerRadius(dp(18));
        bellBtn.setBackground(iconBg2);
        bellBtn.setOnClickListener(v -> navigate("notifications"));

        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        iconParams.leftMargin = dp(8);
        heroIcons.addView(settingsBtn, iconParams);
        heroIcons.addView(bellBtn, iconParams);

        heroTop.addView(heroIcons);
        hero.addView(heroTop, fullWidth());
        page.addView(hero, fullWidth());

        // Weather Card
        LinearLayout weatherCard = new LinearLayout(this);
        weatherCard.setOrientation(LinearLayout.VERTICAL);
        weatherCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable weatherBg = new GradientDrawable();
        weatherBg.setColor(WHITE);
        weatherBg.setCornerRadius(dp(16));
        weatherBg.setStroke(dp(1), Color.rgb(226, 232, 240));
        weatherCard.setBackground(weatherBg);
        weatherCard.setElevation(dp(4));

        LinearLayout.LayoutParams weatherParams = fullWidth();
        weatherParams.topMargin = dp(-14);
        weatherParams.bottomMargin = dp(14);
        weatherParams.leftMargin = dp(8);
        weatherParams.rightMargin = dp(8);

        LinearLayout weatherRow = new LinearLayout(this);
        weatherRow.setOrientation(LinearLayout.HORIZONTAL);
        weatherRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView weatherIcon = text("🌧", 26, Color.rgb(30, 58, 138), false);
        weatherIcon.setPadding(0, 0, dp(12), 0);
        weatherRow.addView(weatherIcon);

        LinearLayout weatherDetails = new LinearLayout(this);
        weatherDetails.setOrientation(LinearLayout.VERTICAL);
        addText(weatherDetails, "Kuala Lumpur · next 3 hours", 12, Color.rgb(100, 116, 139), false);
        addText(weatherDetails, "Heavy rain likely", 18, Color.rgb(15, 23, 42), true);
        addText(weatherDetails, "85% chance · 28 mm expected", 13, Color.rgb(100, 116, 139), false);
        weatherRow.addView(weatherDetails, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView chevron = text("›", 22, Color.rgb(148, 163, 184), false);
        weatherRow.addView(chevron);

        weatherCard.addView(weatherRow, fullWidth());

        TextView rainAlert = new TextView(this);
        rainAlert.setText("⚠ 3 high-risk drains near you need attention");
        rainAlert.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        rainAlert.setTextColor(Color.rgb(217, 119, 6));
        rainAlert.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable alertBg = new GradientDrawable();
        alertBg.setColor(Color.rgb(254, 243, 199));
        alertBg.setCornerRadius(dp(10));
        rainAlert.setBackground(alertBg);
        rainAlert.setPadding(dp(12), dp(10), dp(12), dp(10));

        LinearLayout.LayoutParams alertParams = fullWidth();
        alertParams.topMargin = dp(12);
        weatherCard.addView(rainAlert, alertParams);

        page.addView(weatherCard, weatherParams);

        // Stats Row
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams statsParams = fullWidth();
        statsParams.bottomMargin = dp(14);

        addStatCard(stats, openCount(), "Open");
        addStatCard(stats, countStatus("In progress"), "In progress");
        addStatCard(stats, countStatus("Resolved"), "Resolved");

        page.addView(stats, statsParams);

        // AI Brief Card
        LinearLayout brief = new LinearLayout(this);
        brief.setOrientation(LinearLayout.VERTICAL);
        brief.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable briefBg = new GradientDrawable();
        briefBg.setColor(Color.rgb(27, 73, 101));
        briefBg.setCornerRadius(dp(16));
        brief.setBackground(briefBg);

        LinearLayout.LayoutParams briefParams = fullWidth();
        briefParams.bottomMargin = dp(14);

        addText(brief, "✨  AI INCIDENT BRIEF", 12, Color.rgb(147, 197, 253), true);
        addSpace(brief, 4);
        addText(brief, "4 reports in the past 24h cite blocked box culverts at Faculty of Engineering Block A, caused by fallen acacia leaves after last night's storm. Recurrence pattern matches the October 2025 cluster.",
                14, WHITE, false);
        addSpace(brief, 8);
        addText(brief, "Demo summary · sample community reports", 11, Color.rgb(147, 197, 253), false);

        page.addView(brief, briefParams);

        // Action Card
        LinearLayout actionCard = new LinearLayout(this);
        actionCard.setOrientation(LinearLayout.HORIZONTAL);
        actionCard.setGravity(Gravity.CENTER_VERTICAL);
        actionCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable actionBg = new GradientDrawable();
        actionBg.setColor(Color.rgb(234, 179, 8));
        actionBg.setCornerRadius(dp(14));
        actionCard.setBackground(actionBg);

        TextView cameraIcon = text("📷", 22, Color.rgb(69, 26, 3), true);
        cameraIcon.setPadding(0, 0, dp(12), 0);
        actionCard.addView(cameraIcon);

        LinearLayout actionText = new LinearLayout(this);
        actionText.setOrientation(LinearLayout.VERTICAL);
        addText(actionText, isAdmin() ? "Review campus reports" : "Spotted a clogged drain?", 15, Color.rgb(69, 26, 3), true);
        addText(actionText, isAdmin() ? "Verify and coordinate response" : "Report it in under 30 seconds", 12, Color.rgb(120, 53, 15), false);
        actionCard.addView(actionText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView actionArrow = text("›", 22, Color.rgb(120, 53, 15), true);
        actionCard.addView(actionArrow);

        LinearLayout.LayoutParams actionParams = fullWidth();
        actionParams.bottomMargin = dp(18);
        page.addView(actionCard, actionParams);
        actionCard.setOnClickListener(view -> navigate(isAdmin() ? "all-reports" : "report"));

        // Nearby Reports Header & List
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = fullWidth();
        headerParams.bottomMargin = dp(10);

        TextView sectionTitle = text("Nearby reports", 18, Color.rgb(15, 23, 42), true);
        headerRow.addView(sectionTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView viewMapLink = text("View map", 14, Color.rgb(37, 99, 235), true);
        viewMapLink.setOnClickListener(v -> navigate("map"));
        headerRow.addView(viewMapLink);

        page.addView(headerRow, headerParams);

        int shown = 0;
        for (ReportItem item : reports) {
            if (!item.status.equals("Rejected")) {
                addReportCard(page, item, !isAdmin());
                if (++shown == 3) break;
            }
        }
    }

    private void renderAdminDashboard(LinearLayout page) {
        addText(page, "Campus drainage operations", 23, INK, true);
        addText(page, "Review hazards. Coordinate maintenance. Keep campus moving.", 14, MUTED, false);
        addSpace(page, 8);
        String[][] stats = {
                {"All submitted", String.valueOf(reports.size())},
                {"Awaiting review", String.valueOf(countStatus("Submitted") + countStatus("Under Review"))},
                {"Verified active", String.valueOf(countStatus("Verified"))},
                {"High-risk incidents", String.valueOf(countHighRisk())},
                {"In progress", String.valueOf(countStatus("In progress"))},
                {"Resolved", String.valueOf(countStatus("Resolved"))}
        };
        for (String[] stat : stats) {
            LinearLayout box = card(page, WHITE);
            addText(box, stat[0], 13, MUTED, false);
            addText(box, stat[1], 28, NAVY, true);
        }
        LinearLayout warning = card(page, Color.rgb(255, 249, 235));
        addText(warning, "Pre-rain inspection priority", 15, INK, true);
        addText(warning, "Sample forecast peaks at 2 PM. Prioritise blocked culverts near Engineering and the KK8 walkway.",
                13, MUTED, false);
        addSectionTitle(page, "Reports awaiting review", "All reports", () -> navigate("all-reports"));
        for (ReportItem item : reports) {
            if (item.status.equals("Submitted") || item.status.equals("Under Review")
                    || item.status.equals("Verified")) {
                addReportCard(page, item, false);
            }
        }
    }

    private void renderReportList(LinearLayout page, boolean management) {
        List<ReportItem> initial = visibleReports(management, "");
        addText(page, management ? "All drainage reports" : "Your drainage reports",
                23, Color.rgb(15, 23, 42), true);
        addText(page, initial.size() + " reports · "
                + (management ? "Review, verify and coordinate a response"
                : "Updates from the maintenance team"), 13, MUTED, false);
        addSpace(page, 10);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Search ID or location...");
        search.setPadding(dp(12), dp(10), dp(12), dp(10));
        search.setBackground(createBorderButtonDrawable(WHITE, WHITE, Color.rgb(226, 232, 240), 8));
        LinearLayout.LayoutParams searchParams = fullWidth();
        searchParams.bottomMargin = dp(8);
        page.addView(search, searchParams);

        Spinner status = styledSpinner(new String[] {
                "All", "Submitted", "Under Review", "Verified", "Rejected", "In progress", "Resolved"
        });
        page.addView(status, fullWidth());
        addSpace(page, 6);

        Spinner category = styledSpinner(new String[] {"All", "Clogged", "Stagnant water", "Overflow", "Sediment"});
        page.addView(category, fullWidth());
        addSpace(page, 6);

        Spinner risk = styledSpinner(new String[] {"All", "High", "Medium", "Low"});
        page.addView(risk, fullWidth());
        addSpace(page, 6);

        EditText day = new EditText(this);
        day.setSingleLine(true);
        day.setHint("dd/mm/yyyy 📅");
        day.setPadding(dp(12), dp(10), dp(12), dp(10));
        day.setBackground(createBorderButtonDrawable(WHITE, WHITE, Color.rgb(226, 232, 240), 8));
        LinearLayout.LayoutParams dayParams = fullWidth();
        dayParams.bottomMargin = dp(12);
        page.addView(day, dayParams);

        LinearLayout resultList = new LinearLayout(this);
        resultList.setOrientation(LinearLayout.VERTICAL);
        page.addView(resultList);

        Runnable update = () -> fillReportResults(resultList, management,
                search.getText().toString(), status.getSelectedItem().toString(),
                category.getSelectedItem().toString(), risk.getSelectedItem().toString(),
                day.getText().toString().trim());
        update.run();

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { update.run(); }
            @Override public void afterTextChanged(Editable s) { }
        });

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { update.run(); }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };

        status.setOnItemSelectedListener(filterListener);
        category.setOnItemSelectedListener(filterListener);
        risk.setOnItemSelectedListener(filterListener);

        day.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { update.run(); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void fillReportResults(LinearLayout container, boolean management, String query,
                                  String status, String category, String risk, String day) {
        container.removeAllViews();
        List<ReportItem> matches = visibleReports(management, query, status, category, risk, day);
        if (matches.isEmpty()) {
            addText(container, "No reports match your search.", 14, MUTED, false);
            return;
        }

        android.widget.HorizontalScrollView scrollView = new android.widget.HorizontalScrollView(this);
        scrollView.setHorizontalScrollBarEnabled(false);

        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setBackground(createBorderButtonDrawable(WHITE, WHITE, Color.rgb(226, 232, 240), 8));

        // Table Header
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setBackgroundColor(Color.rgb(235, 243, 245));
        headerRow.setPadding(dp(12), dp(10), dp(12), dp(10));

        TextView th1 = text("Report / Location", 12, Color.rgb(71, 85, 105), true);
        TextView th2 = text("Category", 12, Color.rgb(71, 85, 105), true);
        TextView th3 = text("Risk", 12, Color.rgb(71, 85, 105), true);
        TextView th4 = text("Status", 12, Color.rgb(71, 85, 105), true);
        TextView th5 = text("Action", 12, Color.rgb(71, 85, 105), true);

        headerRow.addView(th1, new LinearLayout.LayoutParams(dp(180), ViewGroup.LayoutParams.WRAP_CONTENT));
        headerRow.addView(th2, new LinearLayout.LayoutParams(dp(100), ViewGroup.LayoutParams.WRAP_CONTENT));
        headerRow.addView(th3, new LinearLayout.LayoutParams(dp(90), ViewGroup.LayoutParams.WRAP_CONTENT));
        headerRow.addView(th4, new LinearLayout.LayoutParams(dp(100), ViewGroup.LayoutParams.WRAP_CONTENT));
        headerRow.addView(th5, new LinearLayout.LayoutParams(dp(100), ViewGroup.LayoutParams.WRAP_CONTENT));

        table.addView(headerRow);

        // Table Rows
        for (ReportItem item : matches) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(12), dp(12), dp(12));
            row.setBackground(createButtonDrawable(WHITE, Color.rgb(248, 250, 252), 0));

            // Col 1: ID & Location
            LinearLayout col1 = new LinearLayout(this);
            col1.setOrientation(LinearLayout.VERTICAL);
            addText(col1, item.id, 14, Color.rgb(15, 23, 42), true);
            addText(col1, item.place, 12, Color.rgb(71, 85, 105), false);
            addText(col1, formatDate(item.createdAt) + ", 16:12", 11, MUTED, false);
            row.addView(col1, new LinearLayout.LayoutParams(dp(180), ViewGroup.LayoutParams.WRAP_CONTENT));

            // Col 2: Category
            TextView col2 = text(item.category, 13, Color.rgb(15, 23, 42), false);
            row.addView(col2, new LinearLayout.LayoutParams(dp(100), ViewGroup.LayoutParams.WRAP_CONTENT));

            // Col 3: Risk
            TextView col3 = text(item.risk + " risk", 13, riskColor(item.risk), true);
            row.addView(col3, new LinearLayout.LayoutParams(dp(90), ViewGroup.LayoutParams.WRAP_CONTENT));

            // Col 4: Status
            TextView col4 = text(item.status, 11, statusColor(item.status), true);
            col4.setPadding(dp(8), dp(4), dp(8), dp(4));
            GradientDrawable statusBg = new GradientDrawable();
            statusBg.setColor(statusBgColor(item.status));
            statusBg.setCornerRadius(dp(6));
            col4.setBackground(statusBg);
            row.addView(col4, new LinearLayout.LayoutParams(dp(100), ViewGroup.LayoutParams.WRAP_CONTENT));

            // Col 5: Action Button
            Button actionBtn = new Button(this);
            actionBtn.setText("View Details");
            actionBtn.setAllCaps(false);
            actionBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            actionBtn.setTextColor(Color.rgb(15, 23, 42));
            actionBtn.setBackground(createBorderButtonDrawable(
                    Color.rgb(241, 245, 249), Color.rgb(226, 232, 240), Color.rgb(203, 213, 225), 6));
            actionBtn.setMinHeight(dp(36));
            actionBtn.setOnClickListener(v -> showReportDetails(item));
            row.addView(actionBtn, new LinearLayout.LayoutParams(dp(100), dp(36)));

            table.addView(row);

            View divider = new View(this);
            divider.setBackgroundColor(Color.rgb(226, 232, 240));
            table.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        }

        scrollView.addView(table);
        container.addView(scrollView, fullWidth());

        // Scroll track bar at bottom
        LinearLayout scrollTrack = new LinearLayout(this);
        scrollTrack.setOrientation(LinearLayout.HORIZONTAL);
        scrollTrack.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams trackParams = fullWidth();
        trackParams.topMargin = dp(6);

        TextView leftArrow = text("◄", 10, Color.rgb(148, 163, 184), false);
        scrollTrack.addView(leftArrow);

        View barLine = new View(this);
        GradientDrawable lineBg = new GradientDrawable();
        lineBg.setColor(Color.rgb(148, 163, 184));
        lineBg.setCornerRadius(dp(3));
        barLine.setBackground(lineBg);
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(0, dp(6), 1);
        lineParams.leftMargin = dp(6);
        lineParams.rightMargin = dp(6);
        scrollTrack.addView(barLine, lineParams);

        TextView rightArrow = text("►", 10, Color.rgb(148, 163, 184), false);
        scrollTrack.addView(rightArrow);

        container.addView(scrollTrack, trackParams);
    }

    private List<ReportItem> visibleReports(boolean management, String query) {
        return visibleReports(management, query, "All", "All", "All", "");
    }

    private List<ReportItem> visibleReports(boolean management, String query, String status,
                                            String category, String risk, String day) {
        List<ReportItem> result = new ArrayList<>();
        String term = query.toLowerCase(Locale.ROOT).trim();
        for (ReportItem item : reports) {
            if (!management && !item.owner.equalsIgnoreCase(userEmail)) continue;
            if (!status.equals("All") && !item.status.equals(status)) continue;
            if (!category.equals("All") && !item.category.equals(category)) continue;
            if (!risk.equals("All") && !item.risk.equals(risk)) continue;
            if (!day.isEmpty() && !item.createdAt.startsWith(day)) continue;
            String content = (item.id + " " + item.place + " " + item.description)
                    .toLowerCase(Locale.ROOT);
            if (content.contains(term)) result.add(item);
        }
        return result;
    }

    private void renderReportForm(LinearLayout page) {
        selectedPhoto = null;
        photoLabel = null;
        addText(page, "Report a drainage issue", 22, Color.rgb(15, 23, 42), true);
        addSpace(page, 10);

        addText(page, "Campus location", 13, Color.rgb(15, 23, 42), true);
        Spinner place = styledSpinner(new String[] {
                "Faculty of Engineering, Block A", "KK8 walkway to bus stop",
                "Main Library back entrance", "Rimba Ilmu car park",
                "Dewan Tunku Canselor side drain"
        });
        page.addView(place, fullWidth());
        addSpace(page, 10);

        addText(page, "Hazard category", 13, Color.rgb(15, 23, 42), true);
        Spinner category = styledSpinner(new String[] {"Clogged", "Stagnant water", "Overflow", "Sediment"});
        page.addView(category, fullWidth());
        addSpace(page, 10);

        addText(page, "Observed severity", 13, Color.rgb(15, 23, 42), true);
        Spinner risk = styledSpinner(new String[] {"Medium", "Low", "High"});
        page.addView(risk, fullWidth());
        addSpace(page, 12);

        // Similar active report box (matching Image 3)
        LinearLayout activeBox = new LinearLayout(this);
        activeBox.setOrientation(LinearLayout.VERTICAL);
        activeBox.setPadding(dp(16), dp(14), dp(16), dp(14));
        activeBox.setBackground(createBorderButtonDrawable(
                Color.rgb(254, 243, 199), Color.rgb(253, 230, 138), Color.rgb(252, 211, 77), 8));

        addText(activeBox, "Similar active report: RG-1042", 15, Color.rgb(15, 23, 42), true);
        addText(activeBox, "Faculty of Engineering, Block A · Clogged · In progress", 13, Color.rgb(69, 26, 3), false);
        addSpace(activeBox, 10);

        Button confirmBtn = new Button(this);
        confirmBtn.setText("Confirm existing report");
        confirmBtn.setAllCaps(false);
        confirmBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        confirmBtn.setTextColor(WHITE);
        confirmBtn.setBackground(createButtonDrawable(
                Color.rgb(27, 73, 101), Color.rgb(17, 50, 71), 6));
        confirmBtn.setMinHeight(dp(40));
        confirmBtn.setOnClickListener(v -> {
            ReportItem item = findReport("RG-1042");
            if (item != null) recordObservation(item, false);
            showToast("Existing report confirmed.");
            currentPage = "reports";
            renderWorkspace();
        });
        activeBox.addView(confirmBtn, fullWidth());
        addSpace(activeBox, 6);

        Button continueBtn = new Button(this);
        continueBtn.setText("Continue with new report");
        continueBtn.setAllCaps(false);
        continueBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        continueBtn.setTextColor(Color.rgb(15, 23, 42));
        continueBtn.setBackground(createBorderButtonDrawable(
                Color.rgb(241, 245, 249), Color.rgb(226, 232, 240), Color.rgb(203, 213, 225), 6));
        continueBtn.setMinHeight(dp(40));
        activeBox.addView(continueBtn, fullWidth());

        page.addView(activeBox, fullWidth());
        addSpace(page, 14);

        addText(page, "Description", 13, Color.rgb(15, 23, 42), true);
        EditText description = new EditText(this);
        description.setHint("Describe the blockage, water level and affected walkway...");
        description.setGravity(Gravity.TOP | Gravity.START);
        description.setMinLines(4);
        description.setPadding(dp(12), dp(12), dp(12), dp(12));
        description.setBackground(createBorderButtonDrawable(WHITE, WHITE, Color.rgb(226, 232, 240), 8));
        page.addView(description, fullWidth());
        addSpace(page, 12);

        addText(page, "Report photo (optional)", 13, Color.rgb(15, 23, 42), true);
        Button photoButton = new Button(this);
        photoButton.setText("Choose File  No file chosen");
        photoButton.setAllCaps(false);
        photoButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        photoButton.setTextColor(Color.rgb(15, 23, 42));
        photoButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        photoButton.setPadding(dp(14), 0, dp(14), 0);
        photoButton.setBackground(createBorderButtonDrawable(WHITE, Color.rgb(248, 250, 252), Color.rgb(226, 232, 240), 8));
        photoButton.setMinHeight(dp(44));
        page.addView(photoButton, fullWidth());
        photoButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_REPORT_PHOTO);
        });
        addSpace(page, 16);

        Button submit = new Button(this);
        submit.setText("＋  Submit report");
        submit.setAllCaps(false);
        submit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        submit.setTextColor(WHITE);
        submit.setBackground(createButtonDrawable(
                Color.rgb(27, 73, 101), Color.rgb(17, 50, 71), 6));
        submit.setMinHeight(dp(44));
        page.addView(submit, fullWidth());

        submit.setOnClickListener(view -> {
            String details = description.getText().toString().trim();
            if (details.length() < 10) {
                description.setError("Describe the issue using at least 10 characters.");
                return;
            }
            createReport(place.getSelectedItem().toString(), category.getSelectedItem().toString(),
                    risk.getSelectedItem().toString(), details);
        });
    }

    private void createReport(String place, String category, String risk, String details) {
        if (role == null || isAdmin()) {
            showToast("Campus users only.");
            return;
        }
        int nextId = 1042;
        for (ReportItem item : reports) {
            try {
                nextId = Math.max(nextId, Integer.parseInt(item.id.replace("RG-", "")));
            } catch (NumberFormatException ignored) {
                // Ignore malformed demo IDs and continue from the known seed range.
            }
        }
        String categoryLabel = category.equals("Overflow") ? "Overflowing drain" : category;
        ReportItem created = new ReportItem("RG-" + (nextId + 1), place, category, categoryLabel,
                "Submitted", risk, "Just now", 0, 0, details, userEmail,
                new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                        .format(new java.util.Date()), null, null, null);
        created.photoUri = selectedPhoto == null ? null : selectedPhoto.toString();
        created.addHistory(timestamp(), "Report submitted. Awaiting administrator review.");
        reports.add(0, created);
        persistReports();
        selectedPhoto = null;
        showToast("Report submitted. Awaiting administrator review.");
        currentPage = "reports";
        renderWorkspace();
    }

    private ReportItem findDuplicate(String place, String category) {
        for (ReportItem item : reports) {
            if (item.place.equals(place) && item.category.equals(category)
                    && !item.status.equals("Resolved") && !item.status.equals("Rejected")) return item;
        }
        return null;
    }

    private void recordObservation(ReportItem item, boolean cleared) {
        if (role == null || isAdmin()) {
            showToast("Campus users only.");
            return;
        }
        if (!isActive(item)) {
            showToast("This report is closed.");
            return;
        }
        if (cleared) item.cleared++;
        else item.votes++;
        persistReports();
    }

    private Spinner styledSpinner(String[] options) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, options) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView tv = (TextView) view;
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                    tv.setTextColor(Color.rgb(15, 23, 42));
                    tv.setPadding(dp(8), dp(4), dp(8), dp(4));
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(WHITE);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), Color.rgb(226, 232, 240));
        spinner.setBackground(bg);
        spinner.setPadding(dp(6), dp(6), dp(6), dp(6));

        return spinner;
    }

    private void renderMap(LinearLayout page) {
        // 2x2 Filter Grid
        LinearLayout gridRow1 = new LinearLayout(this);
        gridRow1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams row1Params = fullWidth();
        row1Params.bottomMargin = dp(8);

        LinearLayout col1 = new LinearLayout(this);
        col1.setOrientation(LinearLayout.VERTICAL);
        addText(col1, "Location", 12, MUTED, false);
        Spinner zone = styledSpinner(new String[] {
                "All", "KK8", "Faculty buildings", "Central campus"
        });
        col1.addView(zone, fullWidth());

        LinearLayout col2 = new LinearLayout(this);
        col2.setOrientation(LinearLayout.VERTICAL);
        addText(col2, "Category", 12, MUTED, false);
        Spinner category = styledSpinner(new String[] {"All", "Clogged", "Stagnant water", "Overflow", "Sediment"});
        col2.addView(category, fullWidth());

        LinearLayout.LayoutParams colParams1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        colParams1.rightMargin = dp(6);
        gridRow1.addView(col1, colParams1);

        LinearLayout.LayoutParams colParams2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        colParams2.leftMargin = dp(6);
        gridRow1.addView(col2, colParams2);

        page.addView(gridRow1, row1Params);

        LinearLayout gridRow2 = new LinearLayout(this);
        gridRow2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams row2Params = fullWidth();
        row2Params.bottomMargin = dp(16);

        LinearLayout col3 = new LinearLayout(this);
        col3.setOrientation(LinearLayout.VERTICAL);
        addText(col3, "Severity", 12, MUTED, false);
        Spinner risk = styledSpinner(new String[] {"All", "High", "Medium", "Low"});
        col3.addView(risk, fullWidth());

        LinearLayout col4 = new LinearLayout(this);
        col4.setOrientation(LinearLayout.VERTICAL);
        addText(col4, "Status", 12, MUTED, false);
        Spinner status = styledSpinner(new String[] {
                "All", "Submitted", "Under Review", "Verified", "In progress", "Resolved"
        });
        col4.addView(status, fullWidth());

        LinearLayout.LayoutParams colParams3 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        colParams3.rightMargin = dp(6);
        gridRow2.addView(col3, colParams3);

        LinearLayout.LayoutParams colParams4 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        colParams4.leftMargin = dp(6);
        gridRow2.addView(col4, colParams4);

        page.addView(gridRow2, row2Params);

        // Header
        addText(page, "CAMPUS DRAINAGE", 11, Color.rgb(100, 116, 139), true);
        addText(page, "Hotspot map", 22, Color.rgb(15, 23, 42), true);
        addSpace(page, 10);

        // Filter Pills Row
        android.widget.HorizontalScrollView pillScrollView = new android.widget.HorizontalScrollView(this);
        pillScrollView.setHorizontalScrollBarEnabled(false);

        LinearLayout pillRow = new LinearLayout(this);
        pillRow.setOrientation(LinearLayout.HORIZONTAL);

        String[] filterPills = {"All", "High risk", "Stagnant water", "Clogged", "Resolved"};
        List<TextView> pillViews = new ArrayList<>();

        for (int i = 0; i < filterPills.length; i++) {
            String pillText = filterPills[i];
            TextView pill = new TextView(this);
            pill.setText(pillText);
            pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            pill.setPadding(dp(16), dp(8), dp(16), dp(8));

            GradientDrawable pillBg = new GradientDrawable();
            pillBg.setCornerRadius(dp(20));
            if (i == 0) {
                pillBg.setColor(Color.rgb(27, 73, 101));
                pill.setTextColor(WHITE);
            } else {
                pillBg.setColor(WHITE);
                pillBg.setStroke(dp(1), Color.rgb(226, 232, 240));
                pill.setTextColor(Color.rgb(15, 23, 42));
            }
            pill.setBackground(pillBg);

            LinearLayout.LayoutParams pillParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pillParams.rightMargin = dp(8);
            pillRow.addView(pill, pillParams);
            pillViews.add(pill);

            pill.setOnClickListener(v -> {
                for (int k = 0; k < pillViews.size(); k++) {
                    TextView p = pillViews.get(k);
                    GradientDrawable bg = new GradientDrawable();
                    bg.setCornerRadius(dp(20));
                    if (k == pillViews.indexOf(v)) {
                        bg.setColor(Color.rgb(27, 73, 101));
                        p.setTextColor(WHITE);
                    } else {
                        bg.setColor(WHITE);
                        bg.setStroke(dp(1), Color.rgb(226, 232, 240));
                        p.setTextColor(Color.rgb(15, 23, 42));
                    }
                    p.setBackground(bg);
                }
                if (pillText.equals("High risk")) risk.setSelection(1);
                else if (pillText.equals("Stagnant water")) category.setSelection(2);
                else if (pillText.equals("Clogged")) category.setSelection(1);
                else if (pillText.equals("Resolved")) status.setSelection(5);
                else {
                    risk.setSelection(0);
                    category.setSelection(0);
                    status.setSelection(0);
                }
            });
        }

        pillScrollView.addView(pillRow);
        page.addView(pillScrollView, fullWidth());

        // Scroll track line below pills
        LinearLayout scrollTrack = new LinearLayout(this);
        scrollTrack.setOrientation(LinearLayout.HORIZONTAL);
        scrollTrack.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams trackParams = fullWidth();
        trackParams.topMargin = dp(6);
        trackParams.bottomMargin = dp(14);

        TextView leftArrow = text("◄", 10, Color.rgb(148, 163, 184), false);
        scrollTrack.addView(leftArrow);

        View barLine = new View(this);
        GradientDrawable lineBg = new GradientDrawable();
        lineBg.setColor(Color.rgb(148, 163, 184));
        lineBg.setCornerRadius(dp(3));
        barLine.setBackground(lineBg);
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(0, dp(6), 1);
        lineParams.leftMargin = dp(6);
        lineParams.rightMargin = dp(6);
        scrollTrack.addView(barLine, lineParams);

        TextView rightArrow = text("►", 10, Color.rgb(148, 163, 184), false);
        scrollTrack.addView(rightArrow);

        page.addView(scrollTrack, trackParams);

        // Map View Container
        FrameLayout map = new FrameLayout(this);
        LinearLayout.LayoutParams mapParams = fullWidth();
        mapParams.bottomMargin = dp(14);
        page.addView(map, mapParams);

        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        page.addView(results);

        Runnable update = () -> {
            List<ReportItem> filtered = filterMapReports(
                    zone.getSelectedItem().toString(),
                    category.getSelectedItem().toString(),
                    risk.getSelectedItem().toString(),
                    status.getSelectedItem().toString());
            map.removeAllViews();
            map.addView(new MapSketchView(filtered), new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(260)));
            results.removeAllViews();
            if (filtered.isEmpty()) {
                addText(results, "No hazards match these filters.", 14, MUTED, false);
            } else {
                for (ReportItem item : filtered) addReportCard(results, item, !isAdmin());
            }
        };

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                update.run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
        zone.setOnItemSelectedListener(listener);
        category.setOnItemSelectedListener(listener);
        risk.setOnItemSelectedListener(listener);
        status.setOnItemSelectedListener(listener);
        update.run();
    }

    private List<ReportItem> filterMapReports(String zone, String category, String risk, String status) {
        List<ReportItem> result = new ArrayList<>();
        for (ReportItem item : reports) {
            if (item.status.equals("Rejected")) continue;
            if (!zone.equals("All") && !zoneFor(item).equals(zone)) continue;
            if (!category.equals("All") && !item.category.equals(category)) continue;
            if (!risk.equals("All") && !item.risk.equals(risk)) continue;
            if (!status.equals("All") && !item.status.equals(status)) continue;
            result.add(item);
        }
        return result;
    }

    private String zoneFor(ReportItem item) {
        if (item.place.contains("KK8")) return "KK8";
        if (item.place.contains("Engineering") || item.place.contains("Faculty")) return "Faculty buildings";
        return "Central campus";
    }

    private void renderRisk(LinearLayout page) {
        addText(page, "Risk Overview", 22, Color.rgb(15, 23, 42), true);
        addText(page, "Sample risk estimates based on rainfall, slope and blockage recurrence.",
                13, MUTED, false);
        addSpace(page, 10);

        String[][] places = {
                {"Faculty of Engineering, Block A", "92", "90", "60", "95",
                        "Clear before 2 PM rain peak; install leaf guard on box culvert"},
                {"KK8 walkway to bus stop", "78", "85", "45", "70",
                        "Jet-flush stagnant section; check gradient near bus stop"},
                {"Main Library back entrance", "54", "70", "30", "40",
                        "Monitor during rain; schedule routine desilting"}
        };

        for (String[] place : places) {
            LinearLayout item = card(page, WHITE);
            addText(item, place[0], 16, Color.rgb(15, 23, 42), true);
            addText(item, "Demo risk score " + place[1] + "/100", 14, Color.rgb(27, 73, 101), true);
            addSpace(item, 4);

            addProgressRow(item, "Rainfall intensity", Integer.parseInt(place[2]));
            addProgressRow(item, "Terrain slope", Integer.parseInt(place[3]));
            addProgressRow(item, "Blockage recurrence", Integer.parseInt(place[4]));
            addSpace(item, 8);

            addText(item, place[5], 12, MUTED, false);
        }

        addSpace(page, 10);

        // High Risk Active Reports List (Matching Image 7)
        List<ReportItem> active = new ArrayList<>(reports);
        active.removeIf(item -> item.status.equals("Resolved") || item.status.equals("Rejected"));
        active.sort(Comparator.comparingInt(item -> riskRank(item.risk)));
        Collections.reverse(active);

        int rank = 1;
        for (ReportItem item : active) {
            LinearLayout itemCard = card(page, WHITE);
            itemCard.setOnClickListener(v -> showReportDetails(item));

            LinearLayout mainRow = new LinearLayout(this);
            mainRow.setOrientation(LinearLayout.HORIZONTAL);
            mainRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView numView = text(String.valueOf(rank++), 20, Color.rgb(100, 116, 139), true);
            numView.setPadding(0, 0, dp(14), 0);
            mainRow.addView(numView);

            LinearLayout detailsBox = new LinearLayout(this);
            detailsBox.setOrientation(LinearLayout.VERTICAL);
            addText(detailsBox, item.place, 15, Color.rgb(15, 23, 42), true);
            addText(detailsBox, item.category + " · " + formatDate(item.createdAt) + ", 16:12", 12, MUTED, false);
            addSpace(detailsBox, 4);
            addText(detailsBox, "Blocked drainage and high pedestrian exposure during expected rain.", 12, MUTED, false);
            mainRow.addView(detailsBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            LinearLayout rightBox = new LinearLayout(this);
            rightBox.setOrientation(LinearLayout.VERTICAL);
            rightBox.setGravity(Gravity.END);

            TextView riskTag = text(item.risk + " risk", 12, riskColor(item.risk), true);
            riskTag.setPadding(0, 0, 0, dp(8));
            rightBox.addView(riskTag);

            Button viewBtn = new Button(this);
            viewBtn.setText("View Details");
            viewBtn.setAllCaps(false);
            viewBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            viewBtn.setTextColor(Color.rgb(15, 23, 42));
            viewBtn.setBackground(createBorderButtonDrawable(
                    Color.rgb(241, 245, 249), Color.rgb(226, 232, 240), Color.rgb(203, 213, 225), 6));
            viewBtn.setMinHeight(dp(36));
            viewBtn.setOnClickListener(v -> showReportDetails(item));
            rightBox.addView(viewBtn, new LinearLayout.LayoutParams(dp(90), dp(36)));

            mainRow.addView(rightBox);
            itemCard.addView(mainRow, fullWidth());
        }
    }

    private void addProgressRow(LinearLayout parent, String label, int percentage) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(2), 0, dp(2));

        TextView nameView = text(label, 12, MUTED, false);
        row.addView(nameView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView pctView = text(percentage + "%", 12, Color.rgb(71, 85, 105), false);
        row.addView(pctView);

        parent.addView(row, fullWidth());

        android.widget.ProgressBar progress = new android.widget.ProgressBar(
                this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(percentage);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(27, 73, 101)));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(226, 232, 240)));

        LinearLayout.LayoutParams pParams = fullWidth();
        pParams.bottomMargin = dp(6);
        parent.addView(progress, pParams);
    }

    private void renderTasks(LinearLayout page) {
        addText(page, "Maintenance Tasks", 23, INK, true);
        addText(page, "Verified reports and assigned work.", 13, MUTED, false);
        Spinner status = spinner(new String[] {
                "All", "Awaiting Assignment", "Assigned", "Inspection in Progress",
                "Repair in Progress", "Completed"
        });
        page.addView(status, fullWidth());
        LinearLayout tasks = new LinearLayout(this);
        tasks.setOrientation(LinearLayout.VERTICAL);
        page.addView(tasks);
        Runnable update = () -> {
            tasks.removeAllViews();
            String selected = status.getSelectedItem().toString();
            int count = 0;
            for (ReportItem item : reports) {
                if (!item.status.equals("Verified") && item.team == null) continue;
                String taskStatus = item.taskStatus == null ? "Awaiting Assignment" : item.taskStatus;
                if (!selected.equals("All") && !selected.equals(taskStatus)) continue;
                LinearLayout task = card(tasks, WHITE);
                task.setOnClickListener(view -> openReport(item));
                addText(task, item.id + " · " + item.place, 14, INK, true);
                addText(task, item.team == null ? "No team allocated" : item.team, 12, MUTED, false);
                addText(task, taskStatus + " · " + item.status, 12, statusColor(item.status), true);
                addText(task, item.instructions == null ? "" : item.instructions, 12, MUTED, false);
                count++;
            }
            if (count == 0) addText(tasks, "No maintenance tasks match this filter.", 14, MUTED, false);
        };
        status.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                update.run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        update.run();
    }

    private void renderAnalytics(LinearLayout page) {
        addText(page, "Analytics & Activity", 23, INK, true);
        addText(page, "Sample report trends for the UM campus demo.", 13, MUTED, false);
        addText(page, "Report status breakdown", 17, INK, true);
        String[] statuses = {"Submitted", "Under Review", "Verified", "In progress", "Resolved", "Rejected"};
        for (String status : statuses) {
            int count = countStatus(status);
            LinearLayout line = row(page);
            line.setPadding(0, dp(5), 0, dp(5));
            TextView label = text(status, 13, INK, false);
            line.addView(label, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            line.addView(text(String.valueOf(count), 13, MUTED, true));
            android.widget.ProgressBar progress = new android.widget.ProgressBar(
                    this, null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(Math.max(1, reports.size()));
            progress.setProgress(count);
            progress.setProgressTintList(android.content.res.ColorStateList.valueOf(NAVY));
            progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(BORDER));
            page.addView(progress, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(7)));
        }
        addSpace(page, 16);
        addText(page, "Recurring hotspots · historical sample", 17, INK, true);
        String[][] hotspots = {
                {"Faculty of Engineering, Block A", "95"},
                {"KK8 walkway to bus stop", "70"},
                {"Main Library back entrance", "40"}
        };
        for (String[] hotspot : hotspots) {
            LinearLayout item = card(page, WHITE);
            addText(item, hotspot[0], 14, INK, true);
            addText(item, "Blockage recurrence factor: " + hotspot[1]
                    + "% · sample historical estimate", 12, MUTED, false);
        }
        addText(page, "Maintenance completed: " + countTasksCompleted() + " / "
                + countTasksAssigned() + " tasks", 13, INK, true);
        addSpace(page, 12);
        addActivity(page, 100);
    }

    private void renderNotifications(LinearLayout page) {
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = text("Notifications", 22, Color.rgb(15, 23, 42), true);
        headerRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button prefBtn = new Button(this);
        prefBtn.setText("⚙ Preferences");
        prefBtn.setAllCaps(false);
        prefBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        prefBtn.setTextColor(Color.rgb(15, 23, 42));
        prefBtn.setBackground(createBorderButtonDrawable(
                WHITE, Color.rgb(241, 245, 249), Color.rgb(226, 232, 240), 6));
        prefBtn.setMinHeight(dp(36));
        prefBtn.setOnClickListener(v -> showPreferencesBottomSheet());
        headerRow.addView(prefBtn);

        page.addView(headerRow, fullWidth());
        addSpace(page, 10);

        // Warning Alert Card
        LinearLayout warningCard = new LinearLayout(this);
        warningCard.setOrientation(LinearLayout.HORIZONTAL);
        warningCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        warningCard.setBackground(createButtonDrawable(
                Color.rgb(255, 251, 235), Color.rgb(254, 243, 199), 6));

        View leftBar = new View(this);
        leftBar.setBackgroundColor(Color.rgb(217, 119, 6));
        warningCard.addView(leftBar, new LinearLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout warningText = new LinearLayout(this);
        warningText.setOrientation(LinearLayout.VERTICAL);
        warningText.setPadding(dp(12), 0, 0, 0);
        addText(warningText, "Sample heavy rainfall warning", 15, Color.rgb(15, 23, 42), true);
        addText(warningText, "Avoid flooded walkways and low-lying parking areas. Use covered campus paths and monitor rising water.",
                13, Color.rgb(51, 65, 85), false);
        warningCard.addView(warningText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        page.addView(warningCard, fullWidth());
        addSpace(page, 14);

        // Report Updates List
        for (ReportItem item : reports) {
            if (!isAdmin() && !item.owner.equalsIgnoreCase(userEmail)) continue;

            LinearLayout notifCard = card(page, WHITE);
            notifCard.setOnClickListener(v -> openReport(item));

            LinearLayout notifRow = new LinearLayout(this);
            notifRow.setOrientation(LinearLayout.HORIZONTAL);
            notifRow.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout notifDetails = new LinearLayout(this);
            notifDetails.setOrientation(LinearLayout.VERTICAL);
            addText(notifDetails, item.id + " · " + item.status, 15, Color.rgb(15, 23, 42), true);
            addText(notifDetails, "Report " + item.id + " received.", 13, MUTED, false);
            notifRow.addView(notifDetails, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            Button viewBtn = new Button(this);
            viewBtn.setText("View Details");
            viewBtn.setAllCaps(false);
            viewBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            viewBtn.setTextColor(Color.rgb(15, 23, 42));
            viewBtn.setBackground(createBorderButtonDrawable(
                    Color.rgb(241, 245, 249), Color.rgb(226, 232, 240), Color.rgb(203, 213, 225), 6));
            viewBtn.setMinHeight(dp(36));
            viewBtn.setOnClickListener(v -> openReport(item));
            notifRow.addView(viewBtn, new LinearLayout.LayoutParams(dp(90), dp(36)));

            notifCard.addView(notifRow, fullWidth());
        }
    }

    private void renderSettings(LinearLayout page) {
        showPreferencesBottomSheet();
    }

    private void showPreferencesBottomSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        LinearLayout sheetView = new LinearLayout(this);
        sheetView.setOrientation(LinearLayout.VERTICAL);
        sheetView.setPadding(dp(20), dp(18), dp(20), dp(24));
        sheetView.setBackgroundColor(WHITE);

        // Header
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        addText(titleBox, "PREFERENCES", 11, MUTED, true);
        addText(titleBox, "Notifications & zones", 20, Color.rgb(15, 23, 42), true);
        headerRow.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView closeBtn = text("✕", 18, Color.rgb(100, 116, 139), false);
        closeBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
        closeBtn.setOnClickListener(v -> sheet.dismiss());
        headerRow.addView(closeBtn);

        sheetView.addView(headerRow, fullWidth());
        addSpace(sheetView, 12);

        // Switches
        String settingsKey = "settings." + userEmail.toLowerCase(Locale.ROOT);
        String[] keys = {"heavyRain", "flashFlood", "weekly"};
        String[] labels = {"Heavy rainfall alerts", "Flash flood warnings", "Weekly campus summary"};
        String[] descriptions = {
                "Sample rainfall warnings for Klang Valley",
                "When blocked drains coincide with heavy rain",
                "Resolved reports and hotspot trends"
        };

        Switch[] switches = new Switch[labels.length];
        for (int i = 0; i < labels.length; i++) {
            LinearLayout itemCard = card(sheetView, WHITE);
            Switch preference = new Switch(this);
            preference.setText(labels[i] + "\n" + descriptions[i]);
            preference.setTextColor(Color.rgb(15, 23, 42));
            preference.setChecked(dataPreferences.getBoolean(settingsKey + "." + keys[i], i < 2));
            switches[i] = preference;
            itemCard.addView(preference, fullWidth());
        }

        addSpace(sheetView, 10);
        addText(sheetView, "Subscribed campus zones", 16, Color.rgb(15, 23, 42), true);
        addText(sheetView, "You'll get drainage alerts for the zones you select.", 12, MUTED, false);
        addSpace(sheetView, 8);

        String[] zones = {
                "KK1", "KK2", "KK3", "KK4", "KK5", "KK6", "KK7", "KK8", "KK9",
                "KK10", "KK11", "KK12", "Faculty buildings", "Central campus"
        };
        String savedZones = dataPreferences.getString(settingsKey + ".zones", "KK8|Faculty buildings");
        List<String> currentZones = new ArrayList<>(Arrays.asList(savedZones.split("\\|", -1)));

        LinearLayout zoneContainer = new LinearLayout(this);
        zoneContainer.setOrientation(LinearLayout.VERTICAL);

        LinearLayout currentRow = new LinearLayout(this);
        currentRow.setOrientation(LinearLayout.HORIZONTAL);
        int currentLineWidth = 0;
        int maxLineWidthDp = 280;

        List<String> selectedZoneList = new ArrayList<>(currentZones);

        for (String zone : zones) {
            TextView zonePill = new TextView(this);
            zonePill.setText(zone);
            zonePill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            zonePill.setPadding(dp(12), dp(6), dp(12), dp(6));

            boolean isSelected = selectedZoneList.contains(zone);
            updateZonePillStyle(zonePill, isSelected);

            zonePill.setOnClickListener(v -> {
                if (selectedZoneList.contains(zone)) {
                    selectedZoneList.remove(zone);
                    updateZonePillStyle(zonePill, false);
                } else {
                    selectedZoneList.add(zone);
                    updateZonePillStyle(zonePill, true);
                }
            });

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.rightMargin = dp(6);
            p.bottomMargin = dp(6);

            int pillApproxDp = zone.length() * 8 + 24;
            if (currentLineWidth + pillApproxDp > maxLineWidthDp) {
                zoneContainer.addView(currentRow, fullWidth());
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentLineWidth = 0;
            }
            currentRow.addView(zonePill, p);
            currentLineWidth += pillApproxDp;
        }
        zoneContainer.addView(currentRow, fullWidth());
        sheetView.addView(zoneContainer, fullWidth());

        addSpace(sheetView, 16);

        Button saveBtn = new Button(this);
        saveBtn.setText("Save preferences");
        saveBtn.setAllCaps(false);
        saveBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        saveBtn.setTextColor(WHITE);
        saveBtn.setBackground(createButtonDrawable(
                Color.rgb(27, 73, 101), Color.rgb(17, 50, 71), 6));
        saveBtn.setMinHeight(dp(44));

        saveBtn.setOnClickListener(v -> {
            SharedPreferences.Editor editor = dataPreferences.edit();
            for (int i = 0; i < keys.length; i++) {
                editor.putBoolean(settingsKey + "." + keys[i], switches[i].isChecked());
            }
            editor.putString(settingsKey + ".zones", android.text.TextUtils.join("|", selectedZoneList));
            editor.apply();
            sheet.dismiss();
            showToast("Preferences saved.");
        });

        sheetView.addView(saveBtn, fullWidth());
        sheet.setContentView(sheetView);
        sheet.show();
    }

    private void updateZonePillStyle(TextView pill, boolean selected) {
        if (selected) {
            pill.setBackground(createButtonDrawable(
                    Color.rgb(27, 73, 101), Color.rgb(17, 50, 71), 16));
            pill.setTextColor(WHITE);
        } else {
            pill.setBackground(createBorderButtonDrawable(
                    WHITE, Color.rgb(241, 245, 249), Color.rgb(226, 232, 240), 16));
            pill.setTextColor(Color.rgb(15, 23, 42));
        }
    }

    private void showProfileBubble(View anchorView) {
        if (anchorView == null) return;

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        box.setBackground(createBorderButtonDrawable(
                WHITE, WHITE, Color.rgb(203, 213, 225), 12));

        TextView emailTv = text(userEmail, 15, Color.rgb(15, 23, 42), true);
        box.addView(emailTv, fullWidth());

        TextView roleTv = text("Current role: " + (isAdmin() ? "Admin" : "Campus User"), 13, MUTED, false);
        LinearLayout.LayoutParams rParams = fullWidth();
        rParams.topMargin = dp(2);
        rParams.bottomMargin = dp(12);
        box.addView(roleTv, rParams);

        Button signOutBtn = new Button(this);
        signOutBtn.setText("↳  Sign out");
        signOutBtn.setAllCaps(false);
        signOutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        signOutBtn.setTextColor(Color.rgb(15, 23, 42));
        signOutBtn.setBackground(createBorderButtonDrawable(
                Color.rgb(241, 245, 249), Color.rgb(226, 232, 240), Color.rgb(203, 213, 225), 6));
        signOutBtn.setMinHeight(dp(38));

        box.addView(signOutBtn, fullWidth());

        PopupWindow popup = new PopupWindow(box, dp(240), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setOutsideTouchable(true);
        popup.setFocusable(true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(dp(8));

        signOutBtn.setOnClickListener(v -> {
            popup.dismiss();
            signOut();
        });

        popup.showAsDropDown(anchorView, -dp(120), dp(4));
    }

    private void addStatCard(LinearLayout parent, int number, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(4), dp(14), dp(4), dp(14));
        box.setBackground(createBorderButtonDrawable(WHITE, Color.rgb(248, 250, 252), Color.rgb(226, 232, 240), 12));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(dp(4), 0, dp(4), 0);
        parent.addView(box, params);

        TextView numView = text(String.valueOf(number), 24, Color.rgb(15, 23, 42), true);
        numView.setGravity(Gravity.CENTER);
        box.addView(numView, fullWidth());

        TextView labelView = text(label, 12, Color.rgb(100, 116, 139), false);
        labelView.setGravity(Gravity.CENTER);
        box.addView(labelView, fullWidth());
    }

    private int statusBgColor(String status) {
        if (status.equals("In progress")) return Color.rgb(254, 243, 199);
        if (status.equals("Resolved") || status.equals("Completed")) return Color.rgb(240, 253, 244);
        if (status.equals("Rejected")) return Color.rgb(254, 226, 226);
        return Color.rgb(239, 246, 255);
    }

    private void addReportCard(LinearLayout parent, ReportItem item, boolean communityActions) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(createBorderButtonDrawable(WHITE, Color.rgb(248, 250, 252), Color.rgb(226, 232, 240), 12));
        card.setOnClickListener(v -> showReportDetails(item));

        // Top row: Dot + Location Title + Time
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView dot = text("●", 12, riskColor(item.risk), true);
        dot.setPadding(0, 0, dp(8), 0);
        topRow.addView(dot);

        TextView placeTitle = text(item.place, 15, Color.rgb(15, 23, 42), true);
        topRow.addView(placeTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView timeAgo = text(item.time, 12, Color.rgb(100, 116, 139), false);
        topRow.addView(timeAgo);

        card.addView(topRow, fullWidth());

        // Subtitle: category · tags · ID
        TextView subTitle = text(item.type + " · " + item.id, 13, Color.rgb(100, 116, 139), false);
        LinearLayout.LayoutParams subParams = fullWidth();
        subParams.topMargin = dp(4);
        card.addView(subTitle, subParams);

        // Status pill + Confirmations count row
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams statusRowParams = fullWidth();
        statusRowParams.topMargin = dp(10);

        TextView statusPill = text(item.status, 12, statusColor(item.status), true);
        statusPill.setPadding(dp(10), dp(4), dp(10), dp(4));
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(statusBgColor(item.status));
        pillBg.setCornerRadius(dp(8));
        statusPill.setBackground(pillBg);
        statusRow.addView(statusPill);

        TextView confirmedText = text("👍 " + item.votes + " confirmed", 12, Color.rgb(100, 116, 139), false);
        confirmedText.setGravity(Gravity.END);
        statusRow.addView(confirmedText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        card.addView(statusRow, statusRowParams);

        // Action buttons: Still There & Cleared
        if (communityActions && !item.status.equals("Resolved") && !item.status.equals("Rejected")) {
            LinearLayout actionRow = new LinearLayout(this);
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams actionRowParams = fullWidth();
            actionRowParams.topMargin = dp(12);

            Button confirmBtn = new Button(this);
            confirmBtn.setText("👍 Still There (" + item.votes + ")");
            confirmBtn.setAllCaps(false);
            confirmBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            confirmBtn.setTextColor(Color.rgb(29, 78, 216));
            confirmBtn.setBackground(createBorderButtonDrawable(
                    Color.rgb(239, 246, 255), Color.rgb(219, 234, 254), Color.rgb(191, 219, 254), 8));
            confirmBtn.setMinHeight(dp(40));

            Button clearedBtn = new Button(this);
            clearedBtn.setText("✓ Cleared (" + item.cleared + ")");
            clearedBtn.setAllCaps(false);
            clearedBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            clearedBtn.setTextColor(Color.rgb(21, 128, 61));
            clearedBtn.setBackground(createBorderButtonDrawable(
                    Color.rgb(240, 253, 244), Color.rgb(220, 252, 231), Color.rgb(187, 247, 208), 8));
            clearedBtn.setMinHeight(dp(40));

            LinearLayout.LayoutParams btnParams1 = new LinearLayout.LayoutParams(0, dp(40), 1);
            btnParams1.rightMargin = dp(6);
            actionRow.addView(confirmBtn, btnParams1);

            LinearLayout.LayoutParams btnParams2 = new LinearLayout.LayoutParams(0, dp(40), 1);
            btnParams2.leftMargin = dp(6);
            actionRow.addView(clearedBtn, btnParams2);

            confirmBtn.setOnClickListener(v -> {
                recordObservation(item, false);
                showToast("Condition confirmed. Thank you.");
                renderWorkspace();
            });
            clearedBtn.setOnClickListener(v -> {
                recordObservation(item, true);
                showToast("Cleared observation recorded.");
                renderWorkspace();
            });

            card.addView(actionRow, actionRowParams);
        }

        LinearLayout.LayoutParams cardParams = fullWidth();
        cardParams.bottomMargin = dp(12);
        parent.addView(card, cardParams);
    }

    private void showReportDetails(ReportItem item) {
        openReport(item);
    }

    private void openReport(ReportItem item) {
        String origin = currentPage.equals("report-details") ? returnPage : currentPage;
        launchScreen("report-details", item.id, origin);
    }

    private ReportItem findReport(String id) {
        if (id == null) return null;
        for (ReportItem item : reports) if (item.id.equals(id)) return item;
        return null;
    }

    private void renderReportDetails(LinearLayout page, ReportItem item) {
        addText(page, item.id + " · " + formatDate(item.createdAt) + ", 16:12 MYT",
                12, MUTED, false);
        addText(page, item.place, 22, Color.rgb(15, 23, 42), true);

        // Status & Risk Pills Row
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView riskView = text(item.risk + " risk", 13, riskColor(item.risk), true);
        riskView.setPadding(0, 0, dp(12), 0);
        statusRow.addView(riskView);

        TextView statusPill = text(item.status, 12, statusColor(item.status), true);
        statusPill.setPadding(dp(10), dp(4), dp(10), dp(4));
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(statusBgColor(item.status));
        pillBg.setCornerRadius(dp(6));
        statusPill.setBackground(pillBg);
        statusRow.addView(statusPill);

        page.addView(statusRow, fullWidth());
        addDivider(page);

        // Original Report
        addText(page, "ORIGINAL REPORT · " + item.category.toUpperCase(Locale.ROOT),
                11, MUTED, true);
        addText(page, item.description, 14, Color.rgb(15, 23, 42), false);
        addSpace(page, 4);
        addText(page, "📷  No photo attached", 12, MUTED, false);
        addDivider(page);

        // Safety Advice Card (Matching Image 2)
        LinearLayout adviceCard = new LinearLayout(this);
        adviceCard.setOrientation(LinearLayout.HORIZONTAL);
        adviceCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        adviceCard.setBackground(createButtonDrawable(
                Color.rgb(255, 251, 235), Color.rgb(254, 243, 199), 6));

        View leftAccent = new View(this);
        leftAccent.setBackgroundColor(Color.rgb(217, 119, 6));
        adviceCard.addView(leftAccent, new LinearLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout adviceContent = new LinearLayout(this);
        adviceContent.setOrientation(LinearLayout.VERTICAL);
        adviceContent.setPadding(dp(12), 0, 0, 0);
        addText(adviceContent, "Safety advice", 15, Color.rgb(15, 23, 42), true);
        addText(adviceContent, "Avoid flooded or slippery walkways. Use covered routes. Do not enter flowing water or approach electrical equipment near ponding.",
                13, Color.rgb(51, 65, 85), false);
        adviceCard.addView(adviceContent, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        page.addView(adviceCard, fullWidth());
        addDivider(page);

        // Maintenance Progress
        if (item.team != null || item.taskStatus != null) {
            addText(page, "Maintenance progress", 16, Color.rgb(15, 23, 42), true);
            addText(page, "Responsible team: " + (item.team == null ? "JPPHB Drainage Team" : item.team), 13, Color.rgb(15, 23, 42), false);

            TextView progressPill = text(item.taskStatus == null ? "Inspection in Progress" : item.taskStatus, 12, Color.rgb(2, 132, 199), true);
            progressPill.setPadding(dp(10), dp(4), dp(10), dp(4));
            GradientDrawable progBg = new GradientDrawable();
            progBg.setColor(Color.rgb(224, 242, 254));
            progBg.setCornerRadius(dp(6));
            progressPill.setBackground(progBg);

            LinearLayout progressBox = new LinearLayout(this);
            progressBox.setOrientation(LinearLayout.VERTICAL);
            progressBox.addView(progressPill, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            page.addView(progressBox, fullWidth());

            addSpace(page, 6);
            addText(page, "Completed maintenance tasks officially change the report to Resolved.", 12, MUTED, false);
            addDivider(page);
        }

        // Report history & updates
        addText(page, "Report history & updates", 16, Color.rgb(15, 23, 42), true);
        for (HistoryEntry entry : item.history) {
            LinearLayout eventRow = new LinearLayout(this);
            eventRow.setOrientation(LinearLayout.HORIZONTAL);
            eventRow.setPadding(0, dp(4), 0, dp(4));

            View bar = new View(this);
            bar.setBackgroundColor(Color.rgb(203, 213, 225));
            eventRow.addView(bar, new LinearLayout.LayoutParams(dp(3), ViewGroup.LayoutParams.MATCH_PARENT));

            LinearLayout eventText = new LinearLayout(this);
            eventText.setOrientation(LinearLayout.VERTICAL);
            eventText.setPadding(dp(10), 0, 0, 0);
            addText(eventText, entry.text, 13, Color.rgb(15, 23, 42), false);
            addText(eventText, entry.date + " MYT", 11, MUTED, false);
            eventRow.addView(eventText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            page.addView(eventRow, fullWidth());
        }

        addSpace(page, 12);

        if (isAdmin()) {
            renderAdminReview(page, item);
        } else if (!item.status.equals("Resolved") && !item.status.equals("Rejected")) {
            addText(page, "Community observations", 16, Color.rgb(15, 23, 42), true);
            LinearLayout observations = new LinearLayout(this);
            observations.setOrientation(LinearLayout.HORIZONTAL);

            Button stillThere = new Button(this);
            stillThere.setText("Still There (" + item.votes + ")");
            stillThere.setAllCaps(false);
            stillThere.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            stillThere.setTextColor(WHITE);
            stillThere.setBackground(createButtonDrawable(
                    Color.rgb(27, 73, 101), Color.rgb(17, 50, 71), 6));
            stillThere.setMinHeight(dp(42));

            Button cleared = new Button(this);
            cleared.setText("Cleared (" + item.cleared + ")");
            cleared.setAllCaps(false);
            cleared.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            cleared.setTextColor(Color.rgb(15, 23, 42));
            cleared.setBackground(createBorderButtonDrawable(
                    Color.rgb(241, 245, 249), Color.rgb(226, 232, 240), Color.rgb(203, 213, 225), 6));
            cleared.setMinHeight(dp(42));

            LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, dp(42), 1);
            p1.rightMargin = dp(8);
            observations.addView(stillThere, p1);

            LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, dp(42), 1);
            observations.addView(cleared, p2);

            stillThere.setOnClickListener(view -> {
                recordObservation(item, false);
                showToast("Still There observation recorded.");
                renderWorkspace();
            });
            cleared.setOnClickListener(view -> {
                recordObservation(item, true);
                showToast("Cleared observation recorded.");
                renderWorkspace();
            });

            page.addView(observations, fullWidth());
            addSpace(page, 6);
            addText(page, "Community observations do not change the official report status.", 12, MUTED, false);
        }
    }

    private void renderAdminReview(LinearLayout page, ReportItem item) {
        LinearLayout review = card(page, Color.rgb(240, 247, 251));
        addText(review, "✦  AI-assisted review", 16, NAVY, true);
        addText(review, "Demo rule-based output · not a live AI model", 11, MUTED, false);
        addText(review, item.summary == null
                ? "Generate a draft summary to support your review."
                : item.summary, 13, INK, false);
        Button summary = smallButton(item.summary == null ? "Generate Summary" : "Regenerate Summary");
        review.addView(summary, fullWidth());
        summary.setOnClickListener(view -> {
            if (!isAdmin()) {
                showToast("Administrators only.");
                return;
            }
            item.summary = item.category + " was reported at " + item.place + ". "
                    + item.description + " The affected area may create a slipping hazard and disrupt "
                    + "pedestrian movement. An on-site inspection is recommended. This is a demo "
                    + "rule-based summary; administrator review is required.";
            persistReports();
            showToast("Demo report summary generated. No status change.");
            renderWorkspace();
        });
        addText(review, "Suggested risk: " + item.risk, 13, INK, true);
        addText(review, "Sample rainfall intensity peaks at 18 mm/h. " + item.category
                + " affects drainage capacity and pedestrian access. Inspect on site before allocating maintenance.",
                12, MUTED, false);
        addText(review, "The administrator makes the final decision. This summary never verifies, assigns or resolves reports.",
                11, MUTED, false);

        int duplicates = 0;
        for (ReportItem other : reports) {
            if (other != item && other.place.equals(item.place)
                    && other.category.equals(item.category)
                    && !other.status.equals("Rejected")) duplicates++;
        }
        if (duplicates > 0) {
            LinearLayout duplicateBox = card(page, WHITE);
            addText(duplicateBox, "Duplicate-report suggestions", 15, INK, true);
            for (ReportItem other : reports) {
                if (other != item && other.place.equals(item.place)
                        && other.category.equals(item.category)
                        && !other.status.equals("Rejected")) {
                    addText(duplicateBox, other.id + " · " + other.category + " · " + other.status,
                            12, MUTED, false);
                }
            }
            addText(duplicateBox, "Review descriptions before deciding whether reports describe the same incident.",
                    11, MUTED, false);
        }

        addText(page, "Administrator actions", 17, INK, true);
        if (item.status.equals("Submitted") || item.status.equals("Under Review")) {
            if (item.status.equals("Submitted")) {
                Button underReview = smallButton("Under Review");
                page.addView(underReview, fullWidth());
                underReview.setOnClickListener(view -> applyReportAction(
                        item, "review", null, null, null));
            }
            Button verify = actionButton("Verify Report");
            page.addView(verify, fullWidth());
            verify.setOnClickListener(view -> applyReportAction(item, "verify", null, null, null));
            Button reject = smallButton("Reject Report");
            page.addView(reject, fullWidth());
            reject.setOnClickListener(view -> showActionDialog(item, "reject"));
        } else if (item.status.equals("Verified")) {
            Button assign = actionButton("⚒  Assign Maintenance");
            page.addView(assign, fullWidth());
            assign.setOnClickListener(view -> showActionDialog(item, "assign"));
        } else if (item.team != null && !item.status.equals("Resolved")) {
            Button update = smallButton("Update Progress");
            page.addView(update, fullWidth());
            update.setOnClickListener(view -> showActionDialog(item, "progress"));
            Button complete = actionButton("✓  Mark as Completed");
            page.addView(complete, fullWidth());
            complete.setOnClickListener(view -> showActionDialog(item, "resolve"));
            Button photo = smallButton("Add inspection / completion photo");
            page.addView(photo, fullWidth());
            photo.setOnClickListener(view -> {
                selectedReportId = item.id;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_COMPLETION_PHOTO);
            });
        } else {
            addText(page, "This report is closed.", 13, MUTED, false);
        }
    }

    private void showActionDialog(ReportItem item, String action) {
        if (!isAdmin()) {
            showToast("Administrators only.");
            return;
        }
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(4));
        Spinner team = null;
        Spinner taskStatus = null;
        if (action.equals("assign")) {
            addText(content, "Responsible team", 13, INK, true);
            team = spinner(new String[] {
                    "JPPHB Drainage Team", "Campus Facilities Team",
                    "Landscape & Grounds Team", "Maintenance Officer Ahmad"
            });
            content.addView(team, fullWidth());
        } else if (action.equals("progress")) {
            addText(content, "Task status", 13, INK, true);
            taskStatus = spinner(new String[] {
                    "Assigned", "Inspection in Progress", "Repair in Progress"
            });
            if (item.taskStatus != null) {
                int index = Arrays.asList("Assigned", "Inspection in Progress", "Repair in Progress")
                        .indexOf(item.taskStatus);
                if (index >= 0) taskStatus.setSelection(index);
            }
            content.addView(taskStatus, fullWidth());
        }
        String label = action.equals("reject") ? "Rejection reason"
                : action.equals("resolve") ? "Resolution notes"
                : action.equals("assign") ? "Maintenance instructions"
                : "Inspection / repair notes";
        addText(content, label, 13, INK, true);
        EditText notes = new EditText(this);
        notes.setHint(label);
        notes.setGravity(Gravity.TOP | Gravity.START);
        notes.setMinLines(3);
        notes.setPadding(dp(10), dp(10), dp(10), dp(10));
        content.addView(notes, fullWidth());

        final Spinner selectedTeam = team;
        final Spinner selectedTaskStatus = taskStatus;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(action.equals("reject") ? "Reject report"
                        : action.equals("assign") ? "Assign maintenance"
                        : action.equals("resolve") ? "Complete task & resolve report"
                        : "Update maintenance progress")
                .setMessage(item.id + " · " + item.place)
                .setView(content)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(action.equals("reject") ? "Confirm rejection"
                        : action.equals("assign") ? "Confirm assignment"
                        : action.equals("resolve") ? "Confirm completion" : "Save progress", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String note = notes.getText().toString().trim();
                    if (note.isEmpty()) {
                        notes.setError(action.equals("resolve")
                                ? "Resolution notes are required."
                                : "Please include a reason or work notes.");
                        return;
                    }
                    String selectedTeamName = selectedTeam == null ? null
                            : selectedTeam.getSelectedItem().toString();
                    String selectedStatus = selectedTaskStatus == null ? null
                            : selectedTaskStatus.getSelectedItem().toString();
                    boolean saved = applyReportAction(item, action, note,
                            selectedTeamName, selectedStatus);
                    if (saved) dialog.dismiss();
                }));
        dialog.show();
    }

    private boolean applyReportAction(ReportItem item, String action, String notes,
                                      String team, String taskStatus) {
        if (!isAdmin()) {
            showToast("Administrators only.");
            return false;
        }
        if (action.equals("review") || action.equals("verify")) {
            if (!item.status.equals("Submitted") && !item.status.equals("Under Review")) {
                showToast("Only submitted reports can be reviewed or verified.");
                return false;
            }
            item.status = action.equals("review") ? "Under Review" : "Verified";
            appendHistory(item, action.equals("review")
                    ? "Report is under administrator review."
                    : "Report verified by maintenance administrator.");
        } else if (action.equals("reject")) {
            if (!item.status.equals("Submitted") && !item.status.equals("Under Review")) {
                showToast("Only unverified reports can be rejected.");
                return false;
            }
            if (notes == null || notes.trim().isEmpty()) {
                showToast("A rejection reason is required.");
                return false;
            }
            item.status = "Rejected";
            appendHistory(item, "Rejected: " + notes.trim());
        } else if (action.equals("assign")) {
            if (!item.status.equals("Verified")) {
                showToast("Verify the report before assigning maintenance.");
                return false;
            }
            if (team == null || team.trim().isEmpty() || notes == null || notes.trim().isEmpty()) {
                showToast("Choose a team and enter maintenance instructions.");
                return false;
            }
            item.status = "In progress";
            item.team = team;
            item.instructions = notes.trim();
            item.taskStatus = "Assigned";
            appendHistory(item, "Maintenance assigned to " + team + ". Instructions: " + notes.trim());
        } else if (action.equals("progress")) {
            if (!isAssigned(item) || !isActive(item)) {
                showToast("Assign maintenance before updating progress.");
                return false;
            }
            if (taskStatus == null || notes == null || notes.trim().isEmpty()) {
                showToast("Select a task status and enter work notes.");
                return false;
            }
            item.taskStatus = taskStatus;
            appendHistory(item, taskStatus + ": " + notes.trim());
        } else if (action.equals("resolve")) {
            if (!isAssigned(item) || !item.status.equals("In progress")) {
                showToast("Assign maintenance before completing the task.");
                return false;
            }
            if (notes == null || notes.trim().isEmpty()) {
                showToast("Resolution notes are required.");
                return false;
            }
            item.status = "Resolved";
            item.taskStatus = "Completed";
            item.resolution = notes.trim();
            item.resolvedAt = timestamp();
            appendHistory(item, "Task Completed; report Resolved. " + notes.trim());
        }
        persistReports();
        showToast(action.equals("verify") ? "Report verified."
                : action.equals("review") ? "Report moved to Under Review."
                : "Report update saved.");
        renderWorkspace();
        return true;
    }

    private boolean isAssigned(ReportItem item) {
        return item.team != null && !item.team.trim().isEmpty();
    }

    private boolean isActive(ReportItem item) {
        return !item.status.equals("Resolved") && !item.status.equals("Rejected");
    }

    private void addReportImage(LinearLayout parent, String uriText, String description) {
        try {
            ImageView image = new ImageView(this);
            image.setContentDescription(description);
            image.setAdjustViewBounds(true);
            image.setMaxHeight(dp(280));
            image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            image.setImageURI(Uri.parse(uriText));
            LinearLayout.LayoutParams params = fullWidth();
            params.topMargin = dp(8);
            parent.addView(image, params);
        } catch (RuntimeException exception) {
            android.util.Log.w("UMRainGuard", "Could not display a saved report photo.", exception);
            addText(parent, "The saved photo is unavailable on this device.", 12, MUTED, false);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException exception) {
            android.util.Log.i("UMRainGuard", "The selected photo provider did not grant persistent access.");
        }
        try (android.content.res.AssetFileDescriptor descriptor =
                     getContentResolver().openAssetFileDescriptor(uri, "r")) {
            if (descriptor != null && descriptor.getLength() > 2L * 1024L * 1024L) {
                showToast("Choose an image smaller than 2 MB.");
                return;
            }
        } catch (java.io.IOException exception) {
            android.util.Log.w("UMRainGuard", "Could not read selected photo metadata.", exception);
        }

        if (requestCode == REQUEST_REPORT_PHOTO) {
            selectedPhoto = uri;
            if (photoLabel != null) {
                photoLabel.setText("Photo selected · " + uri.getLastPathSegment());
            }
        } else if (requestCode == REQUEST_COMPLETION_PHOTO) {
            ReportItem item = findReport(selectedReportId);
            if (item == null || !isAdmin() || !isAssigned(item) || !isActive(item)) {
                showToast("A maintenance photo can only be added to an active assigned task.");
                return;
            }
            item.completionPhotoUri = uri.toString();
            appendHistory(item, "Inspection / completion photo attached.");
            persistReports();
            showToast("Maintenance photo attached.");
            renderWorkspace();
        }
    }

    private void renderBottomNavigation() {
        bottomNavigation.removeAllViews();
        List<String[]> nav = navigation();
        int visible = Math.min(4, nav.size());
        for (int i = 0; i < visible; i++) {
            String page = nav.get(i)[0];
            String label = nav.get(i)[1];
            addNavItem(page, label, 1);
        }
        addNavItem("more", "More", 1);
    }

    private void addNavItem(String page, String label, int weight) {
        TextView item = new TextView(this);
        item.setText(navGlyph(page) + "\n" + label);
        item.setGravity(Gravity.CENTER);
        item.setTextSize(10);
        item.setLineSpacing(0, 0.9f);
        item.setTextColor(currentPage.equals(page) ? NAVY : MUTED);
        item.setPadding(dp(2), dp(4), dp(2), dp(4));
        bottomNavigation.addView(item, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, weight));
        item.setOnClickListener(view -> {
            if (page.equals("more")) showNavigationMenu();
            else navigate(page);
        });
    }

    private List<String[]> navigation() {
        if (isAdmin()) {
            return Arrays.asList(
                    new String[] {"dashboard", "Dashboard"},
                    new String[] {"all-reports", "All Reports"},
                    new String[] {"tasks", "Tasks"},
                    new String[] {"home", "Campus Home"},
                    new String[] {"analytics", "Analytics"},
                    new String[] {"settings", "Settings"});
        }
        return Arrays.asList(
                new String[] {"dashboard", "Home"},
                new String[] {"map", "Hazard Map"},
                new String[] {"report", "Report"},
                new String[] {"reports", "My Reports"},
                new String[] {"risk", "Risk Overview"},
                new String[] {"notifications", "Notifications"},
                new String[] {"settings", "Settings"});
    }

    private void showNavigationMenu() {
        LinearLayout dialogView = new LinearLayout(this);
        dialogView.setOrientation(LinearLayout.VERTICAL);
        dialogView.setPadding(dp(20), dp(18), dp(20), dp(20));

        // Header line: Title + Subtitle + Close button (Matching Image 6)
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        addText(titleBox, isAdmin() ? "Admin navigation" : "Campus user navigation", 18, Color.rgb(15, 23, 42), true);
        addText(titleBox, "UM RainGuard · " + (isAdmin() ? "Admin" : "Campus User"), 12, MUTED, false);
        headerRow.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView closeBtn = text("✕", 18, Color.rgb(100, 116, 139), false);
        closeBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
        headerRow.addView(closeBtn);

        dialogView.addView(headerRow, fullWidth());
        addSpace(dialogView, 12);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        closeBtn.setOnClickListener(v -> dialog.dismiss());

        // Nav List Items
        List<String[]> nav = navigation();
        for (String[] item : nav) {
            String page = item[0];
            String label = item[1];

            TextView navRow = new TextView(this);
            navRow.setText(navGlyph(page) + "   " + label);
            navRow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            navRow.setTextColor(Color.rgb(15, 23, 42));
            navRow.setPadding(dp(12), dp(12), dp(12), dp(12));
            navRow.setBackground(createButtonDrawable(
                    WHITE, Color.rgb(241, 245, 249), 6));

            navRow.setOnClickListener(v -> {
                dialog.dismiss();
                navigate(page);
            });

            LinearLayout.LayoutParams rowParams = fullWidth();
            rowParams.bottomMargin = dp(4);
            dialogView.addView(navRow, rowParams);
        }

        addSpace(dialogView, 12);

        // Sign out button
        Button signOutBtn = new Button(this);
        signOutBtn.setText("↳  Sign out");
        signOutBtn.setAllCaps(false);
        signOutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        signOutBtn.setTextColor(Color.rgb(15, 23, 42));
        signOutBtn.setBackground(createBorderButtonDrawable(
                Color.rgb(248, 250, 252), Color.rgb(226, 232, 240), Color.rgb(226, 232, 240), 6));
        signOutBtn.setMinHeight(dp(44));
        signOutBtn.setOnClickListener(v -> {
            dialog.dismiss();
            signOut();
        });

        dialogView.addView(signOutBtn, fullWidth());
        dialog.show();
    }

    private void navigate(String target) {
        boolean allowed = false;
        if (target.equals("notifications")) allowed = true;
        for (String[] item : navigation()) {
            if (item[0].equals(target)) allowed = true;
        }
        if (!allowed) {
            showToast("This page is not available for your role.");
            target = "dashboard";
        }
        launchScreen(target, null, target);
    }

    private void launchScreen(String page, String reportId, String originPage) {
        Class<? extends MainActivity> screenClass = screenActivity(page);
        Intent intent = new Intent(this, screenClass);
        intent.putExtra(EXTRA_ROLE, role);
        intent.putExtra(EXTRA_EMAIL, userEmail);
        intent.putExtra(EXTRA_PAGE, page);
        intent.putExtra(EXTRA_RETURN_PAGE, originPage);
        if (reportId != null) intent.putExtra(EXTRA_REPORT_ID, reportId);
        startActivity(intent);
        finish();
    }

    private Class<? extends MainActivity> screenActivity(String page) {
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

    private void showProfile() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(18), dp(20), dp(18));

        addText(box, userEmail, 16, Color.rgb(15, 23, 42), true);
        addText(box, "Current role: " + (isAdmin() ? "Admin" : "Campus User"), 13, MUTED, false);
        addSpace(box, 14);

        Button signOutBtn = new Button(this);
        signOutBtn.setText("↳  Sign out");
        signOutBtn.setAllCaps(false);
        signOutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        signOutBtn.setTextColor(Color.rgb(15, 23, 42));
        signOutBtn.setBackground(createBorderButtonDrawable(
                Color.rgb(241, 245, 249), Color.rgb(226, 232, 240), Color.rgb(203, 213, 225), 6));
        signOutBtn.setMinHeight(dp(40));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(box)
                .create();

        signOutBtn.setOnClickListener(v -> {
            dialog.dismiss();
            signOut();
        });

        box.addView(signOutBtn, fullWidth());
        dialog.show();
    }

    private void signOut() {
        preferences.edit().clear().apply();
        role = null;
        userEmail = null;
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private String pageTitle(String page) {
        if (page.equals("report-details")) return "Report details";
        for (String[] item : navigation()) if (item[0].equals(page)) return item[1];
        return "Campus safety";
    }

    private String navGlyph(String page) {
        if (page.equals("map")) return "🗺";
        if (page.equals("report")) return "＋";
        if (page.equals("reports") || page.equals("all-reports")) return "📋";
        if (page.equals("tasks")) return "⚒";
        if (page.equals("risk")) return "⚡";
        if (page.equals("notifications")) return "🔔";
        if (page.equals("settings")) return "⚙";
        if (page.equals("more")) return "≡";
        return "🏠";
    }

    private int openCount() {
        int total = 0;
        for (ReportItem item : reports) {
            if (item.status.equals("Submitted") || item.status.equals("Verified")) total++;
        }
        return total;
    }

    private int countStatus(String status) {
        int total = 0;
        for (ReportItem item : reports) if (item.status.equals(status)) total++;
        return total;
    }

    private int countHighRisk() {
        int total = 0;
        for (ReportItem item : reports) {
            if (item.risk.equals("High") && !item.status.equals("Resolved")
                    && !item.status.equals("Rejected")) total++;
        }
        return total;
    }

    private int countTasksCompleted() {
        int total = 0;
        for (ReportItem item : reports) if ("Completed".equals(item.taskStatus)) total++;
        return total;
    }

    private int countTasksAssigned() {
        int total = 0;
        for (ReportItem item : reports) if (item.taskStatus != null) total++;
        return total;
    }

    private void appendHistory(ReportItem item, String message) {
        item.addHistory(timestamp(), message);
    }

    private String timestamp() {
        java.text.SimpleDateFormat format =
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);
        format.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
        return format.format(new java.util.Date());
    }

    private String formatDate(String value) {
        return value == null || value.isEmpty() ? "Date unavailable" : value;
    }

    private void addFilterLabel(LinearLayout parent, String label) {
        TextView view = text(label, 12, MUTED, true);
        LinearLayout.LayoutParams params = fullWidth();
        params.topMargin = dp(8);
        parent.addView(view, params);
    }

    private void addActivity(LinearLayout parent, int limit) {
        List<HistoryEntry> events = new ArrayList<>();
        for (ReportItem item : reports) {
            for (HistoryEntry entry : item.history) {
                entry.reportId = item.id;
                events.add(entry);
            }
        }
        events.sort((left, right) -> right.date.compareTo(left.date));
        addText(parent, "Recent activity", 18, INK, true);
        int count = 0;
        for (HistoryEntry entry : events) {
            if (count++ >= limit) break;
            LinearLayout row = row(parent);
            TextView dot = text("●", 10, NAVY, true);
            row.addView(dot, new LinearLayout.LayoutParams(dp(24),
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            row.addView(details, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            addText(details, entry.reportId + " · " + entry.text, 13, INK, false);
            addText(details, entry.date, 11, MUTED, false);
        }
        if (events.isEmpty()) addText(parent, "No activity recorded.", 13, MUTED, false);
    }

    private boolean isAdmin() {
        return "admin".equals(role);
    }

    private int riskRank(String risk) {
        if (risk.equals("High")) return 3;
        if (risk.equals("Medium")) return 2;
        return 1;
    }

    private int riskColor(String risk) {
        if (risk.equals("High")) return DANGER;
        if (risk.equals("Medium")) return WARNING;
        return SUCCESS;
    }

    private int statusColor(String status) {
        if (status.equals("Resolved") || status.equals("Completed")) return SUCCESS;
        if (status.equals("Rejected")) return DANGER;
        if (status.equals("Submitted")) return MUTED;
        return NAVY;
    }

    private StateListDrawable createButtonDrawable(int normalColor, int hoverColor, int cornerRadiusDp) {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(normalColor);
        normal.setCornerRadius(dp(cornerRadiusDp));

        GradientDrawable hover = new GradientDrawable();
        hover.setColor(hoverColor);
        hover.setCornerRadius(dp(cornerRadiusDp));

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, hover);
        states.addState(new int[]{android.R.attr.state_hovered}, hover);
        states.addState(new int[]{android.R.attr.state_focused}, hover);
        states.addState(new int[]{}, normal);
        return states;
    }

    private StateListDrawable createBorderButtonDrawable(int normalBg, int hoverBg, int strokeColor, int cornerRadiusDp) {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(normalBg);
        normal.setCornerRadius(dp(cornerRadiusDp));
        normal.setStroke(dp(1), strokeColor);

        GradientDrawable hover = new GradientDrawable();
        hover.setColor(hoverBg);
        hover.setCornerRadius(dp(cornerRadiusDp));
        hover.setStroke(dp(1), strokeColor);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, hover);
        states.addState(new int[]{android.R.attr.state_hovered}, hover);
        states.addState(new int[]{android.R.attr.state_focused}, hover);
        states.addState(new int[]{}, normal);
        return states;
    }

    private LinearLayout card(LinearLayout parent, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(15), dp(14), dp(15), dp(14));
        if (color == WHITE) {
            box.setBackground(createBorderButtonDrawable(WHITE, Color.rgb(248, 250, 252), BORDER, 12));
        } else {
            box.setBackground(createButtonDrawable(color, Color.rgb(20, 58, 82), 12));
        }
        LinearLayout.LayoutParams params = fullWidth();
        params.bottomMargin = dp(12);
        parent.addView(box, params);
        return box;
    }

    private LinearLayout row(LinearLayout parent) {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(line, fullWidth());
        return line;
    }

    private void addStat(LinearLayout parent, int number, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(4), dp(12), dp(4), dp(12));
        box.setBackgroundResource(R.drawable.card_background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(dp(2), 0, dp(2), dp(12));
        parent.addView(box, params);
        addText(box, String.valueOf(number), 23, NAVY, true);
        addText(box, label, 10, MUTED, false);
    }

    private void addSectionTitle(LinearLayout parent, String title, String action,
                                 Runnable callback) {
        LinearLayout line = row(parent);
        TextView heading = text(title, 17, INK, true);
        line.addView(heading, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (action != null) {
            TextView link = text(action, 12, NAVY, true);
            line.addView(link);
            link.setOnClickListener(view -> callback.run());
        }
        addSpace(parent, 8);
    }

    private void addText(LinearLayout parent, String value, int size, int color, boolean bold) {
        TextView view = text(value, size, color, bold);
        LinearLayout.LayoutParams params = fullWidth();
        params.bottomMargin = dp(5);
        parent.addView(view, params);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button actionButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(WHITE);
        button.setBackgroundResource(R.drawable.primary_button_background);
        button.setMinHeight(dp(48));
        return button;
    }

    private Button smallButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(11);
        button.setTextColor(NAVY);
        button.setBackgroundResource(R.drawable.secondary_button_background);
        return button;
    }

    private Spinner spinner(String[] options) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private void addSpace(LinearLayout parent, int height) {
        View space = new View(this);
        parent.addView(space, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(height)));
    }

    private void addDivider(LinearLayout parent) {
        View line = new View(this);
        line.setBackgroundColor(BORDER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        params.topMargin = dp(6);
        params.bottomMargin = dp(8);
        parent.addView(line, params);
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (role != null && currentPage.equals("report-details")) {
            navigate(returnPage);
        } else if (role != null && !currentPage.equals("dashboard")) {
            navigate("dashboard");
        } else if (role != null) {
            signOut();
        } else {
            super.onBackPressed();
        }
    }

    private static final class ReportItem {
        final String id;
        final String place;
        final String category;
        final String type;
        final String time;
        final String risk;
        final String description;
        final String owner;
        final String createdAt;
        final List<HistoryEntry> history = new ArrayList<>();
        String status;
        int votes;
        int cleared;
        String team;
        String instructions;
        String taskStatus;
        String summary;
        String resolution;
        String resolvedAt;
        String photoUri;
        String completionPhotoUri;

        ReportItem(String id, String place, String category, String type, String status,
                   String risk, String time, int votes, int cleared, String description, String owner,
                   String createdAt, String team, String instructions, String taskStatus) {
            this.id = id;
            this.place = place;
            this.category = category;
            this.type = type;
            this.status = status;
            this.risk = risk;
            this.time = time;
            this.votes = votes;
            this.cleared = cleared;
            this.description = description;
            this.owner = owner;
            this.createdAt = createdAt;
            this.team = team;
            this.instructions = instructions;
            this.taskStatus = taskStatus;
        }

        void addHistory(String date, String text) {
            history.add(new HistoryEntry(date, text));
        }

        String latestUpdate() {
            return history.isEmpty() ? "No report history yet."
                    : history.get(history.size() - 1).text;
        }
    }

    private static final class HistoryEntry {
        final String date;
        final String text;
        String reportId;

        HistoryEntry(String date, String text) {
            this.date = date;
            this.text = text;
        }
    }

    private final class MapSketchView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final List<ReportItem> mapReports;

        MapSketchView(List<ReportItem> mapReports) {
            super(MainActivity.this);
            this.mapReports = new ArrayList<>(mapReports);
            setBackgroundColor(Color.rgb(235, 244, 246));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();

            // Canvas Border
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.rgb(203, 213, 225));
            paint.setStrokeWidth(dp(1));
            canvas.drawRoundRect(1, 1, width - 1, height - 1, dp(16), dp(16), paint);

            // Main road curve
            paint.setColor(Color.rgb(196, 218, 217));
            paint.setStrokeWidth(dp(16));
            paint.setStyle(Paint.Style.STROKE);
            Path road = new Path();
            road.moveTo(0, height * .42f);
            road.cubicTo(width * .35f, height * .28f, width * .55f, height * .68f, width, height * .45f);
            canvas.drawPath(road, paint);

            // Dashed branch road
            paint.setStrokeWidth(dp(6));
            paint.setColor(Color.rgb(180, 205, 204));
            paint.setPathEffect(new android.graphics.DashPathEffect(new float[]{dp(8), dp(6)}, 0));
            Path dashRoad = new Path();
            dashRoad.moveTo(0, height * .22f);
            dashRoad.lineTo(width * .45f, height * .52f);
            canvas.drawPath(dashRoad, paint);
            paint.setPathEffect(null);

            // Map Pins & Tooltip
            float[][] positions = {
                    {.32f, .32f}, {.65f, .42f}, {.48f, .55f}, {.25f, .72f}, {.72f, .70f}
            };

            paint.setStyle(Paint.Style.FILL);

            for (int i = 0; i < mapReports.size() && i < positions.length; i++) {
                ReportItem item = mapReports.get(i);
                float px = width * positions[i][0];
                float py = height * positions[i][1];

                // Outer glow circle
                paint.setColor(riskColor(item.risk));
                canvas.drawCircle(px, py, dp(16), paint);

                // Inner symbol drop circle
                paint.setColor(WHITE);
                canvas.drawCircle(px, py, dp(6), paint);

                // Draw tooltip label for main pin
                if (i == 0) {
                    String labelText = item.place.length() > 16 ? item.place.substring(0, 16) + "..." : item.place;
                    paint.setTextSize(dp(11));
                    paint.setTypeface(Typeface.DEFAULT_BOLD);
                    float textWidth = paint.measureText(labelText);

                    float boxWidth = textWidth + dp(20);
                    float boxHeight = dp(24);
                    float boxLeft = px - boxWidth / 2f;
                    float boxTop = py - dp(34);

                    paint.setColor(WHITE);
                    canvas.drawRoundRect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, dp(12), dp(12), paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(Color.rgb(203, 213, 225));
                    paint.setStrokeWidth(dp(1));
                    canvas.drawRoundRect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, dp(12), dp(12), paint);

                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.rgb(15, 23, 42));
                    canvas.drawText(labelText, boxLeft + dp(10), boxTop + dp(16), paint);
                }
            }

            // Map Legend overlay box at bottom left
            float legendLeft = dp(14);
            float legendBottom = height - dp(14);
            float legendWidth = dp(110);
            float legendHeight = dp(62);
            float legendTop = legendBottom - legendHeight;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(WHITE);
            canvas.drawRoundRect(legendLeft, legendTop, legendLeft + legendWidth, legendBottom, dp(10), dp(10), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.rgb(226, 232, 240));
            paint.setStrokeWidth(dp(1));
            canvas.drawRoundRect(legendLeft, legendTop, legendLeft + legendWidth, legendBottom, dp(10), dp(10), paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(dp(10));

            // Legend item 1: High risk
            paint.setColor(DANGER);
            canvas.drawCircle(legendLeft + dp(12), legendTop + dp(14), dp(4), paint);
            paint.setColor(Color.rgb(15, 23, 42));
            canvas.drawText("High risk", legendLeft + dp(22), legendTop + dp(17), paint);

            // Legend item 2: Medium risk
            paint.setColor(WARNING);
            canvas.drawCircle(legendLeft + dp(12), legendTop + dp(31), dp(4), paint);
            paint.setColor(Color.rgb(15, 23, 42));
            canvas.drawText("Medium risk", legendLeft + dp(22), legendTop + dp(34), paint);

            // Legend item 3: Low risk
            paint.setColor(SUCCESS);
            canvas.drawCircle(legendLeft + dp(12), legendTop + dp(48), dp(4), paint);
            paint.setColor(Color.rgb(15, 23, 42));
            canvas.drawText("Low risk", legendLeft + dp(22), legendTop + dp(51), paint);
        }
    }
}
