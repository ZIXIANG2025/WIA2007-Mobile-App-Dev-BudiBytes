package com.um.rainguard;

public class MaintenanceTasksActivity extends MainActivity {
    @Override
    protected int screenLayout() {
        return R.layout.screen_maintenance_tasks;
    }

    @Override
    protected String initialPage() {
        return "tasks";
    }
}
