package com.um.rainguard.database;

/** What an admin / moderator did. Every action writes one VerificationLog row. */
public enum ReviewAction { VERIFY, REJECT, ASSIGN, UPDATE_PROGRESS, RESOLVE, OVERRIDE_AI }
