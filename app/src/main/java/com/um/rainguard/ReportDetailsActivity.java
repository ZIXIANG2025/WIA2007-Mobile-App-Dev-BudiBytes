package com.um.rainguard;

public class ReportDetailsActivity extends MainActivity {
    @Override
    protected int screenLayout() {
        return R.layout.screen_report_details;
    }

    @Override
    protected String initialPage() {
        return "report-details";
    }
}
