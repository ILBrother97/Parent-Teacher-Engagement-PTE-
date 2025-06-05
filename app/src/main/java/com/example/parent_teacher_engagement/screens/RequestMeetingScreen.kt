package com.example.parent_teacher_engagement.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.parent_teacher_engagement.firebase.FirebaseService
import com.example.parent_teacher_engagement.model.MeetingRequest
import com.example.parent_teacher_engagement.model.MeetingStatus
import com.example.parent_teacher_engagement.model.TodoItem
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestMeetingScreen(
    navController: NavController
) {
    val firebaseService = remember { FirebaseService.getInstance() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Current user info
    var currentUser by remember { mutableStateOf<FirebaseService.UserData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var recipients by remember { mutableStateOf<List<FirebaseService.UserListData>>(emptyList()) }
    
    // Get current user data
    LaunchedEffect(Unit) {
        firebaseService.getCurrentUserData { userData ->
            currentUser = userData
            if (userData != null) {
                // Load potential recipients based on current user role
                firebaseService.getPotentialMeetingParticipants(
                    userRole = userData.role ?: "",
                    onSuccess = { users ->
                        recipients = users
                        isLoading = false
                    },
                    onError = { exception ->
                        // Handle error
                        scope.launch {
                            snackbarHostState.showSnackbar("Failed to load recipients: ${exception.message}")
                        }
                        isLoading = false
                    }
                )
            } else {
                isLoading = false
            }
        }
    }
    
    // Form state
    var selectedRecipient by remember { mutableStateOf<FirebaseService.UserListData?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var meetingDate by remember { mutableStateOf(LocalDate.now()) }
    var meetingTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var location by remember { mutableStateOf("At School (In Person)") }
    var relatedStudentId by remember { mutableStateOf<String?>(null) }
    
    // State for date and time pickers
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    // State for recipient selection dialog
    var showRecipientPicker by remember { mutableStateOf(false) }
    
    // State for location dropdown
    var expandedLocationMenu by remember { mutableStateOf(false) }
    val locationOptions = listOf("At School (In Person)", "Online (By Text on PTE)")
    
    // State for submission
    var isSubmitting by remember { mutableStateOf(false) }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = meetingDate.toEpochDay() * 24 * 60 * 60 * 1000,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Convert the selected date to start of day for comparison
                    val selectedDate = Calendar.getInstance().apply {
                        timeInMillis = utcTimeMillis
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    
                    // Allow today and future dates
                    return selectedDate >= today
                }
            }
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Convert milliseconds to LocalDate
                        val selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                        meetingDate = selectedDate
                    }
                    showDatePicker = false
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
    
    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = meetingTime.hour,
            initialMinute = meetingTime.minute
        )
        
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
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
                            meetingTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
    
    // Recipient Selection Dialog
    if (showRecipientPicker) {
        AlertDialog(
            onDismissRequest = { showRecipientPicker = false },
            title = { Text("Select Recipient") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (recipients.isEmpty()) {
                        Text("No recipients available")
                    } else {
                        recipients.forEach { user ->
                            ListItem(
                                headlineContent = { Text(user.name ?: "Unknown") },
                                supportingContent = { 
                                    Text(
                                        "${user.role?.capitalize() ?: ""} | ${user.email ?: ""}"
                                    ) 
                                },
                                modifier = Modifier.clickable {
                                    selectedRecipient = user
                                    showRecipientPicker = false
                                }
                            )
                            Divider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecipientPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Meeting") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Recipient Selection
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showRecipientPicker = true }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Meeting With",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (selectedRecipient != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = selectedRecipient?.name ?: "",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${selectedRecipient?.role?.capitalize() ?: ""} | ${selectedRecipient?.email ?: ""}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Tap to select a recipient",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                
                // Meeting Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Meeting Title") },
                    placeholder = { Text("e.g., Parent-Teacher Conference") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                    singleLine = true
                )
                
                // Meeting Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Briefly describe the purpose of the meeting...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    minLines = 3
                )
                
                // Date and Time Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date Picker
                    OutlinedTextField(
                        value = meetingDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        onValueChange = { },
                        label = { Text("Date") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Select Date")
                            }
                        }
                    )
                    
                    // Time Picker
                    OutlinedTextField(
                        value = meetingTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        onValueChange = { },
                        label = { Text("Time") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showTimePicker = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Select Time")
                            }
                        }
                    )
                }
                
                // Location
                ExposedDropdownMenuBox(
                    expanded = expandedLocationMenu,
                    onExpandedChange = { expandedLocationMenu = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Location") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLocationMenu) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedLocationMenu,
                        onDismissRequest = { expandedLocationMenu = false }
                    ) {
                        locationOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    location = option
                                    expandedLocationMenu = false
                                }
                            )
                        }
                    }
                }
                
                // Submit Button
                Button(
                    onClick = {
                        if (selectedRecipient == null) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please select a recipient")
                            }
                            return@Button
                        }
                        
                        if (title.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please enter a meeting title")
                            }
                            return@Button
                        }
                        
                        isSubmitting = true
                        
                        val meetingRequest = MeetingRequest(
                            requesterId = currentUser?.uid ?: "",
                            requesterName = currentUser?.name ?: "",
                            requesterRole = currentUser?.role ?: "",
                            recipientId = selectedRecipient?.id ?: "",
                            recipientName = selectedRecipient?.name ?: "",
                            recipientRole = selectedRecipient?.role ?: "",
                            title = title,
                            description = description,
                            proposedDate = meetingDate.format(DateTimeFormatter.ISO_DATE),
                            proposedTime = meetingTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            status = MeetingStatus.PENDING,
                            location = location,
                            relatedToStudentId = relatedStudentId
                        )
                        
                        firebaseService.createMeetingRequest(
                            meetingRequest = meetingRequest,
                            onSuccess = {
                                // Create a Todo item for the meeting
                                val todoItem = TodoItem(
                                    id = UUID.randomUUID().toString(),
                                    title = "Meeting: $title",
                                    description = "Meeting with ${selectedRecipient?.name}: $description",
                                    dueDate = meetingDate.format(DateTimeFormatter.ISO_DATE),
                                    dueTime = meetingTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                    priority = TodoItem.Priority.HIGH,
                                    category = "Meetings",
                                    isCompleted = false
                                )
                                
                                // Add success message
                                scope.launch {
                                    snackbarHostState.showSnackbar("Meeting request sent successfully")
                                    // Navigate back
                                    navController.popBackStack()
                                }
                                
                                isSubmitting = false
                            },
                            onError = { exception ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("Failed to send meeting request: ${exception.message}")
                                }
                                isSubmitting = false
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Send Meeting Request")
                }
            }
        }
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
} 