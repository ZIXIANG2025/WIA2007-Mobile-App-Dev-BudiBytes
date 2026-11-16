package com.um.rainguard.ui.admin;

import com.um.rainguard.ui.myreports.MyReportsActivity;

public class AllReportsActivity extends MyReportsActivity {
    @Override
    protected String getInitialPage() {
        return "all-reports";
    }

    @Override
    protected void initView() {
        isManagement = true;
        super.initView();
    }
}
