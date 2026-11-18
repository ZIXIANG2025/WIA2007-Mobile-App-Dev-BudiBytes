package com.um.rainguard.database;

/** Shared by all modules. Never type categories as free strings. */
public enum ReportCategory {
    CLOGGED_DRAIN,   // UI: Clogged
    STAGNANT_WATER,  // UI: Stagnant water
    OVERFLOW,        // UI: Overflow
    SEDIMENT,        // UI: Sediment
    FLOODED_ROAD,    // from problem statement (not yet in UI)
    FALLEN_TREE,     // from problem statement (not yet in UI)
    OTHER
}
