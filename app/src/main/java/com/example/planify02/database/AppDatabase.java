package com.example.planify02.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.planify02.daos.NoteDao;
import com.example.planify02.entities.Note;

@Database(entities = {Note.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract NoteDao noteDao();
}