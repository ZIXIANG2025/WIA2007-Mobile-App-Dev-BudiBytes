package com.um.rainguard.ui.riskoverview;

import android.view.View;

import com.um.rainguard.R;
import com.um.rainguard.data.model.ReportItem;
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
        if (rootView != null) {
            binding = ScreenRiskOverviewBinding.bind(rootView);
            setupRiskUI();
        }
    }

    private void setupRiskUI() {
        if (binding == null) return;

        binding.riskActiveReportsContainer.removeAllViews();

        List<ReportItem> active = new ArrayList<>(repository.getReports());
        active.removeIf(item -> "Resolved".equals(item.status) || "Rejected".equals(item.status));
        active.sort(Comparator.comparingInt(item -> riskRank(item.risk)));
        Collections.reverse(active);

        int rank = 1;
        for (ReportItem item : active) {
            addActiveRiskCard(item, rank++);
        }
    }

    private int riskRank(String risk) {
        if ("High".equals(risk)) return 3;
        if ("Medium".equals(risk)) return 2;
        return 1;
    }

    private void addActiveRiskCard(ReportItem item, int rank) {
        ItemRiskReportCardBinding cardBinding = ItemRiskReportCardBinding.inflate(getLayoutInflater(), binding.riskActiveReportsContainer, false);

        cardBinding.riskRankNum.setText(String.valueOf(rank));
        cardBinding.riskItemPlace.setText(item.place);
        cardBinding.riskItemSubtitle.setText(item.category + " · " + UIUtils.formatDate(item.createdAt) + ", 16:12");

        cardBinding.riskItemTag.setText(item.risk + " risk");
        cardBinding.riskItemTag.setTextColor(UIUtils.riskColor(item.risk));

        cardBinding.riskBtnViewDetails.setOnClickListener(v -> NavigationHelper.navigateTo(this, role, userEmail, "report-details", item.id, currentPage));
        cardBinding.getRoot().setOnClickListener(v -> NavigationHelper.navigateTo(this, role, userEmail, "report-details", item.id, currentPage));

        binding.riskActiveReportsContainer.addView(cardBinding.getRoot());
    }
}
