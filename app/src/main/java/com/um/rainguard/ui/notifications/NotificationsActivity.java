package com.um.rainguard.ui.notifications;

import android.view.View;

import com.um.rainguard.R;
import com.um.rainguard.data.model.Report;
import com.um.rainguard.databinding.ItemNotificationCardBinding;
import com.um.rainguard.databinding.ScreenNotificationsBinding;
import com.um.rainguard.ui.base.BaseActivity;
import com.um.rainguard.utils.NavigationHelper;
import com.um.rainguard.utils.UIUtils;

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
        if (rootView == null) return;

        binding = ScreenNotificationsBinding.bind(rootView);
        binding.btnNotifPreferences.setOnClickListener(v -> showPreferencesBottomSheet());

        observe(repository.observeReports(), this::renderNotifications);
        renderNotifications(repository.getReports());
    }

    private void renderNotifications(List<Report> reports) {
        if (binding == null) return;

        binding.notifReportsContainer.removeAllViews();

        for (Report item : reports) {
            // Ownership is a reporterId comparison now; the reporter's e-mail never reaches the model.
            if (!isAdmin() && !item.reporterId.equals(userId)) continue;

            String code = UIUtils.reportCode(item.reportId);
            ItemNotificationCardBinding cardBinding = ItemNotificationCardBinding.inflate(getLayoutInflater(), binding.notifReportsContainer, false);
            cardBinding.notifItemTitle.setText(code + " · " + UIUtils.statusLabel(item.status));
            cardBinding.notifItemMessage.setText(UIUtils.categoryLabel(item.category) + " at " + item.locationName + ".");

            cardBinding.notifBtnViewDetails.setOnClickListener(v -> NavigationHelper.navigateTo(
                    this, role, userEmail, "report-details", item.reportId, currentPage));
            cardBinding.getRoot().setOnClickListener(v -> NavigationHelper.navigateTo(
                    this, role, userEmail, "report-details", item.reportId, currentPage));

            binding.notifReportsContainer.addView(cardBinding.getRoot());
        }
    }
}
