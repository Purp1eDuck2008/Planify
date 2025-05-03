package com.example.planify02;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Date;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.planify02.database.AppDatabase;
import com.example.planify02.entities.PlanItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlusFragment extends Fragment {

    private AppDatabase db;
    private ViewGroup tasksContainer;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy", new Locale("ru"));

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_plus, container, false);

        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        db = AppDatabase.getInstance(requireContext());
        tasksContainer = view.findViewById(R.id.tasks_container);

        loadTasksFromDatabase();

        MaterialButton addTaskButton = view.findViewById(R.id.add_task_button);
        addTaskButton.setOnClickListener(v -> showTaskDialog(null));

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private void loadTasksFromDatabase() {
        if (tasksContainer == null) return;

        tasksContainer.removeAllViews();
        new Thread(() -> {
            try {
                List<PlanItem> tasks = db.planItemDao().getAllSortedByType();
                requireActivity().runOnUiThread(() -> {
                    for (PlanItem task : tasks) {
                        addTaskCard(task);
                    }
                });
            } catch (Exception e) {
                Log.e("PlusFragment", "Error loading tasks", e);
            }
        }).start();
    }

    private void addTaskCard(PlanItem task) {
        if (!isAdded() || tasksContainer == null) return;

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
                int index = Integer.parseInt(code.trim()) - 1;
                if (index >= 0 && index < dayNames.length) {
                    if (result.length() > 8) result.append(", ");
                    result.append(dayNames[index]);
                }
            } catch (NumberFormatException e) {
                Log.e("PlusFragment", "Error parsing repeat days", e);
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
                        try {
                            db.planItemDao().delete(task);
                            requireActivity().runOnUiThread(() -> {
                                tasksContainer.removeView(cardView);
                                showToast("Задача удалена");
                                updateHomeFragment();
                            });
                        } catch (Exception e) {
                            Log.e("PlusFragment", "Error deleting task", e);
                        }
                    }).start();
                })
                .start();
    }

    private void showTaskDialog(PlanItem existingTask) {
        if (!isAdded()) return;

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

        Map<Integer, String> dayMap = createDayMap();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existingTask != null ? "Редактировать задачу" : "Добавить задачу")
                .setView(dialogView)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    if (validateAndSaveTask(existingTask, etTitle, etEventDate, etStartTime,
                            etEndTime, etLocation, etDescription, rgTaskType, chipGroupDays, dayMap)) {
                        loadTasksFromDatabase();
                        updateHomeFragment();
                    }
                })
                .setNegativeButton("Отмена", null);

        if (existingTask != null) {
            builder.setNeutralButton("Удалить", (dialog, which) -> {
                new Thread(() -> {
                    try {
                        db.planItemDao().delete(existingTask);
                        requireActivity().runOnUiThread(() -> {
                            showToast("Задача удалена");
                            loadTasksFromDatabase();
                            updateHomeFragment();
                        });
                    } catch (Exception e) {
                        Log.e("PlusFragment", "Error deleting task", e);
                    }
                }).start();
            });
        }

        builder.show();
    }

    private void fillDialogFields(PlanItem task, TextInputEditText etTitle,
                                  TextInputEditText etEventDate, TextInputEditText etStartTime,
                                  TextInputEditText etEndTime, TextInputEditText etLocation,
                                  TextInputEditText etDescription, RadioGroup rgTaskType,
                                  ChipGroup chipGroupDays) {
        try {
            etTitle.setText(task.getTitle());
            etEventDate.setText(task.getEventDate());
            etStartTime.setText(task.getStartTime());
            etEndTime.setText(task.getEndTime());
            etLocation.setText(task.getLocation());
            etDescription.setText(task.getDescription());

            switch (task.getTaskType()) {
                case "Неизменные":
                    rgTaskType.check(R.id.rb_permanent);
                    break;
                case "Запланированные":
                    rgTaskType.check(R.id.rb_semi_permanent);
                    break;
                case "Эпизодные":
                    rgTaskType.check(R.id.rb_variable);
                    break;
            }

            if (task.getRepeatDays() != null && !task.getRepeatDays().isEmpty()) {
                String[] days = task.getRepeatDays().split(",");
                for (String day : days) {
                    switch (day.trim()) {
                        case "1":
                            chipGroupDays.check(R.id.chipMonday);
                            break;
                        case "2":
                            chipGroupDays.check(R.id.chipTuesday);
                            break;
                        case "3":
                            chipGroupDays.check(R.id.chipWednesday);
                            break;
                        case "4":
                            chipGroupDays.check(R.id.chipThursday);
                            break;
                        case "5":
                            chipGroupDays.check(R.id.chipFriday);
                            break;
                        case "6":
                            chipGroupDays.check(R.id.chipSaturday);
                            break;
                        case "7":
                            chipGroupDays.check(R.id.chipSunday);
                            break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e("PlusFragment", "Error filling dialog fields", e);
        }
    }

    private void setupDateTimePickers(TextInputEditText etEventDate,
                                      TextInputEditText etStartTime,
                                      TextInputEditText etEndTime) {
        etEventDate.setOnClickListener(v -> showDatePicker(etEventDate));
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime, "Время начала"));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime, "Время окончания"));
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
        try {
            String title = etTitle.getText().toString().trim();
            String eventDate = etEventDate.getText().toString().trim();
            String startTime = etStartTime.getText().toString().trim();
            String endTime = etEndTime.getText().toString().trim();
            String location = etLocation.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String taskType = getSelectedTaskType(rgTaskType);

            List<String> selectedDays = getSelectedDays(chipGroupDays, dayMap);
            String repeatDays = TextUtils.join(",", selectedDays);

            if (!validateInput(etTitle, etEventDate, etStartTime, etEndTime, rgTaskType)) {
                return false;
            }

            saveOrUpdateTask(existingTask, title, eventDate, startTime, endTime,
                    location, description, taskType, repeatDays);
            return true;
        } catch (Exception e) {
            Log.e("PlusFragment", "Error saving task", e);
            showToast("Ошибка сохранения задачи");
            return false;
        }
    }

    private boolean validateInput(TextInputEditText etTitle, TextInputEditText etEventDate,
                                  TextInputEditText etStartTime, TextInputEditText etEndTime,
                                  RadioGroup rgTaskType) {
        boolean isValid = true;

        if (etTitle.getText().toString().trim().isEmpty()) {
            etTitle.setError("Введите название задачи");
            isValid = false;
        } else {
            etTitle.setError(null);
        }

        if (etEventDate.getText().toString().trim().isEmpty()) {
            etEventDate.setError("Выберите дату события");
            isValid = false;
        } else {
            etEventDate.setError(null);
        }

        if (etStartTime.getText().toString().trim().isEmpty()) {
            etStartTime.setError("Выберите время начала");
            isValid = false;
        } else {
            etStartTime.setError(null);
        }

        if (etEndTime.getText().toString().trim().isEmpty()) {
            etEndTime.setError("Выберите время окончания");
            isValid = false;
        } else {
            etEndTime.setError(null);
        }

        if (rgTaskType.getCheckedRadioButtonId() == -1) {
            showToast("Выберите тип задачи");
            isValid = false;
        }

        if (isValid) {
            try {
                String startTime = etStartTime.getText().toString().trim();
                String endTime = etEndTime.getText().toString().trim();

                if (parseTimeToMinutes(startTime) >= parseTimeToMinutes(endTime)) {
                    etEndTime.setError("Время окончания должно быть позже времени начала");
                    isValid = false;
                } else {
                    etEndTime.setError(null);
                }
            } catch (ParseException e) {
                etEndTime.setError("Некорректный формат времени");
                isValid = false;
            }
        }

        return isValid;
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
            try {
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

                requireActivity().runOnUiThread(() -> {
                    showToast("Задача сохранена");
                    updateHomeFragment();
                });
            } catch (Exception e) {
                Log.e("PlusFragment", "Error saving task", e);
                requireActivity().runOnUiThread(() ->
                        showToast("Ошибка сохранения задачи"));
            }
        }).start();
    }

    private String getSelectedTaskType(RadioGroup radioGroup) {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_permanent) return "Неизменные";
        if (selectedId == R.id.rb_semi_permanent) return "Запланированные";
        if (selectedId == R.id.rb_variable) return "Эпизодные";
        return "";
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

    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateHomeFragment() {
        if (!isAdded()) return;

        FragmentManager fragmentManager = getParentFragmentManager();
        if (fragmentManager != null) {
            HomeFragment homeFragment = (HomeFragment) fragmentManager.findFragmentByTag("home_fragment");
            if (homeFragment != null && homeFragment.isAdded()) {
                homeFragment.loadTasksForSelectedDate();
            }
        }
    }
}