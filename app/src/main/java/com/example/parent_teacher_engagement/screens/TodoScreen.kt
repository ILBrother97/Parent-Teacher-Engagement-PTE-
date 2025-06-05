package com.example.parent_teacher_engagement.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.parent_teacher_engagement.model.TodoItem
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import com.example.parent_teacher_engagement.firebase.FirebaseService
import com.example.parent_teacher_engagement.navigation.Screen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TodoScreen(
    navController: NavController
) {
    // Get current user information
    val firebaseService = remember { FirebaseService.getInstance() }
    var userRole by remember { mutableStateOf<String?>(null) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // State for todos
    var todos by remember { mutableStateOf(listOf<TodoItem>()) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Active", "Completed", "Meetings")
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Add search functionality
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    
    // Define role-specific categories
    val categories = when (userRole) {
        "teacher" -> listOf("All", "Classes", "Grading", "Meetings", "School", "Personal")
        "admin" -> listOf("All", "Administrative", "Staff", "Students", "Events", "Meetings", "Personal")
        else -> listOf("All", "School", "Personal", "Work", "Meetings", "Shopping", "Other") // Default for parents
    }
    
    // Function to filter todos
    fun filterTodos(): List<TodoItem> {
        // First filter by category
        val categoryFiltered = if (selectedCategory == "All") {
            todos
        } else {
            todos.filter { it.category == selectedCategory }
        }
        
        // Then filter by completion status
        val statusFiltered = when (selectedFilter) {
            "Active" -> categoryFiltered.filter { !it.isCompleted }
            "Completed" -> categoryFiltered.filter { it.isCompleted }
            "Meetings" -> categoryFiltered.filter { it.category == "Meetings" }
            else -> categoryFiltered
        }
        
        // Finally filter by search query if active
        return if (searchQuery.isNotEmpty()) {
            statusFiltered.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.description.contains(searchQuery, ignoreCase = true)
            }
        } else {
            statusFiltered
        }
    }
    
    // Function to load sample todos (fallback)
    fun loadSampleTodos() {
        todos = when (userRole) {
            "teacher" -> listOf(
                TodoItem(
                    id = UUID.randomUUID().toString(),
                    title = "Grade Math Assignments",
                    description = "Review and grade homework for grade 10",
                    dueDate = "2023-07-15",
                    dueTime = "18:00",
                    priority = TodoItem.Priority.HIGH,
                    category = "Grading",
                    isCompleted = false
                ),
                TodoItem(
                    id = UUID.randomUUID().toString(),
                    title = "Prepare Lesson Plan",
                    description = "Next week's algebra lessons",
                    dueDate = "2023-07-18",
                    dueTime = "10:00",
                    priority = TodoItem.Priority.MEDIUM,
                    category = "Classes",
                    isCompleted = false
                ),
                TodoItem(
                    id = UUID.randomUUID().toString(),
                    title = "Parent-Teacher Meeting",
                    description = "Meeting with student parents to discuss progress",
                    dueDate = "2023-07-20",
                    dueTime = "16:30",
                    priority = TodoItem.Priority.HIGH,
                    category = "Meetings",
                    isCompleted = true
                )
            )
            "admin" -> listOf(
                TodoItem(
                    id = UUID.randomUUID().toString(),
                    title = "Budget Review",
                    description = "Review quarterly budget with finance team",
                    dueDate = "2023-07-15",
                    dueTime = "14:00",
                    priority = TodoItem.Priority.HIGH,
                    category = "Administrative",
                    isCompleted = false
                ),
                TodoItem(
                    id = UUID.randomUUID().toString(),
                    title = "Staff Meeting",
                    description = "Monthly all-staff meeting",
                    dueDate = "2023-07-18",
                    dueTime = "09:00",
                    priority = TodoItem.Priority.MEDIUM,
                    category = "Staff",
                    isCompleted = false
                ),
                TodoItem(
                    id = UUID.randomUUID().toString(),
                    title = "School Event Planning",
                    description = "Finalize details for upcoming school event",
                    dueDate = "2023-07-20",
                    dueTime = "15:00",
                    priority = TodoItem.Priority.HIGH,
                    category = "Events",
                    isCompleted = true
                )
            )
            else -> listOf(
                TodoItem(
                    id = UUID.randomUUID().toString(),
                    title = "Complete Math Homework",
                    description = "Finish exercises 10-15 on page 47",
                    dueDate = "2023-07-15",
                    dueTime = "18:00",
                    priority = TodoItem.Priority.HIGH,
                    category = "School",
                    isCompleted = false
                ),
                TodoItem(
                    id = UUID.randomUUID().toString(),
                    title = "Prepare for Science Test",
                    description = "Review chapters 4-6",
                    dueDate = "2023-07-18",
                    dueTime = "10:00",
                    priority = TodoItem.Priority.MEDIUM,
                    category = "School",
                    isCompleted = false
                ),
                TodoItem(
                    id = UUID.randomUUID().toString(),
                    title = "Submit English Essay",
                    description = "Final draft of analytical essay",
                    dueDate = "2023-07-20",
                    dueTime = "23:59",
                    priority = TodoItem.Priority.HIGH,
                    category = "School",
                    isCompleted = true
                )
            )
        }
    }
    
    // Function to load todos from Firebase
    fun loadTodosFromFirebase(userId: String) {
        isLoading = true
        firebaseService.getTodoItemsForUser(
            userId = userId,
            onSuccess = { todoItems ->
                todos = todoItems
                isLoading = false
            },
            onError = { exception ->
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to load todos: ${exception.message}")
                }
                isLoading = false
                // Load sample data as fallback
                loadSampleTodos()
            }
        )
    }
    
    // Update selected category when user role changes to match the first category for that role
    LaunchedEffect(userRole) {
        selectedCategory = when (userRole) {
            "teacher" -> "All"
            "admin" -> "All"
            else -> "All"
        }
    }
    
    // Load user role
    LaunchedEffect(Unit) {
        firebaseService.getCurrentUserData { userData ->
            userRole = userData?.role
            currentUserId = userData?.uid
            
            // Load todos after getting user data
            if (userData?.uid != null) {
                loadTodosFromFirebase(userData.uid)
            } else {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("To-Do List") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { searchActive = !searchActive }) {
                            Icon(
                                if (searchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (searchActive) "Close Search" else "Search Tasks"
                            )
                        }
                        IconButton(onClick = { showAddTaskDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Task")
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
                        placeholder = { Text("Search tasks...") },
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.RequestMeeting.route) },
                icon = { Icon(Icons.Default.Event, contentDescription = null) },
                text = { Text("Request Meeting") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Category chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            leadingIcon = if (selectedCategory == category) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
                
                // Status filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            leadingIcon = if (selectedFilter == filter) {
                                { Icon(
                                    when(filter) {
                                        "Active" -> Icons.Default.CheckCircleOutline
                                        "Completed" -> Icons.Default.CheckCircle
                                        "Meetings" -> Icons.Default.Event
                                        else -> Icons.Default.Check
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                ) }
                            } else null
                        )
                    }
                }
                
                // Display filtered todos
                val filteredTodos = filterTodos()
                if (filteredTodos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) 
                                "No tasks matching \"$searchQuery\"" 
                            else 
                                "No tasks found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filteredTodos,
                            key = { todo -> todo.id }
                        ) { todo ->
                            TodoItemCard(
                                todoItem = todo,
                                onCompleteToggle = { todoItem ->
                                    // Update todo item in memory
                                    val updatedTodos = todos.map { 
                                        if (it.id == todoItem.id) it.copy(isCompleted = !it.isCompleted) else it 
                                    }
                                    todos = updatedTodos
                                    
                                    // Update in Firebase
                                    currentUserId?.let { userId ->
                                        val updatedTodo = updatedTodos.first { it.id == todoItem.id }
                                        firebaseService.updateTodoItem(
                                            todoItem = updatedTodo,
                                            userId = userId,
                                            onSuccess = {
                                                if (todoItem.isCompleted) {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Task marked as not completed")
                                                    }
                                                } else {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Task completed!")
                                                    }
                                                }
                                            },
                                            onError = { exception ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Failed to update task: ${exception.message}")
                                                }
                                            }
                                        )
                                    }
                                },
                                onDelete = { todoItem ->
                                    // Update todo item in memory
                                    todos = todos.filter { it.id != todoItem.id }
                                    
                                    // Delete from Firebase
                                    currentUserId?.let { userId ->
                                        firebaseService.deleteTodoItem(
                                            todoId = todoItem.id,
                                            userId = userId,
                                            onSuccess = {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Task deleted")
                                                }
                                            },
                                            onError = { exception ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Failed to delete task: ${exception.message}")
                                                }
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier,
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
        
        // Add Task Dialog
        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismiss = { showAddTaskDialog = false },
                onTaskAdded = { newTask ->
                    // Add to memory
                    todos = todos + newTask
                    showAddTaskDialog = false
                    
                    // Save to Firebase
                    currentUserId?.let { userId ->
                        firebaseService.addTodoItem(
                            todoItem = newTask,
                            userId = userId,
                            onSuccess = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Task added")
                                }
                            },
                            onError = { exception ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("Failed to add task: ${exception.message}")
                                }
                            }
                        )
                    }
                },
                categories = categories
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoItemCard(
    todoItem: TodoItem,
    onCompleteToggle: (TodoItem) -> Unit,
    onDelete: (TodoItem) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavController? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val isMeetingTodo = todoItem.meetingId != null && todoItem.meetingId.isNotEmpty()
    
    // Extract meeting status from the title if it's a meeting todo
    val meetingStatus = when {
        todoItem.title.contains("(Accepted)") -> "Accepted"
        todoItem.title.contains("(Rescheduled)") -> "Rescheduled"
        todoItem.title.contains("(Needs Review)") -> "Needs Review"
        todoItem.title.contains("(Pending)") -> "Pending"
        isMeetingTodo -> "Meeting"
        else -> null
    }
    
    // Determine background color based on meeting status
    val backgroundColor = when {
        todoItem.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        meetingStatus == "Needs Review" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        meetingStatus == "Rescheduled" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        meetingStatus == "Accepted" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        todoItem.priority == TodoItem.Priority.HIGH -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        todoItem.priority == TodoItem.Priority.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { 
                // If this is a meeting todo and we have a navController, navigate to meeting details
                if (isMeetingTodo && navController != null) {
                    navController.navigate(Screen.MeetingDetails.createRoute(todoItem.meetingId!!))
                } else {
                    expanded = !expanded
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = todoItem.isCompleted,
                        onCheckedChange = { onCompleteToggle(todoItem) }
                    )
                    
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                // Clean up the title by removing status marker for display
                                text = todoItem.title.replace(Regex("\\([^)]*\\):\\s*"), ""),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    textDecoration = if (todoItem.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (todoItem.isCompleted) 
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Meeting indicator
                            if (isMeetingTodo) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Event,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = when (meetingStatus) {
                                            "Needs Review" -> MaterialTheme.colorScheme.error
                                            "Rescheduled" -> MaterialTheme.colorScheme.tertiary
                                            "Accepted" -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.outline
                                        }
                                    )
                                    
                                    meetingStatus?.let {
                                        Text(
                                            text = it,
                                            fontSize = 12.sp,
                                            color = when (meetingStatus) {
                                                "Needs Review" -> MaterialTheme.colorScheme.error
                                                "Rescheduled" -> MaterialTheme.colorScheme.tertiary
                                                "Accepted" -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.outline
                                            }
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                            
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                todoItem.dueDate,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                todoItem.dueTime,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Add a special icon for meeting todos
                if (isMeetingTodo) {
                    IconButton(
                        onClick = { 
                            if (navController != null) {
                                navController.navigate(Screen.MeetingDetails.createRoute(todoItem.meetingId!!))
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = "View Meeting",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    IconButton(onClick = { onDelete(todoItem) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 40.dp, top = 8.dp)
                ) {
                    Text(
                        text = todoItem.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onTaskAdded: (TodoItem) -> Unit,
    categories: List<String>
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TodoItem.Priority.MEDIUM) }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var dueDate by remember { mutableStateOf(LocalDate.now()) }
    var dueTime by remember { mutableStateOf(LocalTime.of(23, 59)) }
    
    // Using DatePicker and TimePicker from Material 3 instead of sheets-compose-dialogs
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    // Date Picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate.toEpochDay() * 24 * 60 * 60 * 1000
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    datePickerState.selectedDateMillis?.let { millis ->
                        dueDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
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
    
    // Time Picker
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = dueTime.hour,
            initialMinute = dueTime.minute
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
                            dueTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss
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
                    "Add New Task",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                // Categories dropdown
                var categoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        readOnly = true,
                        value = selectedCategory,
                        onValueChange = { },
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }
                    )
                    
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = { 
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Due date and time buttons
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
                            Text(dueDate.format(DateTimeFormatter.ISO_DATE))
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
                            Text(dueTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                        }
                    }
                }
                
                // Priority selection
                Text("Priority", style = MaterialTheme.typography.bodyLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PriorityButton(
                        priority = TodoItem.Priority.LOW,
                        selectedPriority = selectedPriority,
                        onClick = { selectedPriority = TodoItem.Priority.LOW }
                    )
                    PriorityButton(
                        priority = TodoItem.Priority.MEDIUM,
                        selectedPriority = selectedPriority,
                        onClick = { selectedPriority = TodoItem.Priority.MEDIUM }
                    )
                    PriorityButton(
                        priority = TodoItem.Priority.HIGH,
                        selectedPriority = selectedPriority,
                        onClick = { selectedPriority = TodoItem.Priority.HIGH }
                    )
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val newTask = TodoItem(
                                    id = UUID.randomUUID().toString(),
                                    title = title,
                                    description = description,
                                    dueDate = dueDate.format(DateTimeFormatter.ISO_DATE),
                                    dueTime = dueTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                    priority = selectedPriority,
                                    category = selectedCategory,
                                    isCompleted = false
                                )
                                onTaskAdded(newTask)
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text("Add Task")
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityButton(
    priority: TodoItem.Priority,
    selectedPriority: TodoItem.Priority,
    onClick: () -> Unit
) {
    val (color, text) = when (priority) {
        TodoItem.Priority.LOW -> Pair(Color.Green.copy(alpha = 0.7f), "Low")
        TodoItem.Priority.MEDIUM -> Pair(Color.Yellow.copy(alpha = 0.7f), "Medium")
        TodoItem.Priority.HIGH -> Pair(Color.Red.copy(alpha = 0.7f), "High")
    }
    
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                if (selectedPriority == priority) color else Color.Gray.copy(alpha = 0.3f)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            color = if (selectedPriority == priority) 
                Color.Black 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
} 