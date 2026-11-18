package com.um.rainguard.ui.dashboard;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.um.rainguard.R;
import com.um.rainguard.data.model.Report;
import com.um.rainguard.data.model.ReportStatus;
import com.um.rainguard.data.model.VerificationLog;
import com.um.rainguard.databinding.ItemActivityEntryBinding;
import com.um.rainguard.databinding.ItemReportCardBinding;
import com.um.rainguard.databinding.ItemReportTableRowBinding;
import com.um.rainguard.databinding.ScreenAdminDashboardBinding;
import com.um.rainguard.databinding.ScreenDashboardBinding;
import com.um.rainguard.ui.base.BaseActivity;
import com.um.rainguard.utils.NavigationHelper;
import com.um.rainguard.utils.UIUtils;

import java.util.List;

public class DashboardActivity extends BaseActivity {
    private ScreenDashboardBinding studentBinding;
    private ScreenAdminDashboardBinding adminBinding;

    @Override
    protected int getLayoutResourceId() {
        return isAdmin() ? R.layout.screen_admin_dashboard : R.layout.screen_dashboard;
    }

    @Override
    protected String getInitialPage() {
        return "dashboard";
    }

    @Override
    protected void initView() {
        View rootView = findViewById(R.id.main_root_container);
        if (rootView == null) return;

        if (isAdmin()) {
            adminBinding = ScreenAdminDashboardBinding.bind(rootView);
            setupAdminDashboardUI();

            observe(repository.observeReports(), reports -> {
                renderAdminSummary();
                updateAdminTable();
            });
            observe(repository.observeActivity(100), this::renderRecentActivity);

            renderAdminSummary();
            updateAdminTable();
        } else {
            studentBinding = ScreenDashboardBinding.bind(rootView);
            setupStudentDashboardUI();

            observe(repository.observeReports(), this::renderStudentReports);
            renderStudentReports(repository.getReports());
        }
    }

    // --- STUDENT DASHBOARD ---
    private void setupStudentDashboardUI() {
        if (studentBinding == null) return;

        studentBinding.dashSettingsBtn.setOnClickListener(v -> navigate("settings"));
        studentBinding.dashBellBtn.setOnClickListener(v -> navigate("notifications"));

        studentBinding.dashActionTitle.setText("Spotted a clogged drain?");
        studentBinding.dashActionSubtitle.setText("Report it in under 30 seconds");
        studentBinding.dashActionCard.setOnClickListener(v -> navigate("report"));

        studentBinding.dashViewMapLink.setOnClickListener(v -> navigate("map"));
    }

    private void renderStudentReports(List<Report> reports) {
        if (studentBinding == null) return;

        studentBinding.statOpenCount.setText(String.valueOf(repository.openCount()));
        studentBinding.statProgressCount.setText(String.valueOf(repository.countStatus("In progress")));
        studentBinding.statResolvedCount.setText(String.valueOf(repository.countStatus("Resolved")));

        studentBinding.nearbyReportsContainer.removeAllViews();
        int shown = 0;
        for (Report item : reports) {
            if (item.status == ReportStatus.REJECTED) continue;
            addReportCardToStudentContainer(item);
            if (++shown == 3) break;
        }
    }

    private void addReportCardToStudentContainer(Report item) {
        ItemReportCardBinding cardBinding = ItemReportCardBinding.inflate(getLayoutInflater(), studentBinding.nearbyReportsContainer, false);

        String status = UIUtils.statusLabel(item.status);

        cardBinding.reportDot.setTextColor(UIUtils.riskColor(item.severity));
        cardBinding.reportPlace.setText(item.locationName);
        cardBinding.reportTime.setText(UIUtils.relativeTime(item.createdAt));
        cardBinding.reportSubtitle.setText(UIUtils.categoryLabel(item.category) + " · " + UIUtils.reportCode(item.reportId));

        cardBinding.reportStatusPill.setText(status);
        cardBinding.reportStatusPill.setTextColor(UIUtils.statusColor(status));
        cardBinding.reportStatusPill.setBackground(UIUtils.createButtonDrawable(this,
                UIUtils.statusBgColor(status), UIUtils.statusBgColor(status), 8));

        cardBinding.reportConfirmedText.setText("👍 " + item.stillThereCount + " confirmed");

        cardBinding.getRoot().setOnClickListener(v -> NavigationHelper.navigateTo(
                this, role, userEmail, "report-details", item.reportId, currentPage));

        if (item.status != ReportStatus.RESOLVED && item.status != ReportStatus.REJECTED) {
            cardBinding.reportActionRow.setVisibility(View.VISIBLE);
            cardBinding.btnStillThere.setText("👍 Still There (" + item.stillThereCount + ")");
            cardBinding.btnCleared.setText("✓ Cleared (" + item.clearedCount + ")");

            cardBinding.btnStillThere.setOnClickListener(v -> {
                repository.addStillThere(item.reportId);
                showToast("Condition confirmed. Thank you.");
            });

            cardBinding.btnCleared.setOnClickListener(v -> {
                repository.addCleared(item.reportId);
                showToast("Cleared observation recorded.");
            });
        } else {
            cardBinding.reportActionRow.setVisibility(View.GONE);
        }

        studentBinding.nearbyReportsContainer.addView(cardBinding.getRoot());
    }

