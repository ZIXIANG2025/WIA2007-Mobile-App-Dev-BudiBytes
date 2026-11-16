package com.um.rainguard.ui.notifications;

import android.view.View;

import com.um.rainguard.R;
import com.um.rainguard.data.model.ReportItem;
import com.um.rainguard.databinding.ItemNotificationCardBinding;
import com.um.rainguard.databinding.ScreenNotificationsBinding;
import com.um.rainguard.ui.base.BaseActivity;
import com.um.rainguard.utils.NavigationHelper;

import java.util.List;

public class NotificationsActivity extends BaseActivity {
    private ScreenNotificationsBinding binding;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.screen_notifications;
    }

    @Override
    protected String getInitialPage() {
        return "notifications";
    }

    @Override
    protected void initView() {
        View rootView = findViewById(R.id.main_root_container);
        if (rootView != null) {
            binding = ScreenNotificationsBinding.bind(rootView);
            setupNotificationsUI();
        }
    }

    private void setupNotificationsUI() {
        if (binding == null) return;

        binding.btnNotifPreferences.setOnClickListener(v -> showPreferencesBottomSheet());

        binding.notifReportsContainer.removeAllViews();

        List<ReportItem> reports = repository.getReports();
        for (ReportItem item : reports) {
            if (!isAdmin() && !item.owner.equalsIgnoreCase(userEmail)) continue;

            ItemNotificationCardBinding cardBinding = ItemNotificationCardBinding.inflate(getLayoutInflater(), binding.notifReportsContainer, false);
            cardBinding.notifItemTitle.setText(item.id + " · " + item.status);
            cardBinding.notifItemMessage.setText("Report " + item.id + " received.");

            cardBinding.notifBtnViewDetails.setOnClickListener(v -> NavigationHelper.navigateTo(this, role, userEmail, "report-details", item.id, currentPage));
            cardBinding.getRoot().setOnClickListener(v -> NavigationHelper.navigateTo(this, role, userEmail, "report-details", item.id, currentPage));

            binding.notifReportsContainer.addView(cardBinding.getRoot());
        }
    }
}
