package com.example.parent_teacher_engagement.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.parent_teacher_engagement.firebase.FirebaseService
import com.example.parent_teacher_engagement.navigation.Screen
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import android.util.Log
import com.example.parent_teacher_engagement.components.TeacherTopBar
import com.example.parent_teacher_engagement.components.NotificationBell
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.parent_teacher_engagement.service.NotificationService
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.stringResource
import com.example.parent_teacher_engagement.R
import com.example.parent_teacher_engagement.LocalNavController
import java.util.Locale
import java.util.Calendar
import java.util.Date
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState

enum class TeacherTab {
    Profile, Functions
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherScreen(
    onLogout: () -> Unit = {},
    navController: NavController
) {
    var selectedTab by remember { mutableStateOf(TeacherTab.Profile) }
    var currentTeacher by remember { mutableStateOf<FirebaseService.UserData?>(null) }
    
    // State for unread messages
    var unreadMessagesCount by remember { mutableStateOf(0) }
    
    // Shared state for events
    var hasNewEvents by remember { mutableStateOf(false) }
    var eventCount by remember { mutableStateOf(0) }
    var previousEventCount by remember { mutableStateOf(0) }
    
    // Create a coroutine scope that follows the Composable lifecycle
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val notificationService = remember { NotificationService(context) }
    
    // Fetch current teacher data
    LaunchedEffect(Unit) {
        FirebaseService.getInstance().getCurrentUserData { userData ->
            currentTeacher = userData
            
            // Get unread messages count
            if (userData != null) {
                FirebaseService.getInstance().getUnreadMessagesCount(userData.uid) { count ->
                    unreadMessagesCount = count
                }
                
                // Set up periodic check for new unread messages
                coroutineScope.launch {
                    while (true) {
                        kotlinx.coroutines.delay(30000) // Check every 30 seconds
                        FirebaseService.getInstance().getUnreadMessagesCount(userData.uid) { count ->
                            unreadMessagesCount = count
                        }
                    }
                }
            }
        }
    }
    
    // Listen for real-time event updates
    LaunchedEffect(Unit) {
        FirebaseService.getInstance().listenToEventCount { count ->
            if (count > previousEventCount && previousEventCount > 0) {
                // Show system notification when count increases
                notificationService.showNewEventNotification()
                hasNewEvents = true
            }
            previousEventCount = count  // Update previous count after checking for increase
            eventCount = count  // Update the displayed count
        }
    }

    Scaffold(
        topBar = {
            TeacherTopBar(
                title = if (selectedTab == TeacherTab.Profile) "My Profile" else "Functions",
                onMenuClick = { action ->
                    when (action) {
                        "Messages" -> {
                            navController.navigate(Screen.Messaging.route)
                        }
                        "ChangePassword" -> {
                            navController.navigate(Screen.ChangePassword.route)
                        }
                        else -> {
                            // Handle other menu actions
                            Log.d("TeacherScreen", "Menu action: $action")
                        }
                    }
                },
                onLogoutClick = onLogout,
                notificationBell = {
                    NotificationBell(
                        unreadCount = unreadMessagesCount,
                        onClick = {
                            // Simply navigate to messaging screen without setting currentChatPartnerId
                            Log.d("TeacherScreen", "Notification bell clicked, navigating to messaging screen")
                            navController.navigate(Screen.Messaging.route)
                        }
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = selectedTab == TeacherTab.Profile,
                    onClick = { selectedTab = TeacherTab.Profile }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Functions") },
                    label = { Text("Functions") },
                    selected = selectedTab == TeacherTab.Functions,
                    onClick = { selectedTab = TeacherTab.Functions }
                )
            }
        },
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
                text = { Text("My ToDo") },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    ) { padding ->
        when (selectedTab) {
            TeacherTab.Profile -> TeacherProfileTab(
                teacher = currentTeacher,
                modifier = Modifier.padding(padding),
                eventCount = eventCount,
                hasNewEvents = hasNewEvents,
                onViewEvents = { hasNewEvents = false }
            )
            TeacherTab.Functions -> TeacherFunctionsTab(
                modifier = Modifier.padding(padding),
                onLogout = onLogout,
                navController = navController,
                onViewEvents = { hasNewEvents = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherProfileTab(
    teacher: FirebaseService.UserData?,
    modifier: Modifier = Modifier,
    eventCount: Int,
    hasNewEvents: Boolean,
    onViewEvents: () -> Unit
) {
    var showEventsDialog by remember { mutableStateOf(false) }
    var upcomingEvents by remember { mutableStateOf<List<FirebaseService.EventData>>(emptyList()) }
    val firebaseService = remember { FirebaseService.getInstance() }
    val context = LocalContext.current
    val notificationService = remember { NotificationService(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // State for teacher dashboard stats
    var teacherStats by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    var recentActivities by remember { mutableStateOf<List<FirebaseService.ActivityData>>(emptyList()) }
    var calendarEvents by remember { mutableStateOf<Map<Int, List<FirebaseService.EventData>>>(emptyMap()) }
    var assignedClasses by remember { mutableStateOf<List<String>>(emptyList()) }
    var totalStudents by remember { mutableStateOf(0) }
    var isLoadingData by remember { mutableStateOf(true) }
    
    // Class card interactions
    var showStudentList by remember { mutableStateOf(false) }
    var classStudents by remember { mutableStateOf<List<FirebaseService.UserListData>>(emptyList()) }
    var selectedClass by remember { mutableStateOf<String?>(null) }
    
    // Current date for welcome message
    val currentDate = remember { java.util.Calendar.getInstance() }
    val formattedDate = remember {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(currentDate.time)
    }
    
    // Current month for calendar
    val currentYear = remember { currentDate.get(java.util.Calendar.YEAR) }
    val currentMonth = remember { currentDate.get(java.util.Calendar.MONTH) }
    
    // Fetch teacher data when the component loads
    LaunchedEffect(teacher?.uid) {
        teacher?.uid?.let { teacherId ->
            isLoadingData = true
            
            // Load assigned classes
            firebaseService.getTeacherClasses(teacherId) { classes ->
                assignedClasses = classes
                
                // Count total students across all classes
                var studentCount = 0
                var completedClassQueries = 0
                
                if (classes.isEmpty()) {
                    isLoadingData = false
                } else {
                    classes.forEach { grade ->
                        firebaseService.getStudentsInClass(grade) { students ->
                            studentCount += students.size
                            completedClassQueries++
                            
                            if (completedClassQueries >= classes.size) {
                                totalStudents = studentCount
                                
                                // Load recent activities after student count is determined
                                firebaseService.getCalendarEventsForMonth(currentYear, currentMonth) { events ->
                                    calendarEvents = events
                                    isLoadingData = false
                                }
                            }
                        }
                    }
                }
            }
            
            // Load upcoming events
            firebaseService.getUpcomingEvents { events ->
                upcomingEvents = events
            }
        } ?: run {
            isLoadingData = false
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        when {
            teacher == null || isLoadingData -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Welcome Section
                    item {
                        WelcomeTeacherSection(
                            teacherName = teacher.name ?: "Teacher",
                            formattedDate = formattedDate
                        )
                    }
                    
                    // 2. Quick Stats Dashboard
                    item {
                        TeacherQuickStatsSummary(
                            classCount = assignedClasses.size,
                            studentCount = totalStudents,
                            eventCount = eventCount
                        )
                    }
                    
                    // 3. Profile Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.size(100.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = teacher.name ?: "Loading...",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    text = teacher.email ?: "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // 4. Subject Info
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Subject",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = teacher.subject ?: "Not assigned",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // NEW: Recent Activity Timeline
                    item {
                        TeacherActivityTimeline()
                    }
                    
                    // NEW: Calendar Widget
                    item {
                        TeacherCalendarWidget(calendarEvents)
                    }
                    
                    // 5. Classes Section Header
                    if (assignedClasses.isNotEmpty()) {
                        item {
                            Text(
                                text = "My Classes",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                            )
                        }
                        
                        // 6. Class Cards
                        items(assignedClasses.size) { index ->
                            val grade = assignedClasses[index]
                            TeacherClassCard(
                                grade = grade,
                                onClick = { 
                                    // Navigate to class details or show student list
                                    selectedClass = grade
                                    firebaseService.getStudentsInClass(grade) { students ->
                                        classStudents = students
                                        showStudentList = true
                                        
                                        // Show a message if no students found
                                        if (students.isEmpty()) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("No students found in this class")
                                            }
                                        }
                                    }
                                 }
                            )
                        }
                    }
                    
                    // 7. Recent Events Card with Badge
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    firebaseService.getUpcomingEvents { eventsList ->
                                        upcomingEvents = eventsList
                                        showEventsDialog = true
                                        onViewEvents()
                                    }
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null
                                    )
                                    Text(
                                        text = "View Upcoming Events",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (hasNewEvents) {
                                        Badge {
                                            Text(text = "NEW")
                                        }
                                    }
                                }
                                Text(
                                    text = "$eventCount total events",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Add Snackbar Host at the bottom
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    
    // Events Dialog
    if (showEventsDialog) {
        AlertDialog(
            onDismissRequest = { showEventsDialog = false },
            title = { Text("Upcoming Events") },
            text = {
                LazyColumn {
                    if (upcomingEvents.isEmpty()) {
                        item {
                            Text("No upcoming events found")
                        }
                    } else {
                        items(upcomingEvents.size) { index ->
                            val event = upcomingEvents[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = event.title ?: "Untitled Event",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Date: ${event.date ?: "Unknown"}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    if (event.time != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Time: ${event.time}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    if (event.description != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = event.description,
                                            style = MaterialTheme.typography.bodyMedium
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
                    onClick = { showEventsDialog = false }
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Add Student List Dialog
    if (showStudentList) {
        AlertDialog(
            onDismissRequest = { showStudentList = false },
            title = { Text("Students in $selectedClass") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    if (classStudents.isEmpty()) {
                        Text(
                            "No students found in this class",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        classStudents.forEach { student ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = student.name ?: "Unknown Student",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = student.email ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (student != classStudents.last()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStudentList = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// Welcome section for teacher profile
@Composable
private fun WelcomeTeacherSection(
    teacherName: String,
    formattedDate: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
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
                        text = "Welcome, $teacherName",
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
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Have a great day teaching!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// Quick stats summary for teacher profile
@Composable
private fun TeacherQuickStatsSummary(
    classCount: Int,
    studentCount: Int,
    eventCount: Int
) {
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
                text = "Quick Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TeacherStatIndicator(
                    icon = Icons.Default.Class,
                    label = "Classes",
                    value = classCount.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                TeacherStatIndicator(
                    icon = Icons.Default.Group,
                    label = "Students",
                    value = studentCount.toString(),
                    color = MaterialTheme.colorScheme.tertiary
                )
                
                TeacherStatIndicator(
                    icon = Icons.Default.Event,
                    label = "Events",
                    value = eventCount.toString(),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun TeacherStatIndicator(
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
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun TeacherClassCard(
    grade: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Class,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Grade $grade",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Tap to view students and details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherFunctionsTab(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    navController: NavController,
    onViewEvents: () -> Unit
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
    var showAnnouncementDatePicker by remember { mutableStateOf(false) }
    var showAnnouncementPeriodPicker by remember { mutableStateOf(false) }
    var showAnnouncementForm by remember { mutableStateOf(false) }
    var showAnnouncementTimePicker by remember { mutableStateOf(false) }
    var selectedAnnouncementDate by remember { mutableStateOf<Long?>(null) }
    var selectedAnnouncementPeriod by remember { mutableStateOf<Int?>(null) }
    var announcementTitle by remember { mutableStateOf("") }
    var announcementTime by remember { mutableStateOf("") }
    var announcementDescription by remember { mutableStateOf("") }
    
    // Progress assessment related dialog states
    var showProgressSemesterPicker by remember { mutableStateOf(false) }
    var showProgressClassSelection by remember { mutableStateOf(false) }
    var showProgressSubjectSelection by remember { mutableStateOf(false) }
    var showProgressAssessmentCreation by remember { mutableStateOf(false) }
    var showProgressGradingPage by remember { mutableStateOf(false) }
    
    var selectedProgressSemester by remember { mutableStateOf<String?>(null) }
    var selectedProgressClass by remember { mutableStateOf<String?>(null) }
    var selectedProgressSubject by remember { mutableStateOf<String?>(null) }
    
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedPeriod by remember { mutableStateOf<Int?>(null) }
    var selectedClass by remember { mutableStateOf<String?>(null) }
    var selectedStudent by remember { mutableStateOf<FirebaseService.UserListData?>(null) }
    var selectedSemester by remember { mutableStateOf<String?>(null) }
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var gradeInput by remember { mutableStateOf("") }
    var teacherClasses by remember { mutableStateOf<List<String>>(emptyList()) }
    var classStudents by remember { mutableStateOf<List<FirebaseService.UserListData>>(emptyList()) }
    var studentAttendance by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    
    // Assessment-related states
    data class Assessment(val name: String, val totalMarks: Int)
    var assessments by remember { mutableStateOf<List<Assessment>>(emptyList()) }
    var currentAssessmentName by remember { mutableStateOf("") }
    var currentAssessmentMarks by remember { mutableStateOf("") }
    var studentGrades by remember { mutableStateOf<Map<String, Map<String, Int>>>(emptyMap()) }

    // Progress assessment-related states
    var progressAssessments by remember { mutableStateOf<List<Assessment>>(emptyList()) }
    var currentProgressAssessmentName by remember { mutableStateOf("") }
    var currentProgressAssessmentMarks by remember { mutableStateOf("") }
    var progressStudentGrades by remember { mutableStateOf<Map<String, Map<String, Int>>>(emptyMap()) }

    // Get teacher's classes
    LaunchedEffect(Unit) {
        FirebaseService.getInstance().getCurrentUserData { userData ->
            if (userData?.uid != null) {
                FirebaseService.getInstance().getTeacherClasses(userData.uid) { classes ->
                    teacherClasses = classes
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FunctionButton(
                    icon = Icons.Default.CheckCircle,
                    text = "Take Attendance",
                    onClick = { navController.navigate(Screen.AttendanceDate.route) }
                )
            }
            
            item {
                FunctionButton(
                    icon = Icons.Default.Star,
                    text = "Insert Grade",
                    onClick = { showSemesterPicker = true }
                )
            }
            
            item {
                FunctionButton(
                    icon = Icons.Default.BarChart,
                    text = "Marks",
                    onClick = {
                        navController.navigate(Screen.Marks.route)
                    }
                )
            }
            
            item {
                FunctionButton(
                    icon = Icons.Default.Assignment,
                    text = "Insert Progress",
                    onClick = { showProgressSemesterPicker = true }
                )
            }
            
            item {
                FunctionButton(
                    icon = Icons.Default.List,
                    text = "Announcements to Parents",
                    onClick = {
                        showAnnouncementDatePicker = true
                    }
                )
            }
            
            item {
                FunctionButton(
                    icon = Icons.Default.Edit,
                    text = "View Events",
                    onClick = {
                        FirebaseService.getInstance().getUpcomingEvents { eventsList ->
                            upcomingEvents = eventsList
                            showEventsDialog = true
                            onViewEvents()  // Clear new events notification
                        }
                    }
                )
            }
            
            item {
                FunctionButton(
                    icon = Icons.Default.Groups,
                    text = "My Meetings",
                    onClick = {
                        navController.navigate(Screen.MeetingsList.route)
                    }
                )
            }
            
            item {
                FunctionButton(
                    icon = Icons.Default.BugReport,
                    text = "Report Problem",
                    onClick = {
                        navController.navigate(Screen.ProblemTypeSelection.route)
                    }
                )
            }
            
            item {
                FunctionButton(
                    icon = Icons.Default.Email,
                    text = "Messages",
                    onClick = {
                        navController.navigate(Screen.Messaging.route)
                    }
                )
            }
            
            item {
                FunctionButton(
                    icon = Icons.Default.Add,
                    text = "Request Meeting",
                    onClick = {
                        navController.navigate(Screen.RequestMeeting.route)
                    }
                )
            }
            
            item {
                FunctionButton(
                    icon = Icons.Default.Lock,
                    text = "Change Password",
                    onClick = {
                        navController.navigate(Screen.ChangePassword.route)
                    }
                )
            }

            item {
                FunctionButton(
                    icon = Icons.Default.Settings,
                    text = "Settings",
                    onClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
        }

        // Date Picker Dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedDate = datePickerState.selectedDateMillis
                            showDatePicker = false
                            showPeriodPicker = true
                        }
                    ) {
                        Text("Confirm")
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

        // Period Picker Dialog
        if (showPeriodPicker) {
            AlertDialog(
                onDismissRequest = { showPeriodPicker = false },
                title = { Text("Select Period") },
                text = {
                    Column {
                        (1..7).forEach { period ->
                            TextButton(
                                onClick = {
                                    selectedPeriod = period
                                    showPeriodPicker = false
                                    showClassSelection = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Period $period")
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showPeriodPicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Semester Selection Dialog
        if (showSemesterPicker) {
            AlertDialog(
                onDismissRequest = { showSemesterPicker = false },
                title = { Text("Select Semester") },
                text = {
                    Column {
                        TextButton(
                            onClick = {
                                selectedSemester = "Semester One"
                                showSemesterPicker = false
                                showClassSelection = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Semester One")
                        }
                        TextButton(
                            onClick = {
                                selectedSemester = "Semester Two"
                                showSemesterPicker = false
                                showClassSelection = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Semester Two")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showSemesterPicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Modified Class Selection Dialog to handle both attendance and grades
        if (showClassSelection) {
            AlertDialog(
                onDismissRequest = { showClassSelection = false },
                title = { Text(if (selectedSemester != null) "Select Class for Grade Entry" else "Select Class") },
                text = {
                    Column {
                        teacherClasses.forEach { grade ->
                            TextButton(
                                onClick = {
                                    selectedClass = grade
                                    showClassSelection = false
                                    if (selectedSemester != null) {
                                        // For grade entry flow
                                        FirebaseService.getInstance().getStudentsInClass(grade) { students ->
                                            classStudents = students
                                            showSubjectSelection = true
                                        }
                                    } else {
                                        // For attendance flow
                                        showStudentList = true
                                        FirebaseService.getInstance().getStudentsInClass(grade) { students ->
                                            classStudents = students
                                            studentAttendance = students.associate { student -> student.id to false }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(grade)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { 
                        showClassSelection = false
                        selectedSemester = null  // Reset semester selection if cancelled
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Subject Selection Dialog
        if (showSubjectSelection) {
            AlertDialog(
                onDismissRequest = { showSubjectSelection = false },
                title = { Text("Select Subject") },
                text = {
                    SubjectSelectionContent { subject ->
                        selectedSubject = subject
                        showSubjectSelection = false
                        showAssessmentCreation = true
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { 
                        showSubjectSelection = false
                        selectedSemester = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Events Dialog
        if (showEventsDialog) {
            AlertDialog(
                onDismissRequest = { showEventsDialog = false },
                title = { Text("View Events") },
                text = {
                    if (upcomingEvents.isEmpty()) {
                        Text("No events found")
                    } else {
                        Column {
                            upcomingEvents.forEach { event ->
                                TextButton(
                                    onClick = {
                                        showEventsDialog = false
                                        navController.navigate("event_details/${event.id}")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(event.title ?: "Untitled Event")
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showEventsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Assessment Creation Dialog
        if (showAssessmentCreation) {
            AlertDialog(
                onDismissRequest = { showAssessmentCreation = false },
                title = { Text("Create Assessments") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Total marks must add up to 100")
                        Text("Current total: ${assessments.sumOf { it.totalMarks }}")
                        
                        OutlinedTextField(
                            value = currentAssessmentName,
                            onValueChange = { currentAssessmentName = it },
                            label = { Text("Assessment Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = currentAssessmentMarks,
                            onValueChange = { currentAssessmentMarks = it },
                            label = { Text("Total Marks") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Button(
                            onClick = {
                                val marks = currentAssessmentMarks.toIntOrNull() ?: 0
                                if (currentAssessmentName.isNotBlank() && marks > 0) {
                                    val newTotal = assessments.sumOf { it.totalMarks } + marks
                                    if (newTotal <= 100) {
                                        assessments = assessments + Assessment(currentAssessmentName, marks)
                                        currentAssessmentName = ""
                                        currentAssessmentMarks = ""
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Total marks cannot exceed 100")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Assessment")
                        }
                        
                        assessments.forEach { assessment ->
                            Text("${assessment.name}: ${assessment.totalMarks} marks")
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (assessments.sumOf { it.totalMarks } == 100) {
                                showAssessmentCreation = false
                                showGradingPage = true
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Total marks must be exactly 100")
                                }
                            }
                        }
                    ) {
                        Text("Next")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showAssessmentCreation = false
                        assessments = emptyList()
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Grading Page (not a dialog)
        if (showGradingPage) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        "Grade Entry for ${selectedClass}",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyColumn {
                        items(classStudents.size) { index ->
                            val student = classStudents[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        student.name ?: "Unknown Student",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    
                                    assessments.forEach { assessment ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${assessment.name} (${assessment.totalMarks})",
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = studentGrades[student.id]?.get(assessment.name)?.toString() ?: "",
                                                onValueChange = { value ->
                                                    val grade = value.toIntOrNull() ?: 0
                                                    if (grade <= assessment.totalMarks) {
                                                        val studentAssessments = studentGrades[student.id] ?: emptyMap()
                                                        studentGrades = studentGrades + mapOf(student.id to (studentAssessments + mapOf(assessment.name to grade)))
                                                    }
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.width(100.dp)
                                            )
                                        }
                                    }
                                    
                                    val totalGrade = studentGrades[student.id]?.values?.sum() ?: 0
                                    Text(
                                        "Total: $totalGrade/100",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                        
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = {
                                        showGradingPage = false
                                        assessments = emptyList()
                                        studentGrades = emptyMap()
                                    }
                                ) {
                                    Text("Cancel")
                                }
                                
                                Button(
                                    onClick = {
                                        // Validate that all students have grades for all assessments
                                        var allGradesEntered = true
                                        var missingGrades = ""
                                        
                                        classStudents.forEach { student ->
                                            val studentGradeMap = studentGrades[student.id] ?: emptyMap()
                                            assessments.forEach { assessment ->
                                                if (!studentGradeMap.containsKey(assessment.name)) {
                                                    allGradesEntered = false
                                                    missingGrades += "\n${student.name}: ${assessment.name}"
                                                }
                                            }
                                        }
                                        
                                        if (!allGradesEntered) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    "Missing grades for:$missingGrades",
                                                    duration = SnackbarDuration.Long
                                                )
                                            }
                                            return@Button
                                        }
                                        
                                        // Submit grades for each student
                                        classStudents.forEach { student ->
                                            val totalGrade = studentGrades[student.id]?.values?.sum() ?: 0
                                            // Create assessment map for the student
                                            val studentAssessments = studentGrades[student.id]?.mapKeys { entry ->
                                                // Find the assessment with matching name to get its total marks
                                                val assessment = assessments.find { it.name == entry.key }
                                                "${entry.key} (${assessment?.totalMarks ?: 0})"
                                            } ?: emptyMap()
                                            
                                            FirebaseService.getInstance().submitGrade(
                                                teacherId = FirebaseService.getInstance().currentUserData?.uid ?: "",
                                                studentId = student.id,
                                                grade = totalGrade.toString(),
                                                semester = selectedSemester ?: "",
                                                subject = selectedSubject ?: "",
                                                assessments = studentAssessments,
                                                onSuccess = {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Grades submitted successfully")
                                                    }
                                                },
                                                onError = { error ->
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Error submitting grades: ${error.message}")
                                                    }
                                                }
                                            )
                                        }
                                        showGradingPage = false
                                        assessments = emptyList()
                                        studentGrades = emptyMap()
                                    }
                                ) {
                                    Text("Submit All Grades")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Student List Dialog for Attendance
        if (showStudentList && selectedSemester == null) {
            AlertDialog(
                onDismissRequest = { showStudentList = false },
                title = { Text("Mark Attendance for ${selectedClass ?: ""}") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        if (classStudents.isEmpty()) {
                            Text(
                                "No students found in this class",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            classStudents.forEach { student ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = student.name ?: "Unknown Student",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                studentAttendance = studentAttendance + mapOf(student.id to true)
                                            }
                                        ) {
                                            Text(
                                                "✓",
                                                color = if (studentAttendance[student.id] == true)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    Color.Gray,
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                studentAttendance = studentAttendance + mapOf(student.id to false)
                                            }
                                        ) {
                                            Text(
                                                "✗",
                                                color = if (studentAttendance[student.id] == false)
                                                    Color.Red
                                                else
                                                    Color.Gray,
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        }
                                    }
                                }
                                if (student != classStudents.last()) {
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedDate?.let { date ->
                                selectedPeriod?.let { period ->
                                    selectedClass?.let { grade ->
                                        val attendanceRecord = FirebaseService.AttendanceRecord(
                                            date = SimpleDateFormat("yyyy-MM-dd").format(date),
                                            period = period,
                                            grade = grade,
                                            studentAttendance = studentAttendance
                                        )
                                        
                                        FirebaseService.getInstance().getCurrentUserData { userData ->
                                            if (userData?.uid != null) {
                                                FirebaseService.getInstance().submitAttendance(
                                                    teacherId = userData.uid,
                                                    attendanceRecord = attendanceRecord,
                                                    onSuccess = {
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("Attendance submitted successfully")
                                                        }
                                                        showStudentList = false
                                                    },
                                                    onError = { error ->
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("Failed to submit attendance: ${error.message}")
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        enabled = classStudents.isNotEmpty()
                    ) {
                        Text("Submit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStudentList = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Modified Student List Dialog to handle both attendance and grades
        if (showStudentList && selectedSemester != null) {
            AlertDialog(
                onDismissRequest = { 
                    showStudentList = false
                    selectedSemester = null
                },
                title = { Text("Select Student for Grade Entry - ${selectedClass ?: ""}") },
                text = {
                    Column {
                        classStudents.forEach { student ->
                            TextButton(
                                onClick = {
                                    selectedStudent = student
                                    showStudentList = false
                                    showGradeInput = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(student.name ?: "Unknown Student")
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { 
                        showStudentList = false
                        selectedSemester = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Grade Input Dialog
        if (showGradeInput) {
            AlertDialog(
                onDismissRequest = { showGradeInput = false },
                title = { Text("Enter Grade for ${selectedStudent?.name ?: "Unknown Student"}") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = gradeInput,
                            onValueChange = { 
                                // Only allow numbers and limit to 0-100
                                val filtered = it.filter { char -> char.isDigit() }
                                if (filtered.isEmpty()) {
                                    gradeInput = ""
                                } else {
                                    val num = filtered.toInt()
                                    if (num in 0..100) {
                                        gradeInput = num.toString()
                                    }
                                }
                            },
                            label = { Text("Grade (0-100)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (gradeInput.isNotEmpty() && selectedStudent != null && selectedSemester != null) {
                                val grade = gradeInput.toInt()
                                if (grade in 0..100) {
                                    FirebaseService.getInstance().getCurrentUserData { userData ->
                                        if (userData?.uid != null) {
                                            FirebaseService.getInstance().submitGrade(
                                                teacherId = userData.uid,
                                                studentId = selectedStudent!!.id,
                                                grade = grade.toString(),
                                                semester = selectedSemester!!,
                                                subject = userData.subject ?: "",
                                                onSuccess = {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Grade saved successfully!")
                                                    }
                                                    showGradeInput = false
                                                    selectedStudent = null
                                                    selectedSemester = null
                                                    gradeInput = ""
                                                },
                                                onError = { error ->
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Failed to save grade: ${error.message}")
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        enabled = gradeInput.isNotEmpty() && gradeInput.toIntOrNull() in 0..100
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showGradeInput = false
                        selectedStudent = null
                        selectedSemester = null
                        gradeInput = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Announcement Date Picker Dialog
        if (showAnnouncementDatePicker) {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = today,
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
                onDismissRequest = { showAnnouncementDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedAnnouncementDate = datePickerState.selectedDateMillis
                            showAnnouncementDatePicker = false
                            showAnnouncementPeriodPicker = true
                        }
                    ) {
                        Text("Next")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAnnouncementDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Announcement Period Picker Dialog
        if (showAnnouncementPeriodPicker) {
            AlertDialog(
                onDismissRequest = { showAnnouncementPeriodPicker = false },
                title = { Text("Select Period") },
                text = {
                    Column {
                        (1..7).forEach { period ->
                            TextButton(
                                onClick = {
                                    selectedAnnouncementPeriod = period
                                    showAnnouncementPeriodPicker = false
                                    showAnnouncementForm = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Period $period")
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAnnouncementPeriodPicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Announcement Form Dialog
        if (showAnnouncementForm) {
            AlertDialog(
                onDismissRequest = { showAnnouncementForm = false },
                title = { Text("Create Announcement") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = announcementTitle,
                            onValueChange = { announcementTitle = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = announcementTime,
                            onValueChange = {},
                            label = { Text("Time") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showAnnouncementTimePicker = true }) {
                                    Text("Select")
                                }
                            }
                        )
                        
                        OutlinedTextField(
                            value = announcementDescription,
                            onValueChange = { announcementDescription = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (announcementTitle.isBlank() || announcementTime.isBlank() || announcementDescription.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please fill in all fields")
                                }
                                return@TextButton
                            }

                            val currentTeacher = FirebaseService.getInstance().currentUserData
                            if (currentTeacher != null && selectedAnnouncementDate != null && selectedAnnouncementPeriod != null) {
                                val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .format(Date(selectedAnnouncementDate!!))

                                FirebaseService.getInstance().createAnnouncement(
                                    teacherId = currentTeacher.uid,
                                    teacherName = currentTeacher.name ?: "",
                                    date = formattedDate,
                                    subject = currentTeacher.subject ?: "",
                                    period = selectedAnnouncementPeriod!!,
                                    title = announcementTitle,
                                    time = announcementTime,
                                    description = announcementDescription,
                                    onSuccess = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Announcement created successfully")
                                        }
                                        showAnnouncementForm = false
                                        // Reset all states
                                        selectedAnnouncementDate = null
                                        selectedAnnouncementPeriod = null
                                        announcementTitle = ""
                                        announcementTime = ""
                                        announcementDescription = ""
                                    },
                                    onError = { error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Failed to create announcement: ${error.message}")
                                        }
                                    }
                                )
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAnnouncementForm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Announcement Time Picker Dialog
        if (showAnnouncementTimePicker) {
            var selectedHour by remember { mutableStateOf(8) }
            var selectedMinute by remember { mutableStateOf(0) }

            AlertDialog(
                onDismissRequest = { showAnnouncementTimePicker = false },
                title = { Text("Select Time") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hours
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Hours", style = MaterialTheme.typography.titleMedium)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { 
                                            selectedHour = if (selectedHour > 0) selectedHour - 1 else 23
                                        }
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, "Increase hour")
                                    }
                                    Text(
                                        text = String.format("%02d", selectedHour),
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    IconButton(
                                        onClick = { 
                                            selectedHour = if (selectedHour < 23) selectedHour + 1 else 0
                                        }
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, "Decrease hour")
                                    }
                                }
                            }

                            Text(":", style = MaterialTheme.typography.headlineMedium)

                            // Minutes
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Minutes", style = MaterialTheme.typography.titleMedium)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { 
                                            selectedMinute = if (selectedMinute > 0) selectedMinute - 1 else 59
                                        }
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, "Increase minute")
                                    }
                                    Text(
                                        text = String.format("%02d", selectedMinute),
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    IconButton(
                                        onClick = { 
                                            selectedMinute = if (selectedMinute < 59) selectedMinute + 1 else 0
                                        }
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, "Decrease minute")
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            announcementTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                            showAnnouncementTimePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAnnouncementTimePicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Progress Semester Selection Dialog
    if (showProgressSemesterPicker) {
        AlertDialog(
            onDismissRequest = { showProgressSemesterPicker = false },
            title = { Text("Select Semester for Progress") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            selectedProgressSemester = "Semester One"
                            showProgressSemesterPicker = false
                            showProgressClassSelection = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Semester One")
                    }
                    TextButton(
                        onClick = {
                            selectedProgressSemester = "Semester Two"
                            showProgressSemesterPicker = false
                            showProgressClassSelection = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Semester Two")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showProgressSemesterPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Progress Class Selection Dialog
    if (showProgressClassSelection) {
        AlertDialog(
            onDismissRequest = { showProgressClassSelection = false },
            title = { Text("Select Class for Progress Entry") },
            text = {
                Column {
                    teacherClasses.forEach { grade ->
                        TextButton(
                            onClick = {
                                selectedProgressClass = grade
                                showProgressClassSelection = false
                                FirebaseService.getInstance().getStudentsInClass(grade) { students ->
                                    classStudents = students
                                    showProgressSubjectSelection = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(grade)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { 
                    showProgressClassSelection = false
                    selectedProgressSemester = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Progress Subject Selection Dialog
    if (showProgressSubjectSelection) {
        AlertDialog(
            onDismissRequest = { showProgressSubjectSelection = false },
            title = { Text("Select Subject for Progress") },
            text = {
                SubjectSelectionContent { subject ->
                    selectedProgressSubject = subject
                    showProgressSubjectSelection = false
                    showProgressAssessmentCreation = true
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { 
                    showProgressSubjectSelection = false
                    selectedProgressSemester = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Progress Assessment Creation Dialog
    if (showProgressAssessmentCreation) {
        AlertDialog(
            onDismissRequest = { showProgressAssessmentCreation = false },
            title = { Text("Create Progress Assessments") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Current total: ${progressAssessments.sumOf { it.totalMarks }}")
                    
                    OutlinedTextField(
                        value = currentProgressAssessmentName,
                        onValueChange = { currentProgressAssessmentName = it },
                        label = { Text("Assessment Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = currentProgressAssessmentMarks,
                        onValueChange = { currentProgressAssessmentMarks = it },
                        label = { Text("Total Marks") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Button(
                        onClick = {
                            val marks = currentProgressAssessmentMarks.toIntOrNull() ?: 0
                            if (currentProgressAssessmentName.isNotBlank() && marks > 0) {
                                progressAssessments = progressAssessments + Assessment(currentProgressAssessmentName, marks)
                                currentProgressAssessmentName = ""
                                currentProgressAssessmentMarks = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Assessment")
                    }
                    
                    progressAssessments.forEach { assessment ->
                        Text("${assessment.name}: ${assessment.totalMarks} marks")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (progressAssessments.isNotEmpty()) {
                            showProgressAssessmentCreation = false
                            showProgressGradingPage = true
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please add at least one assessment")
                            }
                        }
                    }
                ) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showProgressAssessmentCreation = false
                    progressAssessments = emptyList()
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Progress Grading Page (not a dialog)
    if (showProgressGradingPage) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    "Progress Entry for ${selectedProgressClass}",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn {
                    items(classStudents.size) { index ->
                        val student = classStudents[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    student.name ?: "Unknown Student",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                
                                progressAssessments.forEach { assessment ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${assessment.name} (${assessment.totalMarks})",
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = progressStudentGrades[student.id]?.get(assessment.name)?.toString() ?: "",
                                            onValueChange = { value ->
                                                val grade = value.toIntOrNull() ?: 0
                                                if (grade <= assessment.totalMarks) {
                                                    val studentAssessments = progressStudentGrades[student.id] ?: emptyMap()
                                                    progressStudentGrades = progressStudentGrades + mapOf(student.id to (studentAssessments + mapOf(assessment.name to grade)))
                                                }
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.width(100.dp)
                                        )
                                    }
                                }
                                
                                val totalMarks = progressAssessments.sumOf { it.totalMarks }
                                val totalGrade = progressStudentGrades[student.id]?.values?.sum() ?: 0
                                Text(
                                    "Total: $totalGrade/$totalMarks",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                    
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = {
                                    showProgressGradingPage = false
                                    progressAssessments = emptyList()
                                    progressStudentGrades = emptyMap()
                                }
                            ) {
                                Text("Cancel")
                            }
                            
                            Button(
                                onClick = {
                                    // Submit grades for each student
                                    classStudents.forEach { student ->
                                        val totalMarks = progressAssessments.sumOf { it.totalMarks }
                                        val totalGrade = progressStudentGrades[student.id]?.values?.sum() ?: 0
                                        // Create assessment map for the student
                                        val studentAssessments = progressStudentGrades[student.id]?.mapKeys { entry ->
                                            // Find the assessment with matching name to get its total marks
                                            val assessment = progressAssessments.find { it.name == entry.key }
                                            "${entry.key} (${assessment?.totalMarks ?: 0})"
                                        } ?: emptyMap()
                                        
                                        FirebaseService.getInstance().submitProgress(
                                            teacherId = FirebaseService.getInstance().currentUserData?.uid ?: "",
                                            studentId = student.id,
                                            progress = totalGrade.toString(),
                                            semester = selectedProgressSemester ?: "",
                                            subject = selectedProgressSubject ?: "",
                                            assessments = studentAssessments,
                                            onSuccess = {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Progress submitted successfully")
                                                }
                                            },
                                            onError = { error ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Error submitting progress: ${error.message}")
                                                }
                                            }
                                        )
                                    }
                                    showProgressGradingPage = false
                                    progressAssessments = emptyList()
                                    progressStudentGrades = emptyMap()
                                }
                            ) {
                                Text("Submit All Progress")
                            }
                        }
                    }
                }
            }
        }
    }
}

private val subjects = listOf(
    "Mathematics",
    "Science",
    "English",
    "History",
    "Geography",
    "Art",
    "Music",
    "Physical Education"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectSelectionContent(onSubjectSelected: (String) -> Unit) {
    val currentTeacher = FirebaseService.getInstance().currentUserData
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (currentTeacher?.subject == null) {
            Text(
                text = "No subjects assigned",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            TextButton(
                onClick = { onSubjectSelected(currentTeacher.subject) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = currentTeacher.subject)
            }
        }
    }
}

@Composable
private fun SubjectButton(subject: String, onSubjectSelected: (String) -> Unit) {
    TextButton(
        onClick = { onSubjectSelected(subject) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = subject)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunctionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TeacherActivityTimeline() {
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
                text = "Recent Activity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sample activities - in a real app, these would come from a data source
            TeacherTimelineItem(
                icon = Icons.Default.Create,
                title = "Grades Updated",
                description = "Math Test: Grade 8",
                time = "2 hours ago",
                iconTint = MaterialTheme.colorScheme.primary
            )
            
            TeacherTimelineItem(
                icon = Icons.Default.CheckCircle,
                title = "Attendance Recorded",
                description = "Grade 7 - Morning Session",
                time = "Yesterday",
                iconTint = MaterialTheme.colorScheme.tertiary
            )
            
            TeacherTimelineItem(
                icon = Icons.Default.Assignment,
                title = "Assignment Created",
                description = "Science Project: Grade 9",
                time = "2 days ago",
                iconTint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun TeacherTimelineItem(
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
                shape = androidx.compose.foundation.shape.CircleShape,
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

@Composable
private fun TeacherCalendarWidget(events: Map<Int, List<FirebaseService.EventData>> = emptyMap()) {
    val currentDate = remember { Calendar.getInstance() }
    val currentMonth = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentDate.time) }
    val daysInMonth = remember { currentDate.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val firstDayOfMonth = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.get(Calendar.DAY_OF_WEEK) - 1 // Adjust to 0-indexed for our grid
    }
    
    val currentDay = remember { currentDate.get(Calendar.DAY_OF_MONTH) }
    
    // Sample important dates - in a real app, these would come from a data source
    val importantDates = remember {
        mapOf(
            5 to "Faculty Meeting",
            12 to "Parent Conference",
            18 to "Exam Day",
            25 to "School Holiday"
        )
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
                    text = "Calendar",
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
                                                        Calendar.getInstance().apply {
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
                                        shape = androidx.compose.foundation.shape.CircleShape,
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
                                        shape = androidx.compose.foundation.shape.CircleShape,
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
                                                shape = androidx.compose.foundation.shape.CircleShape
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
            
            // Display upcoming dates with events
            Text(
                text = "Upcoming Events",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Show 3 upcoming events (either from real data or sample data)
            val upcomingEventsToShow = if (events.isEmpty()) {
                importantDates.entries.take(3).map { (day, title) ->
                    CalendarEventItem(
                        day = day,
                        title = title,
                        onClick = {
                            selectedDay = day
                            val mockEvent = FirebaseService.EventData(
                                id = "sample-$day",
                                title = title,
                                date = SimpleDateFormat("yyyy-MM-dd").format(
                                    Calendar.getInstance().apply {
                                        set(Calendar.DAY_OF_MONTH, day)
                                    }.time
                                ),
                                time = "9:00 AM",
                                description = "Details for $title"
                            )
                            selectedDayEvents = listOf(mockEvent)
                            showEventDetails = true
                        }
                    )
                }
            } else {
                events.entries.take(3).map { (day, eventsList) ->
                    CalendarEventItem(
                        day = day,
                        title = eventsList.first().title ?: "Event",
                        onClick = {
                            selectedDay = day
                            selectedDayEvents = eventsList
                            showEventDetails = true
                        }
                    )
                }
            }
            
            upcomingEventsToShow.forEach { item -> item() }
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
                            "Events on $selectedDay $currentMonth"
                        } else {
                            "Event Details"
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
                    Text(text = "Close")
                }
            }
        )
    }
}

@Composable
private fun CalendarEventItem(
    day: Int,
    title: String,
    onClick: () -> Unit
): @Composable () -> Unit = {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
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
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// --- New Attendance Flow Screens ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDateScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Get today's date in milliseconds, ensuring it's in the local timezone
    val today = remember {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 12) // Set to noon to avoid timezone issues
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.timeInMillis
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Date for Attendance") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Please select today's date for attendance",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
            
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = today,
                initialDisplayedMonthMillis = today,
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        // Only allow selecting today's date
                        val selectedDate = Calendar.getInstance().apply {
                            timeInMillis = utcTimeMillis
                            set(Calendar.HOUR_OF_DAY, 12) // Set to noon to avoid timezone issues
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        
                        return selectedDate == today
                    }
                }
            )
            
            DatePicker(
                state = datePickerState,
                modifier = Modifier.padding(16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    val selectedDateMillis = datePickerState.selectedDateMillis
                    if (selectedDateMillis != null) {
                        navController.navigate(Screen.AttendancePeriod.createRoute(selectedDateMillis))
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please select today's date")
                        }
                    }
                },
                enabled = datePickerState.selectedDateMillis != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("Next")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendancePeriodScreen(date: Long, navController: NavController) {
    val formattedDate = remember {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(date))
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Period") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Date display card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Date row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Date",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Period row - removing the incorrect period display here
                }
            }
            
            // Period selection prompt
            Text(
                text = "Select a period for attendance",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Periods list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(7) { index ->
                    val period = index + 1
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate(Screen.AttendanceClass.createRoute(date, period))
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = period.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Text(
                                text = "Period $period",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Select",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceClassScreen(date: Long, period: Int, navController: NavController) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var teacherClasses by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val formattedDate = remember {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(date))
    }
    
    // Load teacher's classes
    LaunchedEffect(Unit) {
        FirebaseService.getInstance().getCurrentUserData { userData ->
            if (userData?.uid != null) {
                FirebaseService.getInstance().getTeacherClasses(userData.uid) { classes ->
                    teacherClasses = classes
                    isLoading = false
                }
            } else {
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Class") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Information card with date and period
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Date row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Date",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Period row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Period",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Period $period",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Removing incorrect class row reference
                }
            }
            
            // Class selection instruction
            Text(
                text = "Select a class for attendance",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (teacherClasses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No classes assigned yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(teacherClasses.size) { index ->
                        val className = teacherClasses[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // URL encode the class name since it will be part of the navigation route
                                    navController.navigate(Screen.AttendanceMark.createRoute(date, period, className))
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Class,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Text(
                                    text = "Grade $className",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                
                                Spacer(modifier = Modifier.weight(1f))
                                
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Select",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceMarkScreen(date: Long, period: Int, className: String, navController: NavController) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var classStudents by remember { mutableStateOf<List<FirebaseService.UserListData>>(emptyList()) }
    var studentAttendance by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    
    val formattedDate = remember {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(date))
    }
    
    // Load students in the selected class
    LaunchedEffect(Unit) {
        FirebaseService.getInstance().getStudentsInClass(className) { students ->
            classStudents = students
            studentAttendance = students.associate { student -> student.id to false }
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mark Attendance") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        enabled = !isSubmitting
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            if (classStudents.isEmpty()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("No students found in this class")
                                }
                                return@Button
                            }
                            
                            isSubmitting = true
                            
                            val attendanceRecord = FirebaseService.AttendanceRecord(
                                date = SimpleDateFormat("yyyy-MM-dd").format(Date(date)),
                                period = period,
                                grade = className,
                                studentAttendance = studentAttendance
                            )
                            
                            FirebaseService.getInstance().getCurrentUserData { userData ->
                                if (userData?.uid != null) {
                                    FirebaseService.getInstance().submitAttendance(
                                        teacherId = userData.uid,
                                        attendanceRecord = attendanceRecord,
                                        onSuccess = {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Attendance submitted successfully")
                                            }
                                            isSubmitting = false
                                            // Navigate back to the teacher function screen
                                            navController.navigate(Screen.Teacher.route) {
                                                popUpTo(Screen.Teacher.route) { inclusive = true }
                                            }
                                        },
                                        onError = { error ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Failed to submit attendance: ${error.message}")
                                            }
                                            isSubmitting = false
                                        }
                                    )
                                } else {
                                    isSubmitting = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("User data not found")
                                    }
                                }
                            }
                        },
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Submit")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Information card with date, period, and class
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Date row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Date",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Period row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Period",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Period $period",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Class row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Class,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Class",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Grade $className",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            Text(
                text = "Mark Attendance",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (classStudents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No students found in this class",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    items(classStudents.size) { index ->
                        val student = classStudents[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Student info
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = student.name ?: "Unknown Student",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    if (student.email != null) {
                                        Text(
                                            text = student.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                // Attendance marking icons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Present mark
                                    IconButton(
                                        onClick = {
                                            studentAttendance = studentAttendance + mapOf(student.id to true)
                                        }
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (studentAttendance[student.id] == true)
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            else
                                                Color.Transparent,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    "✓",
                                                    color = if (studentAttendance[student.id] == true)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        Color.Gray,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Absent mark
                                    IconButton(
                                        onClick = {
                                            studentAttendance = studentAttendance + mapOf(student.id to false)
                                        }
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (studentAttendance[student.id] == false)
                                                Color.Red.copy(alpha = 0.1f)
                                            else
                                                Color.Transparent,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    "✗",
                                                    color = if (studentAttendance[student.id] == false)
                                                        Color.Red
                                                    else
                                                        Color.Gray,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (index < classStudents.size - 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                    
                    // Add bottom padding for better UX with bottom bar
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}