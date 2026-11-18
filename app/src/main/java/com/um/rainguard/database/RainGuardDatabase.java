package com.um.rainguard.database;

import com.um.rainguard.data.model.AiTriage;
import com.um.rainguard.data.model.Alert;
import com.um.rainguard.data.model.Report;
import com.um.rainguard.data.model.User;
import com.um.rainguard.data.model.VerificationLog;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The single Room database for UM RainGuard (5 tables).
 *
 * Usage in any module:
 *   RainGuardDatabase db = RainGuardDatabase.getInstance(context);
 *   LiveData<List<Report>> mine = db.reportDao().observeByReporter(userId);   // UI thread OK
 *
 *   RainGuardDatabase.databaseExecutor.execute(() -> {                         // writes / plain reads
 *       db.reportDao().upsert(report);
 *   });
 *
 * Room stores enums by name automatically (Room 2.3+), so no TypeConverters are needed.
 * If you change ANY entity field, bump `version` and tell the team
 * (during development you can uninstall the app instead of writing a migration).
 */
@Database(
        entities = {User.class, Report.class, AiTriage.class, VerificationLog.class, Alert.class},
        version = 1,
        exportSchema = false
)
public abstract class RainGuardDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract ReportDao reportDao();
    public abstract AiTriageDao aiTriageDao();
    public abstract VerificationLogDao verificationLogDao();
    public abstract AlertDao alertDao();

    private static volatile RainGuardDatabase INSTANCE;

    /** Run every database write and every non-LiveData read on this executor, never on the UI thread. */
    public static final ExecutorService databaseExecutor = Executors.newFixedThreadPool(4);

    public static RainGuardDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (RainGuardDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    RainGuardDatabase.class,
                                    "rainguard.db")
                            .fallbackToDestructiveMigration() // dev only: wipes data when version changes
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
