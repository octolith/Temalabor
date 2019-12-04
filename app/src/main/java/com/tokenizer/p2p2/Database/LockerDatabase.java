package com.tokenizer.p2p2.Database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.tokenizer.p2p2.Model.Locker;

@Database(entities = Locker.class, version = 1, exportSchema = false)
public abstract class LockerDatabase extends RoomDatabase {
    private static final String DB_NAME = "locker_db";
    private static LockerDatabase instance;

    public static synchronized LockerDatabase getInstance(Context context) {
        if(instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), LockerDatabase.class, DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public abstract LockerDao lockerDao();
}