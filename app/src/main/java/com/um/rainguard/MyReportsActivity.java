package com.um.rainguard;

public class MyReportsActivity extends MainActivity {
    @Override
    protected int screenLayout() {
        return R.layout.screen_my_reports;
    }

    @Override
    protected String initialPage() {
        return "reports";
    }
}
