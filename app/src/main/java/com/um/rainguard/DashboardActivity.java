package com.um.rainguard;

public class DashboardActivity extends MainActivity {
    @Override
    protected int screenLayout() {
        return R.layout.screen_dashboard;
    }

    @Override
    protected String initialPage() {
        return "dashboard";
    }
}
