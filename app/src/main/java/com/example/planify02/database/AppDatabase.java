package com.example.planify02.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.planify02.daos.NoteDao;
import com.example.planify02.daos.PlanItemDao;
import com.example.planify02.entities.Note;
import com.example.planify02.entities.PlanItem;

@Database(entities = {Note.class, PlanItem.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "planify-database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return INSTANCE;
    }

    public abstract NoteDao noteDao();
    public abstract PlanItemDao planItemDao();
}