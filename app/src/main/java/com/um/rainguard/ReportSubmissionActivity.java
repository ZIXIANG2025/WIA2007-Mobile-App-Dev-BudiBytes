package com.um.rainguard;

public class ReportSubmissionActivity extends MainActivity {
    @Override
    protected int screenLayout() {
        return R.layout.screen_report_submission;
    }

    @Override
    protected String initialPage() {
        return "report";
    }
}
