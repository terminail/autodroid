package com.autodroid.note.ui.editor

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.autodroid.note.R
import com.autodroid.note.data.database.NoteDatabase
import com.autodroid.note.data.repository.NoteRepository
import com.autodroid.note.model.Note
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var etNoteTitle: TextInputEditText
    private lateinit var etNoteContent: TextInputEditText
    private lateinit var noteRepository: NoteRepository
    
    private var noteId: Long = -1
    private var isExistingNote = false
    
    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_editor)

        // Initialize database and repository
        val database = NoteDatabase.getDatabase(this)
        noteRepository = NoteRepository(database.noteDao())

        setupViews()
        parseIntent()
        loadNoteIfExists()
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        etNoteTitle = findViewById(R.id.etNoteTitle)
        etNoteContent = findViewById(R.id.etNoteContent)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun parseIntent() {
        noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1)
        isExistingNote = noteId != -1L
        if (isExistingNote) {
            supportActionBar?.title = "编辑笔记"
        } else {
            supportActionBar?.title = "新建笔记"
        }
    }

    private fun loadNoteIfExists() {
        if (isExistingNote) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val note = noteRepository.getNoteById(noteId)
                    note?.let { 
                        launch(Dispatchers.Main) {
                            etNoteTitle.setText(it.title)
                            etNoteContent.setText(it.content)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.editor_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_save -> {
                saveNote()
                true
            }
            R.id.action_delete -> {
                if (isExistingNote) {
                    showDeleteConfirmationDialog()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveNote() {
        val title = etNoteTitle.text.toString().trim()
        val content = etNoteContent.text.toString().trim()

        if (title.isEmpty() && content.isEmpty()) {
            // If both title and content are empty, we don't want to save
            finish()
            return
        }

        val note = if (!isExistingNote) {
            // Creating a new note
            Note(
                title = if (title.isEmpty()) "无标题" else title,
                content = content,
                updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
        } else {
            // Updating an existing note
            Note(
                id = noteId,
                title = if (title.isEmpty()) "无标题" else title,
                content = content,
                updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!isExistingNote) {
                    noteRepository.insertNote(note)
                } else {
                    noteRepository.updateNote(note)
                }
                launch(Dispatchers.Main) {
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    // Handle error
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("删除笔记")
            .setMessage("确定要删除这条笔记吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteCurrentNote()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteCurrentNote() {
        if (isExistingNote) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    noteRepository.deleteNoteById(noteId)
                    launch(Dispatchers.Main) {
                        finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    launch(Dispatchers.Main) {
                        // Handle error
                    }
                }
            }
        }
    }
}