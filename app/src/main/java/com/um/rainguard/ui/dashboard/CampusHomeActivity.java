package com.um.rainguard.ui.dashboard;

import android.view.View;

import com.um.rainguard.R;
import com.um.rainguard.data.model.ReportItem;
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
        if (rootView != null) {
            binding = ScreenDashboardBinding.bind(rootView);
            setupCampusHomeUI();
        }
    }

    private void setupCampusHomeUI() {
        if (binding == null) return;

        binding.dashHeroSubtitle.setText(isAdmin() ? "Campus operations overview" : "Campus safety overview");

        binding.statOpenCount.setText(String.valueOf(repository.openCount()));
        binding.statProgressCount.setText(String.valueOf(repository.countStatus("In progress")));
        binding.statResolvedCount.setText(String.valueOf(repository.countStatus("Resolved")));

        binding.dashSettingsBtn.setOnClickListener(v -> navigate("settings"));
        binding.dashBellBtn.setOnClickListener(v -> navigate("notifications"));

        binding.dashActionTitle.setText(isAdmin() ? "Review campus reports" : "Spotted a clogged drain?");
        binding.dashActionSubtitle.setText(isAdmin() ? "Inspect reports and coordinate maintenance" : "Report it in under 30 seconds");
        binding.dashActionCard.setOnClickListener(v -> navigate(isAdmin() ? "all-reports" : "report"));

        binding.dashViewMapLink.setText(isAdmin() ? "All reports" : "View map");
        binding.dashViewMapLink.setOnClickListener(v -> navigate(isAdmin() ? "all-reports" : "map"));

        binding.nearbyReportsContainer.removeAllViews();
        int shown = 0;
        List<ReportItem> reports = repository.getReports();
        for (ReportItem item : reports) {
            if (!"Rejected".equals(item.status)) {
                addReportCard(item, !isAdmin());
                if (++shown == 3) break;
            }
        }
    }

    private void addReportCard(ReportItem item, boolean communityActions) {
        ItemReportCardBinding cardBinding = ItemReportCardBinding.inflate(getLayoutInflater(), binding.nearbyReportsContainer, false);

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

        if (communityActions && !"Resolved".equals(item.status) && !"Rejected".equals(item.status)) {
            cardBinding.reportActionRow.setVisibility(View.VISIBLE);
            cardBinding.btnStillThere.setText("👍 Still There (" + item.votes + ")");
            cardBinding.btnCleared.setText("✓ Cleared (" + item.cleared + ")");

            cardBinding.btnStillThere.setOnClickListener(v -> {
                item.votes++;
                repository.persistReports();
                showToast("Condition confirmed. Thank you.");
                setupCampusHomeUI();
            });

            cardBinding.btnCleared.setOnClickListener(v -> {
                item.cleared++;
                repository.persistReports();
                showToast("Cleared observation recorded.");
                setupCampusHomeUI();
            });
        } else {
            cardBinding.reportActionRow.setVisibility(View.GONE);
        }

        binding.nearbyReportsContainer.addView(cardBinding.getRoot());
    }
}
