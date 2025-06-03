package com.example.planify02.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "plan_items")
public class PlanItem {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String eventDate;
    private String startTime;
    private String endTime;
    private String location;
    private String description;
    private String taskType;
    private String repeatDays;
    private boolean notificationsEnabled;
    private int reminderMinutesBefore;

    public PlanItem(String title, String eventDate, String startTime,
                    String endTime, String location, String description,
                    String taskType, String repeatDays,
                    boolean notificationsEnabled, int reminderMinutesBefore) {
        this.title = title;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.description = description;
        this.taskType = taskType;
        this.repeatDays = repeatDays;
        this.notificationsEnabled = notificationsEnabled;
        this.reminderMinutesBefore = reminderMinutesBefore;
    }


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

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public int getReminderMinutesBefore() {
        return reminderMinutesBefore;
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

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public void setReminderMinutesBefore(int reminderMinutesBefore) {
        this.reminderMinutesBefore = reminderMinutesBefore;
    }

    public String getFullStartDateTime() {
        return String.format("%s %s", eventDate, startTime);
    }

    public String getFullEndDateTime() {
        return String.format("%s %s", eventDate, endTime);
    }

    public String getTimeRange() {
        return String.format("%s - %s", startTime, endTime);
    }


    public boolean repeatsOnDay(int dayNumber) {
        if (repeatDays == null || repeatDays.isEmpty()) {
            return false;
        }
        String[] days = repeatDays.split(",");
        for (String day : days) {
            if (day.trim().equals(String.valueOf(dayNumber))) {
                return true;
            }
        }
        return false;
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
                ", notificationsEnabled=" + notificationsEnabled +
                ", reminderMinutesBefore=" + reminderMinutesBefore +
                '}';
    }
}