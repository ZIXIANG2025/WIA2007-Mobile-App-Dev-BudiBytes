package com.um.rainguard.data.model;

/** Maintenance task progress (admin side). Null on Report until a team is assigned. */
public enum TaskStatus {
    AWAITING_ASSIGNMENT,
    ASSIGNED,
    INSPECTION_IN_PROGRESS,
    REPAIR_IN_PROGRESS,
    COMPLETED
}
