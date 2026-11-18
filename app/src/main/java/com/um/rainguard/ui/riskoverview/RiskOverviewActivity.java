package com.um.rainguard.ui.riskoverview;

import android.view.View;

import com.um.rainguard.R;
import com.um.rainguard.data.model.Report;
import com.um.rainguard.data.model.ReportStatus;
import com.um.rainguard.data.model.Severity;
import com.um.rainguard.databinding.ItemRiskReportCardBinding;
import com.um.rainguard.databinding.ScreenRiskOverviewBinding;
import com.um.rainguard.ui.base.BaseActivity;
import com.um.rainguard.utils.NavigationHelper;
import com.um.rainguard.utils.UIUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RiskOverviewActivity extends BaseActivity {
    private ScreenRiskOverviewBinding binding;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.screen_risk_overview;
    }

    @Override
    protected String getInitialPage() {
        return "risk";
    }

    @Override
    protected void initView() {
        View rootView = findViewById(R.id.main_root_container);
        if (rootView == null) return;

        binding = ScreenRiskOverviewBinding.bind(rootView);

        observe(repository.observeReports(), this::renderRisks);
        renderRisks(repository.getReports());
    }

    private void renderRisks(List<Report> reports) {
        if (binding == null) return;

        binding.riskActiveReportsContainer.removeAllViews();

        List<Report> active = new ArrayList<>(reports);
        active.removeIf(item -> item.status == ReportStatus.RESOLVED || item.status == ReportStatus.REJECTED);
        active.sort(Comparator.comparingInt(item -> riskRank(item.severity)));
        Collections.reverse(active);

        int rank = 1;
        for (Report item : active) {
            addActiveRiskCard(item, rank++);
        }
    }

    private int riskRank(Severity severity) {
        if (severity == Severity.HIGH) return 3;
        if (severity == Severity.MEDIUM) return 2;
        return 1;
    }

    private void addActiveRiskCard(Report item, int rank) {
        ItemRiskReportCardBinding cardBinding = ItemRiskReportCardBinding.inflate(getLayoutInflater(), binding.riskActiveReportsContainer, false);
        String risk = UIUtils.riskLabel(item.severity);

        cardBinding.riskRankNum.setText(String.valueOf(rank));
        cardBinding.riskItemPlace.setText(item.locationName);
        cardBinding.riskItemSubtitle.setText(UIUtils.categoryLabel(item.category) + " · " + UIUtils.formatStamp(item.createdAt));

        cardBinding.riskItemTag.setText(risk + " risk");
        cardBinding.riskItemTag.setTextColor(UIUtils.riskColor(risk));

        cardBinding.riskBtnViewDetails.setOnClickListener(v -> NavigationHelper.navigateTo(
                this, role, userEmail, "report-details", item.reportId, currentPage));
        cardBinding.getRoot().setOnClickListener(v -> NavigationHelper.navigateTo(
                this, role, userEmail, "report-details", item.reportId, currentPage));

        binding.riskActiveReportsContainer.addView(cardBinding.getRoot());
    }
}
