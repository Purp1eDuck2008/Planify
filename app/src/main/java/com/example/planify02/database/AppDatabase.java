package com.example.planify02.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.planify02.daos.NoteDao;
import com.example.planify02.daos.PlanItemDao;
import com.example.planify02.entities.Note;
import com.example.planify02.entities.PlanItem;

@Database(entities = {Note.class, PlanItem.class}, version = 3) // Увеличьте версию
public abstract class AppDatabase extends RoomDatabase {
    public abstract NoteDao noteDao();
    public abstract PlanItemDao planItemDao();
}