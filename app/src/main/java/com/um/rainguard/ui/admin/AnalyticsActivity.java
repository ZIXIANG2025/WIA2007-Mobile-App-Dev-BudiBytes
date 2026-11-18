package com.um.rainguard.ui.admin;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.um.rainguard.R;
import com.um.rainguard.data.model.VerificationLog;
import com.um.rainguard.databinding.ItemActivityEntryBinding;
import com.um.rainguard.databinding.ScreenAnalyticsBinding;
import com.um.rainguard.ui.base.BaseActivity;
import com.um.rainguard.utils.Constants;
import com.um.rainguard.utils.UIUtils;

import java.util.List;

public class AnalyticsActivity extends BaseActivity {
    private ScreenAnalyticsBinding binding;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.screen_analytics;
    }

    @Override
    protected String getInitialPage() {
        return "analytics";
    }

    @Override
    protected void initView() {
        View rootView = findViewById(R.id.main_root_container);
        if (rootView == null) return;

        binding = ScreenAnalyticsBinding.bind(rootView);

        observe(repository.observeReports(), reports -> {
            setupStatusBreakdown();
            binding.analyticsTasksSummaryText.setText("Maintenance completed: "
                    + repository.countTasksCompleted() + " / " + repository.countTasksAssigned() + " tasks");
        });
        observe(repository.observeActivity(100), this::renderRecentActivity);

        setupStatusBreakdown();
        binding.analyticsTasksSummaryText.setText("Maintenance completed: "
                + repository.countTasksCompleted() + " / " + repository.countTasksAssigned() + " tasks");
    }

    private void setupStatusBreakdown() {
        if (binding == null) return;

        binding.analyticsStatusContainer.removeAllViews();

        String[] statuses = {"Submitted", "Under Review", "Verified", "In progress", "Resolved", "Rejected"};
        int totalReports = Math.max(1, repository.countAll());

        for (String status : statuses) {
            int count = repository.countStatus(status);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, UIUtils.dp(this, 5), 0, UIUtils.dp(this, 5));

            TextView label = new TextView(this);
            label.setText(status);
            label.setTextSize(13);
            label.setTextColor(Constants.INK);
            row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            TextView countTv = new TextView(this);
            countTv.setText(String.valueOf(count));
            countTv.setTextSize(13);
            countTv.setTextColor(Constants.MUTED);
            countTv.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(countTv);

            binding.analyticsStatusContainer.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(totalReports);
            progress.setProgress(count);
            progress.setProgressTintList(android.content.res.ColorStateList.valueOf(Constants.NAVY));
            progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Constants.BORDER));

            binding.analyticsStatusContainer.addView(progress, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, UIUtils.dp(this, 7)));
        }
    }

    private void renderRecentActivity(List<VerificationLog> logs) {
        if (binding == null) return;

        binding.analyticsActivityContainer.removeAllViews();

        if (logs == null || logs.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No activity recorded.");
            emptyView.setTextColor(Constants.MUTED);
            emptyView.setTextSize(13);
            binding.analyticsActivityContainer.addView(emptyView);
            return;
        }

        for (VerificationLog entry : logs) {
            ItemActivityEntryBinding entryBinding = ItemActivityEntryBinding.inflate(getLayoutInflater(), binding.analyticsActivityContainer, false);
            entryBinding.activityText.setText(UIUtils.reportCode(entry.reportId) + " · " + entry.remarks);
            entryBinding.activityDate.setText(UIUtils.formatStamp(entry.createdAt));

            binding.analyticsActivityContainer.addView(entryBinding.getRoot());
        }
    }
}
