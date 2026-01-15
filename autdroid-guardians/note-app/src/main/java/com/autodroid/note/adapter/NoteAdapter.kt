package com.autodroid.note.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.note.R
import com.autodroid.note.model.Note

class NoteAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onDeleteClick: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.tvNoteTitle)
        private val contentTextView: TextView = itemView.findViewById(R.id.tvNoteContent)
        private val dateTextView: TextView = itemView.findViewById(R.id.tvNoteDate)

        fun bind(note: Note) {
            titleTextView.text = note.title
            contentTextView.text = note.content
            
            // Show truncated content if too long
            val contentText = if (note.content.length > 100) {
                note.content.substring(0, 100) + "..."
            } else {
                note.content
            }
            
            contentTextView.text = contentText
            dateTextView.text = note.updatedAt

            itemView.setOnClickListener {
                onNoteClick(note)
            }
            
            itemView.setOnLongClickListener {
                onDeleteClick(note)
                true
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}