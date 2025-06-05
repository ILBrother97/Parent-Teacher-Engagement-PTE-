package com.example.parent_teacher_engagement.model

import java.io.Serializable

data class MeetingRequest(
    val id: String = "",                        // Unique identifier
    val requesterId: String = "",               // User who requested the meeting
    val requesterName: String = "",             // Name of requester
    val requesterRole: String = "",             // Role: parent, teacher, admin
    val recipientId: String = "",               // User who receives the meeting request
    val recipientName: String = "",             // Name of recipient
    val recipientRole: String = "",             // Role: parent, teacher, admin
    val title: String = "",                     // Meeting title/purpose
    val description: String = "",               // Detailed description
    val proposedDate: String = "",              // Proposed date
    val proposedTime: String = "",              // Proposed time
    val status: MeetingStatus = MeetingStatus.PENDING, // Status
    val createdAt: Long = System.currentTimeMillis(),  // Timestamp when created
    val updatedAt: Long = System.currentTimeMillis(),  // Timestamp when last updated
    val location: String = "Online",            // Meeting location (default online)
    val relatedToStudentId: String? = null,     // If meeting is about a specific student
    val suggestedAlternatives: List<AlternativeTime> = emptyList(), // Alternative meeting time suggestions
    val responseMessage: String = ""            // Message from recipient when proposing alternatives
) : Serializable {
    
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "requesterId" to requesterId,
            "requesterName" to requesterName,
            "requesterRole" to requesterRole,
            "recipientId" to recipientId,
            "recipientName" to recipientName,
            "recipientRole" to recipientRole,
            "title" to title,
            "description" to description,
            "proposedDate" to proposedDate,
            "proposedTime" to proposedTime,
            "status" to status.name,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "location" to location,
            "relatedToStudentId" to relatedToStudentId,
            "suggestedAlternatives" to suggestedAlternatives.map { it.toMap() },
            "responseMessage" to responseMessage
        )
    }
    
    companion object {
        fun fromMap(map: Map<String, Any?>): MeetingRequest {
            val alternativesData = map["suggestedAlternatives"] as? List<Map<String, Any?>> ?: emptyList()
            val alternatives = alternativesData.map { AlternativeTime.fromMap(it) }
            
            return MeetingRequest(
                id = map["id"] as? String ?: "",
                requesterId = map["requesterId"] as? String ?: "",
                requesterName = map["requesterName"] as? String ?: "",
                requesterRole = map["requesterRole"] as? String ?: "",
                recipientId = map["recipientId"] as? String ?: "",
                recipientName = map["recipientName"] as? String ?: "",
                recipientRole = map["recipientRole"] as? String ?: "",
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                proposedDate = map["proposedDate"] as? String ?: "",
                proposedTime = map["proposedTime"] as? String ?: "",
                status = MeetingStatus.valueOf(map["status"] as? String ?: MeetingStatus.PENDING.name),
                createdAt = map["createdAt"] as? Long ?: System.currentTimeMillis(),
                updatedAt = map["updatedAt"] as? Long ?: System.currentTimeMillis(),
                location = map["location"] as? String ?: "Online",
                relatedToStudentId = map["relatedToStudentId"] as? String,
                suggestedAlternatives = alternatives,
                responseMessage = map["responseMessage"] as? String ?: ""
            )
        }
    }
}

// Model for alternative time suggestions
data class AlternativeTime(
    val date: String,
    val time: String,
    val suggestedBy: String, // User ID of who suggested this time
    val suggestedAt: Long = System.currentTimeMillis(),
    val isSelected: Boolean = false   // If this alternative has been selected
) : Serializable {
    
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "date" to date,
            "time" to time,
            "suggestedBy" to suggestedBy,
            "suggestedAt" to suggestedAt,
            "isSelected" to isSelected
        )
    }
    
    companion object {
        fun fromMap(map: Map<String, Any?>): AlternativeTime {
            return AlternativeTime(
                date = map["date"] as? String ?: "",
                time = map["time"] as? String ?: "",
                suggestedBy = map["suggestedBy"] as? String ?: "",
                suggestedAt = map["suggestedAt"] as? Long ?: System.currentTimeMillis(),
                isSelected = map["isSelected"] as? Boolean ?: false
            )
        }
    }
}

enum class MeetingStatus {
    PENDING, ACCEPTED, REJECTED, COMPLETED, CANCELLED, RESCHEDULED
} 