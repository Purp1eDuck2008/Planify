package com.example.planify02;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.planify02.entities.Note;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    private List<Note> notes;
    private OnNoteDeleteListener onNoteDeleteListener;

    public NotesAdapter(List<Note> notes, OnNoteDeleteListener onNoteDeleteListener) {
        this.notes = notes;
        this.onNoteDeleteListener = onNoteDeleteListener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        if (note != null) {
            holder.title.setText(note.getTitle());
            holder.content.setText(note.getContent());

            holder.deleteButton.setOnClickListener(v -> {
                if (onNoteDeleteListener != null) {
                    onNoteDeleteListener.onDelete(note);
                }
            });
        }
    }


    @Override
    public int getItemCount() {
        return notes.size();
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
        notifyDataSetChanged();
    }

    public interface OnNoteDeleteListener {
        void onDelete(Note note);
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, content;
        MaterialButton deleteButton;
        public NoteViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.note_title);
            content = itemView.findViewById(R.id.note_content);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}
