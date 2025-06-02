package com.example.planify02;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlusFragment extends Fragment {

    private AppDatabase db;
    private ViewGroup tasksContainer;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy", new Locale("ru"));
    private MaterialButton filterButton;
    private TextInputEditText searchInput;
    private List<PlanItem> allTasks = new ArrayList<>();
    private String currentFilter = "none";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_plus, container, false);

        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        db = AppDatabase.getInstance(requireContext());
        tasksContainer = view.findViewById(R.id.tasks_container);

        filterButton = view.findViewById(R.id.filter_button);
        searchInput = view.findViewById(R.id.search_input);

        setupFilterAndSearch();
        loadTasksFromDatabase();

        MaterialButton addTaskButton = view.findViewById(R.id.add_task_button);
        addTaskButton.setOnClickListener(v -> showTaskDialog(null));

        return view;
    }

    private void setupFilterAndSearch() {
        filterButton.setOnClickListener(v -> showFilterMenu());

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                applyFiltersAndSearch();
                return true;
            }
            return false;
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFiltersAndSearch();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void showFilterMenu() {
        PopupMenu popup = new PopupMenu(requireContext(), filterButton);
        popup.getMenuInflater().inflate(R.menu.filter_menu, popup.getMenu());

        switch (currentFilter) {
            case "date_asc": popup.getMenu().findItem(R.id.filter_date_asc).setChecked(true); break;
            case "date_desc": popup.getMenu().findItem(R.id.filter_date_desc).setChecked(true); break;
            case "permanent": popup.getMenu().findItem(R.id.filter_permanent).setChecked(true); break;
            case "semi_permanent": popup.getMenu().findItem(R.id.filter_semi_permanent).setChecked(true); break;
            case "variable": popup.getMenu().findItem(R.id.filter_variable).setChecked(true); break;
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.filter_date_asc) {
                currentFilter = "date_asc";
            } else if (id == R.id.filter_date_desc) {
                currentFilter = "date_desc";
            } else if (id == R.id.filter_permanent) {
                currentFilter = "permanent";
            } else if (id == R.id.filter_semi_permanent) {
                currentFilter = "semi_permanent";
            } else if (id == R.id.filter_variable) {
                currentFilter = "variable";
            } else {
                currentFilter = "none";
            }

            applyFiltersAndSearch();
            return true;
        });

        popup.show();
    }

    private void applyFiltersAndSearch() {
        String searchQuery = searchInput.getText().toString().toLowerCase();

        new Thread(() -> {
            List<PlanItem> filteredTasks = new ArrayList<>(allTasks);

            switch (currentFilter) {
                case "date_asc":
                    filteredTasks.sort((t1, t2) -> {
                        try {
                            Date date1 = dateFormat.parse(t1.getEventDate());
                            Date date2 = dateFormat.parse(t2.getEventDate());
                            return date1.compareTo(date2);
                        } catch (ParseException e) {
                            return 0;
                        }
                    });
                    break;
                case "date_desc":
                    filteredTasks.sort((t1, t2) -> {
                        try {
                            Date date1 = dateFormat.parse(t1.getEventDate());
                            Date date2 = dateFormat.parse(t2.getEventDate());
                            return date2.compareTo(date1);
                        } catch (ParseException e) {
                            return 0;
                        }
                    });
                    break;
                case "permanent":
                    filteredTasks.removeIf(task -> !task.getTaskType().equals("Неизменные"));
                    break;
                case "semi_permanent":
                    filteredTasks.removeIf(task -> !task.getTaskType().equals("Запланированные"));
                    break;
                case "variable":
                    filteredTasks.removeIf(task -> !task.getTaskType().equals("Эпизодные"));
                    break;
            }

            if (!searchQuery.isEmpty()) {
                filteredTasks.removeIf(task ->
                        !task.getTitle().toLowerCase().contains(searchQuery));
            }

            requireActivity().runOnUiThread(() -> {
                tasksContainer.removeAllViews();
                for (PlanItem task : filteredTasks) {
                    addTaskCard(task);
                }
            });
        }).start();
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
                allTasks = db.planItemDao().getAllSortedByType();
                requireActivity().runOnUiThread(() -> {
                    tasksContainer.removeAllViews();
                    for (PlanItem task : allTasks) {
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
                            cancelNotification(task.getId());
                            requireActivity().runOnUiThread(() -> {
                                tasksContainer.removeView(cardView);
                                showToast("Задача удалена");
                                updateHomeFragment();
                                loadTasksFromDatabase();
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
        ToggleButton toggleNotifications = dialogView.findViewById(R.id.toggle_notifications);
        Spinner spinnerReminderTime = dialogView.findViewById(R.id.spinner_reminder_time);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.reminder_times, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReminderTime.setAdapter(adapter);

        toggleNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spinnerReminderTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (existingTask != null && !isChecked) {
                cancelNotification(existingTask.getId());
            }
        });

        if (existingTask != null) {
            fillDialogFields(existingTask, etTitle, etEventDate, etStartTime,
                    etEndTime, etLocation, etDescription, rgTaskType, chipGroupDays,
                    toggleNotifications, spinnerReminderTime);
        }

        setupDateTimePickers(etEventDate, etStartTime, etEndTime);

        Map<Integer, String> dayMap = createDayMap();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existingTask != null ? "Редактировать задачу" : "Добавить задачу")
                .setView(dialogView)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    if (validateAndSaveTask(existingTask, etTitle, etEventDate, etStartTime,
                            etEndTime, etLocation, etDescription, rgTaskType, chipGroupDays,
                            toggleNotifications, spinnerReminderTime, dayMap)) {
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
                        cancelNotification(existingTask.getId());
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
                                  ChipGroup chipGroupDays, ToggleButton toggleNotifications,
                                  Spinner spinnerReminderTime) {
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

            toggleNotifications.setChecked(task.isNotificationsEnabled());
            spinnerReminderTime.setVisibility(task.isNotificationsEnabled() ? View.VISIBLE : View.GONE);

            int minutes = task.getReminderMinutesBefore();
            String[] reminderTimes = getResources().getStringArray(R.array.reminder_times);
            for (int i = 0; i < reminderTimes.length; i++) {
                if (parseReminderTime(reminderTimes[i]) == minutes) {
                    spinnerReminderTime.setSelection(i);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e("PlusFragment", "Error filling dialog fields", e);
        }
    }

    private int parseReminderTime(String time) {
        try {
            return Integer.parseInt(time.split(" ")[0]);
        } catch (Exception e) {
            return 15;
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
                                        ChipGroup chipGroupDays, ToggleButton toggleNotifications,
                                        Spinner spinnerReminderTime, Map<Integer, String> dayMap) {
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

            boolean notificationsEnabled = toggleNotifications.isChecked();
            int reminderMinutesBefore = notificationsEnabled ?
                    parseReminderTime(spinnerReminderTime.getSelectedItem().toString()) : 0;

            if (!validateInput(etTitle, etEventDate, etStartTime, etEndTime, rgTaskType)) {
                return false;
            }

            saveOrUpdateTask(existingTask, title, eventDate, startTime, endTime,
                    location, description, taskType, repeatDays,
                    notificationsEnabled, reminderMinutesBefore);
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
            etTitle.setError("Название задачи обязательно");
            showToast("Пожалуйста, введите название задачи");
            isValid = false;
        } else {
            etTitle.setError(null);
        }

        if (etEventDate.getText().toString().trim().isEmpty()) {
            etEventDate.setError("Дата обязательна");
            showToast("Пожалуйста, выберите дату события");
            isValid = false;
        } else {
            etEventDate.setError(null);
        }

        if (etStartTime.getText().toString().trim().isEmpty()) {
            etStartTime.setError("Время начала обязательно");
            showToast("Пожалуйста, выберите время начала");
            isValid = false;
        } else {
            etStartTime.setError(null);
        }

        if (etEndTime.getText().toString().trim().isEmpty()) {
            etEndTime.setError("Время окончания обязательно");
            showToast("Пожалуйста, выберите время окончания");
            isValid = false;
        } else {
            etEndTime.setError(null);
        }

        if (rgTaskType.getCheckedRadioButtonId() == -1) {
            showToast("Пожалуйста, выберите тип задачи");
            isValid = false;
        }

        if (isValid) {
            try {
                String startTime = etStartTime.getText().toString().trim();
                String endTime = etEndTime.getText().toString().trim();

                if (parseTimeToMinutes(startTime) >= parseTimeToMinutes(endTime)) {
                    etEndTime.setError("Время окончания должно быть позже времени начала");
                    showToast("Время окончания должно быть позже времени начала");
                    isValid = false;
                } else {
                    etEndTime.setError(null);
                }
            } catch (ParseException e) {
                etEndTime.setError("Некорректный формат времени");
                showToast("Некорректный формат времени");
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
                                  String description, String taskType, String repeatDays,
                                  boolean notificationsEnabled, int reminderMinutesBefore) {
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
                    existingTask.setNotificationsEnabled(notificationsEnabled);
                    existingTask.setReminderMinutesBefore(reminderMinutesBefore);
                    db.planItemDao().update(existingTask);

                    // Отменяем старое уведомление и создаем новое, если нужно
                    cancelNotification(existingTask.getId());
                    if (notificationsEnabled) {
                        scheduleNotification(existingTask.getId(), title, date, startTime, reminderMinutesBefore);
                    }
                } else {
                    PlanItem newTask = new PlanItem(title, date, startTime, endTime,
                            location, description, taskType, repeatDays,
                            notificationsEnabled, reminderMinutesBefore);
                    long id = db.planItemDao().insert(newTask);

                    if (notificationsEnabled) {
                        scheduleNotification((int)id, title, date, startTime, reminderMinutesBefore);
                    }
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

    private void showTimePicker(TextInputEditText editText, String title) {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    int hour = hourOfDay % 12;
                    if (hour == 0) hour = 12;
                    String amPm = hourOfDay < 12 ? "AM" : "PM";
                    editText.setText(String.format(Locale.getDefault(),
                            "%d:%02d%s", hour, minute, amPm));
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

    private void showDatePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String formattedDate = String.format(Locale.getDefault(),
                            "%d %s %d", dayOfMonth, getMonthName(month), year);
                    editText.setText(formattedDate);
                    updateHomeFragmentWithDate(year, month, dayOfMonth);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void updateHomeFragmentWithDate(int year, int month, int dayOfMonth) {
        if (!isAdded()) return;

        FragmentManager fragmentManager = getParentFragmentManager();
        if (fragmentManager != null) {
            HomeFragment homeFragment = (HomeFragment) fragmentManager.findFragmentByTag("home_fragment");
            if (homeFragment != null && homeFragment.isAdded()) {
                homeFragment.updateSelectedDateWithCalendar(year, month, dayOfMonth);
            }
        }
    }

    private void scheduleNotification(int taskId, String title, String date,
                                      String startTime, int minutesBefore) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy h:mma", new Locale("ru"));
            Date taskDateTime = dateFormat.parse(date + " " + startTime);

            if (taskDateTime == null) return;

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(taskDateTime);
            calendar.add(Calendar.MINUTE, -minutesBefore);

            Intent intent = new Intent(requireContext(), NotificationReceiver.class);
            intent.putExtra("task_id", taskId);
            intent.putExtra("title", title);
            intent.putExtra("message", minutesBefore + " минут до начала задачи \"" + title + "\"");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    taskId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent);

        } catch (ParseException e) {
            Log.e("PlusFragment", "Error scheduling notification", e);
        }
    }

    private void cancelNotification(int taskId) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        alarmManager.cancel(pendingIntent);
    }
}