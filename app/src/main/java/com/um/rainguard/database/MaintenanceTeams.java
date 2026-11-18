package com.um.rainguard.database;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** The 3 maintenance teams shown in the UI's "Assign Maintenance" dialog. */
public final class MaintenanceTeams {
    private MaintenanceTeams() {}

    public static final List<String> ALL = Collections.unmodifiableList(Arrays.asList(
            "JPPHB Drainage Team", "Campus Facilities Team", "Landscape & Grounds Team"));
}
