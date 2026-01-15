package com.autodroid.note.model

data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String
)