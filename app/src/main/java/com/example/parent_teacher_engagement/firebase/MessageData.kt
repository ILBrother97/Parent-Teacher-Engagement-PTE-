package com.example.parent_teacher_engagement.firebase

data class MessageData(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val senderName: String = ""
)