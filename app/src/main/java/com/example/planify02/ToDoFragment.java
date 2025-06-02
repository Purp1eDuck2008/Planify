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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
public class ToDoFragment extends Fragment {

    private TextInputEditText noteTitleInput, noteContentInput;
    private MaterialButton addNoteButton;
    private RecyclerView recyclerView;
    private NotesAdapter adapter;
    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_to_do, container, false);

        noteTitleInput = view.findViewById(R.id.note_title_input);
        noteContentInput = view.findViewById(R.id.note_content_input);
        addNoteButton = view.findViewById(R.id.add_note_button);
        recyclerView = view.findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        db = Room.databaseBuilder(requireContext(),
                        AppDatabase.class, "notes-db")
                .fallbackToDestructiveMigration()
                .build();

        adapter = new NotesAdapter(new ArrayList<>(), this::deleteNote);
        recyclerView.setAdapter(adapter);

        loadNotes();

        addNoteButton.setOnClickListener(v -> addNote());

        return view;
    }

    private void loadNotes() {
        db.noteDao().getAllNotes().observe(getViewLifecycleOwner(), notes -> {
            if (notes != null) {
                adapter.setNotes(notes);
            }
        });
    }

    private void addNote() {
        String title = noteTitleInput.getText().toString().trim();
        String content = noteContentInput.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Введите заголовок!", Toast.LENGTH_SHORT).show();
            return;
        }

        Note note = new Note(title, content, System.currentTimeMillis());

        Executors.newSingleThreadExecutor().execute(() -> {
            db.noteDao().insert(note);
            requireActivity().runOnUiThread(() -> {
                noteTitleInput.setText("");
                noteContentInput.setText("");
                noteTitleInput.requestFocus();
            });
        });
    }

    private void deleteNote(Note note) {
        Executors.newSingleThreadExecutor().execute(() -> {
            db.noteDao().delete(note);
        });
    }
}

