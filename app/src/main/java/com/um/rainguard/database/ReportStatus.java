package com.um.rainguard.database;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * One status for the whole report life-cycle.
 *
 * PENDING_AI        -> just submitted, waiting for AI triage        (UI: Submitted)
 * PENDING_MODERATOR -> AI done OR AI failed/timed out; admin queue  (UI: Submitted / Under Review)
 * VERIFIED          -> admin confirmed, shown on public map         (UI: Verified)
 * IN_PROGRESS       -> maintenance team assigned and working        (UI: In progress)
 * RESOLVED          -> fixed, closed                                (UI: Resolved)
 * REJECTED          -> admin rejected (remarks required), closed    (UI: Rejected)
 */
public enum ReportStatus {
    PENDING_AI,
    PENDING_MODERATOR,
    VERIFIED,
    IN_PROGRESS,
    RESOLVED,
    REJECTED;

    /** Statuses that may appear on the PUBLIC hazard map. */
    public static final List<ReportStatus> PUBLIC_STATUSES =
            Collections.unmodifiableList(Arrays.asList(VERIFIED, IN_PROGRESS));
}
