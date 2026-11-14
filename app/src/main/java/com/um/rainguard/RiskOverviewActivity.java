package com.um.rainguard;

public class RiskOverviewActivity extends MainActivity {
    @Override
    protected int screenLayout() {
        return R.layout.screen_risk_overview;
    }

    @Override
    protected String initialPage() {
        return "risk";
    }
}
