package com.um.rainguard.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface AiTriageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(AiTriage triage);

    @Query("SELECT * FROM ai_triage WHERE reportId = :reportId")
    AiTriage getByReportId(String reportId);

    @Query("SELECT * FROM ai_triage WHERE reportId = :reportId")
    LiveData<AiTriage> observeByReportId(String reportId);
}
