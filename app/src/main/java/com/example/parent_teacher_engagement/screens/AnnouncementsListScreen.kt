package com.example.parent_teacher_engagement.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.parent_teacher_engagement.firebase.FirebaseService
import com.example.parent_teacher_engagement.navigation.Screen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsListScreen(
    navController: NavController
) {
    val firebaseService = remember { FirebaseService.getInstance() }
    var events by remember { mutableStateOf<List<FirebaseService.EventData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Dialog states
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<FirebaseService.EventData?>(null) }
    
    // Edit states
    var editTitle by remember { mutableStateOf("") }
    var editDate by remember { mutableStateOf("") }
    var editTime by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    
    // Add event states
    var newTitle by remember { mutableStateOf("") }
    var newDate by remember { mutableStateOf("") }
    var newTime by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Load events when screen is displayed
    LaunchedEffect(Unit) {
        loadEvents(firebaseService) { loadedEvents ->
            events = loadedEvents
            isLoading = false
        }
    }
    
    // Function to load events
    fun refreshEvents() {
        isLoading = true
        loadEvents(firebaseService) { loadedEvents ->
            events = loadedEvents
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Announcements") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    // Clear add event form fields
                    newTitle = ""
                    newDate = ""
                    newTime = ""
                    newDescription = ""
                    showAddEventDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Event",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (events.isEmpty()) {
                Text(
                    text = "No announcements found",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(events) { event ->
                        EventCard(
                            event = event,
                            onEdit = {
                                selectedEvent = event
                                editTitle = event.title ?: ""
                                editDate = event.date ?: ""
                                editTime = event.time ?: ""
                                editDescription = event.description ?: ""
                                showEditDialog = true
                            },
                            onDelete = {
                                selectedEvent = event
                                showDeleteDialog = true
                            },
                            onClick = {
                                navController.navigate(Screen.EventDetails.createRoute(event.id ?: ""))
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Edit Event Dialog
    if (showEditDialog && selectedEvent != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Announcement") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDate,
                        onValueChange = { editDate = it },
                        label = { Text("Date (MM/DD/YYYY)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTime,
                        onValueChange = { editTime = it },
                        label = { Text("Time") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val eventId = selectedEvent?.id
                        if (eventId != null) {
                            firebaseService.updateEvent(
                                eventId = eventId,
                                title = editTitle,
                                date = editDate,
                                time = editTime,
                                description = editDescription
                            ) { success ->
                                if (success) {
                                    showEditDialog = false
                                    refreshEvents()
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Announcement updated successfully")
                                    }
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Failed to update announcement")
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text("Save Changes")
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
    if (showDeleteDialog && selectedEvent != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Announcement") },
            text = { Text("Are you sure you want to delete this announcement?") },
            confirmButton = {
                Button(
                    onClick = {
                        val eventId = selectedEvent?.id
                        if (eventId != null) {
                            firebaseService.deleteEvent(eventId) { success ->
                                if (success) {
                                    showDeleteDialog = false
                                    refreshEvents()
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Announcement deleted successfully")
                                    }
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Failed to delete announcement")
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Add Event Dialog
    if (showAddEventDialog) {
        AlertDialog(
            onDismissRequest = { showAddEventDialog = false },
            title = { Text("Add New Announcement") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDate,
                        onValueChange = { newDate = it },
                        label = { Text("Date (MM/DD/YYYY)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTime,
                        onValueChange = { newTime = it },
                        label = { Text("Time") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Parse date string to get timestamp
                        val dateTimestamp = try {
                            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                            dateFormat.parse(newDate)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                        
                        firebaseService.postEvent(
                            date = dateTimestamp,
                            title = newTitle,
                            time = newTime,
                            description = newDescription,
                            onSuccess = {
                                showAddEventDialog = false
                                refreshEvents()
                                scope.launch {
                                    snackbarHostState.showSnackbar("Announcement added successfully")
                                }
                            },
                            onError = { error ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("Failed to add: ${error.localizedMessage}")
                                }
                            }
                        )
                    },
                    enabled = newTitle.isNotBlank() && newDate.isNotBlank() && newTime.isNotBlank()
                ) {
                    Text("Add Announcement")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEventDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventCard(
    event: FirebaseService.EventData,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title ?: "Untitled Event",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = event.date ?: "No date",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = event.time ?: "No time",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = event.description ?: "No description",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun loadEvents(
    firebaseService: FirebaseService,
    onEventsLoaded: (List<FirebaseService.EventData>) -> Unit
) {
    firebaseService.getUpcomingEvents { events ->
        // Sort events by date
        val sortedEvents = events.sortedByDescending { 
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it.date ?: "")?.time
            } catch (e: Exception) {
                0L
            }
        }
        onEventsLoaded(sortedEvents)
    }
} 