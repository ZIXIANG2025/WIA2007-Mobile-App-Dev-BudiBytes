package com.um.rainguard;

public class HazardMapActivity extends MainActivity {
    @Override
    protected int screenLayout() {
        return R.layout.screen_hazard_map;
    }

    @Override
    protected String initialPage() {
        return "map";
    }
}
