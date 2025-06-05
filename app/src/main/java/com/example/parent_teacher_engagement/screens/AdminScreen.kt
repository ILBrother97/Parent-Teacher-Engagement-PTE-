package com.example.parent_teacher_engagement.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Announcement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.parent_teacher_engagement.R
import com.example.parent_teacher_engagement.components.NumberPicker
import com.example.parent_teacher_engagement.components.AdminTopBar
import com.example.parent_teacher_engagement.firebase.FirebaseService
import com.example.parent_teacher_engagement.firebase.FirebaseService.UserData
import kotlinx.coroutines.launch

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.rememberDatePickerState
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.parent_teacher_engagement.navigation.Screen
import com.example.parent_teacher_engagement.LocalNavController
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.rotate
import androidx.compose.runtime.DisposableEffect
import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    userData: UserData?,
    onLogout: () -> Unit = {},
    navController: NavHostController
) {
    // If userData is null, show loading or error state
    if (userData == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var showAddTeacherDialog by remember { mutableStateOf(false) }
    var showAddParentDialog by remember { mutableStateOf(false) }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showGradeSelectionDialog by remember { mutableStateOf(false) }
    var selectedGrade by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    
    var counts by remember { mutableStateOf(mapOf(
        "teachers" to 0,
        "parents" to 0,
        "students" to 0,
        "events" to 0
    )) }
    
    var showTeachersList by remember { mutableStateOf(false) }
    var showParentsList by remember { mutableStateOf(false) }
    var showStudentsList by remember { mutableStateOf(false) }
    var teachersList by remember { mutableStateOf<List<FirebaseService.UserListData>>(emptyList()) }
    var parentsList by remember { mutableStateOf<List<FirebaseService.UserListData>>(emptyList()) }
    var studentsList by remember { mutableStateOf<List<FirebaseService.UserListData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    var showUpdateDialog by remember { mutableStateOf<FirebaseService.UserListData?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<FirebaseService.UserListData?>(null) }
    var updatedName by remember { mutableStateOf("") }
    var updatedEmail by remember { mutableStateOf("") }
    var updatedSubject by remember { mutableStateOf("") }
    var updatedGrade by remember { mutableStateOf("") }
    
    // Add state variables for Grade functionality
    var showGradeDialog by remember { mutableStateOf(false) }
    var showGradeOptionsDialog by remember { mutableStateOf(false) }
    var selectedRoleForGrade by remember { mutableStateOf<String?>(null) }
    var showGradeUsersList by remember { mutableStateOf(false) }
    var selectedGradeForList by remember { mutableStateOf<String?>(null) }
    var gradeUsersList by remember { mutableStateOf<List<FirebaseService.UserListData>>(emptyList()) }

    var showEventDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var eventTitle by remember { mutableStateOf("") }
    var eventTime by remember { mutableStateOf("") }
    var eventDescription by remember { mutableStateOf("") }

    // Add state variables for upcoming events
    var showUpcomingEventsList by remember { mutableStateOf(false) }
    var upcomingEvents by remember { mutableStateOf<List<FirebaseService.EventData>>(emptyList()) }
    var selectedEvent by remember { mutableStateOf<FirebaseService.EventData?>(null) }
    var showEventDetails by remember { mutableStateOf(false) }
    var showEventUpdateDialog by remember { mutableStateOf(false) }
    var showEventDeleteDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val firebaseService = remember { FirebaseService.getInstance() }
    
    // Load counts when screen is first shown
    LaunchedEffect(Unit) {
        firebaseService.getAllCounts { newCounts ->
            counts = newCounts
        }
    }
    
    // Store last notification time
    val lastNotificationTimeKey = "last_problem_notification_time"
    
    // Add a listener for active problems count changes using DisposableEffect
    val context = LocalContext.current
    DisposableEffect(Unit) {
        // Store the previous count to detect increases
        var previousProblemCount = counts["problems"] ?: 0
        
        // Add direct listener to problem reports
        firebaseService.addProblemReportsListener { activeProblems ->
            // Only trigger notification if there's a genuine increase in problems
            if (previousProblemCount > 0 && activeProblems > previousProblemCount) {
                // Get the shared preferences
                val prefs = context.getSharedPreferences("problem_notifications", Context.MODE_PRIVATE)
                val lastNotificationTime = prefs.getLong(lastNotificationTimeKey, 0)
                val currentTime = System.currentTimeMillis()
                
                // Only show notification if at least 5 seconds have passed since the last one
                // This prevents multiple notifications for the same problem
                if (currentTime - lastNotificationTime > 5000) {
                    // Show notification for new problem
                    showNewProblemNotification(context)
                    
                    // Update last notification time
                    prefs.edit().putLong(lastNotificationTimeKey, currentTime).apply()
                }
            }
            
            // Update previous count for next comparison
            previousProblemCount = activeProblems
            
            // Update the UI
            counts = counts.toMutableMap().apply {
                this["problems"] = activeProblems
            }
        }
        
        onDispose {
            // Cleanup will be handled by Firebase when the listener is garbage collected
        }
    }
    
    // Function to refresh counts
    fun refreshCounts() {
        firebaseService.getAllCounts { newCounts ->
            counts = newCounts
        }
    }
    
    fun loadUsersList(role: String) {
        isLoading = true
        firebaseService.getUsersByRole(
            role = role,
            onSuccess = { users ->
                when (role) {
                    "teacher" -> teachersList = users
                    "parent" -> parentsList = users
                    "student" -> studentsList = users
                }
                isLoading = false
            },
            onError = { exception ->
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to load ${role}s: ${exception.message}")
                }
                isLoading = false
            }
        )
    }
    
    Scaffold(
        topBar = {
            AdminTopBar(
                title = "Admin Dashboard",
                onMenuClick = { action ->
                    when (action) {
                        "AddTeacher" -> showAddTeacherDialog = true
                        "AddParent" -> showAddParentDialog = true
                        "AddStudent" -> showAddStudentDialog = true
                        "ListTeachers" -> {
                            showTeachersList = true
                            loadUsersList("teacher")
                        }
                        "ListParents" -> {
                            showParentsList = true
                            loadUsersList("parent")
                        }
                        "ListStudents" -> {
                            showStudentsList = true
                            loadUsersList("student")
                        }
                        "PostEvents" -> {
                            showDatePicker = true
                        }
                        "Grade" -> {
                            showGradeDialog = true
                        }
                        "ChangePassword" -> {
                            navController.navigate(Screen.ChangePassword.route)
                        }
                    }
                },
                onLogoutClick = onLogout
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Menu, contentDescription = "Functions") },
                    label = { Text("Functions") },
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
                text = { Text("My ToDo") },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (selectedTab) {
                0 -> {
                    // Profile Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Profile Header
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Background Header
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    )
                                                )
                                            )
                                    )
                                    
                                    // Profile Content
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Profile Image
                                        Surface(
                                            modifier = Modifier
                                                .size(120.dp)
                                                .padding(4.dp),
                                            shape = CircleShape,
                                            border = BorderStroke(4.dp, MaterialTheme.colorScheme.surface),
                                            color = MaterialTheme.colorScheme.surface,
                                            shadowElevation = 8.dp
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Admin",
                                                    style = MaterialTheme.typography.headlineMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        // Name
                                        Text(
                                            text = userData?.name ?: "Admin",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        // Email
                                        Text(
                                            text = userData?.email ?: "admin@example.com",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Role Badge
                                        Surface(
                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.padding(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "Administrator",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Stats Header
                        item {
                            Text(
                                text = "Dashboard Overview",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 16.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Stats Grid
                        item {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.height(380.dp)
                            ) {
                                item {
                                    StatCard(
                                        title = "Teachers",
                                        value = counts["teachers"].toString(),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        onColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                item {
                                    StatCard(
                                        title = "Parents",
                                        value = counts["parents"].toString(),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        onColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                item {
                                    StatCard(
                                        title = "Students",
                                        value = counts["students"].toString(),
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        onColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                item {
                                    StatCard(
                                        title = "Events",
                                        value = counts["events"].toString(),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        onColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                item {
                                    StatCard(
                                        title = "Problems Reported",
                                        value = counts["problems"]?.toString() ?: "0",
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        onColor = MaterialTheme.colorScheme.onErrorContainer,
                                        icon = Icons.Default.BugReport
                                    )
                                }
                            }
                        }
                        
                        // Quick Actions Header
                        item {
                            Text(
                                text = "Quick Actions",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp, bottom = 16.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Quick Actions
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                QuickActionButton(
                                    text = "Add Teacher",
                                    onClick = { showAddTeacherDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickActionButton(
                                    text = "Add Parent",
                                    onClick = { showAddParentDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickActionButton(
                                    text = "Grade",
                                    onClick = { showGradeDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        // Add some space at the bottom
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
                1 -> {
                    // Functions Tab
                    AdminFunctionsTab(
                        onShowDatePicker = { showDatePicker = true }
                    )
                }
            }
        }
    }

    // Add Teacher Dialog
    if (showAddTeacherDialog) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var subject by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var selectedGrades by remember { mutableStateOf(setOf<String>()) }
        val availableGrades = listOf("9A", "9B", "10A", "10B")
        var expanded by remember { mutableStateOf(false) }
        val subjects = listOf("English", "Civic", "Economics", "History", "Physics", "Chemistry", "Geography", "It", "Math", "Biology", "Oromo", "Amharic", "Sport")

        AlertDialog(
            onDismissRequest = { 
                showAddTeacherDialog = false
                selectedGrade = ""
            },
            title = { Text("Add Teacher") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(if (passwordVisible) "Hide" else "Show")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Subject Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subject") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            subjects.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        subject = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Grade Selection Section
                    Text(
                        text = "Select Grades",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    availableGrades.forEach { grade ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = grade in selectedGrades,
                                onCheckedChange = { checked ->
                                    selectedGrades = if (checked) {
                                        selectedGrades + grade
                                    } else {
                                        selectedGrades - grade
                                    }
                                }
                            )
                            Text(
                                text = "Grade $grade",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        if (selectedGrades.isEmpty()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please select at least one grade")
                            }
                            return@Button
                        }
                        firebaseService.createTeacher(name, email, password, subject, selectedGrades.toList())
                            .addOnSuccessListener {
                                showAddTeacherDialog = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Teacher added successfully")
                                }
                                refreshCounts()
                            }
                            .addOnFailureListener { exception ->
                                errorMessage = exception.message ?: "Failed to add teacher"
                                showErrorDialog = true
                            }
                    },
                    enabled = email.isNotBlank() && password.isNotBlank() && name.isNotBlank() && subject.isNotBlank()
                ) {
                    Text("Add Teacher")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddTeacherDialog = false
                    selectedGrade = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Parent Dialog
    if (showAddParentDialog) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var childEmails by remember { mutableStateOf(listOf("")) }
        var passwordVisible by remember { mutableStateOf(false) }
        var isValidating by remember { mutableStateOf(false) }
        var validationError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddParentDialog = false },
            title = { Text("Add Parent") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(if (passwordVisible) "Hide" else "Show")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Child Emails Section
                    Text(
                        text = "Child Emails",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Column {
                        childEmails.forEachIndexed { index, childEmail ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = childEmail,
                                    onValueChange = { newEmail ->
                                        childEmails = childEmails.toMutableList().apply {
                                            this[index] = newEmail
                                        }
                                        validationError = null // Clear error when input changes
                                    },
                                    label = { Text("Child Email ${index + 1}") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier.weight(1f),
                                    isError = validationError != null
                                )
                                
                                if (index == childEmails.size - 1) {
                                    IconButton(
                                        onClick = {
                                            childEmails = childEmails + ""
                                            validationError = null // Clear error when adding new field
                                        }
                                    ) {
                                        Text("+")
                                    }
                                }
                                
                                if (childEmails.size > 1) {
                                    IconButton(
                                        onClick = {
                                            childEmails = childEmails.filterIndexed { i, _ -> i != index }
                                            validationError = null // Clear error when removing field
                                        }
                                    ) {
                                        Text("X")
                                    }
                                }
                            }
                        }
                    }

                    // Show validation error if any
                    validationError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }

                    if (isValidating) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Validating child emails...",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Filter out empty emails
                        val validChildEmails = childEmails.filter { it.isNotBlank() }
                        if (validChildEmails.isEmpty()) {
                            validationError = "Please add at least one child email"
                            return@Button
                        }

                        isValidating = true
                        validationError = null
                        var validStudentsCount = 0
                        var processedCount = 0

                        validChildEmails.forEach { childEmail ->
                            firebaseService.checkStudentExists(childEmail) { exists ->
                                if (exists) {
                                    validStudentsCount++
                                }
                                processedCount++

                                if (processedCount == validChildEmails.size) {
                                    isValidating = false
                                    if (validStudentsCount == validChildEmails.size) {
                                        // All child emails are valid, proceed with parent registration
                                        firebaseService.createParent(name, email, password, validChildEmails)
                                            .addOnSuccessListener {
                                                showAddParentDialog = false
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Parent added successfully")
                                                }
                                                refreshCounts()
                                            }
                                            .addOnFailureListener { exception ->
                                                errorMessage = exception.message ?: "Failed to add parent"
                                                showErrorDialog = true
                                            }
                                    } else {
                                        validationError = "Some child emails are not registered as students"
                                    }
                                }
                            }
                        }
                    },
                    enabled = name.isNotBlank() && email.isNotBlank() && 
                             password.isNotBlank() && email.contains("@") &&
                             childEmails.any { it.isNotBlank() && it.contains("@") } &&
                             !isValidating
                ) {
                    Text("Add Parent")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showAddParentDialog = false
                        validationError = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Student Dialog
    if (showAddStudentDialog) {
        var email by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var grade by remember { mutableStateOf("") }
        var expanded by remember { mutableStateOf(false) }
        val grades = listOf("9A", "9B", "10A", "10B")

        AlertDialog(
            onDismissRequest = { showAddStudentDialog = false },
            title = { Text("Add Student") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Grade Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = grade,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Grade") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            grades.forEach { gradeOption ->
                                DropdownMenuItem(
                                    text = { Text(gradeOption) },
                                    onClick = {
                                        grade = gradeOption
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Use default password "123456"
                        val defaultPassword = "123456"
                        firebaseService.createStudent(name, email, defaultPassword, grade)
                            .addOnSuccessListener {
                                showAddStudentDialog = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Student added successfully. Default password: $defaultPassword")
                                }
                                refreshCounts()
                            }
                            .addOnFailureListener { exception ->
                                errorMessage = exception.message ?: "Failed to add student"
                                showErrorDialog = true
                            }
                    },
                    enabled = name.isNotBlank() && email.isNotBlank() && 
                             grade.isNotBlank() && email.contains("@")
                ) {
                    Text("Add Student")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStudentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error Dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Update Dialog
    showUpdateDialog?.let { user ->
        AlertDialog(
            onDismissRequest = { showUpdateDialog = null },
            title = { Text("Update ${user.role?.replaceFirstChar { it.toString().uppercase() } ?: "User"}") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = updatedName,
                        onValueChange = { updatedName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = updatedEmail,
                        onValueChange = { updatedEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    when (user.role) {
                        "teacher" -> {
                            OutlinedTextField(
                                value = updatedSubject,
                                onValueChange = { updatedSubject = it },
                                label = { Text("Subject") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "student" -> {
                            OutlinedTextField(
                                value = updatedGrade,
                                onValueChange = { updatedGrade = it },
                                label = { Text("Grade") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updates = mutableMapOf<String, Any>(
                            "name" to updatedName,
                            "email" to updatedEmail
                        )
                        when (user.role) {
                            "teacher" -> updates["subject"] = updatedSubject
                            "student" -> updates["grade"] = updatedGrade
                        }
                        
                        firebaseService.updateUser(
                            userId = user.id,
                            updates = updates,
                            onSuccess = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("User updated successfully")
                                    loadUsersList(user.role ?: "")
                                }
                                showUpdateDialog = null
                            },
                            onError = { exception ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("Failed to update user: ${exception.message}")
                                }
                            }
                        )
                    }
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    showDeleteConfirmation?.let { user ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Delete ${user.role?.replaceFirstChar { it.toString().uppercase() } ?: "User"}") },
            text = {
                Text(
                    if (user.role == "parent") {
                        "Are you sure you want to delete this parent? This will also delete all linked students."
                    } else {
                        "Are you sure you want to delete this ${user.role}?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isLoading = true
                        firebaseService.deleteUser(
                            userId = user.id,
                            role = user.role ?: "",
                            onSuccess = {
                                isLoading = false
                                showDeleteConfirmation = null
                                // Refresh the user list
                                loadUsersList(user.role ?: "")
                                // Refresh the counts after successful deletion
                                refreshCounts()
                                scope.launch {
                                    snackbarHostState.showSnackbar("User deleted successfully")
                                }
                            },
                            onError = { exception ->
                                isLoading = false
                                showDeleteConfirmation = null
                                scope.launch {
                                    snackbarHostState.showSnackbar("Failed to delete user: ${exception.message}")
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Teachers List Dialog
    if (showTeachersList) {
        AlertDialog(
            onDismissRequest = { showTeachersList = false },
            title = { Text("Teachers List") },
            text = {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn {
                        items(teachersList.size) { index ->
                            val teacher = teachersList[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = teacher.name ?: "",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Email: ${teacher.email ?: ""}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    teacher.subject?.let { subject ->
                                        Text(
                                            text = "Subject: $subject",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        // Edit Button
                                        IconButton(
                                            onClick = {
                                                showUpdateDialog = teacher
                                                updatedName = teacher.name ?: ""
                                                updatedEmail = teacher.email ?: ""
                                                updatedSubject = teacher.subject ?: ""
                                            }
                                        ) {
                                            Text("Edit")
                                        }
                                        
                                        // Delete Button
                                        IconButton(
                                            onClick = { showDeleteConfirmation = teacher }
                                        ) {
                                            Text("Delete")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTeachersList = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Parents List Dialog
    if (showParentsList) {
        AlertDialog(
            onDismissRequest = { showParentsList = false },
            title = { Text("Parents List") },
            text = {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn {
                        items(parentsList.size) { index ->
                            val parent = parentsList[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = parent.name ?: "",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Email: ${parent.email ?: ""}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    
                                    // Display child emails if they exist
                                    parent.childEmails?.let { emails ->
                                        if (emails.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Children's Emails:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                                emails.forEach { email ->
                                                    Text(
                                                        text = "• $email",
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        // Edit Button
                                        IconButton(
                                            onClick = {
                                                showUpdateDialog = parent
                                                updatedName = parent.name ?: ""
                                                updatedEmail = parent.email ?: ""
                                            }
                                        ) {
                                            Text("Edit")
                                        }
                                        
                                        // Delete Button
                                        IconButton(
                                            onClick = { showDeleteConfirmation = parent }
                                        ) {
                                            Text("Delete")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showParentsList = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Students List Dialog
    if (showStudentsList) {
        AlertDialog(
            onDismissRequest = { showStudentsList = false },
            title = { Text("Students List") },
            text = {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn {
                        items(studentsList.size) { index ->
                            val student = studentsList[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = student.name ?: "",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Email: ${student.email ?: ""}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    student.grade?.let { grade ->
                                        Text(
                                            text = "Grade: $grade",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        // Edit Button
                                        IconButton(
                                            onClick = {
                                                showUpdateDialog = student
                                                updatedName = student.name ?: ""
                                                updatedEmail = student.email ?: ""
                                                updatedGrade = student.grade ?: ""
                                            }
                                        ) {
                                            Text("Edit")
                                        }
                                        
                                        // Delete Button
                                        IconButton(
                                            onClick = { showDeleteConfirmation = student }
                                        ) {
                                            Text("Delete")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStudentsList = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Grade Dialog
    if (showGradeDialog) {
        AlertDialog(
            onDismissRequest = { showGradeDialog = false },
            title = { Text("Grade Management") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            selectedRoleForGrade = "student"
                            showGradeOptionsDialog = true
                            showGradeDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grade Students")
                    }
                    Button(
                        onClick = { 
                            selectedRoleForGrade = "teacher"
                            showGradeOptionsDialog = true
                            showGradeDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Teacher Grades")
                    }
                }
            },
            confirmButton = { },
            dismissButton = {
                TextButton(onClick = { showGradeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Grade Options Dialog
    if (showGradeOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showGradeOptionsDialog = false },
            title = { Text("Select Grade") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("9A", "9B", "10A", "10B").forEach { grade ->
                        Button(
                            onClick = {
                                selectedGradeForList = grade
                                showGradeOptionsDialog = false
                                showGradeUsersList = true
                                // Load users for the selected grade
                                firebaseService.getUsersByRoleAndGrade(
                                    role = selectedRoleForGrade ?: "student",
                                    grade = grade,
                                    onSuccess = { users: List<FirebaseService.UserListData> ->
                                        gradeUsersList = users
                                    },
                                    onError = { exception: Exception ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(exception.localizedMessage ?: "Failed to load users")
                                        }
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grade $grade")
                        }
                    }
                }
            },
            confirmButton = { },
            dismissButton = {
                TextButton(onClick = { 
                    showGradeOptionsDialog = false
                    selectedRoleForGrade = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Grade Users List Dialog
    if (showGradeUsersList) {
        AlertDialog(
            onDismissRequest = { showGradeUsersList = false },
            title = { Text("${if (selectedRoleForGrade == "student") "Students" else "Teachers"} in Grade ${selectedGradeForList}") },
            text = {
                if (gradeUsersList.isEmpty()) {
                    Text("No ${if (selectedRoleForGrade == "student") "students" else "teachers"} found in this grade.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(gradeUsersList.size) { index ->
                            val user = gradeUsersList[index]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    // Handle grade viewing/editing here
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Grade management coming soon!")
                                    }
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = user.name ?: "",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = user.email ?: "",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { },
            dismissButton = {
                TextButton(onClick = { 
                    showGradeUsersList = false
                    selectedGradeForList = null
                    selectedRoleForGrade = null
                    gradeUsersList = emptyList()
                }) {
                    Text("Close")
                }
            }
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val today = java.time.LocalDate.now().toEpochDay() * 24 * 60 * 60 * 1000
        val datePickerState = rememberDatePickerState(
            initialDisplayMode = DisplayMode.Picker,
            initialSelectedDateMillis = today,
            yearRange = IntRange(
                java.time.LocalDate.now().year,
                java.time.LocalDate.now().year + 5
            )
        )
        
        // Add a validation check when confirming the date
        val isSelectedDateValid = datePickerState.selectedDateMillis?.let { it >= today } ?: false
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isSelectedDateValid) {
                            selectedDate = datePickerState.selectedDateMillis
                            showDatePicker = false
                            showEventDialog = true
                        } else {
                            // Show error or handle invalid date selection
                            scope.launch {
                                snackbarHostState.showSnackbar("Please select today or a future date")
                            }
                        }
                    },
                    enabled = isSelectedDateValid
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
            DatePicker(
                state = datePickerState
            )
        }
    }

    // Event Dialog
    if (showEventDialog) {
        var showTimePicker by remember { mutableStateOf(false) }
        var selectedHour by remember { mutableStateOf(12) }
        var selectedMinute by remember { mutableStateOf(0) }
        var isAM by remember { mutableStateOf(true) }

        // Format the time string when hour or minute changes
        LaunchedEffect(selectedHour, selectedMinute, isAM) {
            val hour = if (isAM) {
                if (selectedHour == 12) "12" else selectedHour.toString()
            } else {
                if (selectedHour == 12) "12" else (selectedHour + 12).toString()
            }
            val minute = if (selectedMinute < 10) "0$selectedMinute" else selectedMinute.toString()
            val period = if (isAM) "AM" else "PM"
            eventTime = "$hour:$minute $period"
        }

        AlertDialog(
            onDismissRequest = { 
                showEventDialog = false
                eventTitle = ""
                eventTime = ""
                eventDescription = ""
            },
            title = { Text("Post Event") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = { eventTitle = it },
                        label = { Text("Event Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = eventTime,
                        onValueChange = {},
                        label = { 
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Clock",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Event Time")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showTimePicker = true }) {
                                Text("Select")
                            }
                        }
                    )
                    OutlinedTextField(
                        value = eventDescription,
                        onValueChange = { eventDescription = it },
                        label = { Text("Event Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedDate != null && eventTitle.isNotBlank() && eventTime.isNotBlank()) {
                            firebaseService.postEvent(
                                date = selectedDate!!,
                                title = eventTitle,
                                time = eventTime,
                                description = eventDescription,
                                onSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Event posted successfully!")
                                    }
                                    showEventDialog = false
                                    eventTitle = ""
                                    eventTime = ""
                                    eventDescription = ""
                                    selectedDate = null
                                    refreshCounts()
                                },
                                onError = { error ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Failed to post event: ${error.message}")
                                    }
                                }
                            )
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please fill in all required fields")
                            }
                        }
                    }
                ) {
                    Text("Post Event")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showEventDialog = false
                    eventTitle = ""
                    eventTime = ""
                    eventDescription = ""
                }) {
                    Text("Cancel")
                }
            }
        )

        // Time Picker Dialog with click and drag capability
        if (showTimePicker) {
            var timeMode by remember { mutableStateOf("hour") } // "hour" or "minute"
            
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("Select Time") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Time display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hour selector (clickable to switch to hour mode)
                            Box(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .clickable { timeMode = "hour" }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedHour.toString().padStart(2, '0'),
                                    fontSize = 24.sp,
                                    fontWeight = if (timeMode == "hour") FontWeight.Bold else FontWeight.Normal,
                                    color = if (timeMode == "hour") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            
                            // Minute selector (clickable to switch to minute mode)
                            Box(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .clickable { timeMode = "minute" }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedMinute.toString().padStart(2, '0'),
                                    fontSize = 24.sp,
                                    fontWeight = if (timeMode == "minute") FontWeight.Bold else FontWeight.Normal,
                                    color = if (timeMode == "minute") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // AM/PM selector
                            Row(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(4.dp)
                            ) {
                                SelectableButton(
                                    text = "AM",
                                    selected = isAM,
                                    onClick = { isAM = true }
                                )
                                SelectableButton(
                                    text = "PM",
                                    selected = !isAM,
                                    onClick = { isAM = false }
                                )
                            }
                        }
                        
                        // Mode tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = { timeMode = "hour" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (timeMode == "hour") 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (timeMode == "hour") 
                                        MaterialTheme.colorScheme.onPrimary 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Hour")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { timeMode = "minute" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (timeMode == "minute") 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (timeMode == "minute") 
                                        MaterialTheme.colorScheme.onPrimary 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Minute")
                            }
                        }
                        
                        if (timeMode == "hour") {
                            // Clock face for hour selection
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                // Clock numbers
                                for (hour in 1..12) {
                                    val angle = Math.PI / 6 * (hour - 3)
                                    val radius = 100.dp
                                    val x = (radius.value * kotlin.math.cos(angle)).dp
                                    val y = (radius.value * kotlin.math.sin(angle)).dp
                                    
                                    Box(
                                        modifier = Modifier
                                            .offset(x = x, y = y)
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (hour == selectedHour) 
                                                    MaterialTheme.colorScheme.primary 
                                                else 
                                                    Color.Transparent
                                            )
                                            .clickable { selectedHour = hour },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = hour.toString(),
                                            color = if (hour == selectedHour) 
                                                MaterialTheme.colorScheme.onPrimary 
                                            else 
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (hour == selectedHour) 
                                                FontWeight.Bold 
                                            else 
                                                FontWeight.Normal
                                        )
                                    }
                                }
                                
                                // Clock center
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                
                                // Clock hand
                                val hourAngle = Math.PI / 6 * (selectedHour - 3)
                                val handLength = 80.dp
                                val handEndX = (handLength.value * kotlin.math.cos(hourAngle)).dp
                                val handEndY = (handLength.value * kotlin.math.sin(hourAngle)).dp
                                
                                Box(
                                    modifier = Modifier
                                        .width(handLength)
                                        .height(2.dp)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .offset(x = handEndX / 2, y = handEndY / 2)
                                        .rotate((hourAngle * 180 / Math.PI).toFloat())
                                )
                            }
                        } else {
                            // Clock face for minute selection
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                // Minute markers (show 5-minute intervals: 0, 5, 10, 15, etc.)
                                for (minute in 0 until 60 step 5) {
                                    val angle = Math.PI / 30 * (minute / 5 - 3)
                                    val radius = 100.dp
                                    val x = (radius.value * kotlin.math.cos(angle)).dp
                                    val y = (radius.value * kotlin.math.sin(angle)).dp
                                    
                                    // Create a clickable area that selects the closest 5-minute interval
                                    Box(
                                        modifier = Modifier
                                            .offset(x = x, y = y)
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (minute == selectedMinute || 
                                                   (minute <= selectedMinute && selectedMinute < minute + 5)) 
                                                    MaterialTheme.colorScheme.primary 
                                                else 
                                                    Color.Transparent
                                            )
                                            .clickable { selectedMinute = minute },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = minute.toString().padStart(2, '0'),
                                            color = if (minute == selectedMinute || 
                                                   (minute <= selectedMinute && selectedMinute < minute + 5)) 
                                                MaterialTheme.colorScheme.onPrimary 
                                            else 
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (minute == selectedMinute || 
                                                         (minute <= selectedMinute && selectedMinute < minute + 5)) 
                                                FontWeight.Bold 
                                            else 
                                                FontWeight.Normal
                                        )
                                    }
                                }
                                
                                // Fine-tuning for minutes (1-minute intervals)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Fine tune: ")
                                    NumberPicker(
                                        value = selectedMinute,
                                        onValueChange = { selectedMinute = it },
                                        range = 0..59,
                                        modifier = Modifier.width(80.dp)
                                    )
                                }
                                
                                // Clock center
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                
                                // Clock hand
                                val minuteAngle = Math.PI / 30 * (selectedMinute - 15)
                                val handLength = 80.dp
                                val handEndX = (handLength.value * kotlin.math.cos(minuteAngle)).dp
                                val handEndY = (handLength.value * kotlin.math.sin(minuteAngle)).dp
                                
                                Box(
                                    modifier = Modifier
                                        .width(handLength)
                                        .height(2.dp)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .offset(x = handEndX / 2, y = handEndY / 2)
                                        .rotate((minuteAngle * 180 / Math.PI).toFloat())
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Add dialog for upcoming events list
    if (showUpcomingEventsList) {
        AlertDialog(
            onDismissRequest = { showUpcomingEventsList = false },
            title = { Text("Upcoming Events") },
            text = {
                LazyColumn {
                    items(upcomingEvents.size) { index ->
                        val event = upcomingEvents[index]
                        TextButton(
                            onClick = {
                                selectedEvent = event
                                showEventDetails = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(event.title ?: "")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUpcomingEventsList = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Add dialog for event details
    if (showEventDetails && selectedEvent != null) {
        AlertDialog(
            onDismissRequest = { showEventDetails = false },
            title = { Text("Event Details") },
            text = {
                Column {
                    Text("Title: ${selectedEvent!!.title ?: ""}")
                    Text("Date: ${selectedEvent!!.date ?: ""}")
                    Text("Time: ${selectedEvent!!.time ?: ""}")
                    Text("Description: ${selectedEvent!!.description ?: ""}")
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            showEventDetails = false
                            showEventUpdateDialog = true
                        }
                    ) {
                        Text("Update")
                    }
                    TextButton(
                        onClick = {
                            showEventDetails = false
                            showEventDeleteDialog = true
                        }
                    ) {
                        Text("Delete")
                    }
                    TextButton(
                        onClick = { showEventDetails = false }
                    ) {
                        Text("Close")
                    }
                }
            }
        )
    }

    // Add dialog for event update
    if (showEventUpdateDialog) {
        selectedEvent?.let { event ->
            var updatedTitle by remember { mutableStateOf(event.title ?: "") }
            var updatedTime by remember { mutableStateOf(event.time ?: "") }
            var updatedDescription by remember { mutableStateOf(event.description ?: "") }

            AlertDialog(
                onDismissRequest = { showEventUpdateDialog = false },
                title = { Text("Update Event") },
                text = {
                    Column {
                        TextField(
                            value = updatedTitle,
                            onValueChange = { updatedTitle = it },
                            label = { Text("Title") }
                        )
                        TextField(
                            value = updatedTime,
                            onValueChange = { updatedTime = it },
                            label = { Text("Time") }
                        )
                        TextField(
                            value = updatedDescription,
                            onValueChange = { updatedDescription = it },
                            label = { Text("Description") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            firebaseService.updateEvent(
                                event.id,
                                updatedTitle,
                                event.date ?: "",
                                updatedTime,
                                updatedDescription
                            ) { success ->
                                if (success) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Event updated successfully")
                                    }
                                    // Refresh the events list
                                    firebaseService.getUpcomingEvents { events ->
                                        upcomingEvents = events
                                    }
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Failed to update event")
                                    }
                                }
                            }
                            showEventUpdateDialog = false
                        }
                    ) {
                        Text("Update")
                    }
                    TextButton(onClick = { showEventUpdateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Add dialog for event deletion confirmation
    if (showEventDeleteDialog) {
        selectedEvent?.let { event ->
            AlertDialog(
                onDismissRequest = { showEventDeleteDialog = false },
                title = { Text("Delete Event") },
                text = { Text("Are you sure you want to delete this event?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            firebaseService.deleteEvent(event.id) { success ->
                                if (success) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Event deleted successfully")
                                    }
                                    // Refresh the events list
                                    firebaseService.getUpcomingEvents { events ->
                                        upcomingEvents = events
                                    }
                                    refreshCounts()
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Failed to delete event")
                                    }
                                }
                            }
                            showEventDeleteDialog = false
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEventDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun FunctionCard(
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
private fun QuickActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Text(text = text)
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    onColor: Color,
    icon: ImageVector? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = onColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onColor
                )
            }
            
            icon?.let {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = onColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ParentEmailsList(emails: List<String>) {
    Column(modifier = Modifier.padding(start = 8.dp)) {
        emails.forEach { email ->
            Text(
                text = "• $email",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SelectableButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) 
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun AdminFunctionsTab(
    onShowDatePicker: () -> Unit = {}
) {
    val navController = LocalNavController.current
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Administrative Functions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            FunctionCard(
                title = "Manage Users",
                icon = Icons.Default.People,
                onClick = {
                    // Navigate to user management screen
                    navController.navigate(Screen.UserManagement.route)
                }
            )
        }
        
        item {
            FunctionCard(
                title = "School Calendar",
                icon = Icons.Default.DateRange,
                onClick = {
                    // Show date picker for posting events
                    onShowDatePicker()
                }
            )
        }
        
        item {
            FunctionCard(
                title = "Announcements",
                icon = Icons.Default.Announcement,
                onClick = {
                    // Navigate to announcement management
                    navController.navigate(Screen.AnnouncementsList.route)
                }
            )
        }
        
        item {
            FunctionCard(
                title = "All Meetings",
                icon = Icons.Default.Groups,
                onClick = {
                    navController.navigate(Screen.MeetingsList.route)
                }
            )
        }
        
        item {
            FunctionCard(
                title = "Meeting Requests",
                icon = Icons.Default.ChatBubble,
                onClick = {
                    navController.navigate(Screen.RequestMeeting.route)
                }
            )
        }
        
        item {
            FunctionCard(
                title = "Check Reported Problems",
                icon = Icons.Default.BugReport,
                onClick = {
                    navController.navigate(Screen.ReportedProblemsList.route)
                }
            )
        }

        item {
            FunctionCard(
                title = "Settings",
                icon = Icons.Default.Settings,
                onClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
    }
}

fun showNewProblemNotification(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    // Create notification channel for Android O and above
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "problem_notification_channel",
            "Problem Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for new problem reports"
            enableLights(true)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, "problem_notification_channel")
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("New Problem Reported")
        .setContentText("There is new problem reported. Check it out!")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    // Use a consistent notification ID for problem notifications
    val PROBLEM_NOTIFICATION_ID = 1001
    notificationManager.notify(PROBLEM_NOTIFICATION_ID, notification)
}