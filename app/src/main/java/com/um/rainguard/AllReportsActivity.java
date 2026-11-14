package com.um.rainguard;

public class AllReportsActivity extends MainActivity {
    @Override
    protected int screenLayout() {
        return R.layout.screen_all_reports;
    }

    @Override
    protected String initialPage() {
        return "all-reports";
    }
}
