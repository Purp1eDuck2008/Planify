package com.example.planify02;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.planify02.databinding.ActivityMainBinding;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private HomeFragment homeFragment;
    private PlusFragment plusFragment;
    private PopupWindow popupWindow;
    private Calendar selectedDate = Calendar.getInstance();
    private boolean isMenuShowing = false;
    private View dimView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Создаем View для затемнения фона
        dimView = new View(this);
        dimView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        dimView.setBackgroundColor(Color.argb(150, 0, 0, 0));
        dimView.setVisibility(View.GONE);
        dimView.setOnClickListener(v -> dismissMenuPanel());

        // Добавляем затемнение в корневой layout
        ViewGroup rootView = (ViewGroup) getWindow().getDecorView();
        rootView.addView(dimView);

        // Инициализация фрагментов
        homeFragment = new HomeFragment();
        plusFragment = new PlusFragment();

        // Настройка нижней навигации
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                replaceFragment(homeFragment, "home_fragment");
            } else if (itemId == R.id.todo) {
                replaceFragment(new ToDoFragment());
            } else if (itemId == R.id.plus) {
                replaceFragment(plusFragment, "plus_fragment");
            }
            return true;
        });

        // Настройка кнопки меню
        binding.buttonMenu.setOnClickListener(v -> toggleMenuPanel());

        // Показываем стартовый фрагмент
        replaceFragment(homeFragment, "home_fragment");
    }

    private void toggleMenuPanel() {
        if (isMenuShowing) {
            dismissMenuPanel();
        } else {
            showMenuPanel();
        }
    }

    private void showMenuPanel() {
        if (popupWindow != null && popupWindow.isShowing()) {
            return;
        }

        // Создаем view для меню
        View popupView = LayoutInflater.from(this).inflate(R.layout.menu_panel, null);

        // Вычисляем ширину экрана
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;

        // Создаем PopupWindow
        popupWindow = new PopupWindow(
                popupView,
                (int)(screenWidth * 0.7), // 70% ширины экрана
                ViewGroup.LayoutParams.MATCH_PARENT,
                true
        );

        // Настройка внешнего вида
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(16f);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setAnimationStyle(R.style.PopupAnimation);

        // Обработчик закрытия меню
        popupWindow.setOnDismissListener(() -> {
            dimView.setVisibility(View.GONE);
            isMenuShowing = false;
        });

        // Находим элементы
        CalendarView calendarView = popupView.findViewById(R.id.calendarView);
        LinearLayout buttonsContainer = popupView.findViewById(R.id.additional_buttons_container);

        // Устанавливаем текущую дату
        calendarView.setDate(selectedDate.getTimeInMillis(), false, true);

        // Обработчик выбора даты
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            updateHomeFragmentWithDate(year, month, dayOfMonth);
            dismissMenuPanel();
        });

        // Показываем затемнение
        dimView.setVisibility(View.VISIBLE);

        // Показываем меню слева
        popupWindow.showAtLocation(
                binding.getRoot(),
                Gravity.START,
                0,
                0
        );

        isMenuShowing = true;
    }

    private void dismissMenuPanel() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        // Удаляем затемнение при уничтожении активности
        if (dimView != null) {
            ViewGroup rootView = (ViewGroup) getWindow().getDecorView();
            rootView.removeView(dimView);
        }
        dismissMenuPanel();
        super.onDestroy();
    }

    private void replaceFragment(Fragment fragment) {
        replaceFragment(fragment, null);
    }

    private void replaceFragment(Fragment fragment, String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment, tag);
        fragmentTransaction.commit();
    }

    public void updateHomeFragmentWithDate(int year, int month, int dayOfMonth) {
        selectedDate.set(year, month, dayOfMonth);
        if (homeFragment != null) {
            homeFragment.updateSelectedDateWithCalendar(year, month, dayOfMonth);
        }
    }

    public HomeFragment getHomeFragment() {
        return homeFragment;
    }
}