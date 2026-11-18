package com.um.rainguard.database;

import com.um.rainguard.data.model.VerificationLog;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface VerificationLogDao {
    @Insert
    void insert(VerificationLog log);

    @Insert
    void insertAll(List<VerificationLog> logs);

    @Query("SELECT * FROM verification_logs WHERE reportId = :reportId ORDER BY createdAt ASC")
    LiveData<List<VerificationLog>> observeForReport(String reportId);

    @Query("SELECT * FROM verification_logs ORDER BY createdAt DESC LIMIT :limit")
    LiveData<List<VerificationLog>> observeRecent(int limit);
}
