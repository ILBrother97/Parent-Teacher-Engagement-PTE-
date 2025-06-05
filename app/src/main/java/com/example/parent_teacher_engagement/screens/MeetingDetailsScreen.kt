package com.example.parent_teacher_engagement.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.parent_teacher_engagement.firebase.FirebaseService
import com.example.parent_teacher_engagement.model.MeetingRequest
import com.example.parent_teacher_engagement.model.MeetingStatus
import com.example.parent_teacher_engagement.model.TodoItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingDetailsScreen(
    meetingId: String,
    navController: NavController
) {
    val firebaseService = remember { FirebaseService.getInstance() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // State
    var currentUser by remember { mutableStateOf<FirebaseService.UserData?>(null) }
    var meeting by remember { mutableStateOf<MeetingRequest?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showConfirmationDialog by remember { mutableStateOf<String?>(null) }
    
    // New state for alternate time suggestion
    var showAlternateTimeDialog by remember { mutableStateOf(false) }
    var alternativeDate by remember { mutableStateOf(LocalDate.now()) }
    var alternativeTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var responseMessage by remember { mutableStateOf("") }
    var alternativeDates by remember { mutableStateOf(listOf<LocalDate>()) }
    var alternativeTimes by remember { mutableStateOf(listOf<LocalTime>()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var editingAlternativeIndex by remember { mutableStateOf(-1) } // -1 for adding new, >= 0 for editing
    
    // Get current user and meeting data
    LaunchedEffect(meetingId) {
        firebaseService.getCurrentUserData { userData ->
            currentUser = userData
            if (userData != null) {
                firebaseService.getMeetingById(
                    meetingId = meetingId,
                    onSuccess = { meetingData ->
                        meeting = meetingData
                        isLoading = false
                    },
                    onError = { exception ->
                        scope.launch {
                            snackbarHostState.showSnackbar("Failed to load meeting: ${exception.message}")
                        }
                        isLoading = false
                    }
                )
            } else {
                isLoading = false
            }
        }
    }
    
    // Action Confirmation Dialog
    if (showConfirmationDialog != null) {
        val action = showConfirmationDialog!!
        val (title, message, status) = when (action) {
            "accept" -> Triple(
                "Accept Meeting",
                "Are you sure you want to accept this meeting request?",
                MeetingStatus.ACCEPTED
            )
            "reject" -> Triple(
                "Reject Meeting",
                "Are you sure you want to reject this meeting request?",
                MeetingStatus.REJECTED
            )
            "cancel" -> Triple(
                "Cancel Meeting",
                "Are you sure you want to cancel this meeting?",
                MeetingStatus.CANCELLED
            )
            "complete" -> Triple(
                "Complete Meeting",
                "Mark this meeting as completed?",
                MeetingStatus.COMPLETED
            )
            else -> Triple("", "", MeetingStatus.PENDING)
        }
        
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        meeting?.let { currentMeeting ->
                            firebaseService.updateMeetingStatus(
                                meetingId = currentMeeting.id,
                                newStatus = status,
                                onSuccess = {
                                    // Update local state
                                    meeting = currentMeeting.copy(
                                        status = status,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    
                                    // Show success message
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Meeting ${status.name.lowercase()} successfully")
                                    }
                                    
                                    // Firebase service now handles todo creation automatically
                                },
                                onError = { exception ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Failed to update meeting: ${exception.message}")
                                    }
                                }
                            )
                        }
                        showConfirmationDialog = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmationDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Alternative Time Dialog
    if (showAlternateTimeDialog) {
        Dialog(
            onDismissRequest = { showAlternateTimeDialog = false }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Suggest Alternative Times",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = responseMessage,
                        onValueChange = { responseMessage = it },
                        label = { Text("Response Message") },
                        placeholder = { Text("I'm not available at that time, but I can meet at...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    
                    // List of already added alternatives
                    if (alternativeDates.isNotEmpty()) {
                        Text(
                            text = "Suggested Times:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            alternativeDates.forEachIndexed { index, date ->
                                val time = alternativeTimes[index]
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${date.format(DateTimeFormatter.ISO_DATE)} at ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    
                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingAlternativeIndex = index
                                                alternativeDate = date
                                                alternativeTime = time
                                                showDatePicker = true
                                            }
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                                        }
                                        
                                        IconButton(
                                            onClick = {
                                                val updatedDates = alternativeDates.toMutableList().apply { removeAt(index) }
                                                val updatedTimes = alternativeTimes.toMutableList().apply { removeAt(index) }
                                                alternativeDates = updatedDates
                                                alternativeTimes = updatedTimes
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Add new alternative section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { showDatePicker = true },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                                Text(alternativeDate.format(DateTimeFormatter.ISO_DATE))
                            }
                        }
                        
                        Button(
                            onClick = { showTimePicker = true },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null)
                                Text(alternativeTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                            }
                        }
                    }
                    
                    Button(
                        onClick = {
                            if (editingAlternativeIndex >= 0 && editingAlternativeIndex < alternativeDates.size) {
                                // Update existing alternative
                                val updatedDates = alternativeDates.toMutableList().apply {
                                    set(editingAlternativeIndex, alternativeDate)
                                }
                                val updatedTimes = alternativeTimes.toMutableList().apply {
                                    set(editingAlternativeIndex, alternativeTime)
                                }
                                alternativeDates = updatedDates
                                alternativeTimes = updatedTimes
                                editingAlternativeIndex = -1 // Reset editing index
                            } else {
                                // Add new alternative
                                alternativeDates = alternativeDates + alternativeDate
                                alternativeTimes = alternativeTimes + alternativeTime
                            }
                            
                            // Reset date and time for next addition
                            alternativeDate = LocalDate.now()
                            alternativeTime = LocalTime.of(9, 0)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (editingAlternativeIndex >= 0) "Update Time" else "Add Time")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Submit button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAlternateTimeDialog = false }) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                if (alternativeDates.isNotEmpty()) {
                                    val alternatives = alternativeDates.mapIndexed { index, date ->
                                        com.example.parent_teacher_engagement.model.AlternativeTime(
                                            date = date.format(DateTimeFormatter.ISO_DATE),
                                            time = alternativeTimes[index].format(DateTimeFormatter.ofPattern("HH:mm")),
                                            suggestedBy = currentUser?.uid ?: "",
                                            suggestedAt = System.currentTimeMillis(),
                                            isSelected = false
                                        )
                                    }
                                    
                                    meeting?.let { currentMeeting ->
                                        firebaseService.suggestAlternativeMeetingTimes(
                                            meetingId = currentMeeting.id,
                                            alternatives = alternatives,
                                            responseMessage = responseMessage,
                                            onSuccess = {
                                                // Update local state
                                                meeting = currentMeeting.copy(
                                                    status = MeetingStatus.RESCHEDULED,
                                                    suggestedAlternatives = currentMeeting.suggestedAlternatives + alternatives,
                                                    responseMessage = responseMessage,
                                                    updatedAt = System.currentTimeMillis()
                                                )
                                                
                                                // Close dialog and show success message
                                                showAlternateTimeDialog = false
                                                alternativeDates = emptyList()
                                                alternativeTimes = emptyList()
                                                responseMessage = ""
                                                
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Alternative times suggested successfully")
                                                }
                                            },
                                            onError = { exception ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Failed to suggest alternative times: ${exception.message}")
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Please add at least one alternative time")
                                    }
                                }
                            },
                            enabled = alternativeDates.isNotEmpty()
                        ) {
                            Text("Suggest Times")
                        }
                    }
                }
            }
        }
    }
    
    // Date Picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = alternativeDate.toEpochDay() * 24 * 60 * 60 * 1000
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    datePickerState.selectedDateMillis?.let { millis ->
                        alternativeDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                    }
                    showDatePicker = false
                    showTimePicker = true // Show time picker after date is selected
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // Time Picker
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = alternativeTime.hour,
            initialMinute = alternativeTime.minute
        )
        
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = timePickerState)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel")
                        }
                        TextButton(onClick = { 
                            alternativeTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(meeting?.title ?: "Meeting Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (meeting == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Meeting not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = getStatusColor(meeting!!.status).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Status: ${meeting!!.status.name}",
                            style = MaterialTheme.typography.titleMedium,
                            color = getStatusColor(meeting!!.status)
                        )
                        
                        StatusBadge(status = meeting!!.status)
                    }
                }
                
                // Display response message if available
                if (meeting!!.responseMessage.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Response Message:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = meeting!!.responseMessage,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Display alternative times if available
                if (meeting!!.suggestedAlternatives.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Alternative Times:",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            meeting!!.suggestedAlternatives.forEachIndexed { index, alternative ->
                                val isSelected = alternative.isSelected
                                val isSuggestedByCurrentUser = alternative.suggestedBy == currentUser?.uid
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) 
                                            MaterialTheme.colorScheme.primaryContainer 
                                        else 
                                            MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${alternative.date} at ${alternative.time}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            )
                                            
                                            Text(
                                                text = "Suggested by: ${if (isSuggestedByCurrentUser) "You" else if (meeting!!.requesterId == alternative.suggestedBy) meeting!!.requesterName else meeting!!.recipientName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        
                                        // If requester, show select button for alternatives suggested by recipient
                                        if (currentUser?.uid == meeting!!.requesterId && !isSuggestedByCurrentUser && !isSelected && meeting!!.status == MeetingStatus.RESCHEDULED) {
                                            Button(
                                                onClick = {
                                                    firebaseService.selectAlternativeMeetingTime(
                                                        meetingId = meeting!!.id,
                                                        selectedAlternativeIndex = index,
                                                        onSuccess = {
                                                            // Update local meeting state with selected time
                                                            val updatedAlternatives = meeting!!.suggestedAlternatives.mapIndexed { i, alt ->
                                                                if (i == index) alt.copy(isSelected = true) else alt.copy(isSelected = false)
                                                            }
                                                            
                                                            meeting = meeting!!.copy(
                                                                proposedDate = alternative.date,
                                                                proposedTime = alternative.time,
                                                                suggestedAlternatives = updatedAlternatives,
                                                                status = MeetingStatus.PENDING
                                                            )
                                                            
                                                            scope.launch {
                                                                snackbarHostState.showSnackbar("New meeting time selected")
                                                            }
                                                        },
                                                        onError = { exception ->
                                                            scope.launch {
                                                                snackbarHostState.showSnackbar("Failed to select time: ${exception.message}")
                                                            }
                                                        }
                                                    )
                                                }
                                            ) {
                                                Text("Select")
                                            }
                                        } else if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Meeting details card
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title
                        Text(
                            text = meeting!!.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Participants
                        Column {
                            Text(
                                text = "Participants",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Requester
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = meeting!!.requesterName,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "${meeting!!.requesterRole.capitalize()} (Requester)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Recipient
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = meeting!!.recipientName,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "${meeting!!.recipientRole.capitalize()} (Recipient)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                        
                        Divider()
                        
                        // Date and Time
                        Column {
                            Text(
                                text = "Schedule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Date: ${meeting!!.proposedDate}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Time: ${meeting!!.proposedTime}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Location: ${meeting!!.location}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        
                        if (meeting!!.description.isNotBlank()) {
                            Divider()
                            
                            // Description
                            Column {
                                Text(
                                    text = "Description",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = meeting!!.description,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
                
                // Buttons section
                if (meeting!!.status == MeetingStatus.PENDING && meeting!!.recipientId == currentUser?.uid) {
                    // Recipient actions for pending meetings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showConfirmationDialog = "reject" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Reject")
                        }
                        
                        OutlinedButton(
                            onClick = { showAlternateTimeDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Suggest Time")
                        }
                        
                        Button(
                            onClick = { showConfirmationDialog = "accept" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Accept")
                        }
                    }
                } else if (meeting!!.status == MeetingStatus.ACCEPTED) {
                    // Actions for accepted meetings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (meeting!!.requesterId == currentUser?.uid || meeting!!.recipientId == currentUser?.uid) {
                            OutlinedButton(
                                onClick = { showConfirmationDialog = "cancel" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Cancel")
                            }
                            
                            Button(
                                onClick = { showConfirmationDialog = "complete" },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Complete")
                            }
                        }
                    }
                } else if (meeting!!.status == MeetingStatus.RESCHEDULED) {
                    // Actions for rescheduled meetings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // If you're the requester and suggested times were given
                        if (meeting!!.requesterId == currentUser?.uid) {
                            if (meeting!!.suggestedAlternatives.any { !it.isSelected && it.suggestedBy != currentUser?.uid }) {
                                Button(
                                    onClick = { 
                                        // Scroll to alternatives section
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Select a Suggested Time")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { showAlternateTimeDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Suggest Time")
                                }
                                
                                OutlinedButton(
                                    onClick = { showConfirmationDialog = "cancel" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Cancel")
                                }
                            }
                        } else {
                            // If you're the recipient
                            OutlinedButton(
                                onClick = { showAlternateTimeDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Suggest Time")
                            }
                            
                            OutlinedButton(
                                onClick = { showConfirmationDialog = "cancel" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                } else if (meeting!!.status == MeetingStatus.PENDING && meeting!!.requesterId == currentUser?.uid) {
                    // Requester actions for pending meetings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAlternateTimeDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Suggest Time")
                        }
                        
                        OutlinedButton(
                            onClick = { showConfirmationDialog = "cancel" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Cancel Request")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun getStatusColor(status: MeetingStatus): Color {
    return when (status) {
        MeetingStatus.PENDING -> MaterialTheme.colorScheme.tertiary
        MeetingStatus.ACCEPTED -> MaterialTheme.colorScheme.primary
        MeetingStatus.REJECTED -> MaterialTheme.colorScheme.error
        MeetingStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
        MeetingStatus.CANCELLED -> Color.Gray
        MeetingStatus.RESCHEDULED -> MaterialTheme.colorScheme.tertiary
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
} 