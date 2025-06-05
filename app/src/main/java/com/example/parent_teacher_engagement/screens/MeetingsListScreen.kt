package com.example.parent_teacher_engagement.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.parent_teacher_engagement.firebase.FirebaseService
import com.example.parent_teacher_engagement.model.MeetingRequest
import com.example.parent_teacher_engagement.model.MeetingStatus
import com.example.parent_teacher_engagement.navigation.Screen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingsListScreen(
    navController: NavController
) {
    val firebaseService = remember { FirebaseService.getInstance() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // State
    var currentUser by remember { mutableStateOf<FirebaseService.UserData?>(null) }
    var allMeetings by remember { mutableStateOf<List<MeetingRequest>>(emptyList()) }
    var filteredMeetings by remember { mutableStateOf<List<MeetingRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Filter state
    val tabs = listOf("All", "Pending", "Accepted", "Completed", "Sent", "Received")
    var selectedTab by remember { mutableStateOf(0) }
    
    // Add search functionality
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    
    // Function to load meetings
    fun loadMeetings() {
        if (currentUser != null) {
            isRefreshing = true
            firebaseService.getAllUserMeetings(
                userId = currentUser!!.uid,
                onSuccess = { meetings ->
                    allMeetings = meetings
                    filteredMeetings = meetings
                    isRefreshing = false
                    isLoading = false
                },
                onError = { exception ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Failed to load meetings: ${exception.message}")
                    }
                    isRefreshing = false
                    isLoading = false
                }
            )
        }
    }
    
    // Get current user and meetings
    LaunchedEffect(Unit) {
        firebaseService.getCurrentUserData { userData ->
            currentUser = userData
            if (userData != null) {
                loadMeetings()
            } else {
                isLoading = false
            }
        }
    }
    
    // Filter meetings when tab changes or search query changes
    LaunchedEffect(selectedTab, allMeetings, searchQuery) {
        // First filter by status tab
        val statusFiltered = when (selectedTab) {
            0 -> allMeetings // All
            1 -> allMeetings.filter { it.status == MeetingStatus.PENDING } // Pending
            2 -> allMeetings.filter { it.status == MeetingStatus.ACCEPTED } // Accepted
            3 -> allMeetings.filter { it.status == MeetingStatus.COMPLETED } // Completed
            4 -> allMeetings.filter { it.requesterId == currentUser?.uid } // Sent
            5 -> allMeetings.filter { it.recipientId == currentUser?.uid } // Received
            else -> allMeetings
        }
        
        // Then filter by search query if not empty
        filteredMeetings = if (searchQuery.isEmpty()) {
            statusFiltered
        } else {
            statusFiltered.filter { meeting ->
                meeting.title.contains(searchQuery, ignoreCase = true) ||
                meeting.description.contains(searchQuery, ignoreCase = true) ||
                meeting.location.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("My Meetings") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { searchActive = !searchActive }) {
                            Icon(
                                if (searchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (searchActive) "Close Search" else "Search Meetings"
                            )
                        }
                        IconButton(
                            onClick = { loadMeetings() },
                            enabled = !isRefreshing
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh meetings")
                            }
                        }
                        IconButton(onClick = {
                            navController.navigate(Screen.RequestMeeting.route)
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Request new meeting")
                        }
                    }
                )
                
                // Search bar appears when search is active
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
                        placeholder = { Text("Search meetings...") },
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs for filtering
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Meetings list
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredMeetings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty())
                            "No meetings matching \"$searchQuery\""
                        else
                            "No meetings found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMeetings) { meeting ->
                        MeetingItem(
                            meeting = meeting,
                            currentUserId = currentUser?.uid ?: "",
                            onClick = {
                                // Navigate to meeting details
                                navController.navigate(Screen.MeetingDetails.createRoute(meeting.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MeetingItem(
    meeting: MeetingRequest,
    currentUserId: String,
    onClick: () -> Unit
) {
    val isSentByMe = meeting.requesterId == currentUserId
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title row with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = meeting.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                StatusBadge(status = meeting.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Date and time
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "${meeting.proposedDate} at ${meeting.proposedTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Person information
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSentByMe) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isSentByMe) {
                        "To: ${meeting.recipientName} (${meeting.recipientRole.capitalize()})"
                    } else {
                        "From: ${meeting.requesterName} (${meeting.requesterRole.capitalize()})"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            
            if (meeting.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = meeting.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: MeetingStatus) {
    val (color, text) = when (status) {
        MeetingStatus.PENDING -> Pair(MaterialTheme.colorScheme.tertiary, "Pending")
        MeetingStatus.ACCEPTED -> Pair(MaterialTheme.colorScheme.primary, "Accepted")
        MeetingStatus.REJECTED -> Pair(MaterialTheme.colorScheme.error, "Rejected")
        MeetingStatus.COMPLETED -> Pair(MaterialTheme.colorScheme.secondary, "Completed")
        MeetingStatus.CANCELLED -> Pair(Color.Gray, "Cancelled")
        MeetingStatus.RESCHEDULED -> Pair(MaterialTheme.colorScheme.tertiary, "Rescheduled")
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
} 