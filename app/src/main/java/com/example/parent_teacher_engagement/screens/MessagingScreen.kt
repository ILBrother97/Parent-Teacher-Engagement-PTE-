package com.example.parent_teacher_engagement.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Chat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import com.example.parent_teacher_engagement.firebase.FirebaseService
import java.text.SimpleDateFormat
import java.util.*

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    var isSelected: Boolean = false,
    val deletedFor: Map<String, Boolean> = emptyMap(),
    val isRead: Boolean = false,
    // File attachment fields
    val hasAttachment: Boolean = false,
    val fileUrl: String = "",
    val fileName: String = "",
    val fileType: String = "",
    val fileSize: Long = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingScreen(
    onBack: () -> Unit,
    navController: androidx.navigation.NavController? = null
) {
    val firebaseService = FirebaseService.getInstance()
    val currentUser = firebaseService.currentUserData
    val currentUserId = currentUser?.uid ?: ""
    
    // State for users list
    var users by remember { mutableStateOf<List<FirebaseService.UserListData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Track unread messages per user
    var unreadMessagesBySender by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    
    // Add search functionality
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    
    // Track latest message timestamps for each user
    var latestMessageTimestamps by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    
    // Load users based on current user role
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            when (currentUser.role) {
                "parent" -> {
                    // Parents can message teachers
                    firebaseService.getUsersByRole("teacher", 
                        onSuccess = { teachersList ->
                            users = teachersList
                            isLoading = false
                        },
                        onError = { error ->
                            Log.e("MessagingScreen", "Error loading teachers", error)
                            isLoading = false
                        },
                        isRealtime = true  // Enable real-time updates
                    )
                }
                "teacher" -> {
                    // Teachers can message parents
                    firebaseService.getUsersByRole("parent", 
                        onSuccess = { parentsList ->
                            users = parentsList
                            isLoading = false
                        },
                        onError = { error ->
                            Log.e("MessagingScreen", "Error loading parents", error)
                            isLoading = false
                        },
                        isRealtime = true  // Enable real-time updates
                    )
                }
                else -> {
                    isLoading = false
                }
            }
        }
    }
    
    // Listen for all messages in real-time
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            // Get all messages where current user is either sender or receiver
            firebaseService.getMessages(currentUserId, "") { messages ->
                // Group messages by conversation partner and get latest timestamp
                val timestamps = messages.groupBy { message ->
                    if (message.senderId == currentUserId) message.receiverId else message.senderId
                }.mapValues { entry ->
                    entry.value.maxOfOrNull { it.timestamp } ?: 0L
                }
                
                latestMessageTimestamps = timestamps
            }
        }
    }

    // Get all unread messages to highlight users with unread messages
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            firebaseService.getUnreadMessages(currentUserId) { unreadMessages ->
                // Count unread messages by sender
                val countBySender = unreadMessages.groupBy { it.senderId }
                    .mapValues { it.value.size }
                unreadMessagesBySender = countBySender
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(text = "Messages") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { searchActive = !searchActive }) {
                            Icon(
                                if (searchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (searchActive) "Close Search" else "Search Contacts"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                
                // Search bar
                AnimatedVisibility(
                    visible = searchActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search contacts...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (users.isEmpty()) {
                // No users available
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No contacts available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Show users list
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Filter users by search query
                    val filteredUsers = if (searchQuery.isEmpty()) {
                        users.sortedByDescending { user -> latestMessageTimestamps[user.id] ?: 0L }
                    } else {
                        users.filter { user ->
                            user.name?.contains(searchQuery, ignoreCase = true) == true ||
                            user.email?.contains(searchQuery, ignoreCase = true) == true
                        }.sortedByDescending { user -> latestMessageTimestamps[user.id] ?: 0L }
                    }
                    
                    if (filteredUsers.isEmpty() && searchQuery.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No contacts matching \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        items(filteredUsers) { user ->
                            ContactItem(
                                user = user,
                                hasUnreadMessages = unreadMessagesBySender.containsKey(user.id) && unreadMessagesBySender[user.id]!! > 0,
                                unreadCount = unreadMessagesBySender[user.id] ?: 0,
                                onClick = {
                                    // Navigate directly to the chat screen without setting currentChatPartnerId
                                    Log.d("MessagingScreen", "Navigating to chat with user: ${user.id}")
                                    navController?.navigate(com.example.parent_teacher_engagement.navigation.Screen.Chat.createRoute(user.id))
                                }
                            )
                            Divider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(
    user: FirebaseService.UserListData,
    hasUnreadMessages: Boolean,
    unreadCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User avatar
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (user.name?.firstOrNull() ?: "?").toString().uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // User info
                Column {
                    Text(
                        text = "${user.name ?: "Unknown"} (${user.email ?: ""})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = user.role?.capitalize() ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Unread badge
            if (hasUnreadMessages) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Text(if (unreadCount > 0) unreadCount.toString() else "New")
                }
            }
        }
    }
}