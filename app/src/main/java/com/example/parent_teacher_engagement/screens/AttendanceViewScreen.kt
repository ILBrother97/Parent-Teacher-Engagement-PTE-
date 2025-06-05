package com.example.parent_teacher_engagement.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*
import com.example.parent_teacher_engagement.firebase.FirebaseService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceViewScreen(
    studentId: String,
    navController: NavController
) {
    var studentData by remember { mutableStateOf<FirebaseService.StudentData?>(null) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedPeriod by remember { mutableStateOf<Int?>(null) }
    var attendanceData by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var showPeriodDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val firebaseService = remember { FirebaseService.getInstance() }
    
    // Load student data
    LaunchedEffect(studentId) {
        firebaseService.getStudentDetails(studentId) { student ->
            studentData = student
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Attendance for ${studentData?.name ?: "Student"}"
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                studentData == null -> {
                    Text(
                        text = "Student data not found",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Student info card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = studentData?.name ?: "",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Grade: ${studentData?.grade ?: ""}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        
                        // Date selection button
                        Button(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedDate != null) 
                                    SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate!!)) 
                                else 
                                    "Select Date"
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Display attendance data if available
                        if (selectedDate != null && selectedPeriod != null) {
                            if (attendanceData.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No attendance records found for this date and period",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate!!)),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Period $selectedPeriod",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Status: ",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            val isPresent = attendanceData.values.firstOrNull() ?: false
                                            Icon(
                                                imageVector = if (isPresent) Icons.Default.CheckCircle else Icons.Default.Info,
                                                contentDescription = if (isPresent) "Present" else "Absent",
                                                tint = if (isPresent) Color.Green else Color.Red
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isPresent) "Present" else "Absent",
                                                color = if (isPresent) Color.Green else Color.Red,
                                                fontWeight = FontWeight.Bold
                                            )
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
                        showPeriodDialog = true  // Show period picker after date selection
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
    
    // Period selection dialog
    if (showPeriodDialog) {
        AlertDialog(
            onDismissRequest = { showPeriodDialog = false },
            title = { Text("Select Period") },
            text = {
                Column {
                    (1..7).forEach { period ->
                        TextButton(
                            onClick = {
                                selectedPeriod = period
                                showPeriodDialog = false
                                // Fetch attendance data with both date and period
                                selectedDate?.let { date ->
                                    firebaseService.getStudentAttendance(
                                        studentId = studentId,
                                        date = SimpleDateFormat("yyyy-MM-dd").format(Date(date)),
                                        period = period
                                    ) { attendance: Map<String, Boolean> ->
                                        attendanceData = attendance
                                    }
                                }
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
                TextButton(onClick = { showPeriodDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
} 