package com.example.planify02;

public class TimeUtils {
    public static int convertToMinutes(String time) {
        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            String minutePart = parts[1];

            int minute = Integer.parseInt(minutePart.substring(0, 2));
            String period = minutePart.substring(2).toUpperCase();

            if (period.equals("PM") && hour != 12) hour += 12;
            if (period.equals("AM") && hour == 12) hour = 0;

            return hour * 60 + minute;
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getDurationMinutes(String startTime, String endTime) {
        int start = convertToMinutes(startTime);
        int end = convertToMinutes(endTime);

        if (end < start) end += 24 * 60;

        return end - start;
    }
}