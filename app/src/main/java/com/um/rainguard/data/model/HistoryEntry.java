package com.um.rainguard.data.model;

public class HistoryEntry {
    public final String date;
    public final String text;
    public String reportId;

    public HistoryEntry(String date, String text) {
        this.date = date;
        this.text = text;
    }
}