    // --- ADMIN DASHBOARD ---
    private void setupAdminDashboardUI() {
        if (adminBinding == null) return;

        setupSpinner(adminBinding.adminStatusSpinner, new String[]{
                "All", "Submitted", "Under Review", "Verified", "Rejected", "In progress", "Resolved"
        });
        setupSpinner(adminBinding.adminCategorySpinner, new String[]{"All", "Clogged", "Stagnant water", "Overflow", "Sediment"});
        setupSpinner(adminBinding.adminRiskSpinner, new String[]{"All", "High", "Medium", "Low"});

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateAdminTable();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        adminBinding.adminSearchInput.addTextChangedListener(textWatcher);
        adminBinding.adminDateInput.addTextChangedListener(textWatcher);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateAdminTable();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        adminBinding.adminStatusSpinner.setOnItemSelectedListener(listener);
        adminBinding.adminCategorySpinner.setOnItemSelectedListener(listener);
        adminBinding.adminRiskSpinner.setOnItemSelectedListener(listener);
    }

    private void setupSpinner(Spinner spinner, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void renderAdminSummary() {
        if (adminBinding == null) return;

        adminBinding.adminStatAllSubmitted.setText(String.valueOf(repository.countAll()));
        adminBinding.adminStatAwaitingReview.setText(String.valueOf(repository.countStatus("Under Review")));
        adminBinding.adminStatVerifiedActive.setText(String.valueOf(repository.countStatus("Verified")));
        adminBinding.adminStatHighRisk.setText(String.valueOf(repository.countHighRisk()));
        adminBinding.adminStatInProgress.setText(String.valueOf(repository.countStatus("In progress")));
        adminBinding.adminStatResolved.setText(String.valueOf(repository.countStatus("Resolved")));
    }

    private void updateAdminTable() {
        if (adminBinding == null) return;

        String query = adminBinding.adminSearchInput.getText().toString();
        String status = adminBinding.adminStatusSpinner.getSelectedItem().toString();
        String category = adminBinding.adminCategorySpinner.getSelectedItem().toString();
        String risk = adminBinding.adminRiskSpinner.getSelectedItem().toString();
        String day = adminBinding.adminDateInput.getText().toString().trim();

        List<Report> matches = repository.visibleReports(true, userId, query, status, category, risk, day);

        adminBinding.adminReportsSubtitle.setText(matches.size() + " reports · Review, verify and coordinate a response");

        adminBinding.adminTableBody.removeAllViews();
        if (matches.isEmpty()) {
            adminBinding.adminReportsEmptyText.setVisibility(View.VISIBLE);
        } else {
            adminBinding.adminReportsEmptyText.setVisibility(View.GONE);
            for (Report item : matches) {
                addAdminTableRow(item);
            }
        }
    }

    private void addAdminTableRow(Report item) {
        ItemReportTableRowBinding rowBinding = ItemReportTableRowBinding.inflate(getLayoutInflater(), adminBinding.adminTableBody, false);

        String status = UIUtils.statusLabel(item.status);
        String risk = UIUtils.riskLabel(item.severity);

        rowBinding.rowReportId.setText(UIUtils.reportCode(item.reportId));
        rowBinding.rowReportPlace.setText(item.locationName);
        rowBinding.rowReportDate.setText(UIUtils.formatStamp(item.createdAt));

        rowBinding.rowReportCategory.setText(UIUtils.categoryLabel(item.category));
        rowBinding.rowReportRisk.setText(risk + " risk");
        rowBinding.rowReportRisk.setTextColor(UIUtils.riskColor(risk));

        rowBinding.rowReportStatus.setText(status);
        rowBinding.rowReportStatus.setTextColor(UIUtils.statusColor(status));
        rowBinding.rowReportStatus.setBackground(UIUtils.createButtonDrawable(this,
                UIUtils.statusBgColor(status), UIUtils.statusBgColor(status), 6));

        rowBinding.rowBtnViewDetails.setOnClickListener(v -> NavigationHelper.navigateTo(
                this, role, userEmail, "report-details", item.reportId, currentPage));

        adminBinding.adminTableBody.addView(rowBinding.getRoot());
    }

    private void renderRecentActivity(List<VerificationLog> logs) {
        if (adminBinding == null) return;

        adminBinding.adminRecentActivityContainer.removeAllViews();
        if (logs == null || logs.isEmpty()) return;

        for (VerificationLog entry : logs) {
            ItemActivityEntryBinding entryBinding = ItemActivityEntryBinding.inflate(getLayoutInflater(), adminBinding.adminRecentActivityContainer, false);
            entryBinding.activityText.setText(UIUtils.reportCode(entry.reportId) + " · " + entry.remarks);
            entryBinding.activityDate.setText(UIUtils.formatStamp(entry.createdAt));

            adminBinding.adminRecentActivityContainer.addView(entryBinding.getRoot());
        }
    }
}
