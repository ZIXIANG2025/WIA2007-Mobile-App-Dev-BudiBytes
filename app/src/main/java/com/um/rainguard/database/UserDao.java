package com.um.rainguard.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * NOTE for all DAOs: methods that return plain objects or return void must be called
 * on a background thread (use RainGuardDatabase.databaseExecutor). Methods that return
 * LiveData may be observed from the UI thread.
 */
@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(User user);

    @Query("SELECT * FROM users WHERE userId = :userId")
    User getById(String userId);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getByEmail(String email);
}
