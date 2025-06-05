package com.soft.bookteria.helpers

import com.soft.bookteria.api.models.Author
import java.util.Locale

object BookUtils{
    fun getAuthorsAsString(authors: List<Author>): String {
        return if (authors.isEmpty()) {
            "Unknown Author"
        } else {
            authors.joinToString(", ") { fixAuthorName(it.name) }
        }
    }
    private fun fixAuthorName(name: String): String {
        return name.split(",").reversed().joinToString(" ") { it.trim() }
    }
    
    fun getSubjectsAsString(subjects: List<String>, limit: Int): String {
        val allSubjects = subjects.flatMap { it.split("--") }.map { it.trim() }.toSet()
        val truncatedSubs = if (allSubjects.size > limit) allSubjects.take(limit) else allSubjects
        return truncatedSubs.joinToString(", ")
    }
}