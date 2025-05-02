package com.example.planify02;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.room.Room;

import com.example.planify02.database.AppDatabase;
import com.example.planify02.entities.PlanItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlusFragment extends Fragment {

    private AppDatabase db;
    private ViewGroup tasksContainer;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_plus, container, false);

        // Фиксируем портретную ориентацию
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Инициализация базы данных
        db = Room.databaseBuilder(requireContext(),
                        AppDatabase.class, "planify-database")
                .allowMainThreadQueries()
                .build();

        tasksContainer = view.findViewById(R.id.tasks_container);
        loadTasksFromDatabase();

        MaterialButton addTaskButton = view.findViewById(R.id.add_task_button);
        addTaskButton.setOnClickListener(v -> showTaskDialog(null));

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Восстанавливаем стандартную ориентацию при уничтожении фрагмента
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        if (db != null) {
            db.close();
        }
    }

    private void loadTasksFromDatabase() {
        tasksContainer.removeAllViews();
        // Получаем задачи, уже отсортированные в БД
        List<PlanItem> tasks = db.planItemDao().getAllSortedByType();
        for (PlanItem task : tasks) {
            addTaskCard(task);
        }
    }

    private void addTaskCard(PlanItem task) {
        MaterialCardView cardView = (MaterialCardView) LayoutInflater.from(requireContext())
                .inflate(R.layout.task_card_item, tasksContainer, false);

        MaterialTextView tvTitle = cardView.findViewById(R.id.card_title);
        MaterialTextView tvDateTime = cardView.findViewById(R.id.card_date_time);
        MaterialTextView tvLocation = cardView.findViewById(R.id.card_location);
        MaterialTextView tvDescription = cardView.findViewById(R.id.card_description);
        MaterialTextView tvTaskType = cardView.findViewById(R.id.card_task_type);
        MaterialTextView tvRepeatDays = cardView.findViewById(R.id.card_repeat_days);
        MaterialButton editButton = cardView.findViewById(R.id.edit_button);
        MaterialButton deleteButton = cardView.findViewById(R.id.delete_button);

        tvTitle.setText(task.getTitle());
        tvDateTime.setText(formatDateTime(task));
        tvLocation.setText(task.getLocation());
        tvDescription.setText(task.getDescription());
        tvTaskType.setText(task.getTaskType());
        tvRepeatDays.setText(formatRepeatDays(task.getRepeatDays()));

        editButton.setOnClickListener(v -> showTaskDialog(task));
        deleteButton.setOnClickListener(v -> deleteTaskWithAnimation(task, cardView));

        cardView.setAlpha(0f);
        tasksContainer.addView(cardView);
        cardView.animate().alpha(1f).setDuration(300).start();
    }

    private String formatDateTime(PlanItem task) {
        return String.format("%s, %s - %s",
                task.getEventDate(), task.getStartTime(), task.getEndTime());
    }

    private String formatRepeatDays(String repeatDays) {
        if (repeatDays == null || repeatDays.isEmpty()) {
            return "Без повторения";
        }

        String[] dayCodes = repeatDays.split(",");
        String[] dayNames = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        StringBuilder result = new StringBuilder("Повтор: ");

        for (String code : dayCodes) {
            try {
                int index = Integer.parseInt(code) - 1;
                if (index >= 0 && index < dayNames.length) {
                    if (result.length() > 8) result.append(", ");
                    result.append(dayNames[index]);
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return result.toString();
    }

    private void deleteTaskWithAnimation(PlanItem task, MaterialCardView cardView) {
        cardView.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    new Thread(() -> {
                        db.planItemDao().delete(task);
                        requireActivity().runOnUiThread(() -> {
                            tasksContainer.removeView(cardView);
                            showToast("Задача удалена");
                        });
                    }).start();
                })
                .start();
    }

    private void showTaskDialog(PlanItem existingTask) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_task, null);

        TextInputEditText etTitle = dialogView.findViewById(R.id.et_title);
        TextInputEditText etEventDate = dialogView.findViewById(R.id.et_event_date);
        TextInputEditText etStartTime = dialogView.findViewById(R.id.et_start_time);
        TextInputEditText etEndTime = dialogView.findViewById(R.id.et_end_time);
        TextInputEditText etLocation = dialogView.findViewById(R.id.et_location);
        TextInputEditText etDescription = dialogView.findViewById(R.id.et_description);
        RadioGroup rgTaskType = dialogView.findViewById(R.id.rg_task_type);
        ChipGroup chipGroupDays = dialogView.findViewById(R.id.chipGroupDays);

        if (existingTask != null) {
            fillDialogFields(existingTask, etTitle, etEventDate, etStartTime,
                    etEndTime, etLocation, etDescription, rgTaskType, chipGroupDays);
        }

        setupDateTimePickers(etEventDate, etStartTime, etEndTime);

        MaterialAlertDialogBuilder builder = createDialogBuilder(existingTask, dialogView,
                etTitle, etEventDate, etStartTime, etEndTime,
                etLocation, etDescription, rgTaskType, chipGroupDays);

        builder.show();
    }

    private void fillDialogFields(PlanItem task, TextInputEditText etTitle,
                                  TextInputEditText etEventDate, TextInputEditText etStartTime,
                                  TextInputEditText etEndTime, TextInputEditText etLocation,
                                  TextInputEditText etDescription, RadioGroup rgTaskType,
                                  ChipGroup chipGroupDays) {
        etTitle.setText(task.getTitle());
        etEventDate.setText(task.getEventDate());
        etStartTime.setText(task.getStartTime());
        etEndTime.setText(task.getEndTime());
        etLocation.setText(task.getLocation());
        etDescription.setText(task.getDescription());

        switch (task.getTaskType()) {
            case "Перманентная": rgTaskType.check(R.id.rb_permanent); break;
            case "Полуперманентная": rgTaskType.check(R.id.rb_semi_permanent); break;
            case "Вариативная": rgTaskType.check(R.id.rb_variable); break;
        }

        if (task.getRepeatDays() != null && !task.getRepeatDays().isEmpty()) {
            String[] days = task.getRepeatDays().split(",");
            for (String day : days) {
                switch (day) {
                    case "1": chipGroupDays.check(R.id.chipMonday); break;
                    case "2": chipGroupDays.check(R.id.chipTuesday); break;
                    case "3": chipGroupDays.check(R.id.chipWednesday); break;
                    case "4": chipGroupDays.check(R.id.chipThursday); break;
                    case "5": chipGroupDays.check(R.id.chipFriday); break;
                    case "6": chipGroupDays.check(R.id.chipSaturday); break;
                    case "7": chipGroupDays.check(R.id.chipSunday); break;
                }
            }
        }
    }

    private void setupDateTimePickers(TextInputEditText etEventDate,
                                      TextInputEditText etStartTime,
                                      TextInputEditText etEndTime) {
        etEventDate.setOnClickListener(v -> showDatePicker(etEventDate));
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime, "Время начала"));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime, "Время окончания"));
    }

    private MaterialAlertDialogBuilder createDialogBuilder(PlanItem existingTask, View dialogView,
                                                           TextInputEditText etTitle,
                                                           TextInputEditText etEventDate,
                                                           TextInputEditText etStartTime,
                                                           TextInputEditText etEndTime,
                                                           TextInputEditText etLocation,
                                                           TextInputEditText etDescription,
                                                           RadioGroup rgTaskType,
                                                           ChipGroup chipGroupDays) {
        Map<Integer, String> dayMap = createDayMap();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existingTask != null ? "Редактировать задачу" : "Добавить задачу")
                .setView(dialogView)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    if (validateAndSaveTask(existingTask, etTitle, etEventDate, etStartTime,
                            etEndTime, etLocation, etDescription, rgTaskType, chipGroupDays, dayMap)) {
                        loadTasksFromDatabase();
                    }
                })
                .setNegativeButton("Отмена", null);

        if (existingTask != null) {
            builder.setNeutralButton("Удалить", (dialog, which) -> {
                new Thread(() -> {
                    db.planItemDao().delete(existingTask);
                    requireActivity().runOnUiThread(() -> {
                        loadTasksFromDatabase();
                        showToast("Задача удалена");
                    });
                }).start();
            });
        }

        return builder;
    }

    private Map<Integer, String> createDayMap() {
        Map<Integer, String> dayMap = new HashMap<>();
        dayMap.put(R.id.chipMonday, "1");
        dayMap.put(R.id.chipTuesday, "2");
        dayMap.put(R.id.chipWednesday, "3");
        dayMap.put(R.id.chipThursday, "4");
        dayMap.put(R.id.chipFriday, "5");
        dayMap.put(R.id.chipSaturday, "6");
        dayMap.put(R.id.chipSunday, "7");
        return dayMap;
    }

    private boolean validateAndSaveTask(PlanItem existingTask, TextInputEditText etTitle,
                                        TextInputEditText etEventDate, TextInputEditText etStartTime,
                                        TextInputEditText etEndTime, TextInputEditText etLocation,
                                        TextInputEditText etDescription, RadioGroup rgTaskType,
                                        ChipGroup chipGroupDays, Map<Integer, String> dayMap) {
        String title = etTitle.getText().toString().trim();
        String eventDate = etEventDate.getText().toString().trim();
        String startTime = etStartTime.getText().toString().trim();
        String endTime = etEndTime.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String taskType = getSelectedTaskType(rgTaskType);

        List<String> selectedDays = getSelectedDays(chipGroupDays, dayMap);
        String repeatDays = TextUtils.join(",", selectedDays);

        if (!validateInput(title, eventDate, startTime, endTime, taskType)) {
            return false;
        }

        saveOrUpdateTask(existingTask, title, eventDate, startTime, endTime,
                location, description, taskType, repeatDays);
        return true;
    }

    private List<String> getSelectedDays(ChipGroup chipGroupDays, Map<Integer, String> dayMap) {
        List<String> selectedDays = new ArrayList<>();
        for (int i = 0; i < chipGroupDays.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupDays.getChildAt(i);
            if (chip.isChecked()) {
                selectedDays.add(dayMap.get(chip.getId()));
            }
        }
        return selectedDays;
    }

    private void saveOrUpdateTask(PlanItem existingTask, String title, String date,
                                  String startTime, String endTime, String location,
                                  String description, String taskType, String repeatDays) {
        new Thread(() -> {
            if (existingTask != null) {
                existingTask.setTitle(title);
                existingTask.setEventDate(date);
                existingTask.setStartTime(startTime);
                existingTask.setEndTime(endTime);
                existingTask.setLocation(location);
                existingTask.setDescription(description);
                existingTask.setTaskType(taskType);
                existingTask.setRepeatDays(repeatDays);
                db.planItemDao().update(existingTask);
            } else {
                PlanItem newTask = new PlanItem(title, date, startTime, endTime,
                        location, description, taskType, repeatDays);
                db.planItemDao().insert(newTask);
            }

            requireActivity().runOnUiThread(() ->
                    showToast("Задача сохранена"));
        }).start();
    }

    private String getSelectedTaskType(RadioGroup radioGroup) {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_permanent) return "Перманентная";
        if (selectedId == R.id.rb_semi_permanent) return "Полуперманентная";
        if (selectedId == R.id.rb_variable) return "Вариативная";
        return "";
    }

    private boolean validateInput(String title, String date, String startTime,
                                  String endTime, String taskType) {
        if (title.isEmpty()) return !showToast("Введите название задачи");
        if (date.isEmpty()) return !showToast("Выберите дату события");
        if (startTime.isEmpty()) return !showToast("Выберите время начала");
        if (endTime.isEmpty()) return !showToast("Выберите время окончания");
        if (taskType.isEmpty()) return !showToast("Выберите тип задачи");
        return true;
    }

    private void showDatePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> editText.setText(
                        String.format(Locale.getDefault(), "%d %s %d",
                                dayOfMonth, getMonthName(month), year)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void showTimePicker(TextInputEditText editText, String title) {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    String amPm = hourOfDay < 12 ? "AM" : "PM";
                    int hour = hourOfDay > 12 ? hourOfDay - 12 : hourOfDay;
                    editText.setText(String.format(Locale.getDefault(),
                            "%d:%02d%s", hour == 0 ? 12 : hour, minute, amPm));
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false);
        timePickerDialog.setTitle(title);
        timePickerDialog.show();
    }

    private String getMonthName(int month) {
        String[] months = {"января", "февраля", "марта", "апреля", "мая", "июня",
                "июля", "августа", "сентября", "октября", "ноября", "декабря"};
        return months[month];
    }

    private boolean showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        return false;
    }
}