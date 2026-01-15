package com.autodroid.note.data.repository

import com.autodroid.note.data.dao.NoteDao
import com.autodroid.note.model.Note

class NoteRepository(private val noteDao: NoteDao) {
    
    suspend fun getAllNotes(): List<Note> = noteDao.getAllNotes()
    
    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)
    
    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)
    
    suspend fun updateNote(note: Note) = noteDao.updateNote(note)
    
    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)
    
    suspend fun deleteNoteById(id: Long) = noteDao.deleteNoteById(id)
    
    suspend fun searchNotes(query: String): List<Note> = noteDao.searchNotes(query)
}