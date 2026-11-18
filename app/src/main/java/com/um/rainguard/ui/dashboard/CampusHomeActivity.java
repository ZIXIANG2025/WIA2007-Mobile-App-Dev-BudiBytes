package com.um.rainguard.ui.dashboard;

import android.view.View;

import com.um.rainguard.R;
import com.um.rainguard.data.model.Report;
import com.um.rainguard.data.model.ReportStatus;
import com.um.rainguard.databinding.ItemReportCardBinding;
import com.um.rainguard.databinding.ScreenDashboardBinding;
import com.um.rainguard.ui.base.BaseActivity;
import com.um.rainguard.utils.NavigationHelper;
import com.um.rainguard.utils.UIUtils;

import java.util.List;

public class CampusHomeActivity extends BaseActivity {
    private ScreenDashboardBinding binding;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.screen_dashboard;
    }

    @Override
    protected String getInitialPage() {
        return "home";
    }

    @Override
    protected void initView() {
        View rootView = findViewById(R.id.main_root_container);
        if (rootView == null) return;

        binding = ScreenDashboardBinding.bind(rootView);
        setupCampusHomeUI();

        observe(repository.observeReports(), this::renderReports);
        renderReports(repository.getReports());
    }

    private void setupCampusHomeUI() {
        if (binding == null) return;

        binding.dashHeroSubtitle.setText(isAdmin() ? "Campus operations overview" : "Campus safety overview");

        binding.dashSettingsBtn.setOnClickListener(v -> navigate("settings"));
        binding.dashBellBtn.setOnClickListener(v -> navigate("notifications"));

        binding.dashActionTitle.setText(isAdmin() ? "Review campus reports" : "Spotted a clogged drain?");
        binding.dashActionSubtitle.setText(isAdmin() ? "Inspect reports and coordinate maintenance" : "Report it in under 30 seconds");
        binding.dashActionCard.setOnClickListener(v -> navigate(isAdmin() ? "all-reports" : "report"));

        binding.dashViewMapLink.setText(isAdmin() ? "All reports" : "View map");
        binding.dashViewMapLink.setOnClickListener(v -> navigate(isAdmin() ? "all-reports" : "map"));
    }

    private void renderReports(List<Report> reports) {
        if (binding == null) return;

        binding.statOpenCount.setText(String.valueOf(repository.openCount()));
        binding.statProgressCount.setText(String.valueOf(repository.countStatus("In progress")));
        binding.statResolvedCount.setText(String.valueOf(repository.countStatus("Resolved")));

        binding.nearbyReportsContainer.removeAllViews();
        int shown = 0;
        for (Report item : reports) {
            if (item.status == ReportStatus.REJECTED) continue;
            addReportCard(item, !isAdmin());
            if (++shown == 3) break;
        }
    }

    private void addReportCard(Report item, boolean communityActions) {
        ItemReportCardBinding cardBinding = ItemReportCardBinding.inflate(getLayoutInflater(), binding.nearbyReportsContainer, false);

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

        if (communityActions && item.status != ReportStatus.RESOLVED && item.status != ReportStatus.REJECTED) {
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

        binding.nearbyReportsContainer.addView(cardBinding.getRoot());
    }
}
