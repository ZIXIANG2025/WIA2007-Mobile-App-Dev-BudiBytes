package com.um.rainguard.database;

import com.um.rainguard.data.model.HazardPublic;
import com.um.rainguard.data.model.Report;
import com.um.rainguard.data.model.ReportStatus;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Upsert;

import java.util.List;

@Dao
public interface ReportDao {

    /** The ONLY columns the public map may read. reporterId is deliberately not here. */
    String PUBLIC_COLUMNS = "reportId, category, severity, description, photoUri, latitude, longitude, "
            + "locationName, zone, status, createdAt, stillThereCount, clearedCount";

    /**
     * Insert a new report, or update the existing row with the same reportId.
     *
     * <p>This is {@code @Upsert}, not {@code INSERT OR REPLACE}: REPLACE deletes the conflicting
     * row first, and because ai_triage and verification_logs cascade on delete that would wipe the
     * report's AI row and its whole audit trail.
     */
    @Upsert
    void upsert(Report report);

    @Update
    void update(Report report);

    @Query("SELECT * FROM reports WHERE reportId = :reportId")
    Report getById(String reportId);

    @Query("SELECT * FROM reports WHERE reportId = :reportId")
    LiveData<Report> observeById(String reportId);

    @Query("SELECT COUNT(*) FROM reports")
    int count();

    // Module 1: "Get My Reports"
    @Query("SELECT * FROM reports WHERE reporterId = :userId ORDER BY createdAt DESC")
    LiveData<List<Report>> observeByReporter(String userId);

    // Module 2: public map. Selects ONLY public columns, so reporterId can never leak.
    @Query("SELECT " + PUBLIC_COLUMNS + " FROM reports "
            + "WHERE status IN ('VERIFIED','IN_PROGRESS') ORDER BY createdAt DESC")
    LiveData<List<HazardPublic>> observePublicHazards();

    @Query("SELECT " + PUBLIC_COLUMNS + " FROM reports "
            + "WHERE reportId = :reportId AND status IN ('VERIFIED','IN_PROGRESS')")
    LiveData<HazardPublic> observePublicHazard(String reportId);

    @Query("SELECT " + PUBLIC_COLUMNS + " FROM reports "
            + "WHERE reportId = :reportId AND status IN ('VERIFIED','IN_PROGRESS')")
    HazardPublic getPublicHazard(String reportId);

    // Module 4: admin queue + "All Reports"
    @Query("SELECT * FROM reports WHERE status = 'PENDING_MODERATOR' ORDER BY createdAt ASC")
    LiveData<List<Report>> observeReviewQueue();

    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    LiveData<List<Report>> observeAll();

    @Query("UPDATE reports SET status = :status, updatedAt = :now WHERE reportId = :reportId")
    void updateStatus(String reportId, ReportStatus status, long now);

    @Query("UPDATE reports SET updatedAt = :now WHERE reportId = :reportId")
    void touch(String reportId, long now);

    // Community confirmation (UI: Still There / Cleared)
    @Query("UPDATE reports SET stillThereCount = stillThereCount + 1 WHERE reportId = :reportId")
    void addStillThere(String reportId);

    @Query("UPDATE reports SET clearedCount = clearedCount + 1 WHERE reportId = :reportId")
    void addCleared(String reportId);
}
