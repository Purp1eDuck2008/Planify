package com.example.planify02;

import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Инициализируем фрагменты
        homeFragment = new HomeFragment();
        plusFragment = new PlusFragment();

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.home) {
                replaceFragment(homeFragment, "home_fragment");
            } else if (itemId == R.id.todo) {
                replaceFragment(new ToDoFragment());
            } else if (itemId == R.id.plus) {
                replaceFragment(plusFragment, "plus_fragment");
            } else {
                return false;
            }

            return true;
        });

        // Показываем стартовый фрагмент
        replaceFragment(homeFragment, "home_fragment");
    }

    private void replaceFragment(Fragment fragment) {
        replaceFragment(fragment, null);
    }

    private void replaceFragment(Fragment fragment, String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.replace(R.id.frame_layout, fragment, tag);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    // Метод для обновления даты в HomeFragment
    public void updateHomeFragmentWithDate(int year, int month, int dayOfMonth) {
        if (homeFragment != null && homeFragment.isAdded()) {
            homeFragment.updateSelectedDateWithCalendar(year, month, dayOfMonth);
        } else {
            // Если фрагмент не создан, создаем его
            homeFragment = new HomeFragment();
            replaceFragment(homeFragment, "home_fragment");

            // Небольшая задержка для инициализации фрагмента
            binding.getRoot().post(() -> {
                homeFragment.updateSelectedDateWithCalendar(year, month, dayOfMonth);
            });
        }
    }

    // Метод для получения текущего HomeFragment
    public HomeFragment getHomeFragment() {
        return homeFragment;
    }
}