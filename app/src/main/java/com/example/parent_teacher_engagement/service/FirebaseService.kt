package com.example.parent_teacher_engagement.service

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID
import java.util.ArrayList

class FirebaseService private constructor() {
    private val database = FirebaseDatabase.getInstance("https://finalproject-374f5-default-rtdb.europe-west1.firebasedatabase.app")
    private val auth = FirebaseAuth.getInstance()
    var currentUserData: UserData? = null
        private set

    companion object {
        @Volatile
        private var instance: FirebaseService? = null

        fun getInstance(): FirebaseService {
            return instance ?: synchronized(this) {
                instance ?: FirebaseService().also { instance = it }
            }
        }
    }

    init {
        // Fetch user data when authenticated
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                fetchUserData(user.uid) { userData ->
                    currentUserData = userData
                }
            } else {
                currentUserData = null
            }
        }
    }

    fun getGrade(
        studentId: String,
        semester: String,
        subject: String,
        onComplete: (String?) -> Unit
    ) {
        database.reference.child("grades")
            .child(semester)
            .child(studentId)
            .child(subject)
            .get()
            .addOnSuccessListener { snapshot ->
                val grade = snapshot.getValue(String::class.java)
                onComplete(grade)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error getting grade: ${exception.message}")
                onComplete(null)
            }
    }

    // Data classes for user management
    data class UserData(
        val uid: String = "",
        val email: String = "",
        val role: String = "",
        val name: String = "",
        val childIds: List<String> = emptyList()
    )

    data class StudentData(
        val uid: String = "",
        val name: String = "",
        val grade: String = "",
        val parentIds: List<String> = emptyList()
    )

    // Event data class for school events
    data class Event(
        val id: String = "",
        val title: String = "",
        val description: String = "",
        val date: Long = 0,
        val createdBy: String = ""
    )

    // Meeting request data classes
    enum class MeetingStatus {
        PENDING, APPROVED, REJECTED, COMPLETED, CANCELLED
    }

    data class MeetingRequest(
        val id: String = UUID.randomUUID().toString(),
        val requesterId: String = "",
        val requesterName: String = "",
        val recipientId: String = "",
        val recipientName: String = "",
        val title: String = "",
        val description: String = "",
        val proposedDate: Long = 0,
        val proposedTime: String = "",
        val status: MeetingStatus = MeetingStatus.PENDING,
        val createdAt: Long = System.currentTimeMillis(),
        val updatedAt: Long = System.currentTimeMillis(),
        val notes: String = ""
    )

    // User authentication and data retrieval methods
    fun fetchUserData(uid: String, onComplete: (UserData?) -> Unit) {
        database.reference.child("users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                val userData = snapshot.getValue(UserData::class.java)
                onComplete(userData)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error fetching user data: ${exception.message}")
                onComplete(null)
            }
    }

    fun getChildrenForParent(parentId: String, onComplete: (List<StudentData>) -> Unit) {
        database.reference.child("users").child(parentId).child("childIds").get()
            .addOnSuccessListener { snapshot ->
                val childIds = ArrayList<String>()
                for (childIdSnapshot in snapshot.children) {
                    val childId = childIdSnapshot.getValue(String::class.java)
                    if (childId != null) {
                        childIds.add(childId)
                    }
                }
                
                if (childIds.isEmpty()) {
                    onComplete(emptyList())
                    return@addOnSuccessListener
                }
                
                val studentsList = mutableListOf<StudentData>()
                var completedQueries = 0
                
                for (childId in childIds) {
                    database.reference.child("students").child(childId).get()
                        .addOnSuccessListener { studentSnapshot ->
                            val student = studentSnapshot.getValue(StudentData::class.java)
                            if (student != null) {
                                studentsList.add(student)
                            }
                            completedQueries++
                            if (completedQueries == childIds.size) {
                                onComplete(studentsList)
                            }
                        }
                        .addOnFailureListener {
                            completedQueries++
                            if (completedQueries == childIds.size) {
                                onComplete(studentsList)
                            }
                        }
                }
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }

    fun getUpcomingEvents(onComplete: (List<Event>) -> Unit) {
        database.reference.child("events").orderByChild("date").get()
            .addOnSuccessListener { snapshot ->
                val events = mutableListOf<Event>()
                for (eventSnapshot in snapshot.children) {
                    val event = eventSnapshot.getValue(Event::class.java)
                    if (event != null && event.date >= System.currentTimeMillis()) {
                        events.add(event)
                    }
                }
                onComplete(events)
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }

    // Meeting request methods
    fun createMeetingRequest(meetingRequest: MeetingRequest, onComplete: (Boolean) -> Unit) {
        database.reference.child("meetings").child(meetingRequest.id).setValue(meetingRequest)
            .addOnSuccessListener {
                sendNotification(
                    meetingRequest.recipientId,
                    "New Meeting Request",
                    "${meetingRequest.requesterName} has requested a meeting with you."
                )
                onComplete(true)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error creating meeting request: ${exception.message}")
                onComplete(false)
            }
    }

    fun updateMeetingRequest(meetingId: String, updates: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        database.reference.child("meetings").child(meetingId).updateChildren(updates)
            .addOnSuccessListener {
                // Get the meeting to send a notification
                getMeetingById(meetingId) { meeting ->
                    meeting?.let {
                        val status = updates["status"] as? String
                        if (status != null) {
                            val recipientId = if (currentUserData?.uid == meeting.requesterId) {
                                meeting.recipientId
                            } else {
                                meeting.requesterId
                            }
                            
                            val statusText = when(status) {
                                MeetingStatus.APPROVED.name -> "approved"
                                MeetingStatus.REJECTED.name -> "rejected"
                                MeetingStatus.COMPLETED.name -> "marked as completed"
                                MeetingStatus.CANCELLED.name -> "cancelled"
                                else -> "updated"
                            }
                            
                            sendNotification(
                                recipientId,
                                "Meeting Request $statusText",
                                "Your meeting request '${meeting.title}' has been $statusText."
                            )
                        }
                    }
                }
                onComplete(true)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error updating meeting request: ${exception.message}")
                onComplete(false)
            }
    }

    fun getMeetingById(meetingId: String, onComplete: (MeetingRequest?) -> Unit) {
        database.reference.child("meetings").child(meetingId).get()
            .addOnSuccessListener { snapshot ->
                val meeting = snapshot.getValue(MeetingRequest::class.java)
                onComplete(meeting)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error getting meeting: ${exception.message}")
                onComplete(null)
            }
    }

    fun getMeetingsForUser(userId: String, onComplete: (List<MeetingRequest>) -> Unit) {
        database.reference.child("meetings").get()
            .addOnSuccessListener { snapshot ->
                val meetings = mutableListOf<MeetingRequest>()
                for (meetingSnapshot in snapshot.children) {
                    val meeting = meetingSnapshot.getValue(MeetingRequest::class.java)
                    if (meeting != null && (meeting.requesterId == userId || meeting.recipientId == userId)) {
                        meetings.add(meeting)
                    }
                }
                onComplete(meetings)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error getting meetings: ${exception.message}")
                onComplete(emptyList())
            }
    }

    fun getAllMeetings(onComplete: (List<MeetingRequest>) -> Unit) {
        database.reference.child("meetings").get()
            .addOnSuccessListener { snapshot ->
                val meetings = mutableListOf<MeetingRequest>()
                for (meetingSnapshot in snapshot.children) {
                    val meeting = meetingSnapshot.getValue(MeetingRequest::class.java)
                    if (meeting != null) {
                        meetings.add(meeting)
                    }
                }
                onComplete(meetings)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error getting all meetings: ${exception.message}")
                onComplete(emptyList())
            }
    }

    fun getAllTeachers(onComplete: (List<UserData>) -> Unit) {
        database.reference.child("users").orderByChild("role").equalTo("teacher").get()
            .addOnSuccessListener { snapshot ->
                val teachers = mutableListOf<UserData>()
                for (teacherSnapshot in snapshot.children) {
                    val teacher = teacherSnapshot.getValue(UserData::class.java)
                    if (teacher != null) {
                        teachers.add(teacher)
                    }
                }
                onComplete(teachers)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error getting teachers: ${exception.message}")
                onComplete(emptyList())
            }
    }

    fun getAllParents(onComplete: (List<UserData>) -> Unit) {
        database.reference.child("users").orderByChild("role").equalTo("parent").get()
            .addOnSuccessListener { snapshot ->
                val parents = mutableListOf<UserData>()
                for (parentSnapshot in snapshot.children) {
                    val parent = parentSnapshot.getValue(UserData::class.java)
                    if (parent != null) {
                        parents.add(parent)
                    }
                }
                onComplete(parents)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error getting parents: ${exception.message}")
                onComplete(emptyList())
            }
    }

    private fun sendNotification(recipientId: String, title: String, message: String) {
        val notification = mapOf(
            "userId" to recipientId,
            "title" to title,
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "read" to false
        )
        
        val notificationId = UUID.randomUUID().toString()
        database.reference.child("notifications").child(recipientId).child(notificationId).setValue(notification)
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error sending notification: ${exception.message}")
            }
    }
}