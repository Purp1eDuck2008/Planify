package com.example.planify02.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.planify02.entities.PlanItem;

import java.util.List;

@Dao
public interface PlanItemDao {
    @Query("SELECT * FROM plan_items ORDER BY id DESC")
    List<PlanItem> getAll();

    @Query("SELECT * FROM plan_items WHERE repeatDays LIKE '%' || :day || '%'")
    List<PlanItem> getTasksByDay(int day);
    @Insert
    long insert(PlanItem planItem);

    @Delete
    void delete(PlanItem planItem);

    @Update
    void update(PlanItem planItem);

    @Query("SELECT * FROM plan_items ORDER BY " +
            "CASE WHEN taskType = 'Неизменные' THEN 1 " +
            "     WHEN taskType = 'Запланированные' THEN 2 " +
            "     WHEN taskType = 'Эпизодные' THEN 3 " +
            "     ELSE 4 END")
    List<PlanItem> getAllSortedByType();

}