package com.um.rainguard;

public class SettingsActivity extends MainActivity {
    @Override
    protected int screenLayout() {
        return R.layout.screen_settings;
    }

    @Override
    protected String initialPage() {
        return "settings";
    }
}
