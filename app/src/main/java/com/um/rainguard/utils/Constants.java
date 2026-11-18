package com.um.rainguard.utils;

import android.graphics.Color;

public class Constants {
    public static final int NAVY = Color.rgb(7, 89, 133);
    public static final int INK = Color.rgb(23, 43, 58);
    public static final int MUTED = Color.rgb(102, 121, 134);
    public static final int BG = Color.rgb(245, 248, 250);
    public static final int WHITE = Color.WHITE;
    public static final int BORDER = Color.rgb(217, 227, 233);
    public static final int WARNING = Color.rgb(182, 106, 8);
    public static final int DANGER = Color.rgb(180, 35, 24);
    public static final int SUCCESS = Color.rgb(21, 115, 71);

    /** Sign-in "remember me" values only — report and preference data live in Room now. */
    public static final String PREFERENCES = "rainguard-demo-session";

    public static final String EXTRA_ROLE = "screen_role";
    public static final String EXTRA_EMAIL = "screen_email";
    public static final String EXTRA_PAGE = "screen_page";
    public static final String EXTRA_REPORT_ID = "screen_report_id";
    public static final String EXTRA_RETURN_PAGE = "screen_return_page";

    public static final int REQUEST_REPORT_PHOTO = 1001;
    public static final int REQUEST_COMPLETION_PHOTO = 1002;
}
