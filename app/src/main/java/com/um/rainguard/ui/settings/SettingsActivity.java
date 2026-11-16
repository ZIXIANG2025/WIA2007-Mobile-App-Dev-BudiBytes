package com.um.rainguard.ui.settings;

import android.view.View;

import com.um.rainguard.R;
import com.um.rainguard.databinding.ScreenSettingsBinding;
import com.um.rainguard.ui.base.BaseActivity;

public class SettingsActivity extends BaseActivity {
    private ScreenSettingsBinding binding;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.screen_settings;
    }

    @Override
    protected String getInitialPage() {
        return "settings";
    }

    @Override
    protected void initView() {
        View rootView = findViewById(R.id.main_root_container);
        if (rootView != null) {
            binding = ScreenSettingsBinding.bind(rootView);
            setupSettingsUI();
        }
    }

    private void setupSettingsUI() {
        if (binding == null) return;

        binding.settingsEmailValue.setText(userEmail);
        binding.settingsRoleValue.setText(isAdmin()
                ? "Admin / Maintenance Team"
                : "UM Campus User · Student, Staff or Lecturer");

        binding.btnNotificationPreferences.setOnClickListener(v -> showPreferencesBottomSheet());
        binding.btnSettingsSignOut.setOnClickListener(v -> signOut());
    }
}
