package com.um.rainguard.data.model;

/** Outcome of one AI triage attempt. FAILED / TIMEOUT => report still goes to the moderator. */
public enum AiStatus { SUCCESS, FAILED, TIMEOUT }
