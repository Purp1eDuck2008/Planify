package com.example.planify02;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.planify02.database.AppDatabase;
import com.example.planify02.daos.PlanItemDao;
import com.example.planify02.entities.PlanItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private AppDatabase db;
    private PlanItemDao planItemDao;
    private FrameLayout tasksContainer;
    private MaterialButton[] dayButtons;
    private Calendar selectedDate = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy", new Locale("ru"));
    private SimpleDateFormat timeDisplayFormat = new SimpleDateFormat("h:mma", Locale.US);




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
        Log.d(TAG, "Loading tasks for: " + dateFormat.format(selectedDate.getTime()));

        new Thread(() -> {
            try {
                String currentDateStr = dateFormat.format(selectedDate.getTime());
                int currentDayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK);

                List<PlanItem> allTasks = planItemDao.getAll();
                Log.d(TAG, "Total tasks in DB: " + allTasks.size());

                requireActivity().runOnUiThread(() -> {
                    int displayedTasks = 0;
                    for (PlanItem task : allTasks) {
                        try {
                            if (isTaskForSelectedDate(task, currentDateStr, currentDayOfWeek)) {
                                addTaskToTimeline(task);
                                displayedTasks++;
                            }
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing task date", e);
                        }
                    }
                    Log.d(TAG, displayedTasks + " tasks displayed");
                    if (displayedTasks == 0 && isAdded()) {
                        Toast.makeText(requireContext(), "Нет задач на выбранную дату", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading tasks", e);
            }
        }).start();
    }

    private boolean isTaskForSelectedDate(PlanItem task, String currentDateStr, int currentDayOfWeek)
            throws ParseException {
        // Если дата задачи совпадает с выбранной датой - показываем
        if (task.getEventDate().equals(currentDateStr)) {
            return true;
        }

        // Для повторяющихся задач
        if (task.repeatsOnDay(currentDayOfWeek)) {
            Date taskDate = dateFormat.parse(task.getEventDate());
            Date selectedDateObj = selectedDate.getTime();

            // Проверяем, что выбранная дата не раньше даты создания задачи
            if (!selectedDateObj.before(taskDate)) {
                // Для еженедельных повторений просто проверяем день недели
                return true;
            }
        }

        return false;
    }

    private void addTaskToTimeline(PlanItem task) {
        if (tasksContainer == null || !isAdded()) return;

        View taskView = LayoutInflater.from(requireContext())
                .inflate(R.layout.task_timeline_item, tasksContainer, false);

        TextView tvTitle = taskView.findViewById(R.id.task_title);
        TextView tvTime = taskView.findViewById(R.id.task_time);
        tvTitle.setText(task.getTitle());
        tvTime.setText(task.getStartTime() + " - " + task.getEndTime());

        try {
            // Конвертируем время в минуты от начала дня
            int startMinutes = parseTimeToMinutes(task.getStartTime());
            int endMinutes = parseTimeToMinutes(task.getEndTime());
            int durationMinutes = endMinutes - startMinutes;

            // Получаем плотность экрана
            float density = getResources().getDisplayMetrics().density;

            // Рассчитываем позицию и высоту в пикселях
            int topMarginPx = (int) (startMinutes * density);
            int heightPx = (int) (durationMinutes * density);

            // Минимальная высота - 30 минут (для видимости)
            int minHeightPx = (int) (30 * density);
            heightPx = Math.max(heightPx, minHeightPx);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    heightPx
            );
            params.topMargin = topMarginPx;
            params.leftMargin = (int) (8 * density);
            params.rightMargin = (int) (8 * density);

            taskView.setLayoutParams(params);
            taskView.setBackgroundColor(getTaskColor(task.getTaskType()));
            taskView.setOnClickListener(v -> showTaskDetailsDialog(task));

            tasksContainer.addView(taskView);

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing task time: " + task.getStartTime() + "-" + task.getEndTime(), e);
        }
    }

    private int parseTimeToMinutes(String timeStr) throws ParseException {
        try {
            // Нормализуем формат (удаляем пробелы, приводим к верхнему регистру)
            String normalized = timeStr.replaceAll(" ", "").toUpperCase();

            // Разделяем часы и минуты
            String[] parts = normalized.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1].substring(0, 2));

            // Обрабатываем AM/PM
            if (parts[1].endsWith("PM") && hours != 12) {
                hours += 12;
            } else if (parts[1].endsWith("AM") && hours == 12) {
                hours = 0;
            }

            return hours * 60 + minutes;
        } catch (Exception e) {
            throw new ParseException("Invalid time format: " + timeStr, 0);
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
    private void showTaskDetailsDialog(PlanItem task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.AppDialogTheme);
        builder.setTitle("Детали задачи");

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_task_details, null);

        // Находим все элементы
        TextView tvTitle = dialogView.findViewById(R.id.dialog_title);
        TextView tvTime = dialogView.findViewById(R.id.dialog_time);
        TextView tvLocation = dialogView.findViewById(R.id.dialog_location);
        TextView tvDescription = dialogView.findViewById(R.id.dialog_description);
        TextView tvTaskType = dialogView.findViewById(R.id.dialog_task_type);
        TextView tvRepeatDays = dialogView.findViewById(R.id.dialog_repeat_days);
        Button btnClose = dialogView.findViewById(R.id.dialog_close_button);
        Button btnDelete = dialogView.findViewById(R.id.dialog_delete_button);

        // Заполняем данные
        tvTitle.setText(task.getTitle());
        tvTime.setText(String.format("%s - %s", task.getStartTime(), task.getEndTime()));
        tvLocation.setText(task.getLocation());
        tvDescription.setText(task.getDescription());
        tvTaskType.setText(task.getTaskType());
        tvRepeatDays.setText(formatRepeatDays(task.getRepeatDays()));

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Удаление задачи")
                    .setMessage("Вы уверены, что хотите удалить эту задачу?")
                    .setPositiveButton("Удалить", (d, which) -> {
                        new Thread(() -> {
                            db.planItemDao().delete(task);
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Задача удалена", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                loadTasksForSelectedDate(); // Обновляем список задач
                            });
                        }).start();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        dialog.show();
    }

    private String formatRepeatDays(String repeatDays) {
        if (repeatDays == null || repeatDays.isEmpty()) {
            return "Не повторяется";
        }

        String[] dayCodes = repeatDays.split(",");
        String[] dayNames = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        StringBuilder result = new StringBuilder("Повторяется: ");

        for (String code : dayCodes) {
            try {
                int index = Integer.parseInt(code.trim()) - 1;
                if (index >= 0 && index < dayNames.length) {
                    if (result.length() > 13) result.append(", ");
                    result.append(dayNames[index]);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing repeat day: " + code, e);
            }
        }

        return result.toString();
    }

    public void updateSelectedDateWithCalendar(int year, int month, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, dayOfMonth);
        selectedDate = cal;

        // Определяем день недели (1-7, где 1=воскресенье, 2=понедельник и т.д.)
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        // Преобразуем в наш формат (0=понедельник, 6=воскресенье)
        int buttonIndex = (dayOfWeek + 5) % 7;

        // Выбираем соответствующую кнопку
        if (buttonIndex >= 0 && buttonIndex < dayButtons.length) {
            dayButtons[buttonIndex].performClick();
        }

        updateDaySelection();
    }


}