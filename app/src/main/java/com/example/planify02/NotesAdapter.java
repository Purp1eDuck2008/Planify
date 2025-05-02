package com.example.planify02;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton; // Для кнопки удаления
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.planify02.entities.Note;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    private List<Note> notes;
    private OnNoteDeleteListener onNoteDeleteListener; // Интерфейс для удаления заметки

    // Конструктор с передачей слушателя для удаления
    public NotesAdapter(List<Note> notes, OnNoteDeleteListener onNoteDeleteListener) {
        this.notes = notes;
        this.onNoteDeleteListener = onNoteDeleteListener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Замените R.layout.note_item на R.layout.item_note
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.title.setText(note.getTitle());
        holder.content.setText(note.getContent());

        // Обработчик нажатия на кнопку удаления
        holder.deleteButton.setOnClickListener(v -> {
            if (onNoteDeleteListener != null) {
                onNoteDeleteListener.onDelete(note);  // Удаление заметки
            }
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    // Метод для обновления данных в адаптере
    public void setNotes(List<Note> notes) {
        this.notes = notes;
        notifyDataSetChanged();
    }

    // Интерфейс для обработки удаления
    public interface OnNoteDeleteListener {
        void onDelete(Note note);
    }

    // Вьюхолдер для элемента списка
    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, content;
        MaterialButton deleteButton;  // Кнопка для удаления заметки

        public NoteViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.note_title);
            content = itemView.findViewById(R.id.note_content);
            deleteButton = itemView.findViewById(R.id.delete_button); // Инициализация кнопки удаления
        }
    }
}
