package com.example.planify02.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "plan_items")
public class PlanItem {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String eventDate;  // Формат: "8 мая 2025"
    private String startTime;  // Формат: "8:00AM" - используется для сортировки
    private String endTime;    // Формат: "2:00PM"
    private String location;
    private String description;
    private String taskType;   // Тип задачи: "Перманентная", "Полуперманентная", "Вариативная"
    private String repeatDays; // Дни повторения через запятую: "1,3,5" (Пн,Ср,Пт)

    public PlanItem(String title, String eventDate, String startTime,
                    String endTime, String location, String description,
                    String taskType, String repeatDays) {
        this.title = title;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.description = description;
        this.taskType = taskType;
        this.repeatDays = repeatDays;
    }

    // Геттеры
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getEventDate() {
        return eventDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public String getTaskType() {
        return taskType;
    }

    public String getRepeatDays() {
        return repeatDays;
    }

    // Сеттеры
    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public void setRepeatDays(String repeatDays) {
        this.repeatDays = repeatDays;
    }

    // Дополнительные методы
    public String getFullStartDateTime() {
        return String.format("%s %s", eventDate, startTime);
    }

    public String getFullEndDateTime() {
        return String.format("%s %s", eventDate, endTime);
    }

    public String getTimeRange() {
        return String.format("%s - %s", startTime, endTime);
    }

    /**
     * Проверяет, повторяется ли задача в указанный день недели
     * @param dayNumber номер дня недели (1-7)
     * @return true если задача повторяется в этот день
     */
    public boolean repeatsOnDay(int dayNumber) {
        if (repeatDays == null || repeatDays.isEmpty()) {
            return false;
        }
        String dayStr = String.valueOf(dayNumber);
        return repeatDays.contains(dayStr);
    }

    @Override
    public String toString() {
        return "PlanItem{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", eventDate='" + eventDate + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", location='" + location + '\'' +
                ", description='" + description + '\'' +
                ", taskType='" + taskType + '\'' +
                ", repeatDays='" + repeatDays + '\'' +
                '}';
    }
}