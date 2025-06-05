package com.example.parent_teacher_engagement.firebase

import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import com.example.parent_teacher_engagement.screens.Message
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class FirebaseService private constructor() {
    private val auth = FirebaseAuth.getInstance()
    val database: FirebaseDatabase
    private val storage: FirebaseStorage
    
    init {
        database = FirebaseDatabase.getInstance("https://finalproject-374f5-default-rtdb.europe-west1.firebasedatabase.app")
        database.setPersistenceEnabled(true) // Enable offline persistence
        
        // Initialize Firebase Storage with the correct bucket
        storage = FirebaseStorage.getInstance("gs://finalproject-374f5.appspot.com")
        
        Log.d("FirebaseService", "Database URL: ${database.reference}")
        Log.d("FirebaseService", "Storage bucket: ${storage.reference}")

        // Add auth state listener to maintain user session
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in, fetch their data
                getCurrentUserData { userData ->
                    currentUserData = userData
                }
            } else {
                // User is signed out, clear current user data
                currentUserData = null
            }
        }
    }
    
    var currentUserData: UserData? = null
        private set
        
    var currentChatPartnerId: String? = null

    // Navigation callback for chat screen
    var onNavigateToChatScreen: ((String) -> Unit)? = null

    companion object {
        @Volatile
        private var instance: FirebaseService? = null

        fun getInstance(): FirebaseService {
            return instance ?: synchronized(this) {
                instance ?: FirebaseService().also { instance = it }
            }
        }
    }

    fun createTeacher(
        name: String,
        email: String,
        password: String,
        subject: String,
        grades: List<String>
    ): Task<Void> {
        return auth.createUserWithEmailAndPassword(email, password)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                val userId = task.result?.user?.uid ?: throw Exception("Failed to get user ID")
                val userData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "role" to "teacher",
                    "subject" to subject,
                    "grades" to grades
                )
                database.reference.child("users").child(userId).setValue(userData)
            }
    }

    fun createParent(name: String, email: String, password: String, childEmails: List<String>): Task<String> {
        val taskCompletionSource = TaskCompletionSource<String>()
        
        Log.d("FirebaseService", "Creating parent with email: $email")
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: run {
                    Log.e("FirebaseService", "Failed to get user ID after authentication")
                    taskCompletionSource.setException(Exception("Failed to get user ID"))
                    return@addOnSuccessListener
                }
                
                Log.d("FirebaseService", "User created with ID: $userId")
                
                val parentData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "role" to "parent",
                    "childEmails" to childEmails
                )
                
                database.reference.child("users").child(userId).setValue(parentData)
                    .addOnSuccessListener {
                        Log.d("FirebaseService", "Parent data saved successfully")
                        taskCompletionSource.setResult(userId)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseService", "Failed to save parent data", exception)
                        taskCompletionSource.setException(exception)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to create user authentication", exception)
                taskCompletionSource.setException(exception)
            }
            
        return taskCompletionSource.task
    }

    fun createStudent(name: String, email: String, password: String, grade: String): Task<String> {
        val taskCompletionSource = TaskCompletionSource<String>()
        
        Log.d("FirebaseService", "Creating student with email: $email")
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: run {
                    Log.e("FirebaseService", "Failed to get user ID after authentication")
                    taskCompletionSource.setException(Exception("Failed to get user ID"))
                    return@addOnSuccessListener
                }

                // Create default subjects list
                val defaultSubjects = listOf("English", "Civic", "Economics", "History", "Physics", 
                    "Chemistry", "Geography", "It", "Math", "Biology", "Oromo", "Amharic", "Sport")

                // Create student data with default subjects
                val studentData = mapOf(
                    "name" to name,
                    "email" to email,
                    "role" to "student",
                    "grade" to grade,
                    "subjects" to defaultSubjects
                )

                // Save student data to database
                database.reference.child("users").child(userId).setValue(studentData)
                    .addOnSuccessListener {
                        Log.d("FirebaseService", "Student data saved successfully")
                        taskCompletionSource.setResult(userId)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseService", "Failed to save student data", exception)
                        taskCompletionSource.setException(exception)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to create user authentication", exception)
                taskCompletionSource.setException(exception)
            }
            
        return taskCompletionSource.task
    }

    fun createUser(email: String, password: String, name: String, role: String): Task<String> {
        val taskCompletionSource = TaskCompletionSource<String>()
        
        Log.d("FirebaseService", "Creating user with email: $email")
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: run {
                    Log.e("FirebaseService", "Failed to get user ID after authentication")
                    taskCompletionSource.setException(Exception("Failed to get user ID"))
                    return@addOnSuccessListener
                }
                
                Log.d("FirebaseService", "User created with ID: $userId")
                
                val userData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "role" to role
                )
                
                database.reference.child("users").child(userId).setValue(userData)
                    .addOnSuccessListener {
                        Log.d("FirebaseService", "User data saved successfully")
                        taskCompletionSource.setResult(userId)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseService", "Failed to save user data", exception)
                        taskCompletionSource.setException(exception)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to create user authentication", exception)
                taskCompletionSource.setException(exception)
            }
            
        return taskCompletionSource.task
    }

    fun createAdmin(name: String, email: String, password: String): Task<String> {
        val taskCompletionSource = TaskCompletionSource<String>()
        
        Log.d("FirebaseService", "Creating admin with email: $email")
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: run {
                    Log.e("FirebaseService", "Failed to get user ID after authentication")
                    taskCompletionSource.setException(Exception("Failed to get user ID"))
                    return@addOnSuccessListener
                }
                
                Log.d("FirebaseService", "Admin created with ID: $userId")
                
                val adminData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "role" to "admin"
                )
                
                database.reference.child("users").child(userId).setValue(adminData)
                    .addOnSuccessListener {
                        Log.d("FirebaseService", "Admin data saved successfully")
                        taskCompletionSource.setResult(userId)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseService", "Failed to save admin data", exception)
                        taskCompletionSource.setException(exception)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to create admin authentication", exception)
                taskCompletionSource.setException(exception)
            }
            
        return taskCompletionSource.task
    }

    fun signIn(email: String, password: String): Task<UserData> {
        val taskCompletionSource = TaskCompletionSource<UserData>()
        
        Log.d("FirebaseService", "Signing in user with email: $email")
        
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: run {
                    Log.e("FirebaseService", "Failed to get user ID after authentication")
                    taskCompletionSource.setException(Exception("Failed to get user ID"))
                    return@addOnSuccessListener
                }
                
                Log.d("FirebaseService", "User signed in with ID: $userId")
                
                database.reference.child("users").child(userId).get()
                    .addOnSuccessListener { snapshot ->
                        Log.d("FirebaseService", "Got data snapshot: exists=${snapshot.exists()}")
                        if (!snapshot.exists()) {
                            Log.e("FirebaseService", "User data not found")
                            taskCompletionSource.setException(Exception("User data not found"))
                            return@addOnSuccessListener
                        }
                        
                        try {
                            Log.d("FirebaseService", "Raw snapshot data: ${snapshot.value}")
                            val map = snapshot.value as? Map<*, *>
                            val name = map?.get("name")?.toString()
                            val email = map?.get("email")?.toString()
                            val role = map?.get("role")?.toString()
                            val subject = map?.get("subject")?.toString()
                            val grade = map?.get("grade")?.toString()
                            val childEmails = when (val emailsValue = map?.get("childEmails")) {
                                is List<*> -> emailsValue.filterIsInstance<String>().ifEmpty { null }
                                else -> null
                            }
                            val grades = when (val gradesValue = map?.get("grades")) {
                                is List<*> -> gradesValue.filterIsInstance<String>().ifEmpty { null }
                                else -> null
                            }
                            
                            Log.d("FirebaseService", "Parsed data - name: $name, email: $email, role: $role")
                            
                            if (name == null || email == null || role == null) {
                                Log.e("FirebaseService", "Missing required user data fields")
                                taskCompletionSource.setException(Exception("Missing required user data fields"))
                                return@addOnSuccessListener
                            }
                            
                            val userData = UserData(
                                uid = userId,
                                name = name,
                                email = email,
                                role = role,
                                subject = subject,
                                grade = grade,
                                grades = grades,
                                childEmails = childEmails
                            )
                            
                            // Set currentUserData before completing the task
                            currentUserData = userData
                            Log.d("FirebaseService", "Successfully signed in and set currentUserData: $userData")
                            taskCompletionSource.setResult(userData)
                        } catch (e: Exception) {
                            Log.e("FirebaseService", "Failed to parse user data", e)
                            taskCompletionSource.setException(e)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseService", "Failed to retrieve user data", exception)
                        taskCompletionSource.setException(exception)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to sign in user", exception)
                taskCompletionSource.setException(exception)
            }
            
        return taskCompletionSource.task
    }

    fun getCurrentUserData(onSuccess: (UserData?) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onSuccess(null)
            return
        }

        database.reference.child("users").child(currentUser.uid).get()
            .addOnSuccessListener { snapshot ->
                try {
                    val map = snapshot.value as? Map<*, *>
                    if (map != null) {
                        val name = map["name"]?.toString()
                        val email = map["email"]?.toString()
                        val role = map["role"]?.toString()
                        val subject = map["subject"]?.toString()
                        val grade = map["grade"]?.toString()
                        val childEmails = when (val emailsValue = map["childEmails"]) {
                            is List<*> -> emailsValue.filterIsInstance<String>().ifEmpty { null }
                            else -> null
                        }
                        val grades = when (val gradesValue = map["grades"]) {
                            is List<*> -> gradesValue.filterIsInstance<String>().ifEmpty { null }
                            else -> null
                        }
                        
                        Log.d("FirebaseService", "Parsed data - name: $name, email: $email, role: $role")
                        
                        if (role == null) {
                            Log.e("FirebaseService", "Role is null in user data")
                            onSuccess(null)
                            return@addOnSuccessListener
                        }
                        
                        val userData = UserData(
                            uid = snapshot.key ?: "",
                            name = name,
                            email = email,
                            role = role,
                            subject = subject,
                            grade = grade,
                            grades = grades,
                            childEmails = childEmails
                        )
                        
                        onSuccess(userData)
                    } else {
                        Log.e("FirebaseService", "User data map is null")
                        onSuccess(null)
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseService", "Error parsing user data", e)
                    onSuccess(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to get user data", exception)
                onSuccess(null)
            }
    }

    fun getUserCountByRole(role: String, callback: (Int) -> Unit) {
        Log.d("FirebaseService", "Getting user count by role: $role")
        
        database.reference.child("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val count = snapshot.children.count { 
                    it.child("role").getValue(String::class.java) == role 
                }
                Log.d("FirebaseService", "User count by role: $count")
                callback(count)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to get user count by role", exception)
                callback(0)
            }
    }

    fun getAllCounts(callback: (Map<String, Int>) -> Unit) {
        Log.d("FirebaseService", "Getting all user counts")
        
        val counts = mutableMapOf<String, Int>()
        var completedQueries = 0
        val totalQueries = 3 // Updated to include problem reports
        
        // Get user counts
        database.reference.child("users")
            .get()
            .addOnSuccessListener { snapshot ->
                counts["teachers"] = snapshot.children.count { 
                    it.child("role").getValue(String::class.java) == "teacher" 
                }
                counts["parents"] = snapshot.children.count { 
                    it.child("role").getValue(String::class.java) == "parent" 
                }
                counts["students"] = snapshot.children.count { 
                    it.child("role").getValue(String::class.java) == "student" 
                }
                completedQueries++
                if (completedQueries == totalQueries) {
                    Log.d("FirebaseService", "Final counts: $counts")
                    callback(counts)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to get user counts", exception)
                completedQueries++
                if (completedQueries == totalQueries) {
                    callback(counts)
                }
            }

        // Get events count
        database.reference.child("events")
            .get()
            .addOnSuccessListener { snapshot ->
                counts["events"] = snapshot.children.count()
                completedQueries++
                if (completedQueries == totalQueries) {
                    Log.d("FirebaseService", "Final counts: $counts")
                    callback(counts)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to get events count", exception)
                counts["events"] = 0
                completedQueries++
                if (completedQueries == totalQueries) {
                    callback(counts)
                }
            }
            
        // Get problem reports count
        database.reference.child("problem_reports")
            .get()
            .addOnSuccessListener { snapshot ->
                // Count only non-resolved problems (status != "resolved")
                val activeProblems = snapshot.children.count { problemSnapshot -> 
                    val status = problemSnapshot.child("status").getValue(String::class.java)
                    status != "resolved"
                }
                counts["problems"] = activeProblems
                
                // Store active problems count in a separate node for easy access
                database.reference.child("counts").child("problems_active").setValue(activeProblems)
                
                completedQueries++
                if (completedQueries == totalQueries) {
                    Log.d("FirebaseService", "Final counts: $counts")
                    callback(counts)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to get problem reports count", exception)
                counts["problems"] = 0
                completedQueries++
                if (completedQueries == totalQueries) {
                    callback(counts)
                }
            }
    }

    fun getUsersByRole(
        role: String,
        onSuccess: (List<UserListData>) -> Unit,
        onError: (Exception) -> Unit,
        isRealtime: Boolean = false
    ) {
        val usersRef = database.reference.child("users")
        val query = usersRef.orderByChild("role").equalTo(role)

        if (isRealtime) {
            // Use ValueEventListener for real-time updates
            query.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val usersList = mutableListOf<UserListData>()
                    snapshot.children.forEach { child ->
                        val userData = child.getValue(UserData::class.java)
                        if (userData != null) {
                            usersList.add(UserListData(
                                id = child.key ?: "",
                                name = userData.name,
                                email = userData.email,
                                role = userData.role
                            ))
                        }
                    }
                    onSuccess(usersList)
                }

                override fun onCancelled(error: DatabaseError) {
                    onError(error.toException())
                }
            })
        } else {
            // Use single value event for one-time load
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val usersList = mutableListOf<UserListData>()
                    snapshot.children.forEach { child ->
                        val userData = child.getValue(UserData::class.java)
                        if (userData != null) {
                            usersList.add(UserListData(
                                id = child.key ?: "",
                                name = userData.name,
                                email = userData.email,
                                role = userData.role
                            ))
                        }
                    }
                    onSuccess(usersList)
                }

                override fun onCancelled(error: DatabaseError) {
                    onError(error.toException())
                }
            })
        }
    }

    // Overloaded version for UserManagementScreen
    fun getUsersByRole(role: String, onComplete: (List<UserListData>?, Exception?) -> Unit) {
        database.reference.child("users").orderByChild("role").equalTo(role)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val users = mutableListOf<UserListData>()
                    for (userSnapshot in snapshot.children) {
                        val user = UserListData(
                            id = userSnapshot.key ?: "",
                            name = userSnapshot.child("name").getValue(String::class.java),
                            email = userSnapshot.child("email").getValue(String::class.java),
                            role = userSnapshot.child("role").getValue(String::class.java),
                            subject = userSnapshot.child("subject").getValue(String::class.java),
                            childEmails = userSnapshot.child("childEmails").getValue(object : GenericTypeIndicator<List<String>>() {})
                                ?: emptyList(),
                            grade = userSnapshot.child("grade").getValue(String::class.java)
                        )
                        users.add(user)
                    }
                    onComplete(users, null)
                }

                override fun onCancelled(error: DatabaseError) {
                    onComplete(null, error.toException())
                }
            })
    }

    fun getUsersByGrade(
        grade: String,
        role: String,
        onSuccess: (List<UserListData>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        database.reference.child("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val users = mutableListOf<UserListData>()
                snapshot.children.forEach { child ->
                    val map = child.value as? Map<*, *>
                    if (map?.get("role") == role && map["grade"] == grade) {
                        users.add(
                            UserListData(
                                id = child.key ?: "",
                                name = map["name"]?.toString() ?: "",
                                email = map["email"]?.toString() ?: "",
                                role = map["role"]?.toString() ?: "",
                                grade = map["grade"]?.toString() ?: "",
                                subject = if (role == "teacher") map["subject"]?.toString() else null
                            )
                        )
                    }
                }
                onSuccess(users)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to fetch ${role}s by grade", exception)
                onError(exception)
            }
    }

    fun sendMessage(message: Message) {
        val messageId = database.reference.child("messages").push().key ?: return
        
        Log.d("FirebaseService", "Sending message: from=${message.senderId}, to=${message.receiverId}, content=${message.content}")
        
        val messageData = mapOf(
            "id" to messageId,
            "senderId" to message.senderId,
            "senderName" to message.senderName,
            "receiverId" to message.receiverId,
            "content" to message.content,
            "timestamp" to message.timestamp,
            "isRead" to false,
            "hasAttachment" to message.hasAttachment,
            "fileUrl" to message.fileUrl,
            "fileName" to message.fileName,
            "fileType" to message.fileType,
            "fileSize" to message.fileSize
        )
        
        database.reference.child("messages").child(messageId).setValue(messageData)
            .addOnSuccessListener {
                Log.d("FirebaseService", "Message sent successfully: $messageId")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseService", "Failed to send message: ${e.message}")
            }
    }

    fun updateMessage(message: Message) {
        // Update message in Firebase
        val messageRef = database.reference.child("messages").child(message.id)
        messageRef.setValue(message)
    }

    fun deleteMessage(messageId: String, deleteForBoth: Boolean = true) {
        val messageRef = database.reference.child("messages").child(messageId)
        if (deleteForBoth) {
            // Delete message completely
            messageRef.removeValue()
        } else {
            // Get the current user ID
            val currentUserId = auth.currentUser?.uid ?: return
            
            // Update the message to mark it as deleted for this user
            messageRef.child("deletedFor").child(currentUserId).setValue(true)
        }
    }

    fun getMessages(userId1: String, userId2: String, onMessagesLoaded: (List<Message>) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return onMessagesLoaded(emptyList())
        
        database.reference.child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = mutableListOf<Message>()
                    snapshot.children.forEach { child ->
                        val map = child.value as? Map<*, *>
                        if (map != null) {
                            val senderId = map["senderId"]?.toString() ?: ""
                            val receiverId = map["receiverId"]?.toString() ?: ""
                            
                            // Check if this message is between the two users
                            if ((senderId == userId1 && receiverId == userId2) ||
                                (senderId == userId2 && receiverId == userId1)) {
                                    
                                // Get the deletedFor map
                                @Suppress("UNCHECKED_CAST")
                                val deletedFor = (map["deletedFor"] as? Map<String, Boolean>) ?: emptyMap()
                                
                                // Only add the message if it's not deleted for the current user
                                if (!deletedFor.containsKey(currentUserId)) {
                                    val messageId = map["id"]?.toString() ?: ""
                                    val isRead = map["isRead"] as? Boolean ?: false
                                    
                                    messages.add(
                                        Message(
                                            id = messageId,
                                            senderId = senderId,
                                            senderName = map["senderName"]?.toString() ?: "",
                                            receiverId = receiverId,
                                            content = map["content"]?.toString() ?: "",
                                            timestamp = (map["timestamp"] as? Long) ?: System.currentTimeMillis(),
                                            deletedFor = deletedFor,
                                            isRead = isRead,
                                            hasAttachment = map["hasAttachment"] as? Boolean ?: false,
                                            fileUrl = map["fileUrl"]?.toString() ?: "",
                                            fileName = map["fileName"]?.toString() ?: "",
                                            fileType = map["fileType"]?.toString() ?: "",
                                            fileSize = (map["fileSize"] as? Long) ?: 0
                                        )
                                    )
                                }
                            }
                        }
                    }
                    onMessagesLoaded(messages)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseService", "Failed to load messages", error.toException())
                    onMessagesLoaded(emptyList())
                }
            })
    }

    fun markMessageAsRead(messageId: String) {
        Log.d("FirebaseService", "Marking message as read: $messageId")
        database.reference.child("messages").child(messageId).child("isRead").setValue(true)
    }
    
    fun markAllMessagesAsRead(userId1: String, userId2: String, context: android.content.Context) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        Log.d("FirebaseService", "Marking all messages as read between $userId1 and $userId2")
        
        database.reference.child("messages")
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.children.forEach { child ->
                    val message = child.getValue(Message::class.java)
                    if (message != null) {
                        val senderId = message.senderId
                        val receiverId = message.receiverId
                        
                        // Check if this message is between the two users and current user is the receiver
                        if ((senderId == userId1 && receiverId == userId2 && receiverId == currentUserId) ||
                            (senderId == userId2 && receiverId == userId1 && receiverId == currentUserId)) {
                            
                            if (!message.isRead) {
                                markMessageAsRead(message.id)
                            }
                        }
                    }
                }
                
                // Update the last notification time to current time
                val prefs = context.getSharedPreferences("message_notifications", android.content.Context.MODE_PRIVATE)
                prefs.edit().putLong("last_notification_time", System.currentTimeMillis()).apply()
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to mark messages as read", exception)
            }
    }
    
    fun getUnreadMessagesCount(userId: String, onCountLoaded: (Int) -> Unit) {
        database.reference.child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var count = 0
                    snapshot.children.forEach { child ->
                        val map = child.value as? Map<*, *>
                        if (map != null) {
                            val receiverId = map["receiverId"]?.toString() ?: ""
                            val isRead = map["isRead"] as? Boolean ?: true
                            
                            // Count unread messages where the user is the receiver
                            if (receiverId == userId && !isRead) {
                                count++
                            }
                        }
                    }
                    onCountLoaded(count)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseService", "Failed to load unread messages count", error.toException())
                    onCountLoaded(0)
                }
            })
    }
    
    fun getUnreadMessages(userId: String, onMessagesLoaded: (List<Message>) -> Unit) {
        database.reference.child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = mutableListOf<Message>()
                    snapshot.children.forEach { child ->
                        val map = child.value as? Map<*, *>
                        if (map != null) {
                            val receiverId = map["receiverId"]?.toString() ?: ""
                            val isRead = map["isRead"] as? Boolean ?: true
                            
                            // Get unread messages where the user is the receiver
                            if (receiverId == userId && !isRead) {
                                @Suppress("UNCHECKED_CAST")
                                val deletedFor = (map["deletedFor"] as? Map<String, Boolean>) ?: emptyMap()
                                
                                messages.add(
                                    Message(
                                        id = map["id"]?.toString() ?: "",
                                        senderId = map["senderId"]?.toString() ?: "",
                                        senderName = map["senderName"]?.toString() ?: "",
                                        receiverId = receiverId,
                                        content = map["content"]?.toString() ?: "",
                                        timestamp = (map["timestamp"] as? Long) ?: System.currentTimeMillis(),
                                        deletedFor = deletedFor,
                                        isRead = isRead,
                                        hasAttachment = map["hasAttachment"] as? Boolean ?: false,
                                        fileUrl = map["fileUrl"]?.toString() ?: "",
                                        fileName = map["fileName"]?.toString() ?: "",
                                        fileType = map["fileType"]?.toString() ?: "",
                                        fileSize = (map["fileSize"] as? Long) ?: 0
                                    )
                                )
                            }
                        }
                    }
                    onMessagesLoaded(messages)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseService", "Failed to load unread messages", error.toException())
                    onMessagesLoaded(emptyList())
                }
            })
    }

    fun getUsersByRoleAndGrade(
        role: String,
        grade: String,
        onSuccess: (List<UserListData>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val usersRef = database.reference.child("users")
        
        usersRef.get().addOnSuccessListener { snapshot ->
            try {
                val usersList = mutableListOf<UserListData>()
                
                for (userSnapshot in snapshot.children) {
                    val userData = userSnapshot.getValue(UserData::class.java)
                    if (userData != null && userData.role == role) {
                        // For teachers, check if the grade is in their grades list
                        if (role == "teacher") {
                            val teacherGrades = userData.grades ?: emptyList()
                            if (grade in teacherGrades) {
                                usersList.add(
                                    UserListData(
                                        id = userSnapshot.key ?: "",
                                        name = userData.name,
                                        email = userData.email,
                                        role = userData.role,
                                        grade = grade,
                                        subject = userData.subject
                                    )
                                )
                            }
                        } 
                        // For students, check exact grade match
                        else if (userData.grade == grade) {
                            usersList.add(
                                UserListData(
                                    id = userSnapshot.key ?: "",
                                    name = userData.name,
                                    email = userData.email,
                                    role = userData.role,
                                    grade = userData.grade,
                                    subject = userData.subject
                                )
                            )
                        }
                    }
                }
                
                onSuccess(usersList)
            } catch (e: Exception) {
                onError(e)
            }
        }.addOnFailureListener { exception ->
            onError(exception)
        }
    }

    fun getChildrenForParent(parentId: String, callback: (List<StudentData>) -> Unit) {
        database.reference.child("users").child(parentId).child("childEmails").get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    Log.d("FirebaseService", "No childEmails found for parent $parentId")
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                // Get child emails from the list stored in Firebase
                val childEmails = try {
                    snapshot.getValue() as? ArrayList<*>
                } catch (e: Exception) {
                    Log.e("FirebaseService", "Error getting childEmails", e)
                    null
                }

                if (childEmails == null || childEmails.isEmpty()) {
                    Log.d("FirebaseService", "Empty or null childEmails for parent $parentId")
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                Log.d("FirebaseService", "Found ${childEmails.size} child emails: $childEmails")

                // Find all students with matching emails
                database.reference.child("users").get()
                    .addOnSuccessListener { usersSnapshot ->
                        val children = mutableListOf<StudentData>()
                        for (studentSnapshot in usersSnapshot.children) {
                            val email = studentSnapshot.child("email").getValue(String::class.java)
                            if (email != null && childEmails.contains(email)) {
                                val name = studentSnapshot.child("name").getValue(String::class.java)
                                val grade = studentSnapshot.child("grade").getValue(String::class.java)
                                if (name != null && grade != null) {
                                    children.add(StudentData(
                                        id = studentSnapshot.key ?: "",
                                        name = name,
                                        email = email,
                                        grade = grade
                                    ))
                                }
                            }
                        }
                        Log.d("FirebaseService", "Found ${children.size} children for parent $parentId")
                        callback(children)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseService", "Failed to get students", exception)
                        callback(emptyList())
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to get childEmails", exception)
                callback(emptyList())
            }
    }

    fun getStudentStats(studentId: String, callback: (Map<String, Any>) -> Unit) {
        database.reference.child("users").child(studentId).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    callback(emptyMap())
                    return@addOnSuccessListener
                }

                val stats = mutableMapOf<String, Any>()
                // Add basic student info
                stats["name"] = snapshot.child("name").getValue(String::class.java) ?: ""
                stats["grade"] = snapshot.child("grade").getValue(String::class.java) ?: ""
                
                // Get attendance (you'll need to implement this based on your data structure)
                database.reference.child("attendance").child(studentId).get()
                    .addOnSuccessListener { attendanceSnapshot ->
                        stats["attendance"] = attendanceSnapshot.getValue(String::class.java) ?: "N/A"
                        callback(stats)
                    }
                    .addOnFailureListener {
                        stats["attendance"] = "N/A"
                        callback(stats)
                    }
            }
            .addOnFailureListener {
                callback(emptyMap())
            }
    }
    
    fun getParentDashboardStats(parentId: String, callback: (Map<String, Any>) -> Unit) {
        // First get the parent's children
        getChildrenForParent(parentId) { children ->
            if (children.isEmpty()) {
                callback(mapOf("childCount" to 0))
                return@getChildrenForParent
            }
            
            val stats = mutableMapOf<String, Any>()
            stats["childCount"] = children.size
            
            // Track completions
            var completedQueries = 0
            val totalQueries = children.size + 1 // Children + events
            
            // Initialize aggregated data
            var totalAttendance = 0.0
            val recentGrades = mutableListOf<Pair<String, String>>() // Child name, grade
            
            // Collect data for each child
            children.forEach { child ->
                getStudentStats(child.id) { childStats ->
                    // Process attendance data
                    val attendanceStr = childStats["attendance"]?.toString() ?: "N/A"
                    if (attendanceStr != "N/A") {
                        try {
                            val attendance = attendanceStr.toDouble()
                            totalAttendance += attendance
                        } catch (e: Exception) {
                            Log.e("FirebaseService", "Error parsing attendance: $attendanceStr", e)
                        }
                    }
                    
                    // Get recent grades for this child
                    val childGrades = mutableListOf<Pair<String, String>>()
                    database.reference.child("grades")
                        .child("current") // Assuming "current" is the current semester
                        .child(child.id)
                        .limitToLast(3) // Get only recent grades
                        .get()
                        .addOnSuccessListener { gradesSnapshot ->
                            for (subjectSnapshot in gradesSnapshot.children) {
                                val subject = subjectSnapshot.key ?: continue
                                val grade = subjectSnapshot.getValue(String::class.java) ?: continue
                                childGrades.add(Pair(subject, grade))
                            }
                            
                            // Add child's grades to the overall list
                            if (childGrades.isNotEmpty()) {
                                recentGrades.add(Pair(child.name, childGrades.first().second))
                            }
                            
                            // Check if all children are processed
                            completedQueries++
                            if (completedQueries >= totalQueries) {
                                // Calculate average attendance
                                if (children.size > 0) {
                                    stats["averageAttendance"] = (totalAttendance / children.size).toString()
                                }
                                
                                // Add recent grades
                                stats["recentGrades"] = recentGrades
                                
                                // Return the aggregated stats
                                callback(stats)
                            }
                        }
                        .addOnFailureListener {
                            completedQueries++
                            if (completedQueries >= totalQueries) {
                                if (children.size > 0) {
                                    stats["averageAttendance"] = (totalAttendance / children.size).toString()
                                }
                                stats["recentGrades"] = recentGrades
                                callback(stats)
                            }
                        }
                }
            }
            
            // Get upcoming events count
            getUpcomingEvents { events ->
                stats["upcomingEvents"] = events.size
                
                completedQueries++
                if (completedQueries >= totalQueries) {
                    if (children.size > 0 && totalAttendance > 0) {
                        stats["averageAttendance"] = (totalAttendance / children.size).toString()
                    } else {
                        stats["averageAttendance"] = "N/A"
                    }
                    stats["recentGrades"] = recentGrades
                    callback(stats)
                }
            }
        }
    }

    data class StudentData(
        val id: String,
        val name: String,
        val email: String,
        val grade: String
    )

    fun deleteUser(userId: String, role: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        if (role == "parent") {
            // First get the parent's data to find linked students
            database.reference.child("users").child(userId).get()
                .addOnSuccessListener { snapshot ->
                    val parentData = snapshot.getValue(UserData::class.java)
                    val childEmails = parentData?.childEmails ?: emptyList()
                    
                    // Find and delete all linked students
                    database.reference.child("users").get()
                        .addOnSuccessListener { usersSnapshot ->
                            val batch = mutableListOf<Pair<String, Boolean>>()
                            
                            // Find student IDs to delete
                            usersSnapshot.children.forEach { child ->
                                val userData = child.getValue(UserData::class.java)
                                if (userData?.role == "student" && userData.email in childEmails) {
                                    batch.add(child.key!! to false)
                                }
                            }
                            
                            // Add parent to deletion batch
                            batch.add(userId to true)
                            
                            // Delete all users in batch
                            var completed = 0
                            batch.forEach { (id, isParent) ->
                                database.reference.child("users").child(id).removeValue()
                                    .addOnSuccessListener {
                                        completed++
                                        if (completed == batch.size && isParent) {
                                            // Only call onSuccess after parent is deleted
                                            onSuccess()
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        onError(exception)
                                    }
                            }
                        }
                        .addOnFailureListener { exception ->
                            onError(exception)
                        }
                }
                .addOnFailureListener { exception ->
                    onError(exception)
                }
        } else {
            // For teachers and students, simply delete the user
            database.reference.child("users").child(userId).removeValue()
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    onError(exception)
                }
        }
    }

    fun updateUser(
        userId: String,
        updates: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        database.reference.child("users").child(userId)
            .updateChildren(updates)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun postEvent(
        date: Long,
        title: String,
        time: String,
        description: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val eventData = hashMapOf(
            "date" to date,
            "title" to title,
            "time" to time,
            "description" to description,
            "createdAt" to System.currentTimeMillis()
        )

        database.reference.child("events")
            .push()
            .setValue(eventData)
            .addOnSuccessListener {
                Log.d("FirebaseService", "Event posted successfully")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to post event", exception)
                onError(exception)
            }
    }

    fun getUpcomingEvents(callback: (List<EventData>) -> Unit) {
        database.reference.child("events").get()
            .addOnSuccessListener { snapshot ->
                try {
                    val events = mutableListOf<EventData>()
                    for (eventSnapshot in snapshot.children) {
                        try {
                            val dateValue = eventSnapshot.child("date").getValue(Long::class.java)
                            val dateStr = if (dateValue != null) {
                                java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date(dateValue))
                            } else {
                                eventSnapshot.child("date").getValue(String::class.java)
                            }
                            
                            val event = EventData(
                                id = eventSnapshot.key ?: "",
                                title = eventSnapshot.child("title").getValue(String::class.java),
                                date = dateStr,
                                time = eventSnapshot.child("time").getValue(String::class.java),
                                description = eventSnapshot.child("description").getValue(String::class.java)
                            )
                            events.add(event)
                        } catch (e: Exception) {
                            Log.e("FirebaseService", "Error parsing event: ${eventSnapshot.key}", e)
                            // Continue with next event
                        }
                    }
                    callback(events)
                } catch (e: Exception) {
                    Log.e("FirebaseService", "Error getting events", e)
                    callback(emptyList())
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseService", "Failed to get events", e)
                callback(emptyList())
            }
    }

    fun updateEvent(
        eventId: String,
        title: String,
        date: String,
        time: String,
        description: String,
        callback: (Boolean) -> Unit
    ) {
        try {
            // Convert date string to timestamp
            val dateTimestamp = try {
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd")
                dateFormat.parse(date)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e("FirebaseService", "Error parsing date: $date", e)
                System.currentTimeMillis()
            }

            val eventData = hashMapOf(
                "title" to title,
                "date" to dateTimestamp,
                "time" to time,
                "description" to description
            )

            database.reference.child("events").child(eventId).updateChildren(eventData as Map<String, Any>)
                .addOnSuccessListener {
                    Log.d("FirebaseService", "Event updated successfully: $eventId")
                    callback(true)
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseService", "Failed to update event: $eventId", e)
                    callback(false)
                }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error in updateEvent", e)
            callback(false)
        }
    }

    fun deleteEvent(eventId: String, callback: (Boolean) -> Unit) {
        database.reference.child("events").child(eventId).removeValue()
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    data class UserData(
        val uid: String = "",
        val name: String? = null,
        val email: String? = null,
        val role: String? = null,
        val subject: String? = null,
        val grade: String? = null,
        val grades: List<String>? = null,
        val childEmails: List<String>? = null
    )

    data class UserListData(
        val id: String = "",
        val name: String? = null,
        val email: String? = null,
        val role: String? = null,
        val subject: String? = null,
        val grade: String? = null,
        val grades: List<String>? = null,
        val childEmails: List<String>? = null
    )

    data class EventData(
        val id: String = "",
        val title: String? = null,
        val date: String? = null,
        val time: String? = null,
        val description: String? = null
    )

    // Add these new data classes for attendance
    data class ClassData(
        val grade: String = "",
        val students: List<String> = emptyList()
    )

    data class AttendanceRecord(
        val date: String = "",
        val day: String = "",
        val period: Int = 0,
        val grade: String = "",
        val studentAttendance: Map<String, Boolean> = emptyMap()
    )

    fun getTeacherClasses(teacherId: String, callback: (List<String>) -> Unit) {
        Log.d("FirebaseService", "Getting classes for teacher: $teacherId")
        database.reference.child("users").child(teacherId).child("grades")
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    val gradesList = mutableListOf<String>()
                    snapshot.children.forEach { child ->
                        child.getValue(String::class.java)?.let { grade ->
                            Log.d("FirebaseService", "Found grade for teacher: $grade")
                            gradesList.add(grade)
                        }
                    }
                    Log.d("FirebaseService", "Teacher's grades: $gradesList")
                    callback(gradesList)
                } catch (e: Exception) {
                    Log.e("FirebaseService", "Error parsing teacher's grades", e)
                    callback(emptyList())
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseService", "Error getting teacher's grades", e)
                callback(emptyList())
            }
    }

    fun getStudentsInClass(grade: String, callback: (List<UserListData>) -> Unit) {
        Log.d("FirebaseService", "Getting students for grade: '$grade'")
        getUsersByRoleAndGrade(
            role = "student",
            grade = grade,
            onSuccess = { students ->
                Log.d("FirebaseService", "Found ${students.size} students in grade '$grade'")
                callback(students)
            },
            onError = { e ->
                Log.e("FirebaseService", "Error getting students in class", e)
                callback(emptyList())
            }
        )
    }

    fun submitAttendance(
        teacherId: String,
        attendanceRecord: AttendanceRecord,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val attendanceKey = database.reference.child("attendance").push().key ?: return
        val attendanceData = hashMapOf(
            "teacherId" to teacherId,
            "date" to attendanceRecord.date,
            "day" to attendanceRecord.day,
            "period" to attendanceRecord.period,
            "grade" to attendanceRecord.grade,
            "studentAttendance" to attendanceRecord.studentAttendance
        )

        database.reference.child("attendance").child(attendanceKey)
            .setValue(attendanceData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun submitGrade(
        teacherId: String,
        studentId: String,
        grade: String,
        semester: String,
        subject: String,
        assessments: Map<String, Int>? = null,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        // First, save the overall grade
        val gradeRef = database.reference.child("grades")
            .child(semester)
            .child(studentId)
            .child(subject)

        gradeRef.setValue(grade)
            .addOnSuccessListener {
                // If there are assessments, save them too
                if (assessments != null) {
                    val assessmentRef = database.reference.child("assessments")
                        .child(semester)
                        .child(studentId)
                        .child(subject)

                    assessmentRef.setValue(assessments)
                        .addOnSuccessListener {
                            onSuccess()
                        }
                        .addOnFailureListener { exception ->
                            onError(exception)
                        }
                } else {
                    onSuccess()
                }
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun submitGrade(
        teacherId: String,
        studentId: String,
        grade: String,
        semester: String,
        subject: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val gradeRef = database.reference.child("grades")
            .child(semester)
            .child(studentId)
            .child(subject)

        gradeRef.setValue(grade)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun getGrade(studentId: String, semester: String, subject: String, callback: (String?) -> Unit) {
        database.reference.child("grades")
            .child(semester)
            .child(studentId)
            .child(subject)
            .get()
            .addOnSuccessListener { snapshot ->
                val grade = snapshot.getValue(String::class.java)
                callback(grade)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to get grade for student $studentId", exception)
                callback(null)
            }
    }

    fun checkStudentExists(email: String, callback: (Boolean) -> Unit) {
        database.reference.child("users")
            .orderByChild("email")
            .equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var exists = false
                    for (child in snapshot.children) {
                        val role = child.child("role").getValue(String::class.java)
                        if (role == "student") {
                            exists = true
                            break
                        }
                    }
                    callback(exists)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseService", "Error checking student existence", error.toException())
                    callback(false)
                }
            })
    }

    fun getStudentAttendance(
        studentId: String,
        date: String,
        period: Int? = null,
        onComplete: (Map<String, Boolean>) -> Unit
    ) {
        database.reference.child("attendance")
            .get()
            .addOnSuccessListener { snapshot ->
                val attendanceMap = mutableMapOf<String, Boolean>()
                snapshot.children.forEach { attendanceSnapshot ->
                    val attendanceData = attendanceSnapshot.getValue(AttendanceRecord::class.java)
                    if (attendanceData != null && 
                        attendanceData.date == date && 
                        attendanceData.period == period &&
                        attendanceData.studentAttendance.containsKey(studentId)
                    ) {
                        attendanceMap[attendanceData.period.toString()] = attendanceData.studentAttendance[studentId] ?: false
                    }
                }
                onComplete(attendanceMap)
            }
            .addOnFailureListener {
                onComplete(emptyMap())
            }
    }

    data class AssessmentDetail(
        val name: String = "",
        val maxPoints: Int = 0,
        val score: Int = 0
    )

    fun getAssessmentDetails(
        studentId: String,
        semester: String,
        subject: String,
        onComplete: (List<AssessmentDetail>) -> Unit
    ) {
        Log.d("FirebaseService", "Getting assessment details for student: $studentId, semester: $semester, subject: $subject")
        
        // Simplified path structure
        val subjectRef = database.reference
            .child("assessments")
            .child(semester)
            .child(studentId)
            .child(subject)

        Log.d("FirebaseService", "Database reference path: ${subjectRef}")

        subjectRef.get().addOnSuccessListener { snapshot ->
            Log.d("FirebaseService", "Got assessment data: exists=${snapshot.exists()}, value=${snapshot.value}")
            if (!snapshot.exists()) {
                Log.d("FirebaseService", "No assessment data found")
                onComplete(emptyList())
                return@addOnSuccessListener
            }
            processAssessments(snapshot, onComplete)
        }.addOnFailureListener { exception ->
            Log.e("FirebaseService", "Error getting assessment details", exception)
            onComplete(emptyList())
        }
    }

    private fun processAssessments(snapshot: DataSnapshot, onComplete: (List<AssessmentDetail>) -> Unit) {
        val assessments = mutableListOf<AssessmentDetail>()
        Log.d("FirebaseService", "Starting to process assessments from snapshot: ${snapshot.value}")

        try {
            when (val value = snapshot.value) {
                is Map<*, *> -> {
                    // Handle map-based structure
                    value.forEach { (key, value) ->
                        try {
                            // Parse the key which is in format "Assessment Name (MaxPoints)"
                            val keyStr = key?.toString() ?: return@forEach
                            val nameAndPoints = keyStr.split(" (")
                            if (nameAndPoints.size == 2) {
                                val name = nameAndPoints[0]
                                val maxPoints = nameAndPoints[1].removeSuffix(")").toIntOrNull() ?: 0
                                val score = (value as? Number)?.toInt() ?: 0
                                assessments.add(AssessmentDetail(name, maxPoints, score))
                                Log.d("FirebaseService", "Added assessment: name=$name, maxPoints=$maxPoints, score=$score")
                            }
                        } catch (e: Exception) {
                            Log.e("FirebaseService", "Error processing assessment entry: $key", e)
                        }
                    }
                }
                else -> {
                    Log.d("FirebaseService", "Unsupported assessment data format: ${value?.javaClass}")
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error processing assessments", e)
        }

        Log.d("FirebaseService", "Processed ${assessments.size} assessments")
        onComplete(assessments)
    }

    fun listenToEventCount(callback: (Int) -> Unit) {
        database.reference.child("events").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(snapshot.childrenCount.toInt())
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseService", "Error listening to event count", error.toException())
            }
        })
    }

    fun listenForNewMessages(context: android.content.Context) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        Log.d("FirebaseService", "Starting to listen for new messages for user: $currentUserId")
        
        // Get the last notification timestamp from shared preferences
        val prefs = context.getSharedPreferences("message_notifications", android.content.Context.MODE_PRIVATE)
        val lastNotificationTime = prefs.getLong("last_notification_time", 0)
        
        Log.d("FirebaseService", "Last notification time: $lastNotificationTime")
        
        database.reference.child("messages")
            .addChildEventListener(object : com.google.firebase.database.ChildEventListener {
                override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                    try {
                        val message = snapshot.getValue(com.example.parent_teacher_engagement.screens.Message::class.java)
                        Log.d("FirebaseService", "Message detected: ${message?.id}, to: ${message?.receiverId}, from: ${message?.senderId}, isRead: ${message?.isRead}, timestamp: ${message?.timestamp}")
                        
                        if (message != null) {
                            // Log each condition separately for better debugging
                            val isReceiver = message.receiverId == currentUserId
                            val isUnread = !message.isRead
                            val isNewer = message.timestamp > lastNotificationTime
                            
                            Log.d("FirebaseService", "Notification conditions: isReceiver=$isReceiver, isUnread=$isUnread, isNewer=$isNewer")
                            
                            if (isReceiver && isUnread && isNewer) {
                                Log.d("FirebaseService", "New unread message detected, showing notification")
                                
                                // Update the last notification time
                                prefs.edit().putLong("last_notification_time", System.currentTimeMillis()).apply()
                                
                                // Show notification for new message
                                com.example.parent_teacher_engagement.service.NotificationService.getInstance(context)
                                    .showMessageNotification(message)
                            } else {
                                Log.d("FirebaseService", "Message does not meet notification criteria")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseService", "Error processing new message", e)
                    }
                }
                
                override fun onChildChanged(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                    // Not needed for notifications
                }
                
                override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {
                    // Not needed for notifications
                }
                
                override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                    // Not needed for notifications
                }
                
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    Log.e("FirebaseService", "Failed to listen for new messages", error.toException())
                }
            })
    }

    // Navigate to chat screen with a specific user
    fun navigateToChatScreen(userId: String) {
        onNavigateToChatScreen?.invoke(userId)
    }
    
    // Get user by ID
    fun getUserById(userId: String, onComplete: (UserListData?) -> Unit) {
        database.reference.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userData = snapshot.getValue(UserData::class.java)
                if (userData != null) {
                    onComplete(UserListData(
                        id = userId,
                        name = userData.name,
                        email = userData.email,
                        role = userData.role
                    ))
                } else {
                    onComplete(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseService", "Error getting user by ID", error.toException())
                onComplete(null)
            }
        })
    }

    // Sync messages after network reconnection
    fun syncMessagesAfterReconnect(context: android.content.Context) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        Log.d("FirebaseService", "Syncing messages after reconnect for user: $currentUserId")
        
        // Get the last notification timestamp from shared preferences
        val prefs = context.getSharedPreferences("message_notifications", android.content.Context.MODE_PRIVATE)
        val lastNotificationTime = prefs.getLong("last_notification_time", 0)
        val currentTime = System.currentTimeMillis()
        
        Log.d("FirebaseService", "Last notification time: $lastNotificationTime, Current time: $currentTime")
        
        // Update the last sync time
        prefs.edit().putLong("last_sync_time", currentTime).apply()
        
        // Query for messages received since last notification
        database.reference.child("messages")
            .orderByChild("timestamp")
            .startAt(lastNotificationTime.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val newMessages = mutableListOf<Message>()
                    
                    Log.d("FirebaseService", "Found ${snapshot.childrenCount} messages since last notification")
                    
                    snapshot.children.forEach { child ->
                        val message = child.getValue(Message::class.java)
                        if (message != null) {
                            // Log each condition separately for better debugging
                            val isReceiver = message.receiverId == currentUserId
                            val isUnread = !message.isRead
                            val isNewer = message.timestamp > lastNotificationTime
                            
                            Log.d("FirebaseService", "Message sync check: ${message.id}, isReceiver=$isReceiver, isUnread=$isUnread, isNewer=$isNewer")
                            
                            if (isReceiver && isUnread && isNewer) {
                                newMessages.add(message)
                            }
                        }
                    }
                    
                    Log.d("FirebaseService", "Found ${newMessages.size} new unread messages for notification")
                    
                    // Show notifications for new messages
                    if (newMessages.isNotEmpty()) {
                        // Update the last notification time
                        prefs.edit().putLong("last_notification_time", currentTime).apply()
                        
                        // Group messages by sender
                        val messagesBySender = newMessages.groupBy { it.senderId }
                        
                        // Show notifications for each sender
                        messagesBySender.forEach { (senderId, messages) ->
                            val senderName = messages.firstOrNull()?.senderName ?: "Unknown"
                            Log.d("FirebaseService", "Showing notification for ${messages.size} messages from $senderName")
                            com.example.parent_teacher_engagement.service.NotificationService.getInstance(context)
                                .showMessagesNotification(senderId, senderName, messages)
                        }
                    } else {
                        Log.d("FirebaseService", "No new messages to notify about")
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseService", "Error syncing messages", error.toException())
                }
            })
    }

    // File upload functionality
    fun uploadFile(
        fileUri: Uri,
        fileName: String,
        fileType: String,
        onProgress: (Int) -> Unit,
        onSuccess: (String, Long) -> Unit,
        onError: (Exception) -> Unit
    ) {
        Log.d("FirebaseService", "Starting file upload process for URI: $fileUri")
        
        try {
            val storageRef = storage.reference
            // Create a unique file name to avoid collisions
            val timestamp = System.currentTimeMillis()
            val safeFileName = fileName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            val fileRef = storageRef.child("message_attachments/${timestamp}_$safeFileName")
            
            Log.d("FirebaseService", "Storage reference created: ${fileRef.path}")
            
            // Get file size - this approach is more reliable
            var fileSize = 0L
            
            // Create upload task
            val uploadTask = fileRef.putFile(fileUri)
            
            // Monitor upload progress
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                Log.d("FirebaseService", "Upload progress: $progress%")
                onProgress(progress)
            }
            
            // Handle upload success
            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    Log.e("FirebaseService", "Upload task failed", task.exception)
                    task.exception?.let { throw it }
                }
                Log.d("FirebaseService", "Upload completed, getting download URL")
                fileRef.downloadUrl
            }.addOnSuccessListener { downloadUri ->
                Log.d("FirebaseService", "Download URL obtained: $downloadUri")
                onSuccess(downloadUri.toString(), fileSize)
            }.addOnFailureListener { exception ->
                Log.e("FirebaseService", "File upload failed", exception)
                onError(exception)
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Exception in uploadFile method", e)
            onError(e)
        }
    }

    // Meeting requests related methods
    fun createMeetingRequest(meetingRequest: com.example.parent_teacher_engagement.model.MeetingRequest, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val meetingId = meetingRequest.id.ifEmpty { database.reference.child("meeting_requests").push().key!! }
        
        val updatedMeeting = if (meetingRequest.id.isEmpty()) meetingRequest.copy(id = meetingId) else meetingRequest
        
        // Store meeting request in main table
        database.reference.child("meeting_requests").child(meetingId)
            .setValue(updatedMeeting.toMap())
            .addOnSuccessListener {
                // Add reference to requester's sent meetings
                database.reference.child("user_meetings")
                    .child(updatedMeeting.requesterId)
                    .child("sent")
                    .child(meetingId)
                    .setValue(true)
                    .addOnSuccessListener {
                        // Add reference to recipient's received meetings
                        database.reference.child("user_meetings")
                            .child(updatedMeeting.recipientId)
                            .child("received")
                            .child(meetingId)
                            .setValue(true)
                            .addOnSuccessListener {
                                onSuccess()
                            }
                            .addOnFailureListener { exception ->
                                onError(exception)
                            }
                    }
                    .addOnFailureListener { exception ->
                        onError(exception)
                    }
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
    
    fun updateMeetingStatus(
        meetingId: String, 
        newStatus: com.example.parent_teacher_engagement.model.MeetingStatus,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val updates = hashMapOf<String, Any>(
            "status" to newStatus.name,
            "updatedAt" to System.currentTimeMillis()
        )
        
        // Get current meeting details first to create todo items
        getMeetingById(
            meetingId = meetingId,
            onSuccess = { meeting ->
                // Update meeting status
                database.reference.child("meeting_requests").child(meetingId)
                    .updateChildren(updates)
                    .addOnSuccessListener {
                        // Create todo items for both parties if meeting is accepted or rescheduled
                        if (newStatus == com.example.parent_teacher_engagement.model.MeetingStatus.ACCEPTED ||
                            newStatus == com.example.parent_teacher_engagement.model.MeetingStatus.RESCHEDULED) {
                            
                            val statusText = if (newStatus == com.example.parent_teacher_engagement.model.MeetingStatus.ACCEPTED) 
                                "Accepted" else "Rescheduled"
                            
                            // Create todo item for requester
                            val requesterTodoItem = com.example.parent_teacher_engagement.model.TodoItem(
                                id = UUID.randomUUID().toString(),
                                title = "Meeting ($statusText): ${meeting.title}",
                                description = "Meeting with ${meeting.recipientName}: ${meeting.description}",
                                dueDate = meeting.proposedDate,
                                dueTime = meeting.proposedTime,
                                priority = com.example.parent_teacher_engagement.model.TodoItem.Priority.HIGH,
                                category = "Meetings",
                                isCompleted = false,
                                meetingId = meeting.id
                            )
                            
                            // Create todo item for recipient
                            val recipientTodoItem = com.example.parent_teacher_engagement.model.TodoItem(
                                id = UUID.randomUUID().toString(),
                                title = "Meeting ($statusText): ${meeting.title}",
                                description = "Meeting with ${meeting.requesterName}: ${meeting.description}",
                                dueDate = meeting.proposedDate,
                                dueTime = meeting.proposedTime,
                                priority = com.example.parent_teacher_engagement.model.TodoItem.Priority.HIGH,
                                category = "Meetings",
                                isCompleted = false,
                                meetingId = meeting.id
                            )
                            
                            // Add todo items to Firebase for both parties
                            addTodoItem(
                                todoItem = requesterTodoItem,
                                userId = meeting.requesterId,
                                onSuccess = {},
                                onError = { e -> 
                                    Log.e("FirebaseService", "Failed to create todo for requester: ${e.message}")
                                }
                            )
                            
                            addTodoItem(
                                todoItem = recipientTodoItem,
                                userId = meeting.recipientId,
                                onSuccess = {},
                                onError = { e -> 
                                    Log.e("FirebaseService", "Failed to create todo for recipient: ${e.message}")
                                }
                            )
                            
                            // Send notifications to both parties
                            val notificationMessage = "Meeting '${meeting.title}' has been $statusText"
                            sendNotification(meeting.requesterId, "Meeting $statusText", notificationMessage)
                            sendNotification(meeting.recipientId, "Meeting $statusText", notificationMessage)
                        }
                        
                        onSuccess()
                    }
                    .addOnFailureListener { exception ->
                        onError(exception)
                    }
            },
            onError = { exception ->
                onError(exception)
            }
        )
    }
    
    fun getMeetingById(
        meetingId: String,
        onSuccess: (com.example.parent_teacher_engagement.model.MeetingRequest) -> Unit,
        onError: (Exception) -> Unit
    ) {
        database.reference.child("meeting_requests").child(meetingId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        try {
                            val meetingMap = snapshot.value as? Map<String, Any?>
                            if (meetingMap != null) {
                                val meeting = com.example.parent_teacher_engagement.model.MeetingRequest.fromMap(meetingMap)
                                onSuccess(meeting)
                            } else {
                                onError(Exception("Invalid meeting data format"))
                            }
                        } catch (e: Exception) {
                            onError(e)
                        }
                    } else {
                        onError(Exception("Meeting not found"))
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    onError(Exception(error.message))
                }
            })
    }
    
    fun getSentMeetings(
        userId: String,
        onSuccess: (List<com.example.parent_teacher_engagement.model.MeetingRequest>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        database.reference.child("user_meetings").child(userId).child("sent")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val meetingIds = snapshot.children.mapNotNull { it.key }
                        if (meetingIds.isEmpty()) {
                            onSuccess(emptyList())
                            return
                        }
                        
                        val meetings = mutableListOf<com.example.parent_teacher_engagement.model.MeetingRequest>()
                        var completedCount = 0
                        
                        for (meetingId in meetingIds) {
                            getMeetingById(
                                meetingId = meetingId,
                                onSuccess = { meeting ->
                                    meetings.add(meeting)
                                    completedCount++
                                    if (completedCount == meetingIds.size) {
                                        onSuccess(meetings.sortedByDescending { it.updatedAt })
                                    }
                                },
                                onError = { exception ->
                                    completedCount++
                                    if (completedCount == meetingIds.size) {
                                        onSuccess(meetings.sortedByDescending { it.updatedAt })
                                    }
                                }
                            )
                        }
                    } else {
                        onSuccess(emptyList())
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    onError(Exception(error.message))
                }
            })
    }
    
    fun getReceivedMeetings(
        userId: String,
        onSuccess: (List<com.example.parent_teacher_engagement.model.MeetingRequest>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        database.reference.child("user_meetings").child(userId).child("received")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val meetingIds = snapshot.children.mapNotNull { it.key }
                        if (meetingIds.isEmpty()) {
                            onSuccess(emptyList())
                            return
                        }
                        
                        val meetings = mutableListOf<com.example.parent_teacher_engagement.model.MeetingRequest>()
                        var completedCount = 0
                        
                        for (meetingId in meetingIds) {
                            getMeetingById(
                                meetingId = meetingId,
                                onSuccess = { meeting ->
                                    meetings.add(meeting)
                                    completedCount++
                                    if (completedCount == meetingIds.size) {
                                        onSuccess(meetings.sortedByDescending { it.updatedAt })
                                    }
                                },
                                onError = { exception ->
                                    completedCount++
                                    if (completedCount == meetingIds.size) {
                                        onSuccess(meetings.sortedByDescending { it.updatedAt })
                                    }
                                }
                            )
                        }
                    } else {
                        onSuccess(emptyList())
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    onError(Exception(error.message))
                }
            })
    }
    
    fun getAllUserMeetings(
        userId: String,
        onSuccess: (List<com.example.parent_teacher_engagement.model.MeetingRequest>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val allMeetings = mutableListOf<com.example.parent_teacher_engagement.model.MeetingRequest>()
        var completed = 0
        var hasErrors = false
        
        getSentMeetings(
            userId = userId,
            onSuccess = { sentMeetings ->
                allMeetings.addAll(sentMeetings)
                completed++
                if (completed == 2) {
                    onSuccess(allMeetings.distinctBy { it.id }.sortedByDescending { it.updatedAt })
                }
            },
            onError = { exception ->
                hasErrors = true
                onError(exception)
            }
        )
        
        getReceivedMeetings(
            userId = userId,
            onSuccess = { receivedMeetings ->
                allMeetings.addAll(receivedMeetings)
                completed++
                if (completed == 2 && !hasErrors) {
                    onSuccess(allMeetings.distinctBy { it.id }.sortedByDescending { it.updatedAt })
                }
            },
            onError = { exception ->
                if (!hasErrors) {
                    hasErrors = true
                    onError(exception)
                }
            }
        )
    }
    
    fun getPotentialMeetingParticipants(
        userRole: String,
        onSuccess: (List<UserListData>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Determine which roles to fetch based on current user's role
        val rolesToFetch = when (userRole) {
            "parent" -> listOf("teacher", "admin")
            "teacher" -> listOf("parent", "admin")
            "admin" -> listOf("teacher", "parent")
            else -> emptyList()
        }
        
        if (rolesToFetch.isEmpty()) {
            onSuccess(emptyList())
            return
        }
        
        val allUsers = mutableListOf<UserListData>()
        var completedRoles = 0
        
        for (role in rolesToFetch) {
            getUsersByRole(
                role = role,
                onSuccess = { users ->
                    allUsers.addAll(users)
                    completedRoles++
                    if (completedRoles == rolesToFetch.size) {
                        onSuccess(allUsers.sortedBy { it.name })
                    }
                },
                onError = { exception ->
                    completedRoles++
                    if (completedRoles == rolesToFetch.size) {
                        if (allUsers.isNotEmpty()) {
                            onSuccess(allUsers.sortedBy { it.name })
                        } else {
                            onError(exception)
                        }
                    }
                }
            )
        }
    }

    fun getStudentDetails(studentId: String, callback: (StudentData?) -> Unit) {
        database.reference.child("users").child(studentId).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    callback(null)
                    return@addOnSuccessListener
                }
                
                val name = snapshot.child("name").getValue(String::class.java)
                val email = snapshot.child("email").getValue(String::class.java)
                val grade = snapshot.child("grade").getValue(String::class.java)
                
                if (name != null && email != null && grade != null) {
                    callback(StudentData(
                        id = studentId,
                        name = name,
                        email = email,
                        grade = grade
                    ))
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    // Todo Management Functions
    fun addTodoItem(
        todoItem: com.example.parent_teacher_engagement.model.TodoItem,
        userId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val todoItemMap = mapOf(
                "id" to todoItem.id,
                "title" to todoItem.title,
                "description" to todoItem.description,
                "dueDate" to todoItem.dueDate,
                "dueTime" to todoItem.dueTime,
                "priority" to todoItem.priority.name,
                "category" to todoItem.category,
                "isCompleted" to todoItem.isCompleted,
                "meetingId" to (todoItem.meetingId ?: ""),
                "createdAt" to System.currentTimeMillis()
            )
            
            database.reference.child("todos").child(userId).child(todoItem.id)
                .setValue(todoItemMap)
                .addOnSuccessListener {
                    Log.d("FirebaseService", "Todo item added successfully")
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    Log.e("FirebaseService", "Failed to add todo item", exception)
                    onError(exception)
                }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error adding todo item", e)
            onError(e)
        }
    }
    
    fun getTodoItemsForUser(
        userId: String,
        onSuccess: (List<com.example.parent_teacher_engagement.model.TodoItem>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        database.reference.child("todos").child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val todoItems = mutableListOf<com.example.parent_teacher_engagement.model.TodoItem>()
                
                for (todoSnapshot in snapshot.children) {
                    try {
                        val todoMap = todoSnapshot.value as? Map<*, *>
                        if (todoMap != null) {
                            val priorityString = todoMap["priority"] as? String ?: "MEDIUM"
                            val priority = try {
                                com.example.parent_teacher_engagement.model.TodoItem.Priority.valueOf(priorityString)
                            } catch (e: Exception) {
                                com.example.parent_teacher_engagement.model.TodoItem.Priority.MEDIUM
                            }
                            
                            val todoItem = com.example.parent_teacher_engagement.model.TodoItem(
                                id = todoMap["id"] as? String ?: "",
                                title = todoMap["title"] as? String ?: "",
                                description = todoMap["description"] as? String ?: "",
                                dueDate = todoMap["dueDate"] as? String ?: "",
                                dueTime = todoMap["dueTime"] as? String ?: "",
                                priority = priority,
                                category = todoMap["category"] as? String ?: "",
                                isCompleted = todoMap["isCompleted"] as? Boolean ?: false,
                                meetingId = todoMap["meetingId"] as? String
                            )
                            todoItems.add(todoItem)
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseService", "Error parsing todo item", e)
                    }
                }
                
                onSuccess(todoItems)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to get todo items", exception)
                onError(exception)
            }
    }
    
    fun updateTodoItem(
        todoItem: com.example.parent_teacher_engagement.model.TodoItem,
        userId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val updates = mapOf(
                "title" to todoItem.title,
                "description" to todoItem.description,
                "dueDate" to todoItem.dueDate,
                "dueTime" to todoItem.dueTime,
                "priority" to todoItem.priority.name,
                "category" to todoItem.category,
                "isCompleted" to todoItem.isCompleted,
                "meetingId" to (todoItem.meetingId ?: ""),
                "updatedAt" to System.currentTimeMillis()
            )
            
            database.reference.child("todos").child(userId).child(todoItem.id)
                .updateChildren(updates)
                .addOnSuccessListener {
                    Log.d("FirebaseService", "Todo item updated successfully")
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    Log.e("FirebaseService", "Failed to update todo item", exception)
                    onError(exception)
                }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error updating todo item", e)
            onError(e)
        }
    }
    
    fun deleteTodoItem(
        todoId: String,
        userId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        database.reference.child("todos").child(userId).child(todoId)
            .removeValue()
            .addOnSuccessListener {
                Log.d("FirebaseService", "Todo item deleted successfully")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to delete todo item", exception)
                onError(exception)
            }
    }

    // New function to suggest alternative meeting times
    fun suggestAlternativeMeetingTimes(
        meetingId: String,
        alternatives: List<com.example.parent_teacher_engagement.model.AlternativeTime>,
        responseMessage: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            // Get the current meeting first to preserve its state
            getMeetingById(
                meetingId = meetingId,
                onSuccess = { meeting ->
                    // Create list of all alternatives including existing ones
                    val updatedAlternatives = meeting.suggestedAlternatives + alternatives
                    
                    // Update the meeting with new alternatives and status
                    val updates = hashMapOf<String, Any>(
                        "suggestedAlternatives" to updatedAlternatives.map { it.toMap() },
                        "responseMessage" to responseMessage,
                        "status" to com.example.parent_teacher_engagement.model.MeetingStatus.RESCHEDULED.name,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    
                    database.reference.child("meeting_requests").child(meetingId)
                        .updateChildren(updates)
                        .addOnSuccessListener {
                            // Create todo items for both parties to reflect meeting is in RESCHEDULED state
                            // Create todo item for requester
                            val requesterTodoItem = com.example.parent_teacher_engagement.model.TodoItem(
                                id = UUID.randomUUID().toString(),
                                title = "Meeting (Needs Review): ${meeting.title}",
                                description = "Alternative meeting times have been suggested for meeting with ${meeting.recipientName}",
                                dueDate = meeting.proposedDate, // Keep original date for now
                                dueTime = meeting.proposedTime, // Keep original time for now
                                priority = com.example.parent_teacher_engagement.model.TodoItem.Priority.HIGH,
                                category = "Meetings",
                                isCompleted = false,
                                meetingId = meeting.id
                            )
                            
                            // Add todo item for the requester to review alternatives
                            addTodoItem(
                                todoItem = requesterTodoItem,
                                userId = meeting.requesterId,
                                onSuccess = {},
                                onError = { e -> 
                                    Log.e("FirebaseService", "Failed to create todo for requester: ${e.message}")
                                }
                            )
                            
                            // Send notification to meeting requester
                            val notificationRecipientId = if (getCurrentUserId() == meeting.requesterId) {
                                meeting.recipientId
                            } else {
                                meeting.requesterId
                            }
                            
                            sendNotification(
                                notificationRecipientId,
                                "Meeting Alternative Suggested",
                                "Alternative meeting times have been suggested for '${meeting.title}'."
                            )
                            
                            onSuccess()
                        }
                        .addOnFailureListener { exception ->
                            onError(exception)
                        }
                },
                onError = { exception ->
                    onError(exception)
                }
            )
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error suggesting alternatives", e)
            onError(e)
        }
    }
    
    // Function to select an alternative time
    fun selectAlternativeMeetingTime(
        meetingId: String,
        selectedAlternativeIndex: Int,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            // Get the current meeting first
            getMeetingById(
                meetingId = meetingId,
                onSuccess = { meeting ->
                    if (selectedAlternativeIndex >= 0 && selectedAlternativeIndex < meeting.suggestedAlternatives.size) {
                        val selectedAlternative = meeting.suggestedAlternatives[selectedAlternativeIndex]
                        
                        // Update the alternatives to mark the selected one
                        val updatedAlternatives = meeting.suggestedAlternatives.mapIndexed { index, alternative ->
                            if (index == selectedAlternativeIndex) {
                                alternative.copy(isSelected = true)
                            } else {
                                alternative.copy(isSelected = false)
                            }
                        }
                        
                        // Update the meeting with the selected alternative
                        val updates = hashMapOf<String, Any>(
                            "suggestedAlternatives" to updatedAlternatives.map { it.toMap() },
                            "proposedDate" to selectedAlternative.date,
                            "proposedTime" to selectedAlternative.time,
                            "status" to com.example.parent_teacher_engagement.model.MeetingStatus.PENDING.name,
                            "updatedAt" to System.currentTimeMillis()
                        )
                        
                        database.reference.child("meeting_requests").child(meetingId)
                            .updateChildren(updates)
                            .addOnSuccessListener {
                                // Create updated todo items for both parties
                                val updatedMeeting = meeting.copy(
                                    proposedDate = selectedAlternative.date,
                                    proposedTime = selectedAlternative.time,
                                    status = com.example.parent_teacher_engagement.model.MeetingStatus.PENDING
                                )
                                
                                // Create todo item for requester with updated time
                                val requesterTodoItem = com.example.parent_teacher_engagement.model.TodoItem(
                                    id = UUID.randomUUID().toString(),
                                    title = "Meeting (Rescheduled): ${updatedMeeting.title}",
                                    description = "Meeting with ${updatedMeeting.recipientName}: ${updatedMeeting.description}",
                                    dueDate = selectedAlternative.date,
                                    dueTime = selectedAlternative.time,
                                    priority = com.example.parent_teacher_engagement.model.TodoItem.Priority.HIGH,
                                    category = "Meetings",
                                    isCompleted = false,
                                    meetingId = updatedMeeting.id
                                )
                                
                                // Create todo item for recipient with updated time
                                val recipientTodoItem = com.example.parent_teacher_engagement.model.TodoItem(
                                    id = UUID.randomUUID().toString(),
                                    title = "Meeting (Rescheduled): ${updatedMeeting.title}",
                                    description = "Meeting with ${updatedMeeting.requesterName}: ${updatedMeeting.description}",
                                    dueDate = selectedAlternative.date,
                                    dueTime = selectedAlternative.time,
                                    priority = com.example.parent_teacher_engagement.model.TodoItem.Priority.HIGH,
                                    category = "Meetings",
                                    isCompleted = false,
                                    meetingId = updatedMeeting.id
                                )
                                
                                // Update existing todos for both parties by adding new ones
                                // This approach adds new todos rather than updating existing ones
                                addTodoItem(
                                    todoItem = requesterTodoItem,
                                    userId = updatedMeeting.requesterId,
                                    onSuccess = {},
                                    onError = { e -> 
                                        Log.e("FirebaseService", "Failed to create todo for requester: ${e.message}")
                                    }
                                )
                                
                                addTodoItem(
                                    todoItem = recipientTodoItem,
                                    userId = updatedMeeting.recipientId,
                                    onSuccess = {},
                                    onError = { e -> 
                                        Log.e("FirebaseService", "Failed to create todo for recipient: ${e.message}")
                                    }
                                )
                                
                                // Send notification
                                val notificationRecipientId = if (getCurrentUserId() == meeting.requesterId) {
                                    meeting.recipientId
                                } else {
                                    meeting.requesterId
                                }
                                
                                sendNotification(
                                    notificationRecipientId,
                                    "Meeting Time Updated",
                                    "A new time has been selected for the meeting '${meeting.title}'."
                                )
                                
                                onSuccess()
                            }
                            .addOnFailureListener { exception ->
                                onError(exception)
                            }
                    } else {
                        onError(Exception("Invalid selection index"))
                    }
                },
                onError = { exception ->
                    onError(exception)
                }
            )
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error selecting alternative time", e)
            onError(e)
        }
    }
    
    // Helper function to get current user ID
    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }
    
    // Add notification method
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

    data class ActivityData(
        val id: String = "",
        val type: String = "", // "grade", "attendance", "message", "event"
        val title: String = "",
        val description: String = "",
        val studentId: String = "",
        val studentName: String = "",
        val timestamp: Long = 0,
        val relatedData: Map<String, Any> = emptyMap()
    )

    fun getRecentActivities(parentId: String, limit: Int = 5, callback: (List<ActivityData>) -> Unit) {
        // First get the parent's children
        getChildrenForParent(parentId) { children ->
            if (children.isEmpty()) {
                callback(emptyList())
                return@getChildrenForParent
            }
            
            val allActivities = mutableListOf<ActivityData>()
            var completedQueries = 0
            val totalQueries = 3 // Grades, attendance, messages
            
            // 1. Get recent grade activities
            val recentGradesQuery = database.reference.child("activities")
                .child("grades")
                .orderByChild("timestamp")
                .limitToLast(limit)
            
            recentGradesQuery.get().addOnSuccessListener { snapshot ->
                for (activitySnapshot in snapshot.children) {
                    val studentId = activitySnapshot.child("studentId").getValue(String::class.java) ?: continue
                    
                    // Check if this student belongs to the parent
                    if (children.any { it.id == studentId }) {
                        val student = children.first { it.id == studentId }
                        
                        val activity = ActivityData(
                            id = activitySnapshot.key ?: "",
                            type = "grade",
                            title = activitySnapshot.child("title").getValue(String::class.java) ?: "New Grade",
                            description = activitySnapshot.child("description").getValue(String::class.java) 
                                ?: "A new grade was posted",
                            studentId = studentId,
                            studentName = student.name,
                            timestamp = activitySnapshot.child("timestamp").getValue(Long::class.java) ?: 0
                        )
                        allActivities.add(activity)
                    }
                }
                
                completedQueries++
                if (completedQueries >= totalQueries) {
                    deliverSortedActivities(allActivities, limit, callback)
                }
            }.addOnFailureListener {
                completedQueries++
                if (completedQueries >= totalQueries) {
                    deliverSortedActivities(allActivities, limit, callback)
                }
            }
            
            // 2. Get recent attendance activities
            val recentAttendanceQuery = database.reference.child("activities")
                .child("attendance")
                .orderByChild("timestamp")
                .limitToLast(limit)
            
            recentAttendanceQuery.get().addOnSuccessListener { snapshot ->
                for (activitySnapshot in snapshot.children) {
                    val studentId = activitySnapshot.child("studentId").getValue(String::class.java) ?: continue
                    
                    // Check if this student belongs to the parent
                    if (children.any { it.id == studentId }) {
                        val student = children.first { it.id == studentId }
                        
                        val activity = ActivityData(
                            id = activitySnapshot.key ?: "",
                            type = "attendance",
                            title = activitySnapshot.child("title").getValue(String::class.java) ?: "Attendance Updated",
                            description = activitySnapshot.child("description").getValue(String::class.java) 
                                ?: "Attendance has been recorded",
                            studentId = studentId,
                            studentName = student.name,
                            timestamp = activitySnapshot.child("timestamp").getValue(Long::class.java) ?: 0
                        )
                        allActivities.add(activity)
                    }
                }
                
                completedQueries++
                if (completedQueries >= totalQueries) {
                    deliverSortedActivities(allActivities, limit, callback)
                }
            }.addOnFailureListener {
                completedQueries++
                if (completedQueries >= totalQueries) {
                    deliverSortedActivities(allActivities, limit, callback)
                }
            }
            
            // 3. Get recent messages
            val recentMessagesQuery = database.reference.child("messages")
                .orderByChild("timestamp")
                .limitToLast(limit)
            
            recentMessagesQuery.get().addOnSuccessListener { snapshot ->
                for (messageSnapshot in snapshot.children) {
                    val recipientId = messageSnapshot.child("recipientId").getValue(String::class.java) ?: continue
                    
                    // Check if this message is for the parent
                    if (recipientId == parentId) {
                        val senderId = messageSnapshot.child("senderId").getValue(String::class.java) ?: ""
                        val senderName = messageSnapshot.child("senderName").getValue(String::class.java) ?: "Teacher"
                        
                        val activity = ActivityData(
                            id = messageSnapshot.key ?: "",
                            type = "message",
                            title = "New Message",
                            description = "From $senderName",
                            studentId = "", // Messages might not be tied to a specific student
                            studentName = "",
                            timestamp = messageSnapshot.child("timestamp").getValue(Long::class.java) ?: 0
                        )
                        allActivities.add(activity)
                    }
                }
                
                completedQueries++
                if (completedQueries >= totalQueries) {
                    deliverSortedActivities(allActivities, limit, callback)
                }
            }.addOnFailureListener {
                completedQueries++
                if (completedQueries >= totalQueries) {
                    deliverSortedActivities(allActivities, limit, callback)
                }
            }
        }
    }
    
    private fun deliverSortedActivities(activities: List<ActivityData>, limit: Int, callback: (List<ActivityData>) -> Unit) {
        // Sort all activities by timestamp (most recent first)
        val sortedActivities = activities.sortedByDescending { it.timestamp }.take(limit)
        callback(sortedActivities)
    }
    
    // Method to log an activity when grades are entered
    fun logGradeActivity(studentId: String, subject: String, grade: String) {
        getUserDataById(studentId) { student ->
            if (student != null) {
                val activityData = hashMapOf(
                    "type" to "grade",
                    "title" to "New Grade Posted",
                    "description" to "$subject: $grade",
                    "studentId" to studentId,
                    "timestamp" to System.currentTimeMillis()
                )
                
                database.reference.child("activities")
                    .child("grades")
                    .push()
                    .setValue(activityData)
            }
        }
    }
    
    // Method to log an activity when attendance is recorded
    fun logAttendanceActivity(studentId: String, date: String, status: Boolean) {
        getUserDataById(studentId) { student ->
            if (student != null) {
                val statusText = if (status) "Present" else "Absent"
                val activityData = hashMapOf(
                    "type" to "attendance",
                    "title" to "Attendance Recorded",
                    "description" to "$statusText on $date",
                    "studentId" to studentId,
                    "timestamp" to System.currentTimeMillis()
                )
                
                database.reference.child("activities")
                    .child("attendance")
                    .push()
                    .setValue(activityData)
            }
        }
    }

    fun getUserDataById(userId: String, callback: (UserData?) -> Unit) {
        database.reference.child("users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val userData = snapshot.getValue(UserData::class.java)
                callback(userData)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to get user data by ID", exception)
                callback(null)
            }
    }
    
    fun getCalendarEventsForMonth(year: Int, month: Int, callback: (Map<Int, List<EventData>>) -> Unit) {
        // Calculate start and end timestamps for the month
        val calendar = java.util.Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        
        calendar.add(java.util.Calendar.MONTH, 1)
        calendar.add(java.util.Calendar.MILLISECOND, -1)
        val endOfMonth = calendar.timeInMillis
        
        // Query events within the month range
        database.reference.child("events")
            .orderByChild("date")
            .startAt(startOfMonth.toDouble())
            .endAt(endOfMonth.toDouble())
            .get()
            .addOnSuccessListener { snapshot ->
                val eventsByDay = mutableMapOf<Int, MutableList<EventData>>()
                
                for (eventSnapshot in snapshot.children) {
                    try {
                        val dateValue = eventSnapshot.child("date").getValue(Long::class.java)
                        if (dateValue != null) {
                            // Convert timestamp to day of month
                            val eventCalendar = java.util.Calendar.getInstance()
                            eventCalendar.timeInMillis = dateValue
                            val dayOfMonth = eventCalendar.get(java.util.Calendar.DAY_OF_MONTH)
                            
                            // Format the date string
                            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date(dateValue))
                            
                            // Create event data
                            val event = EventData(
                                id = eventSnapshot.key ?: "",
                                title = eventSnapshot.child("title").getValue(String::class.java),
                                date = dateStr,
                                time = eventSnapshot.child("time").getValue(String::class.java),
                                description = eventSnapshot.child("description").getValue(String::class.java)
                            )
                            
                            // Add to the map
                            if (!eventsByDay.containsKey(dayOfMonth)) {
                                eventsByDay[dayOfMonth] = mutableListOf()
                            }
                            eventsByDay[dayOfMonth]?.add(event)
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseService", "Error parsing event for calendar", e)
                    }
                }
                
                callback(eventsByDay)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseService", "Failed to get calendar events", e)
                callback(emptyMap())
            }
    }

    fun submitProgress(
        teacherId: String,
        studentId: String,
        progress: String,
        semester: String,
        subject: String,
        assessments: Map<String, Int>? = null,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        // First, save the overall progress score
        val progressRef = database.reference.child("progress") // Use a different path than "grades"
            .child(semester)
            .child(studentId)
            .child(subject)

        progressRef.setValue(progress)
            .addOnSuccessListener {
                // If there are assessments, save them too
                if (assessments != null) {
                    val assessmentRef = database.reference.child("progress_assessments") // Use a different path than "assessments"
                        .child(semester)
                        .child(studentId)
                        .child(subject)

                    assessmentRef.setValue(assessments)
                        .addOnSuccessListener {
                            onSuccess()
                        }
                        .addOnFailureListener { exception ->
                            onError(exception)
                        }
                } else {
                    onSuccess()
                }
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun getProgressData(studentId: String, semester: String, subject: String, callback: (String?) -> Unit) {
        database.reference.child("progress")
            .child(semester)
            .child(studentId)
            .child(subject)
            .get()
            .addOnSuccessListener { snapshot ->
                val progress = snapshot.getValue(String::class.java)
                callback(progress)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to get progress for student $studentId", exception)
                callback(null)
            }
    }

    fun getProgressAssessmentDetails(
        studentId: String,
        semester: String,
        subject: String,
        onComplete: (List<AssessmentDetail>) -> Unit
    ) {
        Log.d("FirebaseService", "Getting progress assessment details for student: $studentId, semester: $semester, subject: $subject")
        
        // Use the progress_assessments path
        val subjectRef = database.reference
            .child("progress_assessments")
            .child(semester)
            .child(studentId)
            .child(subject)

        Log.d("FirebaseService", "Database reference path: ${subjectRef}")

        subjectRef.get().addOnSuccessListener { snapshot ->
            Log.d("FirebaseService", "Got progress assessment data: exists=${snapshot.exists()}, value=${snapshot.value}")
            if (!snapshot.exists()) {
                Log.d("FirebaseService", "No progress assessment data found")
                onComplete(emptyList())
                return@addOnSuccessListener
            }
            processAssessments(snapshot, onComplete)
        }.addOnFailureListener { exception ->
            Log.e("FirebaseService", "Error getting progress assessment details", exception)
            onComplete(emptyList())
        }
    }

    // Problem reporting methods
    fun submitSystemProblemReport(
        userId: String,
        userRole: String,
        userName: String,
        description: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val reportId = database.reference.child("problem_reports").push().key ?: return
        
        val reportData = hashMapOf(
            "id" to reportId,
            "userId" to userId,
            "userRole" to userRole,
            "userName" to userName,
            "type" to "system",
            "description" to description,
            "timestamp" to System.currentTimeMillis(),
            "status" to "pending"
        )
        
        database.reference.child("problem_reports").child(reportId)
            .setValue(reportData)
            .addOnSuccessListener {
                // Update the active problems count immediately
                updateActiveProblemCount()
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun submitUserProblemReport(
        userId: String,
        userRole: String, 
        userName: String,
        reportedUserEmail: String,
        reportType: String, // "teacher" or "parent"
        description: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val reportId = database.reference.child("problem_reports").push().key ?: return
        
        val reportData = hashMapOf(
            "id" to reportId,
            "userId" to userId,
            "userRole" to userRole,
            "userName" to userName,
            "type" to "user",
            "reportType" to reportType,
            "reportedUserEmail" to reportedUserEmail,
            "description" to description,
            "timestamp" to System.currentTimeMillis(),
            "status" to "pending"
        )
        
        database.reference.child("problem_reports").child(reportId)
            .setValue(reportData)
            .addOnSuccessListener {
                // Update the active problems count immediately
                updateActiveProblemCount()
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
    
    // Private helper method to update the active problem count
    private var isUpdatingProblems = false
    private var lastProblemUpdate = 0L
    private val MIN_UPDATE_INTERVAL = 500L // 500ms minimum between updates
    
    private fun updateActiveProblemCount() {
        val currentTime = System.currentTimeMillis()
        
        // If an update is already in progress or we updated very recently, skip this update
        if (isUpdatingProblems || (currentTime - lastProblemUpdate < MIN_UPDATE_INTERVAL)) {
            return
        }
        
        isUpdatingProblems = true
        database.reference.child("problem_reports")
            .get()
            .addOnSuccessListener { snapshot ->
                val activeProblems = snapshot.children.count { problemSnapshot -> 
                    val status = problemSnapshot.child("status").getValue(String::class.java)
                    status != "resolved"
                }
                
                // Update the count in the dedicated counter node
                database.reference.child("counts").child("problems_active")
                    .setValue(activeProblems)
                    .addOnCompleteListener {
                        isUpdatingProblems = false
                        lastProblemUpdate = System.currentTimeMillis()
                    }
            }
            .addOnFailureListener {
                isUpdatingProblems = false
            }
    }
    
    // Methods for admin problem management
    fun getProblemReportsByRoleAndType(
        role: String,
        type: String,
        callback: (List<com.example.parent_teacher_engagement.screens.ProblemReport>, Exception?) -> Unit
    ) {
        database.reference.child("problem_reports")
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    val problems = mutableListOf<com.example.parent_teacher_engagement.screens.ProblemReport>()
                    
                    for (reportSnapshot in snapshot.children) {
                        val map = reportSnapshot.value as? Map<*, *> ?: continue
                        
                        val userRole = map["userRole"]?.toString() ?: continue
                        val reportType = map["type"]?.toString() ?: continue
                        
                        // Filter by role and type
                        if (userRole == role && reportType == type) {
                            val problem = com.example.parent_teacher_engagement.screens.ProblemReport(
                                id = map["id"]?.toString() ?: "",
                                userId = map["userId"]?.toString() ?: "",
                                userName = map["userName"]?.toString() ?: "",
                                userRole = userRole,
                                type = reportType,
                                reportType = map["reportType"]?.toString(),
                                reportedUserEmail = map["reportedUserEmail"]?.toString(),
                                description = map["description"]?.toString() ?: "",
                                timestamp = (map["timestamp"] as? Long) ?: 0L,
                                status = map["status"]?.toString() ?: "pending"
                            )
                            problems.add(problem)
                        }
                    }
                    
                    callback(problems.sortedByDescending { it.timestamp }, null)
                } catch (e: Exception) {
                    Log.e("FirebaseService", "Error getting problem reports", e)
                    callback(emptyList(), e)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to get problem reports", exception)
                callback(emptyList(), exception)
            }
    }
    
    fun getProblemReportById(
        problemId: String,
        callback: (com.example.parent_teacher_engagement.screens.ProblemReport?, Exception?) -> Unit
    ) {
        database.reference.child("problem_reports").child(problemId)
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    val map = snapshot.value as? Map<*, *>
                    if (map == null) {
                        callback(null, Exception("Problem report not found"))
                        return@addOnSuccessListener
                    }
                    
                    val problem = com.example.parent_teacher_engagement.screens.ProblemReport(
                        id = map["id"]?.toString() ?: "",
                        userId = map["userId"]?.toString() ?: "",
                        userName = map["userName"]?.toString() ?: "",
                        userRole = map["userRole"]?.toString() ?: "",
                        type = map["type"]?.toString() ?: "",
                        reportType = map["reportType"]?.toString(),
                        reportedUserEmail = map["reportedUserEmail"]?.toString(),
                        description = map["description"]?.toString() ?: "",
                        timestamp = (map["timestamp"] as? Long) ?: 0L,
                        status = map["status"]?.toString() ?: "pending"
                    )
                    
                    callback(problem, null)
                } catch (e: Exception) {
                    Log.e("FirebaseService", "Error getting problem report by ID", e)
                    callback(null, e)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to get problem report by ID", exception)
                callback(null, exception)
            }
    }
    
    fun updateProblemStatus(
        problemId: String,
        status: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val updates = mapOf(
            "status" to status,
            "updatedAt" to System.currentTimeMillis()
        )
        
        database.reference.child("problem_reports").child(problemId)
            .updateChildren(updates)
            .addOnSuccessListener {
                // Update the active problems count
                updateActiveProblemCount()
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
    
    // Get the total count of problem reports
    fun getProblemReportsCount(callback: (Int) -> Unit) {
        database.reference.child("problem_reports")
            .get()
            .addOnSuccessListener { snapshot ->
                // Count only non-resolved problems
                val activeProblems = snapshot.children.count { problemSnapshot -> 
                    val status = problemSnapshot.child("status").getValue(String::class.java)
                    status != "resolved"
                }
                callback(activeProblems)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to get problem reports count", exception)
                callback(0)
            }
    }

    // Add a real-time listener for problem reports
    fun addProblemReportsListener(onCountUpdated: (Int) -> Unit) {
        // Track the last time we updated to prevent too-frequent updates
        var lastUpdateTime = 0L
        
        database.reference.child("problem_reports")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        // Count only non-resolved problems
                        val activeProblems = snapshot.children.count { problemSnapshot -> 
                            val status = problemSnapshot.child("status").getValue(String::class.java)
                            status != "resolved"
                        }
                        
                        // Update the active problems count in a separate node for easy access
                        database.reference.child("counts").child("problems_active")
                            .setValue(activeProblems)
                        
                        // Prevent too-frequent updates by adding a minimum interval
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastUpdateTime > 300) { // 300ms minimum between updates
                            // Notify listener of the updated count
                            onCountUpdated(activeProblems)
                            lastUpdateTime = currentTime
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseService", "Error counting problem reports", e)
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseService", "Problem reports listener cancelled", error.toException())
                }
            })
    }

    fun getUserRole(userId: String, onRoleLoaded: (String) -> Unit) {
        database.reference.child("users")
            .child(userId)
            .child("role")
            .get()
            .addOnSuccessListener { snapshot ->
                val role = snapshot.value as? String ?: ""
                onRoleLoaded(role)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to get user role", exception)
                onRoleLoaded("")
            }
    }

    fun getTeacherSubmittedMarks(
        studentId: String,
        teacherId: String,
        type: String,
        semester: String,
        callback: (Map<String, String>) -> Unit
    ) {
        val path = if (type == "Grade") "grades" else "progress"
        database.reference.child(path)
            .child(semester)
            .child(studentId)
            .get()
            .addOnSuccessListener { snapshot ->
                val marks = mutableMapOf<String, String>()
                snapshot.children.forEach { subjectSnapshot ->
                    val subject = subjectSnapshot.key ?: return@forEach
                    val mark = subjectSnapshot.getValue(String::class.java) ?: return@forEach
                    marks[subject] = mark
                }
                callback(marks)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to get $type for student $studentId", exception)
                callback(emptyMap())
            }
    }

    fun updateTeacherSubmittedMark(
        studentId: String,
        teacherId: String,
        type: String,
        semester: String,
        subject: String,
        newMark: String,
        onSuccess: () -> Unit
    ) {
        val path = if (type == "Grade") "grades" else "progress"
        database.reference.child(path)
            .child(semester)
            .child(studentId)
            .child(subject)
            .setValue(newMark)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to update $type for student $studentId", exception)
            }
    }

    fun deleteTeacherSubmittedMark(
        studentId: String,
        teacherId: String,
        type: String,
        semester: String,
        subject: String,
        onSuccess: () -> Unit
    ) {
        val path = if (type == "Grade") "grades" else "progress"
        database.reference.child(path)
            .child(semester)
            .child(studentId)
            .child(subject)
            .removeValue()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to delete $type for student $studentId", exception)
            }
    }

    data class AnnouncementData(
        val id: String = "",
        val teacherId: String = "",
        val teacherName: String = "",
        val date: String = "",
        val subject: String = "",
        val period: Int = 0,
        val title: String = "",
        val time: String = "",
        val description: String = "",
        val createdAt: Long = 0
    )

    fun createAnnouncement(
        teacherId: String,
        teacherName: String,
        date: String,
        subject: String,
        period: Int,
        title: String,
        time: String,
        description: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val announcementId = database.reference.child("announcements").push().key ?: return
        
        val announcementData = AnnouncementData(
            id = announcementId,
            teacherId = teacherId,
            teacherName = teacherName,
            date = date,
            subject = subject,
            period = period,
            title = title,
            time = time,
            description = description,
            createdAt = System.currentTimeMillis()
        )

        database.reference.child("announcements")
            .child(announcementId)
            .setValue(announcementData)
            .addOnSuccessListener {
                Log.d("FirebaseService", "Announcement created successfully")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Failed to create announcement", exception)
                onError(exception)
            }
    }

    fun getAnnouncements(
        onSuccess: (List<AnnouncementData>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        database.reference.child("announcements")
            .orderByChild("createdAt")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val announcements = mutableListOf<AnnouncementData>()
                    for (announcementSnapshot in snapshot.children) {
                        val announcement = announcementSnapshot.getValue(AnnouncementData::class.java)
                        if (announcement != null) {
                            announcements.add(announcement)
                        }
                    }
                    onSuccess(announcements.sortedByDescending { it.createdAt })
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseService", "Failed to get announcements", error.toException())
                    onError(error.toException())
                }
            })
    }
}
