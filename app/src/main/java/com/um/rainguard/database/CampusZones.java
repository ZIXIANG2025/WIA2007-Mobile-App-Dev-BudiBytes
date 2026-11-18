package com.um.rainguard.database;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Zones used by report location and alert subscriptions (UI: Notification & zone preferences). */
public final class CampusZones {
    private CampusZones() {}

    public static final List<String> ALL = Collections.unmodifiableList(Arrays.asList(
            "KK1", "KK2", "KK3", "KK4", "KK5", "KK6",
            "KK7", "KK8", "KK9", "KK10", "KK11", "KK12",
            "Faculty buildings", "Central campus"));
}
