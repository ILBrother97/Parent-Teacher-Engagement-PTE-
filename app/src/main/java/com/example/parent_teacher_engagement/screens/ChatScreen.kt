package com.example.parent_teacher_engagement.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.parent_teacher_engagement.firebase.FirebaseService
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    partnerId: String,
    onBack: () -> Unit
) {
    val firebaseService = FirebaseService.getInstance()
    val currentUser = firebaseService.currentUserData
    val currentUserId = currentUser?.uid ?: ""
    val context = LocalContext.current
    
    // State for messages and partner info
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var partnerUser by remember { mutableStateOf<FirebaseService.UserListData?>(null) }
    var messageText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    
    // Message manipulation states
    var selectedMessagesSet by remember { mutableStateOf<Set<Message>>(emptySet()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var editedContent by remember { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showForwardDialog by remember { mutableStateOf(false) }
    
    // State for users list (for forwarding)
    var users by remember { mutableStateOf<List<FirebaseService.UserListData>>(emptyList()) }
    
    // Track if screen is active to prevent unintended back navigation
    var isScreenActive by remember { mutableStateOf(false) }
    
    // Load users for forwarding
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            when (currentUser.role) {
                "parent" -> {
                    firebaseService.getUsersByRole("teacher", 
                        onSuccess = { teachersList ->
                            users = teachersList
                        },
                        onError = { error ->
                            Log.e("ChatScreen", "Error loading teachers", error)
                        }
                    )
                }
                "teacher" -> {
                    firebaseService.getUsersByRole("parent", 
                        onSuccess = { parentsList ->
                            users = parentsList
                        },
                        onError = { error ->
                            Log.e("ChatScreen", "Error loading parents", error)
                        }
                    )
                }
            }
        }
    }
    
    // Load partner user info
    LaunchedEffect(partnerId) {
        firebaseService.getUserById(partnerId) { user ->
            partnerUser = user
        }
    }
    
    // Load messages
    LaunchedEffect(currentUserId, partnerId) {
        if (currentUserId.isNotEmpty() && partnerId.isNotEmpty()) {
            isLoading = true
            firebaseService.getMessages(
                userId1 = currentUserId,
                userId2 = partnerId
            ) { messagesList ->
                messages = messagesList.sortedBy { it.timestamp }
                isLoading = false
                isScreenActive = true
            }
        }
    }

    // Mark messages as read when screen is active
    LaunchedEffect(isScreenActive) {
        if (isScreenActive && currentUserId.isNotEmpty() && partnerId.isNotEmpty()) {
            firebaseService.markAllMessagesAsRead(currentUserId, partnerId, context)
        }
    }

    // Prevent unintended back navigation
    DisposableEffect(Unit) {
        onDispose {
            isScreenActive = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (selectedMessagesSet.isNotEmpty()) {
                        // Selection mode title
                        Text("${selectedMessagesSet.size} selected")
                    } else {
                        // Normal title with partner info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            // Partner avatar
                            if (partnerUser != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = (partnerUser?.name?.firstOrNull() ?: "?").toString().uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column {
                                    Text(
                                        text = partnerUser?.name ?: "Chat",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    partnerUser?.role?.let { role ->
                                        Text(
                                            text = role.capitalize(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Text(text = "Chat")
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (selectedMessagesSet.isNotEmpty()) {
                        // Close selection mode
                        IconButton(onClick = { selectedMessagesSet = emptySet() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear selection"
                            )
                        }
                    } else {
                        // Back button - only allow back navigation when screen is active
                        IconButton(onClick = { 
                            if (isScreenActive) {
                                onBack()
                            }
                        }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    // Show actions when messages are selected
                    if (selectedMessagesSet.isNotEmpty()) {
                        // Edit action (only for single message)
                        if (selectedMessagesSet.size == 1) {
                            val message = selectedMessagesSet.first()
                            // Only allow editing own messages
                            if (message.senderId == currentUserId) {
                                IconButton(onClick = {
                                    editingMessage = message
                                    editedContent = message.content
                                    showEditDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                            }
                        }
                        
                        // Forward action
                        IconButton(onClick = { showForwardDialog = true }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Forward")
                        }
                        
                        // Delete action
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (selectedMessagesSet.isNotEmpty()) 
                        MaterialTheme.colorScheme.primaryContainer
                    else 
                        MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages list
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    reverseLayout = true
                ) {
                    items(messages.sortedByDescending { it.timestamp }) { message ->
                        MessageBubble(
                            message = message,
                            isCurrentUser = message.senderId == currentUserId,
                            isSelected = selectedMessagesSet.contains(message),
                            onLongClick = {
                                selectedMessagesSet = if (selectedMessagesSet.contains(message)) {
                                    selectedMessagesSet - message
                                } else {
                                    selectedMessagesSet + message
                                }
                            },
                            onClick = {
                                if (selectedMessagesSet.isNotEmpty()) {
                                    selectedMessagesSet = if (selectedMessagesSet.contains(message)) {
                                        selectedMessagesSet - message
                                    } else {
                                        selectedMessagesSet + message
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            // Message input
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message") },
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                // Send text-only message
                                val message = Message(
                                    senderId = currentUserId,
                                    senderName = currentUser?.name ?: "",
                                    receiverId = partnerId,
                                    content = messageText
                                )
                                firebaseService.sendMessage(message)
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (messageText.isNotBlank()) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Send, 
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank()) 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    // Edit Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Message") },
            text = {
                OutlinedTextField(
                    value = editedContent,
                    onValueChange = { editedContent = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        editingMessage?.let { message ->
                            FirebaseService.getInstance().updateMessage(
                                message.copy(content = editedContent)
                            )
                        }
                        showEditDialog = false
                        selectedMessagesSet = emptySet()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Messages") },
            text = { Text("How would you like to delete the selected messages?") },
            confirmButton = {
                Column {
                    TextButton(
                        onClick = {
                            selectedMessagesSet.forEach { message ->
                                FirebaseService.getInstance().deleteMessage(message.id, true)
                            }
                            showDeleteConfirmation = false
                            selectedMessagesSet = emptySet()
                        }
                    ) {
                        Text("Delete for everyone")
                    }
                    TextButton(
                        onClick = {
                            selectedMessagesSet.forEach { message ->
                                FirebaseService.getInstance().deleteMessage(message.id, false)
                            }
                            showDeleteConfirmation = false
                            selectedMessagesSet = emptySet()
                        }
                    ) {
                        Text("Delete for me only")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Forward Dialog
    if (showForwardDialog) {
        AlertDialog(
            onDismissRequest = { showForwardDialog = false },
            title = { Text("Forward To") },
            text = {
                LazyColumn {
                    items(users.filter { it.id != partnerId }) { user ->
                        ListItem(
                            headlineContent = { Text(user.name ?: "Unknown") },
                            modifier = Modifier.clickable {
                                selectedMessagesSet.forEach { message ->
                                    FirebaseService.getInstance().sendMessage(
                                        message.copy(
                                            id = "",
                                            senderId = currentUserId,
                                            senderName = currentUser?.name ?: "",
                                            receiverId = user.id,
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                }
                                showForwardDialog = false
                                selectedMessagesSet = emptySet()
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showForwardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            else if (isCurrentUser) 
                MaterialTheme.colorScheme.primaryContainer 
            else if (!message.isRead && !isCurrentUser)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
            else 
                MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 0.dp,
                bottomEnd = if (isCurrentUser) 0.dp else 16.dp
            ),
            modifier = Modifier
                .widthIn(max = 280.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongClick() },
                        onTap = { onClick() }
                    )
                }
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Message content
                if (message.content.isNotBlank()) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // File attachment
                if (message.hasAttachment) {
                    if (message.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    val context = LocalContext.current
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Open file
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse(message.fileUrl)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                
                                // Safely start the activity
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("ChatScreen", "Error opening file", e)
                                    // Handle error (could show a Toast here)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // File icon based on type
                            Icon(
                                imageVector = when {
                                    message.fileType.startsWith("image/") -> Icons.Default.Image
                                    message.fileType.startsWith("video/") -> Icons.Default.VideoFile
                                    message.fileType.startsWith("audio/") -> Icons.Default.AudioFile
                                    message.fileType.startsWith("application/pdf") -> Icons.Default.PictureAsPdf
                                    else -> Icons.Default.InsertDriveFile
                                },
                                contentDescription = "File",
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = message.fileName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                // File size
                                if (message.fileSize > 0) {
                                    Text(
                                        text = formatFileSize(message.fileSize),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                // Message timestamp
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

// Helper function to format file size
private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

// Helper function to format timestamp
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
} 