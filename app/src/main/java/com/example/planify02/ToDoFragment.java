package com.example.planify02;

import androidx.room.Room;
import android.widget.Toast;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.planify02.database.AppDatabase;
import com.example.planify02.daos.NoteDao;
import com.example.planify02.entities.Note;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import androidx.lifecycle.Observer;
import androidx.lifecycle.LiveData;

public class ToDoFragment extends Fragment {

    private EditText noteTitleInput, noteContentInput;
    private MaterialButton addNoteButton;
    private RecyclerView recyclerView;
    private NotesAdapter adapter;
    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_to_do, container, false);

        // Инициализация элементов
        noteTitleInput = view.findViewById(R.id.note_title_input);
        noteContentInput = view.findViewById(R.id.note_content_input);
        addNoteButton = view.findViewById(R.id.add_note_button);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Инициализация Room
        db = Room.databaseBuilder(requireContext(), AppDatabase.class, "notes-db")
                .allowMainThreadQueries()
                .build();

        // Инициализация адаптера с передачей слушателя
        adapter = new NotesAdapter(new ArrayList<Note>(), note -> deleteNote(note)); // Передаем список заметок и слушатель
        recyclerView.setAdapter(adapter);

        // Загрузка заметок
        loadNotes();

        // Обработчик кнопки добавления заметки
        addNoteButton.setOnClickListener(v -> addNote());

        return view;
    }

    private void loadNotes() {
        db.noteDao().getAllNotes().observe(getViewLifecycleOwner(), notes -> {
            adapter.setNotes(notes);
        });
    }

    private void addNote() {
        String title = noteTitleInput.getText().toString().trim();
        String content = noteContentInput.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(getContext(), "Заполните все поля!", Toast.LENGTH_SHORT).show();
            return;
        }

        Note note = new Note(title, content, System.currentTimeMillis());

        // Сохранение заметки в базу данных
        Executors.newSingleThreadExecutor().execute(() -> {
            db.noteDao().insert(note);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Заметка сохранена!", Toast.LENGTH_SHORT).show();
                noteTitleInput.setText("");
                noteContentInput.setText("");
            });
        });
    }

    private void deleteNote(Note note) {
        // Удаление заметки из базы данных
        Executors.newSingleThreadExecutor().execute(() -> {
            db.noteDao().delete(note);  // Метод delete() должен быть в Dao
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Заметка удалена!", Toast.LENGTH_SHORT).show();
            });
        });
    }
}
