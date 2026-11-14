package com.um.rainguard;

public class NotificationsActivity extends MainActivity {
    @Override
    protected int screenLayout() {
        return R.layout.screen_notifications;
    }

    @Override
    protected String initialPage() {
        return "notifications";
    }
}
