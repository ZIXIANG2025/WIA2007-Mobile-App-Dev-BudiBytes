package com.um.rainguard.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(Alert alert);

    @Query("SELECT * FROM alerts WHERE alertId = :alertId")
    Alert getById(String alertId);

    @Query("SELECT * FROM alerts WHERE isActive = 1 ORDER BY createdAt DESC")
    LiveData<List<Alert>> observeActive();

    @Query("UPDATE alerts SET isActive = 0 WHERE alertId = :alertId")
    void deactivate(String alertId);
}
