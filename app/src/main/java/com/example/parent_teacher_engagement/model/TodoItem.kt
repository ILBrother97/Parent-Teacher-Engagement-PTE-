package com.example.parent_teacher_engagement.model

data class TodoItem(
    val id: String,
    val title: String,
    val description: String,
    val dueDate: String,
    val dueTime: String,
    val priority: Priority,
    val category: String,
    val isCompleted: Boolean = false,
    val meetingId: String? = null
) {
    enum class Priority {
        LOW,
        MEDIUM,
        HIGH
    }
} 