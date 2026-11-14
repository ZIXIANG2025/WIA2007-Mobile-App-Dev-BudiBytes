package com.um.rainguard.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.um.rainguard.data.model.HistoryEntry;
import com.um.rainguard.data.model.ReportItem;
import com.um.rainguard.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ReportRepository {
    private static ReportRepository instance;
    private final SharedPreferences dataPreferences;
    private final List<ReportItem> reports = new ArrayList<>();

    private ReportRepository(Context context) {
        dataPreferences = context.getApplicationContext().getSharedPreferences(Constants.DATA_PREFERENCES, Context.MODE_PRIVATE);
        loadReports();
    }

    public static synchronized ReportRepository getInstance(Context context) {
        if (instance == null) {
            instance = new ReportRepository(context);
        }
        return instance;
    }

    public List<ReportItem> getReports() {
        return reports;
    }

    public ReportItem findReport(String id) {
        if (id == null) return null;
        for (ReportItem item : reports) {
            if (item.id.equals(id)) return item;
        }
        return null;
    }

    public void addReport(ReportItem item) {
        reports.add(0, item);
        persistReports();
    }

    public void persistReports() {
        JSONArray data = new JSONArray();
        try {
            for (ReportItem item : reports) {
                JSONObject object = new JSONObject();
                object.put("id", item.id);
                object.put("place", item.place);
                object.put("category", item.category);
                object.put("type", item.type);
                object.put("status", item.status);
                object.put("risk", item.risk);
                object.put("time", item.time);
                object.put("votes", item.votes);
                object.put("cleared", item.cleared);
                object.put("description", item.description);
                object.put("owner", item.owner);
                object.put("createdAt", item.createdAt);
                object.put("team", item.team);
                object.put("instructions", item.instructions);
                object.put("taskStatus", item.taskStatus);
                object.put("summary", item.summary);
                object.put("resolution", item.resolution);
                object.put("resolvedAt", item.resolvedAt);
                object.put("photoUri", item.photoUri);
                object.put("completionPhotoUri", item.completionPhotoUri);
                JSONArray history = new JSONArray();
                for (HistoryEntry entry : item.history) {
                    JSONObject event = new JSONObject();
                    event.put("date", entry.date);
                    event.put("text", entry.text);
                    history.put(event);
                }
                object.put("history", history);
                data.put(object);
            }
            dataPreferences.edit().putString("reports", data.toString()).apply();
        } catch (JSONException exception) {
            Log.e("UMRainGuard", "Could not save demo reports.", exception);
        }
    }

    private void seedReports() {
        reports.clear();
        reports.add(new ReportItem("RG-1042", "Faculty of Engineering, Block A", "Clogged",
                "Clogged drain · leaves", "In progress", "High", "2h ago", 14, 0,
                "Clogged observed after rainfall. Please inspect the drainage channel and nearby walkway.",
                "student@um.edu.my", "2026-10-24", "JPPHB Drainage Team",
                "Inspect the box culvert and clear fallen leaves.", "Inspection in Progress"));
        reports.add(new ReportItem("RG-1039", "KK8 walkway to bus stop", "Stagnant water",
                "Stagnant water", "Verified", "High", "5h ago", 9, 1,
                "Stagnant water observed along the walkway after rainfall.", "community@um.edu.my",
                "2026-10-23", null, null, null));
        reports.add(new ReportItem("RG-1031", "Main Library back entrance", "Overflow",
                "Overflowing drain", "Submitted", "Medium", "1d ago", 4, 0,
                "Overflowing drain observed near the back entrance.", "student@um.edu.my",
                "2026-10-22", null, null, null));
        reports.add(new ReportItem("RG-1020", "Rimba Ilmu car park", "Sediment",
                "Sediment build-up", "Resolved", "Low", "3d ago", 6, 5,
                "Sediment was building up in the drainage channel.", "community@um.edu.my",
                "2026-10-21", "Campus Facilities Team",
                "Sediment removed and drainage flow restored.", "Completed"));
        reports.add(new ReportItem("RG-1015", "Dewan Tunku Canselor side drain", "Clogged",
                "Clogged drain · litter", "Submitted", "Medium", "3d ago", 3, 0,
                "Litter is restricting water flow in the side drain.", "community@um.edu.my",
                "2026-10-20", null, null, null));
        for (ReportItem item : reports) item.addHistory(item.createdAt, "Report " + item.id + " received.");
        reports.get(3).resolution = "Sediment removed and drainage flow restored.";
        reports.get(3).resolvedAt = "2026-10-23 11:30";
    }

    private void loadReports() {
        String saved = dataPreferences.getString("reports", null);
        if (saved == null) {
            seedReports();
            persistReports();
            return;
        }
        try {
            JSONArray data = new JSONArray(saved);
            List<ReportItem> loaded = new ArrayList<>();
            for (int i = 0; i < data.length(); i++) {
                JSONObject object = data.getJSONObject(i);
                ReportItem item = new ReportItem(
                        object.getString("id"),
                        object.getString("place"),
                        object.getString("category"),
                        object.getString("type"),
                        object.getString("status"),
                        object.getString("risk"),
                        object.optString("time", ""),
                        object.optInt("votes", 0),
                        object.optInt("cleared", 0),
                        object.optString("description", ""),
                        object.optString("owner", ""),
                        object.optString("createdAt", ""),
                        object.optString("team", null),
                        object.optString("instructions", null),
                        object.optString("taskStatus", null));
                item.summary = object.optString("summary", null);
                item.resolution = object.optString("resolution", null);
                item.resolvedAt = object.optString("resolvedAt", null);
                item.photoUri = object.optString("photoUri", null);
                item.completionPhotoUri = object.optString("completionPhotoUri", null);
                JSONArray history = object.optJSONArray("history");
                if (history != null) {
                    for (int j = 0; j < history.length(); j++) {
                        JSONObject entry = history.getJSONObject(j);
                        item.history.add(new HistoryEntry(
                                entry.optString("date", ""), entry.optString("text", "")));
                    }
                }
                loaded.add(item);
            }
            reports.clear();
            reports.addAll(loaded);
        } catch (JSONException exception) {
            Log.w("UMRainGuard", "Saved demo reports were invalid; restoring sample reports.", exception);
            seedReports();
            persistReports();
        }
    }

    public List<ReportItem> visibleReports(boolean management, String userEmail, String query,
                                           String status, String category, String risk, String day) {
        List<ReportItem> result = new ArrayList<>();
        String term = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        for (ReportItem item : reports) {
            if (!management && userEmail != null && !item.owner.equalsIgnoreCase(userEmail)) continue;
            if (status != null && !status.equals("All") && !item.status.equals(status)) continue;
            if (category != null && !category.equals("All") && !item.category.equals(category)) continue;
            if (risk != null && !risk.equals("All") && !item.risk.equals(risk)) continue;
            if (day != null && !day.isEmpty() && !item.createdAt.startsWith(day)) continue;
            String content = (item.id + " " + item.place + " " + item.description).toLowerCase(Locale.ROOT);
            if (content.contains(term)) result.add(item);
        }
        return result;
    }

    public int openCount() {
        int total = 0;
        for (ReportItem item : reports) {
            if (item.status.equals("Submitted") || item.status.equals("Verified")) total++;
        }
        return total;
    }

    public int countStatus(String status) {
        int total = 0;
        for (ReportItem item : reports) {
            if (item.status.equals(status)) total++;
        }
        return total;
    }

    public int countHighRisk() {
        int total = 0;
        for (ReportItem item : reports) {
            if (item.risk.equals("High") && !item.status.equals("Resolved") && !item.status.equals("Rejected")) total++;
        }
        return total;
    }

    public int countTasksCompleted() {
        int total = 0;
        for (ReportItem item : reports) {
            if ("Completed".equals(item.taskStatus)) total++;
        }
        return total;
    }

    public int countTasksAssigned() {
        int total = 0;
        for (ReportItem item : reports) {
            if (item.taskStatus != null) total++;
        }
        return total;
    }
}
