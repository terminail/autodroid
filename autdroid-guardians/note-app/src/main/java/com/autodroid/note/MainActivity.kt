package com.autodroid.note

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.guardiansdk.GuardianSdk
import com.autodroid.note.adapter.NoteAdapter
import com.autodroid.note.data.database.NoteDatabase
import com.autodroid.note.data.repository.NoteRepository
import com.autodroid.note.ui.editor.NoteEditorActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var noteRepository: NoteRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化 Guardian SDK
        GuardianSdk.initialize(this)

        // Initialize database and repository
        val database = NoteDatabase.getDatabase(this)
        noteRepository = NoteRepository(database.noteDao())

        setupViews()
        loadNotes()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerViewNotes)
        fabAddNote = findViewById(R.id.fabAddNote)

        noteAdapter = NoteAdapter(
            onNoteClick = { note ->
                // Open note for editing
                val intent = Intent(this, NoteEditorActivity::class.java).apply {
                    putExtra(NoteEditorActivity.EXTRA_NOTE_ID, note.id)
                }
                startActivity(intent)
            },
            onDeleteClick = { note ->
                showDeleteConfirmationDialog(note)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = noteAdapter
        }

        fabAddNote.setOnClickListener {
            // Open editor to create a new note
            val intent = Intent(this, NoteEditorActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadNotes() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notes = noteRepository.getAllNotes()
                launch(Dispatchers.Main) {
                    noteAdapter.submitList(notes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showDeleteConfirmationDialog(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("删除笔记")
            .setMessage("确定要删除这条笔记吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteNote(note)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteNote(note: Note) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                noteRepository.deleteNote(note)
                launch(Dispatchers.Main) {
                    loadNotes() // Refresh the list
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotes() // Reload notes when returning to main activity
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                GuardianSdk.getInstance().startSettingActivity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
