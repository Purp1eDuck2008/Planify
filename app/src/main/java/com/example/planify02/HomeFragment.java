package com.example.planify02;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.planify02.database.AppDatabase;
import com.example.planify02.daos.PlanItemDao;
import com.example.planify02.entities.PlanItem;
import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private AppDatabase db;
    private PlanItemDao planItemDao;
    private FrameLayout tasksContainer;
    private MaterialButton[] dayButtons;
    private Calendar selectedDate = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy", new Locale("ru"));

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = AppDatabase.getInstance(requireContext());
        planItemDao = db.planItemDao();

        tasksContainer = view.findViewById(R.id.tasks_container);
        setupDayButtons(view);
        updateDaySelection();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTasksForSelectedDate();
    }

    private void setupDayButtons(View view) {
        dayButtons = new MaterialButton[]{
                view.findViewById(R.id.btn_monday),
                view.findViewById(R.id.btn_tuesday),
                view.findViewById(R.id.btn_wednesday),
                view.findViewById(R.id.btn_thursday),
                view.findViewById(R.id.btn_friday),
                view.findViewById(R.id.btn_saturday),
                view.findViewById(R.id.btn_sunday)
        };

        for (int i = 0; i < dayButtons.length; i++) {
            final int dayOfWeek = i + 2;
            dayButtons[i].setOnClickListener(v -> {
                updateSelectedDate(dayOfWeek);
                loadTasksForSelectedDate();
            });
        }
    }

    private void updateSelectedDate(int targetDayOfWeek) {
        Calendar cal = Calendar.getInstance();
        int currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int diff = targetDayOfWeek - currentDayOfWeek;
        if (diff <= 0) diff += 7;
        cal.add(Calendar.DAY_OF_MONTH, diff);
        selectedDate = cal;
        updateDaySelection();
    }

    private void updateDaySelection() {
        int currentDayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK);
        int buttonIndex = (currentDayOfWeek + 5) % 7;

        for (int i = 0; i < dayButtons.length; i++) {
            if (i == buttonIndex) {
                dayButtons[i].setBackgroundColor(getResources().getColor(R.color.white));
                dayButtons[i].setTextColor(getResources().getColor(R.color.divider_line));
            } else {
                dayButtons[i].setBackgroundColor(getResources().getColor(android.R.color.transparent));
                dayButtons[i].setTextColor(getResources().getColor(R.color.white));
            }
        }
    }

    public void loadTasksForSelectedDate() {
        if (tasksContainer == null) return;

        tasksContainer.removeAllViews();
        Log.d("HomeFragment", "Loading tasks for: " + dateFormat.format(selectedDate.getTime()));

        new Thread(() -> {
            try {
                String currentDateStr = dateFormat.format(selectedDate.getTime());
                int currentDayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK);

                List<PlanItem> allTasks = planItemDao.getAll();
                Log.d("HomeFragment", "Total tasks in DB: " + allTasks.size());

                requireActivity().runOnUiThread(() -> {
                    int displayedTasks = 0;
                    for (PlanItem task : allTasks) {
                        try {
                            if (isTaskForSelectedDate(task, currentDateStr, currentDayOfWeek)) {
                                addTaskToTimeline(task);
                                displayedTasks++;
                            }
                        } catch (ParseException e) {
                            Log.e("HomeFragment", "Error parsing task date", e);
                        }
                    }
                    Log.d("HomeFragment", displayedTasks + " tasks displayed");
                    if (displayedTasks == 0 && isAdded()) {
                        Toast.makeText(requireContext(), "Нет задач на выбранную дату", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e("HomeFragment", "Error loading tasks", e);
            }
        }).start();
    }

    private boolean isTaskForSelectedDate(PlanItem task, String currentDateStr, int currentDayOfWeek)
            throws ParseException {
        if (task.getEventDate().equals(currentDateStr)) {
            return true;
        }

        if (task.getRepeatDays() != null && !task.getRepeatDays().isEmpty()) {
            String[] repeatDays = task.getRepeatDays().split(",");
            for (String day : repeatDays) {
                if (day.trim().equals(String.valueOf(currentDayOfWeek))) {
                    Date taskDate = dateFormat.parse(task.getEventDate());
                    return !selectedDate.getTime().before(taskDate);
                }
            }
        }
        return false;
    }

    private void addTaskToTimeline(PlanItem task) {
        if (tasksContainer == null || !isAdded()) return;

        View taskView = LayoutInflater.from(requireContext())
                .inflate(R.layout.task_timeline_item, tasksContainer, false);

        TextView tvTitle = taskView.findViewById(R.id.task_title);
        tvTitle.setText(task.getTitle());

        try {
            int startMinutes = parseTimeToMinutes(task.getStartTime());
            int endMinutes = parseTimeToMinutes(task.getEndTime());

            int top = (int) (startMinutes * 1.333f);
            int height = (int) ((endMinutes - startMinutes) * 1.333f);
            height = Math.max(height, 48); // Минимальная высота

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    height
            );
            params.topMargin = top;
            params.leftMargin = 8;
            params.rightMargin = 8;
            taskView.setLayoutParams(params);

            taskView.setBackgroundColor(getTaskColor(task.getTaskType()));

            taskView.setOnClickListener(v -> {
                if (isAdded()) {
                    Toast.makeText(requireContext(),
                            task.getTitle() + "\n" + task.getStartTime() + " - " + task.getEndTime(),
                            Toast.LENGTH_SHORT).show();
                }
            });

            tasksContainer.addView(taskView);
        } catch (ParseException e) {
            Log.e("HomeFragment", "Error parsing task time", e);
        }
    }

    private int parseTimeToMinutes(String time) throws ParseException {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mma", Locale.US);
            Date date = sdf.parse(time);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        } catch (ParseException e) {
            try {
                String[] parts = time.split(":");
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1].substring(0, 2));
                String ampm = parts[1].substring(2).toUpperCase();

                if (ampm.equals("PM") && hours != 12) hours += 12;
                if (ampm.equals("AM") && hours == 12) hours = 0;

                return hours * 60 + minutes;
            } catch (Exception ex) {
                throw new ParseException("Invalid time format: " + time, 0);
            }
        }
    }

    private int getTaskColor(String taskType) {
        if (!isAdded()) return 0;

        switch (taskType) {
            case "Неизменные": return getResources().getColor(R.color.permanent_task);
            case "Запланированные": return getResources().getColor(R.color.semi_permanent_task);
            case "Эпизодные": return getResources().getColor(R.color.variable_task);
            default: return getResources().getColor(R.color.default_task);
        }
    }
}