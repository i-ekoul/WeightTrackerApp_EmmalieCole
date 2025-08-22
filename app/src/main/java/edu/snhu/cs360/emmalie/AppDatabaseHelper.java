package edu.snhu.cs360.emmalie;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite database * Schema (v2):
 *  - users   (_id INTEGER PK, username TEXT UNIQUE, password TEXT)
 *  - weights (_id INTEGER PK, user_id INTEGER, date TEXT, weight REAL)
    - keep the version bump below and let onUpgrade() rebuild the DB.
 */
public class AppDatabaseHelper extends SQLiteOpenHelper {

    // --- DB meta ---
    private static final String DB_NAME = "weight_tracker.db";
    // Bump this when schema changes (e.g., when we added the _id column)
    private static final int DB_VERSION = 2;

    // --- Tables / columns ---
    // users
    private static final String T_USERS = "users";
    private static final String C_ID = "_id";                // required for adapters/cursors
    private static final String C_USERNAME = "username";
    private static final String C_PASSWORD = "password";

    // weights
    private static final String T_WEIGHTS = "weights";
    private static final String C_USER_ID = "user_id";       // FK -> users._id
    private static final String C_DATE = "date";             // store as ISO string YYYY-MM-DD
    private static final String C_WEIGHT = "weight";         // numeric

    public AppDatabaseHelper(@NonNull Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        // Users table (username must be unique)
        db.execSQL(
                "CREATE TABLE " + T_USERS + " (" +
                        C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        C_USERNAME + " TEXT NOT NULL UNIQUE, " +
                        C_PASSWORD + " TEXT NOT NULL" +
                        ")"
        );

        // Weights table
        db.execSQL(
                "CREATE TABLE " + T_WEIGHTS + " (" +
                        C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        C_USER_ID + " INTEGER NOT NULL, " +
                        C_DATE + " TEXT NOT NULL, " +
                        C_WEIGHT + " REAL NOT NULL, " +
                        // (Optional) soft FK; SQLite enforces FK only if PRAGMA foreign_keys=ON
                        "FOREIGN KEY (" + C_USER_ID + ") REFERENCES " + T_USERS + "(" + C_ID + ")" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        // For this course project, a simple rebuild is acceptable
        db.execSQL("DROP TABLE IF EXISTS " + T_WEIGHTS);
        db.execSQL("DROP TABLE IF EXISTS " + T_USERS);
        onCreate(db);
    }

    // ---------------------------------------------------------------------
    // Login / Users
    // ---------------------------------------------------------------------

    /** Insert a new user. Returns new rowId (>0) or -1 if failed/duplicate. */
    public long createUser(@NonNull String username, @NonNull String password) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_USERNAME, username.trim());
        cv.put(C_PASSWORD, password);
        return db.insert(T_USERS, null, cv); // will return -1 on UNIQUE violation
    }

    /** Check login. Returns userId (>0) if credentials are valid; otherwise -1. */
    public long checkLogin(@NonNull String username, @NonNull String password) {
        SQLiteDatabase db = getReadableDatabase();
        long userId = -1;

        try (Cursor c = db.rawQuery(
                "SELECT " + C_ID + " FROM " + T_USERS +
                        " WHERE " + C_USERNAME + "=? AND " + C_PASSWORD + "=?",
                new String[]{username.trim(), password}
        )) {
            if (c.moveToFirst()) {
                userId = c.getLong(0);
            }
        }
        return userId;
    }

    // ---------------------------------------------------------------------
    // Weights CRUD
    // ---------------------------------------------------------------------

    /** Model for a row in the weights table. */
    public static class WeightEntry {
        public final long id;        // row _id in weights
        public final long userId;    // FK -> users._id
        public final String date;    // YYYY-MM-DD
        public final double weight;  // numeric

        public WeightEntry(long id, long userId, String date, double weight) {
            this.id = id;
            this.userId = userId;
            this.date = date;
            this.weight = weight;
        }
    }

    /** Create a new weight entry. Returns rowId (>0) or -1 on failure. */
    public long insertWeight(long userId, @NonNull String date, double weight) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_USER_ID, userId);
        cv.put(C_DATE, date);
        cv.put(C_WEIGHT, weight);
        return db.insert(T_WEIGHTS, null, cv);
    }

    /** Update an existing weight entry by its _id. Returns number of rows updated. */
    public int updateWeight(long id, @NonNull String date, double weight) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_DATE, date);
        cv.put(C_WEIGHT, weight);
        return db.update(T_WEIGHTS, cv, C_ID + "=?", new String[]{ String.valueOf(id) });
    }

    /** Delete a weight entry by its _id. Returns number of rows deleted. */
    public int deleteWeight(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(T_WEIGHTS, C_ID + "=?", new String[]{ String.valueOf(id) });
    }

    /** Read all weight entries for a user, newest date first. */
    @NonNull
    public List<WeightEntry> getWeights(long userId) {
        SQLiteDatabase db = getReadableDatabase();
        List<WeightEntry> out = new ArrayList<>();

        try (Cursor c = db.rawQuery(
                "SELECT " + C_ID + "," + C_USER_ID + "," + C_DATE + "," + C_WEIGHT +
                        " FROM " + T_WEIGHTS +
                        " WHERE " + C_USER_ID + "=? " +
                        " ORDER BY " + C_DATE + " DESC",
                new String[]{String.valueOf(userId)}
        )) {
            while (c.moveToNext()) {
                long id = c.getLong(0);
                long uid = c.getLong(1);
                String date = c.getString(2);
                double w = c.getDouble(3);
                out.add(new WeightEntry(id, uid, date, w));
            }
        }
        return out;
    }
}
