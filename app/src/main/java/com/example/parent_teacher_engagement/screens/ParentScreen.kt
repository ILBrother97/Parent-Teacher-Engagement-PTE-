package com.example.parent_teacher_engagement.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Announcement
import android.content.Context
import android.content.res.Configuration
import android.app.Activity
import java.util.Locale
import java.util.Calendar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.parent_teacher_engagement.R
import com.example.parent_teacher_engagement.utils.Language
import com.example.parent_teacher_engagement.components.ParentTopBar
import com.example.parent_teacher_engagement.components.NotificationBell
import com.example.parent_teacher_engagement.firebase.FirebaseService
import com.example.parent_teacher_engagement.navigation.Screen
import androidx.navigation.NavController
import com.example.parent_teacher_engagement.service.NotificationService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import androidx.compose.foundation.clickable
import com.example.parent_teacher_engagement.LocalNavController
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import java.util.Date
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentScreen(
    onLogout: () -> Unit = {},
    navController: NavController
) {
    // Language selection state
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    // Function to change app language with better update mechanism
    fun setLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        
        // Force recreation of the activity to ensure all UI components are updated
        (context as? Activity)?.recreate()
    }
    var selectedTab by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val firebaseService = remember { FirebaseService.getInstance() }
    
    // Shared state for events
    var hasNewEvents by remember { mutableStateOf(false) }
    var eventCount by remember { mutableStateOf(0) }
    var previousEventCount by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val notificationService = remember { NotificationService(context) }
    
    // Listen for real-time event updates
    LaunchedEffect(Unit) {
        firebaseService.listenToEventCount { count ->
            if (count > previousEventCount && previousEventCount > 0) {
                // Show system notification when count increases
                notificationService.showNewEventNotification()
                hasNewEvents = true
            }
            previousEventCount = count  // Update previous count after checking for increase
            eventCount = count  // Update the displayed count
        }
    }
    
    // Dialog states
    var showChildSelection by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPeriodPicker by remember { mutableStateOf(false) }
    var showAttendanceStatus by remember { mutableStateOf(false) }
    var showSemesterPicker by remember { mutableStateOf(false) }
    var showGrades by remember { mutableStateOf(false) }
    var showAssessmentDetails by remember { mutableStateOf(false) }
    var showEventsDialog by remember { mutableStateOf(false) }
    var upcomingEvents by remember { mutableStateOf<List<FirebaseService.EventData>>(emptyList()) }
    
    // Progress tracker dialog states
    var showProgressSemesterPicker by remember { mutableStateOf(false) }
    var showProgressChildSelection by remember { mutableStateOf(false) }
    var showProgressData by remember { mutableStateOf(false) }
    var showProgressAssessmentDetails by remember { mutableStateOf(false) }
    
    // Selection states
    var selectedChild by remember { mutableStateOf<FirebaseService.StudentData?>(null) }
    var selectedSemester by remember { mutableStateOf<String?>(null) }
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var studentGrades by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var assessmentDetails by remember { mutableStateOf<List<FirebaseService.AssessmentDetail>>(emptyList()) }
    
    // Progress tracker selection states
    var selectedProgressChild by remember { mutableStateOf<FirebaseService.StudentData?>(null) }
    var selectedProgressSemester by remember { mutableStateOf<String?>(null) }
    var selectedProgressSubject by remember { mutableStateOf<String?>(null) }
    var progressData by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var progressAssessmentDetails by remember { mutableStateOf<List<FirebaseService.AssessmentDetail>>(emptyList()) }
    
    // State for children data
    var children by remember { mutableStateOf<List<FirebaseService.StudentData>>(emptyList()) }
    var childrenStats by remember { mutableStateOf<Map<String, Map<String, Any>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // State for unread messages
    var unreadMessagesCount by remember { mutableStateOf(0) }
    
    // Load children data when screen is first shown
    LaunchedEffect(Unit) {
        val currentUser = firebaseService.currentUserData
        if (currentUser != null) {
            // Get unread messages count
            firebaseService.getUnreadMessagesCount(currentUser.uid) { count ->
                unreadMessagesCount = count
            }
            
            firebaseService.getChildrenForParent(currentUser.uid) { studentList ->
                children = studentList
                isLoading = false
                // Get stats for each child
                studentList.forEach { student ->
                    firebaseService.getStudentStats(student.id) { stats ->
                        childrenStats = childrenStats + (student.id to stats)
                    }
                }
            }
        } else {
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            ParentTopBar(
                title = stringResource(R.string.parent_dashboard),
                onMenuClick = { action ->
                    when (action) {
                        "Language" -> {
                            showLanguageDialog = true
                        }
                        "ViewGrades" -> {
                            showSemesterPicker = true
                        }
                        "ContactTeacher" -> {
                            navController.navigate(Screen.Messaging.route)
                        }
                        "ChangePassword" -> {
                            navController.navigate(Screen.ChangePassword.route)
                        }
                        "ViewEvents" -> {
                            firebaseService.getUpcomingEvents { eventsList ->
                                upcomingEvents = eventsList
                                showEventsDialog = true
                                hasNewEvents = false  // Clear new events flag
                            }
                        }
                        "ViewAttendance" -> {
                            // Change from showing snackbar to showing child selection for attendance
                            showChildSelection = true
                            selectedSemester = null // Clear selectedSemester to indicate attendance flow
                        }
                        else -> {
                            scope.launch {
                                snackbarHostState.showSnackbar("$action - Coming Soon")
                            }
                        }
                    }
                },
                onLogoutClick = onLogout,
                notificationBell = {
                    NotificationBell(
                        unreadCount = unreadMessagesCount,
                        onClick = {
                            // Simply navigate to messaging screen without setting currentChatPartnerId
                            Log.d("ParentScreen", "Notification bell clicked, navigating to messaging screen")
                            navController.navigate(Screen.Messaging.route)
                        }
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = stringResource(R.string.profile)) },
                    label = { Text(stringResource(R.string.profile)) },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.functions)) },
                    label = { Text(stringResource(R.string.functions)) },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    navController.navigate(Screen.Todo.route)
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "My ToDo",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                text = { Text(stringResource(R.string.my_todo)) },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            // Language selector in top corner
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                
            }

            when (selectedTab) {
                0 -> {
                    // Profile Tab
                    ProfileTab(
                        isLoading = isLoading,
                        children = children,
                        childrenStats = childrenStats,
                        eventCount = eventCount,
                        hasNewEvents = hasNewEvents,
                        onViewEvents = {
                            firebaseService.getUpcomingEvents { eventsList ->
                                upcomingEvents = eventsList
                                showEventsDialog = true
                                hasNewEvents = false  // Clear new events flag
                            }
                        }
                    )
                }
                1 -> {
                    // Functions Tab
                    ParentFunctionsTab(
                        onShowSemesterPicker = {
                            showSemesterPicker = true
                        },
                        onShowChildSelection = {
                            showChildSelection = true
                            selectedSemester = null // Clear selectedSemester to indicate attendance flow
                        },
                        onShowProgressSemesterPicker = {
                            showProgressSemesterPicker = true
                        },
                        onViewEvents = {
                            firebaseService.getUpcomingEvents { eventsList ->
                                upcomingEvents = eventsList
                                showEventsDialog = true
                            }
                        },
                        navController = navController
                    )
                }
            }

            // Semester Selection Dialog
            if (showSemesterPicker) {
                AlertDialog(
                    onDismissRequest = { showSemesterPicker = false },
                    title = { Text(stringResource(R.string.select_semester)) },
                    text = {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    selectedSemester = "Semester One"
                                    showSemesterPicker = false
                                    showChildSelection = true // Next, select a child
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.semester_one))
                            }
                            TextButton(
                                onClick = {
                                    selectedSemester = "Semester Two"
                                    showSemesterPicker = false
                                    showChildSelection = true // Next, select a child
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.semester_two))
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showSemesterPicker = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            // Child Selection Dialog
            if (showChildSelection) {
                AlertDialog(
                    onDismissRequest = { showChildSelection = false },
                    title = { Text(stringResource(R.string.select_child)) },
                    text = {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (children.isEmpty()) {
                                Text(stringResource(R.string.no_children_found))
                            } else {
                                // Capture the string resource at Composable level
                                val notSubmittedText = stringResource(R.string.not_submitted)
                                children.forEach { child ->
                                    Button(
                                        onClick = {
                                            selectedChild = child
                                            if (selectedSemester != null) {
                                                // If semester is selected, we're viewing grades
                                                showChildSelection = false
                                                showGrades = true
                                                // Load grades using the same subjects as the topbar "View Grades" functionality
                                                val subjects = listOf("English", "Civic", "Economics", "History", "Physics", 
                                                    "Chemistry", "Geography", "It", "Math", "Biology", "Oromo", "Amharic", "Sport")
                                                studentGrades = emptyMap() // Clear previous grades
                                                subjects.forEach { subject ->
                                                    firebaseService.getGrade(
                                                        studentId = child.id,
                                                        semester = selectedSemester ?: "",
                                                        subject = subject
                                                    ) { grade ->
                                                        studentGrades = studentGrades + (subject to (grade ?: notSubmittedText))
                                                    }
                                                }
                                            } else {
                                                // For attendance view, navigate to the attendance screen
                                                showChildSelection = false
                                                navController.navigate(Screen.AttendanceView.createRoute(child.id))
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(child.name)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showChildSelection = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            // Assessment Details Dialog
            if (showAssessmentDetails) {
                AlertDialog(
                    onDismissRequest = { showAssessmentDetails = false },
                    title = { Text(stringResource(R.string.subject_assessment_details, selectedSubject ?: "")) },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            assessmentDetails.forEach { detail ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                                text = detail.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = stringResource(R.string.max_points, detail.maxPoints),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${detail.score}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showAssessmentDetails = false }) {
                            Text(stringResource(R.string.close))
                        }
                    }
                )
            }

            // Events Dialog
            if (showEventsDialog) {
                AlertDialog(
                    onDismissRequest = { showEventsDialog = false },
                    title = { Text(stringResource(R.string.view_events)) },
                    text = {
                        if (upcomingEvents.isEmpty()) {
                            Text(stringResource(R.string.no_events_found))
                        } else {
                            Column {
                                upcomingEvents.forEach { event ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                showEventsDialog = false
                                                navController.navigate("event_details/${event.id}")
                                            },
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                text = event.title ?: stringResource(R.string.untitled_event),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = stringResource(R.string.event_date, event.date ?: ""),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = stringResource(R.string.event_time, event.time ?: ""),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showEventsDialog = false }) {
                            Text(stringResource(R.string.close))
                        }
                    }
                )
            }

            // Language Selection Dialog
            if (showLanguageDialog) {
        val context = LocalContext.current
        val activity = context as? android.app.Activity
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.select_language)) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                                    setLocale(context, Language.ENGLISH.code)
                            showLanguageDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.english))
                    }
                    TextButton(
                        onClick = {
                                    setLocale(context, Language.AMHARIC.code)
                            showLanguageDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.amharic))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showGrades) {
                // Capture string resource here at Composable level
                val notSubmittedText = stringResource(R.string.not_submitted)
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.grades_for, selectedChild?.name ?: ""),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            IconButton(onClick = { 
                                showGrades = false
                                selectedChild = null
                                selectedSemester = null
                                studentGrades = emptyMap()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                            }
                        }
                        
                        Text(
                            "$selectedSemester",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(studentGrades.toList()) { (subject, grade) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(subject)
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                grade,
                                                color = if (grade == notSubmittedText) 
                                                    MaterialTheme.colorScheme.error 
                                                else 
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                            Button(
                                                onClick = {
                                                    selectedSubject = subject
                                                    Log.d("ParentScreen", "Fetching assessment details for student: ${selectedChild?.id}, semester: $selectedSemester, subject: $subject")
                                                    firebaseService.getAssessmentDetails(
                                                        studentId = selectedChild?.id ?: "",
                                                        semester = selectedSemester ?: "",
                                                        subject = subject
                                                    ) { details ->
                                                        Log.d("ParentScreen", "Received assessment details: $details")
                                                        assessmentDetails = details
                                                        showAssessmentDetails = true
                                                    }
                                                },
                                                modifier = Modifier.height(32.dp).width(80.dp)
                                            ) {
                                                Text(stringResource(R.string.detail), fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Progress Semester Selection Dialog
    if (showProgressSemesterPicker) {
        AlertDialog(
            onDismissRequest = { showProgressSemesterPicker = false },
            title = { Text(stringResource(R.string.select_semester_for_progress)) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            selectedProgressSemester = "Semester One"
                            showProgressSemesterPicker = false
                            showProgressChildSelection = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.semester_one))
                    }
                    TextButton(
                        onClick = {
                            selectedProgressSemester = "Semester Two"
                            showProgressSemesterPicker = false
                            showProgressChildSelection = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.semester_two))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showProgressSemesterPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Progress Child Selection Dialog
    if (showProgressChildSelection) {
        // Capture the string resource at Composable level
        val notSubmittedText = stringResource(R.string.not_submitted)
        
        AlertDialog(
            onDismissRequest = { showProgressChildSelection = false },
            title = { Text(stringResource(R.string.select_child_for_progress)) },
            text = {
                Column {
                    children.forEach { child ->
                        Button(
                            onClick = {
                                selectedProgressChild = child
                                showProgressChildSelection = false
                                showProgressData = true
                                
                                // Load grades using the same subjects as regular grades
                                val subjects = listOf("English", "Civic", "Economics", "History", "Physics", 
                                    "Chemistry", "Geography", "It", "Math", "Biology", "Oromo", "Amharic", "Sport")
                                progressData = emptyMap() // Clear previous progress data
                                subjects.forEach { subject ->
                                    firebaseService.getProgressData(
                                        studentId = child.id,
                                        semester = selectedProgressSemester ?: "",
                                        subject = subject
                                    ) { progress ->
                                        progressData = progressData + (subject to (progress ?: notSubmittedText))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(child.name ?: "Unknown Student")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { 
                        showProgressChildSelection = false
                        selectedProgressSemester = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Progress Data Display Screen
    if (showProgressData) {
        AlertDialog(
            onDismissRequest = {
                showProgressData = false
                selectedProgressChild = null
                selectedProgressSemester = null
            },
            title = {
                Text(
                    text = "${selectedProgressChild?.name}'s Progress - ${selectedProgressSemester}",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(progressData.toList()) { (subject, progress) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = subject,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(R.string.progress_colon, progress),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                
                                // View details button
                                Button(
                                    onClick = {
                                        selectedProgressSubject = subject
                                        firebaseService.getProgressAssessmentDetails(
                                            studentId = selectedProgressChild?.id ?: "",
                                            semester = selectedProgressSemester ?: "",
                                            subject = subject
                                        ) { details ->
                                            progressAssessmentDetails = details
                                            showProgressAssessmentDetails = true
                                        }
                                    },
                                    modifier = Modifier.height(32.dp).width(80.dp)
                                ) {
                                    Text(stringResource(R.string.detail), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showProgressData = false
                        selectedProgressChild = null
                        selectedProgressSemester = null
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        )
    }

    // Progress Assessment Details Dialog
    if (showProgressAssessmentDetails) {
        AlertDialog(
            onDismissRequest = { showProgressAssessmentDetails = false },
            title = {
                Text(
                    text = stringResource(R.string.progress_assessment_details, selectedProgressSubject ?: ""),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                if (progressAssessmentDetails.isEmpty()) {
                    Text(stringResource(R.string.no_progress_details))
                } else {
                    LazyColumn {
                        items(progressAssessmentDetails.size) { index ->
                            val assessment = progressAssessmentDetails[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
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
                                        text = assessment.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(R.string.max_points, assessment.maxPoints),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        
                                        Text(
                                            text = "Score: ${assessment.score}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (assessment.score >= assessment.maxPoints * 0.7) 
                                                Color.Green else Color.Red
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProgressAssessmentDetails = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun ProfileTab(
    isLoading: Boolean,
    children: List<FirebaseService.StudentData>,
    childrenStats: Map<String, Map<String, Any>>,
    eventCount: Int,
    hasNewEvents: Boolean,
    onViewEvents: (() -> Unit)
) {
    // Add state for parent dashboard stats
    val firebaseService = FirebaseService.getInstance()
    val currentUser = firebaseService.currentUserData
    var dashboardStats by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    var recentActivities by remember { mutableStateOf<List<FirebaseService.ActivityData>>(emptyList()) }
    var calendarEvents by remember { mutableStateOf<Map<Int, List<FirebaseService.EventData>>>(emptyMap()) }
    var isLoadingData by remember { mutableStateOf(true) }
    
    // Current month for calendar
    val currentDate = remember { java.util.Calendar.getInstance() }
    val currentYear = remember { currentDate.get(java.util.Calendar.YEAR) }
    val currentMonth = remember { currentDate.get(java.util.Calendar.MONTH) }
    
    // Load dashboard data
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { parentId ->
            isLoadingData = true
            
            // Load dashboard stats
            firebaseService.getParentDashboardStats(parentId) { stats ->
                dashboardStats = stats
                
                // Load recent activities
                firebaseService.getRecentActivities(parentId, 5) { activities ->
                    recentActivities = activities
                    
                    // Load calendar events
                    firebaseService.getCalendarEventsForMonth(currentYear, currentMonth) { events ->
                        calendarEvents = events
                        isLoadingData = false
                    }
                }
            }
        } ?: run {
            isLoadingData = false
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading || isLoadingData -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            children.isEmpty() -> EmptyStateMessage(stringResource(R.string.no_children_found))
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Welcome Section
                    item {
                        WelcomeSection(children.size)
                    }
                    
                    // 2. Quick Stats Summary
                    item {
                        QuickStatsSummary(
                            children = children, 
                            childrenStats = childrenStats, 
                            eventCount = eventCount,
                            dashboardStats = dashboardStats
                        )
                    }
                    
                    // Children Section Header
                    item {
                        Text(
                            text = stringResource(R.string.my_children),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                    }
                    
                    items(children) { child ->
                        val stats = childrenStats[child.id] ?: emptyMap()
                        ChildOverviewCard(
                            name = child.name,
                            grade = child.grade,
                            attendance = stats["attendance"]?.toString() ?: "N/A",
                            onClick = { /* Navigate to child detail screen */ }
                        )
                    }
                    
                    // 4. Recent Activity Timeline with real data
                    item {
                        RecentActivityTimeline(recentActivities)
                    }
                    
                    // 5. Calendar Widget with real events
                    item {
                        CalendarWidget(calendarEvents)
                    }

                    // Recent Events Card with Badge
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onViewEvents()
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = stringResource(R.string.view_events),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    
                                    if (hasNewEvents) {
                                        Badge(
                                            modifier = Modifier.offset(x = 8.dp, y = (-4).dp),
                                            containerColor = MaterialTheme.colorScheme.error
                                        ) {
                                            Text(stringResource(R.string.new_tag))
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.recent_events),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = eventCount.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// 1. Welcome Section Composable
@Composable
private fun WelcomeSection(childCount: Int) {
    val currentTime = remember { System.currentTimeMillis() }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val formattedDate = remember { dateFormat.format(currentTime) }
    val firebaseService = remember { FirebaseService.getInstance() }
    val userName = remember { mutableStateOf("") }
    
    // Fetch current user's name
    LaunchedEffect(Unit) {
            val currentUser = firebaseService.currentUserData
        userName.value = currentUser?.name ?: ""
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
        modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
            Text(
                        text = stringResource(R.string.welcome_message, userName.value),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.dashboard_summary, childCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// 2. Quick Stats Summary
@Composable
private fun QuickStatsSummary(
    children: List<FirebaseService.StudentData>,
    childrenStats: Map<String, Map<String, Any>>,
    eventCount: Int,
    dashboardStats: Map<String, Any> = emptyMap()
) {
    // Calculate average attendance
    val averageAttendance = dashboardStats["averageAttendance"]?.toString() ?: run {
        if (children.isEmpty()) "N/A" else {
            val attendanceValues = children.mapNotNull { child ->
                val stats = childrenStats[child.id] ?: return@mapNotNull null
                val attendance = stats["attendance"]?.toString() ?: return@mapNotNull null
                try {
                    attendance.toDouble()
                } catch (e: Exception) {
                    null
                }
            }
            
            if (attendanceValues.isEmpty()) "N/A" else {
                String.format("%.1f%%", attendanceValues.average())
            }
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.quick_stats),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatIndicator(
                    icon = Icons.Default.CheckCircle,
                    label = stringResource(R.string.attendance),
                    value = averageAttendance,
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatIndicator(
                    icon = Icons.Default.Event,
                    label = stringResource(R.string.events),
                    value = eventCount.toString(),
                    color = MaterialTheme.colorScheme.tertiary
                )
                
                StatIndicator(
                    icon = Icons.Default.Star,
                    label = stringResource(R.string.children),
                    value = children.size.toString(),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun StatIndicator(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 3. Enhanced Child Card
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChildOverviewCard(
    name: String,
    grade: String,
    attendance: String,
    onClick: () -> Unit = {}
) {
    val attendanceValue = attendance.removeSuffix("%").toFloatOrNull() ?: 0f
    val attendanceColor = when {
        attendanceValue >= 90f -> Color(0xFF4CAF50) // Good - Green
        attendanceValue >= 75f -> Color(0xFFFFC107) // Average - Yellow
        else -> Color(0xFFF44336) // Poor - Red
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar/Icon for the child
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = name.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                Column {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.grade_colon, grade),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Attendance progress indicator
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.attendance),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = attendance,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = attendanceColor
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                LinearProgressIndicator(
                    progress = { attendanceValue / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = attendanceColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Quick action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
                        ) {
                AssistChip(
                    onClick = { /* Handle grades view */ },
                    label = { Text(stringResource(R.string.grades)) },
                    leadingIcon = {
                            Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                AssistChip(
                    onClick = { /* Handle attendance view */ },
                    label = { Text(stringResource(R.string.details)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

// 4. Recent Activity Timeline
@Composable
private fun RecentActivityTimeline(activities: List<FirebaseService.ActivityData> = emptyList()) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
                Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.recent_activity),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (activities.isEmpty()) {
                // Display sample data if no real activities
            TimelineItem(
                icon = Icons.Default.Star,
                title = stringResource(R.string.new_grade_posted),
                description = stringResource(R.string.math_test_grade),
                time = "2 hours ago",
                iconTint = MaterialTheme.colorScheme.primary
            )
            
            TimelineItem(
                icon = Icons.Default.CheckCircle,
                title = stringResource(R.string.attendance_recorded),
                description = stringResource(R.string.present_all_classes),
                time = "Yesterday",
                iconTint = MaterialTheme.colorScheme.tertiary
            )
            
            TimelineItem(
                icon = Icons.Default.Email,
                title = stringResource(R.string.new_message),
                description = stringResource(R.string.from_math_teacher),
                time = "2 days ago",
                iconTint = MaterialTheme.colorScheme.secondary
            )
            } else {
                // Display real activities
                activities.forEach { activity ->
                    val icon = when (activity.type) {
                        "grade" -> Icons.Default.Star
                        "attendance" -> Icons.Default.CheckCircle
                        "message" -> Icons.Default.Email
                        else -> Icons.Default.Notifications
                    }
                    
                    val iconTint = when (activity.type) {
                        "grade" -> MaterialTheme.colorScheme.primary
                        "attendance" -> MaterialTheme.colorScheme.tertiary
                        "message" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    
                    // Format time difference
                    val timeDiff = System.currentTimeMillis() - activity.timestamp
                    val timeAgo = when {
                        timeDiff < 60 * 60 * 1000 -> "${timeDiff / (60 * 1000)} minutes ago"
                        timeDiff < 24 * 60 * 60 * 1000 -> "${timeDiff / (60 * 60 * 1000)} hours ago"
                        timeDiff < 7 * 24 * 60 * 60 * 1000 -> "${timeDiff / (24 * 60 * 60 * 1000)} days ago"
                        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(activity.timestamp))
                    }
                    
                    val description = if (activity.studentName.isNotEmpty()) {
                        "${activity.description} (${activity.studentName})"
                    } else {
                        activity.description
                    }
                    
                    TimelineItem(
                        icon = icon,
                        title = activity.title,
                        description = description,
                        time = timeAgo,
                        iconTint = iconTint
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(
    icon: ImageVector,
    title: String,
    description: String,
    time: String,
    iconTint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Timeline connector with icon
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// 5. Calendar Widget
@Composable
private fun CalendarWidget(events: Map<Int, List<FirebaseService.EventData>> = emptyMap()) {
    val currentDate = remember { java.util.Calendar.getInstance() }
    val currentMonth = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentDate.time) }
    val daysInMonth = remember { currentDate.getActualMaximum(java.util.Calendar.DAY_OF_MONTH) }
    val firstDayOfMonth = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.get(java.util.Calendar.DAY_OF_WEEK) - 1 // Adjust to 0-indexed for our grid
    }
    
    val currentDay = remember { currentDate.get(java.util.Calendar.DAY_OF_MONTH) }
    
    // Get events for the month or use sample data if none
    val importantDates = if (events.isEmpty()) {
        remember {
        mapOf(
            15 to "Parent Meeting",
            20 to "Science Test",
            25 to "School Holiday"
        )
        }
    } else {
        events.mapValues { (_, eventList) -> eventList.first().title ?: "Event" }
    }
    
    // State for selected day details
    var selectedDayEvents by remember { mutableStateOf<List<FirebaseService.EventData>?>(null) }
    var showEventDetails by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                    Text(
                    text = stringResource(R.string.calendar),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                    text = currentMonth,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Days of week headers
            Row(modifier = Modifier.fillMaxWidth()) {
                val daysOfWeek = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Calendar grid
            val totalCells = firstDayOfMonth + daysInMonth
            val rows = (totalCells + 6) / 7 // Round up to complete weeks
            
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayOfMonth = cellIndex - firstDayOfMonth + 1
                        
                        if (dayOfMonth in 1..daysInMonth) {
                            // This cell contains a valid day
                            val isCurrentDay = dayOfMonth == currentDay
                            val hasEvent = events.containsKey(dayOfMonth) || importantDates.containsKey(dayOfMonth)
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clickable(enabled = hasEvent) {
                                        // Show event details when clicked
                                        if (hasEvent) {
                                            selectedDay = dayOfMonth
                                            if (events.containsKey(dayOfMonth)) {
                                                selectedDayEvents = events[dayOfMonth]
                                                showEventDetails = true
                                            } else if (importantDates.containsKey(dayOfMonth)) {
                                                // For sample data, create a mock event
                                                val mockEvent = FirebaseService.EventData(
                                                    id = "sample-$dayOfMonth",
                                                    title = importantDates[dayOfMonth],
                                                    date = SimpleDateFormat("yyyy-MM-dd").format(
                                                        java.util.Calendar.getInstance().apply {
                                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                        }.time
                                                    ),
                                                    time = "9:00 AM",
                                                    description = "Details for ${importantDates[dayOfMonth]}"
                                                )
                                                selectedDayEvents = listOf(mockEvent)
                                                showEventDetails = true
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Apply different visual treatments for days with events
                                if (isCurrentDay) {
                                    // Current day gets a filled circle
                                Surface(
                                        modifier = Modifier.size(32.dp),
                                    shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = dayOfMonth.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                } else if (hasEvent) {
                                    // Days with events get a subtle background
                                    Surface(
                                        modifier = Modifier.size(32.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = dayOfMonth.toString(),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                } else {
                                    // Regular days just have text
                                    Text(
                                        text = dayOfMonth.toString(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                
                                // Event indicator dot
                                if (hasEvent && !isCurrentDay) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 2.dp)
                                            .size(4.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        } else {
                            // Empty cell (padding for the grid)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Display event info for days with events
            if (events.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.upcoming_events),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                events.entries.take(3).forEach { (day, eventsList) ->
                    val event = eventsList.first() // Show just the first event for this day
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                // Show event details when clicking on event row
                                selectedDay = day
                                selectedDayEvents = eventsList
                                showEventDetails = true
                            },
                verticalAlignment = Alignment.CenterVertically
            ) {
                        Text(
                            text = day.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                Text(
                            text = event.title ?: "Event",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                if (events.size > 3) {
                    TextButton(
                        onClick = { /* Show all events */ },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(text = stringResource(R.string.view_all))
                    }
                }
            }
        }
    }
    
    // Event details dialog
    if (showEventDetails && selectedDayEvents != null) {
        AlertDialog(
            onDismissRequest = { showEventDetails = false },
            title = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedDay != null) {
                            "Events on $selectedDay ${currentMonth}"
                        } else {
                            stringResource(R.string.event_details)
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    selectedDayEvents?.forEach { event ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = event.title ?: "Event",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                                        text = event.date ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    if (event.time != null) {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = event.time,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                if (event.description != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = event.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showEventDetails = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = stringResource(R.string.close))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentFunctionsTab(
    onShowSemesterPicker: () -> Unit,
    onShowChildSelection: () -> Unit,
    onShowProgressSemesterPicker: () -> Unit,
    onViewEvents: () -> Unit,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Dialog states
    var showDatePicker by remember { mutableStateOf(false) }
    var showPeriodPicker by remember { mutableStateOf(false) }
    var showClassSelection by remember { mutableStateOf(false) }
    var showStudentList by remember { mutableStateOf(false) }
    var showSemesterPicker by remember { mutableStateOf(false) }
    var showGradeInput by remember { mutableStateOf(false) }
    var showSubjectSelection by remember { mutableStateOf(false) }
    var showAssessmentCreation by remember { mutableStateOf(false) }
    var showGradingPage by remember { mutableStateOf(false) }
    var showEventsDialog by remember { mutableStateOf(false) }
    var upcomingEvents by remember { mutableStateOf<List<FirebaseService.EventData>>(emptyList()) }
    
    // Announcement states
    var showAnnouncementsList by remember { mutableStateOf(false) }
    var announcements by remember { mutableStateOf<List<FirebaseService.AnnouncementData>>(emptyList()) }
    var selectedAnnouncement by remember { mutableStateOf<FirebaseService.AnnouncementData?>(null) }
    var showAnnouncementDetails by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.quick_actions),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                ParentFunctionCard(
                    title = stringResource(R.string.check_attendance),
                    icon = Icons.Default.EventAvailable,
                    onClick = onShowChildSelection
                )
            }
            
            item {
                ParentFunctionCard(
                    title = stringResource(R.string.grade_overview),
                    icon = Icons.Default.Star,
                    onClick = onShowSemesterPicker
                )
            }
            
            item {
                ParentFunctionCard(
                    title = stringResource(R.string.progress_tracker),
                    icon = Icons.Default.Assignment,
                    onClick = onShowProgressSemesterPicker
                )
            }
            
            item {
                ParentFunctionCard(
                    title = stringResource(R.string.view_upcoming_events),
                    icon = Icons.Default.Event,
                    onClick = onViewEvents
                )
            }
            
            item {
                ParentFunctionCard(
                    title = stringResource(R.string.my_meetings),
                    icon = Icons.Default.Groups,
                    onClick = {
                        navController.navigate(Screen.MeetingsList.route)
                    }
                )
            }
            
            item {
                ParentFunctionCard(
                    title = stringResource(R.string.request_meeting),
                    icon = Icons.Default.ChatBubble,
                    onClick = {
                        navController.navigate(Screen.RequestMeeting.route)
                    }
                )
            }
            
            item {
                ParentFunctionCard(
                    title = stringResource(R.string.report_problem),
                    icon = Icons.Default.BugReport,
                    onClick = {
                        navController.navigate(Screen.ProblemTypeSelection.route)
                    }
                )
            }

            item {
                ParentFunctionCard(
                    title = stringResource(R.string.settings),
                    icon = Icons.Default.Settings,
                    onClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            item {
                ParentFunctionCard(
                    title = stringResource(R.string.announcements_from_teachers),
                    icon = Icons.Default.Announcement,
                    onClick = {
                        showAnnouncementsList = true
                        // Load announcements
                        FirebaseService.getInstance().getAnnouncements(
                            onSuccess = { announcementsList ->
                                announcements = announcementsList
                            },
                            onError = { error ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("Failed to load announcements: ${error.message}")
                                }
                            }
                        )
                    }
                )
            }
        }

        // Announcements List Dialog
        if (showAnnouncementsList) {
            AlertDialog(
                onDismissRequest = { showAnnouncementsList = false },
                title = { Text(stringResource(R.string.announcements)) },
                text = {
                    if (announcements.isEmpty()) {
                        Text(stringResource(R.string.no_announcements_available))
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(announcements) { announcement ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedAnnouncement = announcement
                                            showAnnouncementDetails = true
                                        },
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = announcement.title,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "From: ${announcement.teacherName}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Date: ${announcement.date}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Subject: ${announcement.subject}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Period: ${announcement.period}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAnnouncementsList = false }) {
                        Text(stringResource(R.string.close))
                    }
                }
            )
        }

        // Announcement Details Dialog
        if (showAnnouncementDetails && selectedAnnouncement != null) {
            AlertDialog(
                onDismissRequest = { 
                    showAnnouncementDetails = false
                    selectedAnnouncement = null
                },
                title = { Text(selectedAnnouncement!!.title) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.from_label) + " ${selectedAnnouncement!!.teacherName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.date_label) + " ${selectedAnnouncement!!.date}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.time_label) + " ${selectedAnnouncement!!.time}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.subject_label) + " ${selectedAnnouncement!!.subject}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.period_label) + " ${selectedAnnouncement!!.period}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Divider()
                        Text(
                            text = stringResource(R.string.description_label),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = selectedAnnouncement!!.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showAnnouncementDetails = false
                            selectedAnnouncement = null
                        }
                    ) {
                        Text(stringResource(R.string.close))
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun ParentFunctionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
                modifier = Modifier.size(48.dp)
        )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmptyStateMessage(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class FunctionItem(
    val title: String,
    val icon: ImageVector
)

private fun getFunctionItems(): List<FunctionItem> {
    return listOf(
        FunctionItem("View Attendance", Icons.Default.DateRange),
        FunctionItem("View Grades", Icons.Default.List),
        FunctionItem("View Events", Icons.Default.Star),
        FunctionItem("Contact Teacher", Icons.Default.Email)
    )
}