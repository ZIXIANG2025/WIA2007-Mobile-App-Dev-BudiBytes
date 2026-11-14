package com.um.rainguard;

public class AnalyticsActivity extends MainActivity {
    @Override
    protected int screenLayout() {
        return R.layout.screen_analytics;
    }

    @Override
    protected String initialPage() {
        return "analytics";
    }
}
