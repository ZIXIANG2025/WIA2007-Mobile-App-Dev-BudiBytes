package com.um.rainguard;

public class CampusHomeActivity extends MainActivity {
    @Override
    protected int screenLayout() {
        return R.layout.screen_campus_home;
    }

    @Override
    protected String initialPage() {
        return "home";
    }
}
