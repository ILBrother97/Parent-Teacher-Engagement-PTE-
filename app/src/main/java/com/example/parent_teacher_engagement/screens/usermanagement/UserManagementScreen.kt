package com.example.parent_teacher_engagement.screens.usermanagement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.parent_teacher_engagement.firebase.FirebaseService
import com.example.parent_teacher_engagement.components.UserCard
import com.example.parent_teacher_engagement.components.SearchBar
import com.example.parent_teacher_engagement.components.EmptyStateMessage
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun UserManagementScreen(
    navController: NavHostController,
    firebaseService: FirebaseService = FirebaseService.getInstance()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Teachers", "Parents", "Students")
    
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Track users for each category
    var teachersList by remember { mutableStateOf<List<FirebaseService.UserListData>>(emptyList()) }
    var parentsList by remember { mutableStateOf<List<FirebaseService.UserListData>>(emptyList()) }
    var studentsList by remember { mutableStateOf<List<FirebaseService.UserListData>>(emptyList()) }
    
    // Dialog states
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showUserDetailsDialog by remember { mutableStateOf<FirebaseService.UserListData?>(null) }
    var showEditUserDialog by remember { mutableStateOf<FirebaseService.UserListData?>(null) }
    
    // Filter state
    var gradeFilter by remember { mutableStateOf("") }
    var subjectFilter by remember { mutableStateOf("") }
    
    // Refresh trigger
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Function to refresh all user lists
    fun refreshAllUserLists() {
        refreshTrigger++
    }
    
    // Function to refresh the current tab's user list
    fun refreshCurrentTabUserList() {
        isLoading = true
        when (selectedTab) {
            0 -> firebaseService.getUsersByRole("teacher") { users, _ ->
                teachersList = users ?: emptyList()
                isLoading = false
            }
            1 -> firebaseService.getUsersByRole("parent") { users, _ ->
                parentsList = users ?: emptyList()
                isLoading = false
            }
            2 -> firebaseService.getUsersByRole("student") { users, _ ->
                studentsList = users ?: emptyList()
                isLoading = false
            }
        }
    }
    
    // Filter based on search query and additional filters
    val filteredTeachers = remember(teachersList, searchQuery, subjectFilter) {
        var filtered = if (searchQuery.isEmpty()) teachersList
        else teachersList.filter { 
            it.name?.contains(searchQuery, ignoreCase = true) == true ||
            it.email?.contains(searchQuery, ignoreCase = true) == true ||
            it.subject?.contains(searchQuery, ignoreCase = true) == true
        }
        
        // Apply subject filter if set
        if (subjectFilter.isNotEmpty()) {
            filtered = filtered.filter {
                it.subject?.contains(subjectFilter, ignoreCase = true) == true
            }
        }
        
        filtered
    }
    
    val filteredParents = remember(parentsList, searchQuery) {
        if (searchQuery.isEmpty()) parentsList
        else parentsList.filter { 
            it.name?.contains(searchQuery, ignoreCase = true) == true ||
            it.email?.contains(searchQuery, ignoreCase = true) == true
        }
    }
    
    val filteredStudents = remember(studentsList, searchQuery, gradeFilter) {
        var filtered = if (searchQuery.isEmpty()) studentsList
        else studentsList.filter { 
            it.name?.contains(searchQuery, ignoreCase = true) == true ||
            it.email?.contains(searchQuery, ignoreCase = true) == true ||
            it.grade?.contains(searchQuery, ignoreCase = true) == true
        }
        
        // Apply grade filter if set
        if (gradeFilter.isNotEmpty()) {
            filtered = filtered.filter {
                it.grade == gradeFilter
            }
        }
        
        filtered
    }
    
    // Load users when tab changes or refresh is triggered
    LaunchedEffect(selectedTab, refreshTrigger) {
        refreshCurrentTabUserList()
    }
    
    // UI for confirmation dialogs
    var showDeleteConfirmation by remember { mutableStateOf<FirebaseService.UserListData?>(null) }
    
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Management") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(onClick = { refreshCurrentTabUserList() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    // Add user button
                    IconButton(onClick = { showAddUserDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add User")
                    }
                    // Filter button
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            SearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            // Tab bar
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        when (selectedTab) {
                            0 -> UserList(
                                users = filteredTeachers,
                                onDeleteUser = { user -> showDeleteConfirmation = user },
                                onEditUser = { user -> showEditUserDialog = user },
                                onViewUser = { user -> showUserDetailsDialog = user },
                                emptyMessage = "No teachers found"
                            )
                            1 -> UserList(
                                users = filteredParents,
                                onDeleteUser = { user -> showDeleteConfirmation = user },
                                onEditUser = { user -> showEditUserDialog = user },
                                onViewUser = { user -> showUserDetailsDialog = user },
                                emptyMessage = "No parents found"
                            )
                            2 -> UserList(
                                users = filteredStudents,
                                onDeleteUser = { user -> showDeleteConfirmation = user },
                                onEditUser = { user -> showEditUserDialog = user },
                                onViewUser = { user -> showUserDetailsDialog = user },
                                emptyMessage = "No students found"
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Add User Dialog
    if (showAddUserDialog) {
        val userType = when (selectedTab) {
            0 -> "teacher"
            1 -> "parent"
            else -> "student"
        }
        
        AddUserDialog(
            userType = userType,
            onDismiss = { showAddUserDialog = false },
            onAdd = { name, email, password, additionalData ->
                isLoading = true
                when (userType) {
                    "teacher" -> {
                        val subject = additionalData["subject"] ?: ""
                        val gradesString = additionalData["grades"] ?: ""
                        val gradesList = gradesString.split(",").filter { it.isNotBlank() }
                        
                        firebaseService.createTeacher(
                            name = name,
                            email = email,
                            password = password,
                            subject = subject,
                            grades = gradesList
                        ).addOnSuccessListener {
                            // Show success message
                            scope.launch {
                                snackbarHostState.showSnackbar("Teacher added successfully")
                            }
                            // Refresh immediately after adding
                            refreshCurrentTabUserList()
                        }.addOnFailureListener { exception ->
                            isLoading = false
                            // Show error message
                            scope.launch {
                                snackbarHostState.showSnackbar("Failed to add teacher: ${exception.message}")
                            }
                        }
                    }
                    "parent" -> {
                        val childEmailsString = additionalData["childEmails"] ?: ""
                        val childEmailsList = childEmailsString.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        
                        firebaseService.createParent(
                            name = name,
                            email = email,
                            password = password,
                            childEmails = childEmailsList
                        ).addOnSuccessListener {
                            // Show success message
                            scope.launch {
                                snackbarHostState.showSnackbar("Parent added successfully")
                            }
                            // Refresh immediately after adding
                            refreshCurrentTabUserList()
                        }.addOnFailureListener { exception ->
                            isLoading = false
                            // Show error message
                            scope.launch {
                                snackbarHostState.showSnackbar("Failed to add parent: ${exception.message}")
                            }
                        }
                    }
                    "student" -> {
                        val grade = additionalData["grade"] ?: ""
                        val defaultPassword = "123456"
                        
                        firebaseService.createStudent(
                            name = name,
                            email = email,
                            password = defaultPassword,
                            grade = grade
                        ).addOnSuccessListener {
                            // Show success message
                            scope.launch {
                                snackbarHostState.showSnackbar("Student added successfully")
                            }
                            // Refresh immediately after adding
                            refreshCurrentTabUserList()
                        }.addOnFailureListener { exception ->
                            isLoading = false
                            // Show error message
                            scope.launch {
                                snackbarHostState.showSnackbar("Failed to add student: ${exception.message}")
                            }
                        }
                    }
                }
                showAddUserDialog = false
            }
        )
    }
    
    // Filter Dialog
    if (showFilterDialog) {
        FilterDialog(
            userType = when (selectedTab) {
                0 -> "teacher"
                1 -> "parent"
                else -> "student"
            },
            currentGradeFilter = gradeFilter,
            currentSubjectFilter = subjectFilter,
            onFilterUpdate = { newGradeFilter, newSubjectFilter ->
                gradeFilter = newGradeFilter
                subjectFilter = newSubjectFilter
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
    
    // User Details Dialog
    showUserDetailsDialog?.let { user ->
        UserDetailsDialog(
            user = user,
            onDismiss = { showUserDetailsDialog = null }
        )
    }
    
    // Edit User Dialog
    showEditUserDialog?.let { user ->
        EditUserDialog(
            user = user,
            onDismiss = { showEditUserDialog = null },
            onUpdate = { updatedName, updatedEmail, updatedData ->
                // Function to update user details based on role
                isLoading = true
                // TODO: Implement the update logic based on user role
                // For example:
                when (user.role) {
                    "teacher" -> {
                        // Update teacher
                    }
                    "parent" -> {
                        // Update parent
                    }
                    "student" -> {
                        // Update student
                    }
                }
                showEditUserDialog = null
                // Refresh the list after update
                refreshCurrentTabUserList()
            }
        )
    }
    
    // Delete confirmation dialog
    showDeleteConfirmation?.let { user ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Delete User") },
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
                                
                                // Refresh after deletion
                                refreshCurrentTabUserList()
                                
                                // Show success message
                                scope.launch {
                                    snackbarHostState.showSnackbar("User deleted successfully")
                                }
                            },
                            onError = { exception ->
                                isLoading = false
                                showDeleteConfirmation = null
                                
                                // Show error message
                                scope.launch {
                                    snackbarHostState.showSnackbar("Failed to delete user: ${exception.message}")
                                }
                            }
                        )
                    }
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
}

@Composable
fun UserList(
    users: List<FirebaseService.UserListData>,
    onDeleteUser: (FirebaseService.UserListData) -> Unit,
    onEditUser: (FirebaseService.UserListData) -> Unit,
    onViewUser: (FirebaseService.UserListData) -> Unit,
    emptyMessage: String
) {
    if (users.isEmpty()) {
        EmptyStateMessage(message = emptyMessage)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users.size) { index ->
                val user = users[index]
                UserCard(
                    user = user,
                    onDelete = { onDeleteUser(user) },
                    onEdit = { onEditUser(user) },
                    onView = { onViewUser(user) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddUserDialog(
    userType: String,
    onDismiss: () -> Unit,
    onAdd: (name: String, email: String, password: String, additionalData: Map<String, String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Additional fields based on user type
    var subject by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var grades by remember { mutableStateOf<List<String>>(emptyList()) }
    var childEmails by remember { mutableStateOf(listOf("")) }
    var isValidating by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    
    // Dropdown states
    var expandedSubjectDropdown by remember { mutableStateOf(false) }
    var expandedGradeDropdown by remember { mutableStateOf(false) }
    var expandedGradesDropdown by remember { mutableStateOf(false) }
    
    // Available options
    val availableSubjects = listOf("English", "Civic", "Economics", "History", "Physics", "Chemistry", 
        "Geography", "It", "Math", "Biology", "Oromo", "Amharic", "Sport")
    val availableGrades = listOf("9A", "9B", "10A", "10B")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${userType.replaceFirstChar { it.uppercase() }}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
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
                
                if (userType != "student") {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                when (userType) {
                    "teacher" -> {
                        // Subject dropdown
                        @OptIn(ExperimentalMaterial3Api::class)
                        ExposedDropdownMenuBox(
                            expanded = expandedSubjectDropdown,
                            onExpandedChange = { expandedSubjectDropdown = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = subject,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Subject") },
                                trailingIcon = {
                                    @OptIn(ExperimentalMaterial3Api::class)
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubjectDropdown)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            
                            @OptIn(ExperimentalMaterial3Api::class)
                            ExposedDropdownMenu(
                                expanded = expandedSubjectDropdown,
                                onDismissRequest = { expandedSubjectDropdown = false }
                            ) {
                                availableSubjects.forEach { option ->
                                    @OptIn(ExperimentalMaterial3Api::class)
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            subject = option
                                            expandedSubjectDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Grades selection
                        Text(
                            text = "Assigned Grades",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableGrades.forEach { gradeOption ->
                                val isSelected = grades.contains(gradeOption)
                                @OptIn(ExperimentalMaterial3Api::class)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        grades = if (isSelected) {
                                            grades - gradeOption
                                        } else {
                                            grades + gradeOption
                                        }
                                    },
                                    label = { Text(gradeOption) },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                    "parent" -> {
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
                                            Icon(Icons.Default.Add, contentDescription = "Add child email")
                                        }
                                    }
                                    
                                    if (childEmails.size > 1) {
                                        IconButton(
                                            onClick = {
                                                childEmails = childEmails.filterIndexed { i, _ -> i != index }
                                                validationError = null // Clear error when removing field
                                            }
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove child email")
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
                    "student" -> {
                        // Grade dropdown
                        @OptIn(ExperimentalMaterial3Api::class)
                        ExposedDropdownMenuBox(
                            expanded = expandedGradeDropdown,
                            onExpandedChange = { expandedGradeDropdown = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = grade,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Grade") },
                                trailingIcon = {
                                    @OptIn(ExperimentalMaterial3Api::class)
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGradeDropdown)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            
                            @OptIn(ExperimentalMaterial3Api::class)
                            ExposedDropdownMenu(
                                expanded = expandedGradeDropdown,
                                onDismissRequest = { expandedGradeDropdown = false }
                            ) {
                                availableGrades.forEach { option ->
                                    @OptIn(ExperimentalMaterial3Api::class)
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            grade = option
                                            expandedGradeDropdown = false
                                        }
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
                onClick = {
                    when (userType) {
                        "parent" -> {
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
                                FirebaseService.getInstance().checkStudentExists(childEmail) { exists ->
                                    if (exists) {
                                        validStudentsCount++
                                    }
                                    processedCount++

                                    if (processedCount == validChildEmails.size) {
                                        isValidating = false
                                        if (validStudentsCount == validChildEmails.size) {
                                            // All child emails are valid, proceed with parent registration
                                            val additionalData = mapOf(
                                                "childEmails" to validChildEmails.joinToString(",")
                                            )
                                            onAdd(name, email, password, additionalData)
                                        } else {
                                            validationError = "Some child emails are not registered as students"
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            val additionalData = when (userType) {
                                "teacher" -> mapOf(
                                    "subject" to subject,
                                    "grades" to grades.joinToString(",")
                                )
                                "student" -> mapOf(
                                    "grade" to grade
                                )
                                else -> emptyMap()
                            }
                            onAdd(name, email, password, additionalData)
                        }
                    }
                },
                enabled = when (userType) {
                    "teacher" -> name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && subject.isNotBlank() && grades.isNotEmpty()
                    "parent" -> name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && 
                              email.contains("@") && childEmails.any { it.isNotBlank() && it.contains("@") } &&
                              !isValidating
                    "student" -> name.isNotBlank() && email.isNotBlank() && grade.isNotBlank()
                    else -> false
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    userType: String,
    currentGradeFilter: String,
    currentSubjectFilter: String,
    onFilterUpdate: (gradeFilter: String, subjectFilter: String) -> Unit,
    onDismiss: () -> Unit
) {
    var gradeFilter by remember { mutableStateOf(currentGradeFilter) }
    var subjectFilter by remember { mutableStateOf(currentSubjectFilter) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter ${userType.replaceFirstChar { it.uppercase() }}s") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (userType) {
                    "teacher" -> {
                        OutlinedTextField(
                            value = subjectFilter,
                            onValueChange = { subjectFilter = it },
                            label = { Text("Subject") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "student" -> {
                        OutlinedTextField(
                            value = gradeFilter,
                            onValueChange = { gradeFilter = it },
                            label = { Text("Grade") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onFilterUpdate(gradeFilter, subjectFilter) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = { 
                // Clear filters
                onFilterUpdate("", "")
            }) {
                Text("Clear Filters")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailsDialog(
    user: FirebaseService.UserListData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("User Details") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = user.name ?: "",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Divider()
                
                Row {
                    Text(
                        text = "Role:",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(100.dp)
                    )
                    Text(text = user.role?.replaceFirstChar { it.uppercase() } ?: "")
                }
                
                Row {
                    Text(
                        text = "Email:",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(100.dp)
                    )
                    Text(text = user.email ?: "")
                }
                
                // Role-specific details
                when (user.role) {
                    "teacher" -> {
                        if (!user.subject.isNullOrEmpty()) {
                            Row {
                                Text(
                                    text = "Subject:",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(100.dp)
                                )
                                Text(text = user.subject)
                            }
                        }
                    }
                    "student" -> {
                        if (!user.grade.isNullOrEmpty()) {
                            Row {
                                Text(
                                    text = "Grade:",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(100.dp)
                                )
                                Text(text = user.grade)
                            }
                        }
                    }
                    "parent" -> {
                        if (!user.childEmails.isNullOrEmpty()) {
                            Text(
                                text = "Children:",
                                fontWeight = FontWeight.Bold
                            )
                            Column(modifier = Modifier.padding(start = 16.dp)) {
                                user.childEmails.forEach { email ->
                                    Text(text = "• $email")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserDialog(
    user: FirebaseService.UserListData,
    onDismiss: () -> Unit,
    onUpdate: (name: String, email: String, additionalData: Map<String, String>) -> Unit
) {
    var name by remember { mutableStateOf(user.name ?: "") }
    var email by remember { mutableStateOf(user.email ?: "") }
    
    // Additional fields based on user role
    var subject by remember { mutableStateOf(user.subject ?: "") }
    var grade by remember { mutableStateOf(user.grade ?: "") }
    var childEmails by remember { mutableStateOf(user.childEmails?.joinToString(",") ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${user.role?.replaceFirstChar { it.uppercase() }}") },
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
                
                when (user.role) {
                    "teacher" -> {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Subject") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "parent" -> {
                        OutlinedTextField(
                            value = childEmails,
                            onValueChange = { childEmails = it },
                            label = { Text("Child Emails (comma separated)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "student" -> {
                        OutlinedTextField(
                            value = grade,
                            onValueChange = { grade = it },
                            label = { Text("Grade") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val additionalData = when (user.role) {
                        "teacher" -> mapOf(
                            "subject" to subject
                        )
                        "parent" -> mapOf(
                            "childEmails" to childEmails
                        )
                        "student" -> mapOf(
                            "grade" to grade
                        )
                        else -> emptyMap()
                    }
                    onUpdate(name, email, additionalData)
                },
                enabled = name.isNotBlank() && email.isNotBlank()
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 