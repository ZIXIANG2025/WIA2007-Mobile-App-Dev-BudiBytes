package com.um.rainguard.data.model;

import java.util.ArrayList;
import java.util.List;

public class ReportItem {
    public final String id;
    public final String place;
    public final String category;
    public final String type;
    public final String time;
    public final String risk;
    public final String description;
    public final String owner;
    public final String createdAt;
    public final List<HistoryEntry> history = new ArrayList<>();
    public String status;
    public int votes;
    public int cleared;
    public String team;
    public String instructions;
    public String taskStatus;
    public String summary;
    public String resolution;
    public String resolvedAt;
    public String photoUri;
    public String completionPhotoUri;

    public ReportItem(String id, String place, String category, String type, String status,
                      String risk, String time, int votes, int cleared, String description, String owner,
                      String createdAt, String team, String instructions, String taskStatus) {
        this.id = id;
        this.place = place;
        this.category = category;
        this.type = type;
        this.status = status;
        this.risk = risk;
        this.time = time;
        this.votes = votes;
        this.cleared = cleared;
        this.description = description;
        this.owner = owner;
        this.createdAt = createdAt;
        this.team = team;
        this.instructions = instructions;
        this.taskStatus = taskStatus;
    }

    public void addHistory(String date, String text) {
        history.add(new HistoryEntry(date, text));
    }

    public String latestUpdate() {
        return history.isEmpty() ? "No report history yet."
                : history.get(history.size() - 1).text;
    }
}
