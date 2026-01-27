package com.autodroid.teachitback.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KnowledgeBaseManager {
    private val knowledgeBase = mutableListOf<KnowledgeBaseEntry>()
    
    fun addEntry(entry: KnowledgeBaseEntry) {
        knowledgeBase.add(entry)
    }
    
    fun addEntries(entries: List<KnowledgeBaseEntry>) {
        knowledgeBase.addAll(entries)
    }
    
    fun getAllEntries(): List<KnowledgeBaseEntry> {
        return knowledgeBase.toList()
    }
    
    fun getEntriesByCategory(category: String): List<KnowledgeBaseEntry> {
        return knowledgeBase.filter { it.category == category }
    }
    
    fun searchByTag(tag: String): List<KnowledgeBaseEntry> {
        return knowledgeBase.filter { tag in it.tags }
    }
    
    fun getEntryById(id: String): KnowledgeBaseEntry? {
        return knowledgeBase.find { it.id == id }
    }
    
    fun clear() {
        knowledgeBase.clear()
    }
    
    suspend fun loadFromFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = java.io.File(filePath)
            if (!file.exists()) return@withContext false
            
            val lines = file.readLines()
            lines.forEach { line ->
                val parts = line.split("|")
                if (parts.size >= 3) {
                    val entry = KnowledgeBaseEntry(
                        id = parts[0],
                        question = parts[1],
                        answer = parts[2],
                        category = if (parts.size > 3) parts[3] else "",
                        tags = if (parts.size > 4) parts[4].split(",") else emptyList()
                    )
                    knowledgeBase.add(entry)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
