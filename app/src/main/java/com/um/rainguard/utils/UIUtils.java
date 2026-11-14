package com.um.rainguard.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class UIUtils {

    public static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static int riskColor(String risk) {
        if ("High".equals(risk)) return Constants.DANGER;
        if ("Medium".equals(risk)) return Constants.WARNING;
        return Constants.SUCCESS;
    }

    public static int statusColor(String status) {
        if ("Resolved".equals(status) || "Completed".equals(status)) return Constants.SUCCESS;
        if ("Rejected".equals(status)) return Constants.DANGER;
        if ("Submitted".equals(status)) return Constants.MUTED;
        return Constants.NAVY;
    }

    public static int statusBgColor(String status) {
        if ("In progress".equals(status)) return Color.rgb(254, 243, 199);
        if ("Resolved".equals(status) || "Completed".equals(status)) return Color.rgb(240, 253, 244);
        if ("Rejected".equals(status)) return Color.rgb(254, 226, 226);
        return Color.rgb(239, 246, 255);
    }

    public static StateListDrawable createButtonDrawable(Context context, int normalColor, int hoverColor, int cornerRadiusDp) {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(normalColor);
        normal.setCornerRadius(dp(context, cornerRadiusDp));

        GradientDrawable hover = new GradientDrawable();
        hover.setColor(hoverColor);
        hover.setCornerRadius(dp(context, cornerRadiusDp));

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, hover);
        states.addState(new int[]{android.R.attr.state_hovered}, hover);
        states.addState(new int[]{android.R.attr.state_focused}, hover);
        states.addState(new int[]{}, normal);
        return states;
    }

    public static StateListDrawable createBorderButtonDrawable(Context context, int normalBg, int hoverBg, int strokeColor, int cornerRadiusDp) {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(normalBg);
        normal.setCornerRadius(dp(context, cornerRadiusDp));
        normal.setStroke(dp(context, 1), strokeColor);

        GradientDrawable hover = new GradientDrawable();
        hover.setColor(hoverBg);
        hover.setCornerRadius(dp(context, cornerRadiusDp));
        hover.setStroke(dp(context, 1), strokeColor);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, hover);
        states.addState(new int[]{android.R.attr.state_hovered}, hover);
        states.addState(new int[]{android.R.attr.state_focused}, hover);
        states.addState(new int[]{}, normal);
        return states;
    }

    public static String timestamp() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
        return format.format(new Date());
    }

    public static String formatDate(String value) {
        return value == null || value.isEmpty() ? "Date unavailable" : value;
    }
}
