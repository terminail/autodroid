package com.autodroid.teachitback.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FileEntity(
    val id: String = "",
    val topicId: String,
    val fileName: String,
    val content: String,
    val fileType: String = "txt",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable