package com.um.rainguard.ui.dashboard;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.um.rainguard.R;
import com.um.rainguard.data.model.HistoryEntry;
import com.um.rainguard.data.model.ReportItem;
import com.um.rainguard.databinding.ItemActivityEntryBinding;
import com.um.rainguard.databinding.ItemReportCardBinding;
import com.um.rainguard.databinding.ItemReportTableRowBinding;
import com.um.rainguard.databinding.ScreenAdminDashboardBinding;
import com.um.rainguard.databinding.ScreenDashboardBinding;
import com.um.rainguard.ui.base.BaseActivity;
import com.um.rainguard.utils.NavigationHelper;
import com.um.rainguard.utils.UIUtils;

import java.util.ArrayList;
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
        if (rootView != null) {
            if (isAdmin()) {
                adminBinding = ScreenAdminDashboardBinding.bind(rootView);
                setupAdminDashboardUI();
            } else {
                studentBinding = ScreenDashboardBinding.bind(rootView);
                setupStudentDashboardUI();
            }
        }
    }

    // --- STUDENT DASHBOARD ---
    private void setupStudentDashboardUI() {
        if (studentBinding == null) return;

        studentBinding.statOpenCount.setText(String.valueOf(repository.openCount()));
        studentBinding.statProgressCount.setText(String.valueOf(repository.countStatus("In progress")));
        studentBinding.statResolvedCount.setText(String.valueOf(repository.countStatus("Resolved")));

        studentBinding.dashSettingsBtn.setOnClickListener(v -> navigate("settings"));
        studentBinding.dashBellBtn.setOnClickListener(v -> navigate("notifications"));

        studentBinding.dashActionTitle.setText("Spotted a clogged drain?");
        studentBinding.dashActionSubtitle.setText("Report it in under 30 seconds");
        studentBinding.dashActionCard.setOnClickListener(v -> navigate("report"));

        studentBinding.dashViewMapLink.setOnClickListener(v -> navigate("map"));

        studentBinding.nearbyReportsContainer.removeAllViews();
        int shown = 0;
        List<ReportItem> reports = repository.getReports();
        for (ReportItem item : reports) {
            if (!"Rejected".equals(item.status)) {
                addReportCardToStudentContainer(item);
                if (++shown == 3) break;
            }
        }
    }

    private void addReportCardToStudentContainer(ReportItem item) {
        ItemReportCardBinding cardBinding = ItemReportCardBinding.inflate(getLayoutInflater(), studentBinding.nearbyReportsContainer, false);

        cardBinding.reportDot.setTextColor(UIUtils.riskColor(item.risk));
        cardBinding.reportPlace.setText(item.place);
        cardBinding.reportTime.setText(item.time);
        cardBinding.reportSubtitle.setText(item.type + " · " + item.id);

        cardBinding.reportStatusPill.setText(item.status);
        cardBinding.reportStatusPill.setTextColor(UIUtils.statusColor(item.status));
        cardBinding.reportStatusPill.setBackground(UIUtils.createButtonDrawable(this,
                UIUtils.statusBgColor(item.status), UIUtils.statusBgColor(item.status), 8));

        cardBinding.reportConfirmedText.setText("👍 " + item.votes + " confirmed");

        cardBinding.getRoot().setOnClickListener(v -> NavigationHelper.navigateTo(this, role, userEmail, "report-details", item.id, currentPage));

        if (!"Resolved".equals(item.status) && !"Rejected".equals(item.status)) {
            cardBinding.reportActionRow.setVisibility(View.VISIBLE);
            cardBinding.btnStillThere.setText("👍 Still There (" + item.votes + ")");
            cardBinding.btnCleared.setText("✓ Cleared (" + item.cleared + ")");

            cardBinding.btnStillThere.setOnClickListener(v -> {
                item.votes++;
                repository.persistReports();
                showToast("Condition confirmed. Thank you.");
                setupStudentDashboardUI();
            });

            cardBinding.btnCleared.setOnClickListener(v -> {
                item.cleared++;
                repository.persistReports();
                showToast("Cleared observation recorded.");
                setupStudentDashboardUI();
            });
        } else {
            cardBinding.reportActionRow.setVisibility(View.GONE);
        }

        studentBinding.nearbyReportsContainer.addView(cardBinding.getRoot());
    }

    // --- ADMIN DASHBOARD ---
    private void setupAdminDashboardUI() {
        if (adminBinding == null) return;

        adminBinding.adminStatAllSubmitted.setText(String.valueOf(repository.getReports().size()));
        adminBinding.adminStatAwaitingReview.setText(String.valueOf(repository.countStatus("Submitted")));
        adminBinding.adminStatVerifiedActive.setText(String.valueOf(repository.countStatus("Verified")));
        adminBinding.adminStatHighRisk.setText(String.valueOf(repository.countHighRisk()));
        adminBinding.adminStatInProgress.setText(String.valueOf(repository.countStatus("In progress")));
        adminBinding.adminStatResolved.setText(String.valueOf(repository.countStatus("Resolved")));

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

        updateAdminTable();
        setupAdminRecentActivity();
    }

    private void setupSpinner(Spinner spinner, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void updateAdminTable() {
        if (adminBinding == null) return;

        String query = adminBinding.adminSearchInput.getText().toString();
        String status = adminBinding.adminStatusSpinner.getSelectedItem().toString();
        String category = adminBinding.adminCategorySpinner.getSelectedItem().toString();
        String risk = adminBinding.adminRiskSpinner.getSelectedItem().toString();
        String day = adminBinding.adminDateInput.getText().toString().trim();

        List<ReportItem> matches = repository.visibleReports(true, userEmail, query, status, category, risk, day);

        adminBinding.adminReportsSubtitle.setText(matches.size() + " reports · Review, verify and coordinate a response");

        adminBinding.adminTableBody.removeAllViews();
        if (matches.isEmpty()) {
            adminBinding.adminReportsEmptyText.setVisibility(View.VISIBLE);
        } else {
            adminBinding.adminReportsEmptyText.setVisibility(View.GONE);
            for (ReportItem item : matches) {
                addAdminTableRow(item);
            }
        }
    }

    private void addAdminTableRow(ReportItem item) {
        ItemReportTableRowBinding rowBinding = ItemReportTableRowBinding.inflate(getLayoutInflater(), adminBinding.adminTableBody, false);

        rowBinding.rowReportId.setText(item.id);
        rowBinding.rowReportPlace.setText(item.place);
        rowBinding.rowReportDate.setText(UIUtils.formatDate(item.createdAt) + ", 16:12");

        rowBinding.rowReportCategory.setText(item.category);
        rowBinding.rowReportRisk.setText(item.risk + " risk");
        rowBinding.rowReportRisk.setTextColor(UIUtils.riskColor(item.risk));

        rowBinding.rowReportStatus.setText(item.status);
        rowBinding.rowReportStatus.setTextColor(UIUtils.statusColor(item.status));
        rowBinding.rowReportStatus.setBackground(UIUtils.createButtonDrawable(this,
                UIUtils.statusBgColor(item.status), UIUtils.statusBgColor(item.status), 6));

        rowBinding.rowBtnViewDetails.setOnClickListener(v -> NavigationHelper.navigateTo(this, role, userEmail, "report-details", item.id, currentPage));

        adminBinding.adminTableBody.addView(rowBinding.getRoot());
    }

    private void setupAdminRecentActivity() {
        if (adminBinding == null) return;

        adminBinding.adminRecentActivityContainer.removeAllViews();

        List<HistoryEntry> events = new ArrayList<>();
        for (ReportItem item : repository.getReports()) {
            for (HistoryEntry entry : item.history) {
                entry.reportId = item.id;
                events.add(entry);
            }
        }
        events.sort((left, right) -> right.date.compareTo(left.date));

        int count = 0;
        int limit = 100;
        for (HistoryEntry entry : events) {
            if (count++ >= limit) break;

            ItemActivityEntryBinding entryBinding = ItemActivityEntryBinding.inflate(getLayoutInflater(), adminBinding.adminRecentActivityContainer, false);
            entryBinding.activityText.setText(entry.reportId + " · " + entry.text);
            entryBinding.activityDate.setText(entry.date);

            adminBinding.adminRecentActivityContainer.addView(entryBinding.getRoot());
        }
    }
}
