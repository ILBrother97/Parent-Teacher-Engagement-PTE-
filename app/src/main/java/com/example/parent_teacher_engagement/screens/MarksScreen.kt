package com.example.parent_teacher_engagement.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.parent_teacher_engagement.firebase.FirebaseService
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarksScreen(navController: NavController) {
    var selectedType by remember { mutableStateOf<String?>(null) } // "Grade" or "Progress"
    var selectedClass by remember { mutableStateOf<String?>(null) }
    var selectedSemester by remember { mutableStateOf<String?>(null) }
    var teacherClasses by remember { mutableStateOf<List<String>>(emptyList()) }
    var classStudents by remember { mutableStateOf<List<FirebaseService.UserListData>>(emptyList()) }
    var studentMarks by remember { mutableStateOf<Map<String, Map<String, String>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedStudent by remember { mutableStateOf<FirebaseService.UserListData?>(null) }
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var editedMark by remember { mutableStateOf("") }
    var assessmentDetails by remember { mutableStateOf<List<FirebaseService.AssessmentDetail>>(emptyList()) }
    var editedAssessments by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var teacherSubject by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Load teacher's classes and subject
    LaunchedEffect(Unit) {
        FirebaseService.getInstance().getCurrentUserData { userData ->
            if (userData?.uid != null) {
                teacherSubject = userData.subject
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
                title = { Text("Student ${selectedType ?: "Marks"}") },
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
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Initial Type Selection (Grade/Progress)
                if (selectedType == null) {
                    Text(
                        text = "Select Type",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedType = "Grade" }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Grade,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Grade",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Select"
                                )
                            }
                        }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedType = "Progress" }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Progress",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Select"
                                )
                            }
                        }
                    }
                }
                // Class Selection
                else if (selectedClass == null) {
                    Text(
                        text = "Select a Class",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(teacherClasses) { className ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedClass = className
                                        // Load students for the selected class
                                        FirebaseService.getInstance().getStudentsInClass(className) { students ->
                                            classStudents = students
                                            // Load teacher's submitted marks for each student
                                            students.forEach { student ->
                                                FirebaseService.getInstance().getTeacherSubmittedMarks(
                                                    studentId = student.id,
                                                    teacherId = FirebaseService.getInstance().currentUserData?.uid ?: "",
                                                    type = selectedType ?: "Grade",
                                                    semester = selectedSemester ?: "Semester One"
                                                ) { marks ->
                                                    studentMarks = studentMarks.toMutableMap().apply {
                                                        put(student.id, marks)
                                                    }
                                                }
                                            }
                                        }
                                    }
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
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "Grade $className",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Select"
                                    )
                                }
                            }
                        }
                    }
                }
                // Semester Selection
                else if (selectedSemester == null) {
                    Text(
                        text = "Select Semester",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { 
                                    selectedSemester = "Semester One"
                                    // Reload marks for the selected semester
                                    classStudents.forEach { student ->
                                        FirebaseService.getInstance().getTeacherSubmittedMarks(
                                            studentId = student.id,
                                            teacherId = FirebaseService.getInstance().currentUserData?.uid ?: "",
                                            type = selectedType ?: "Grade",
                                            semester = "Semester One"
                                        ) { marks ->
                                            studentMarks = studentMarks.toMutableMap().apply {
                                                put(student.id, marks)
                                            }
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Semester One",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Select"
                                )
                            }
                        }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { 
                                    selectedSemester = "Semester Two"
                                    // Reload marks for the selected semester
                                    classStudents.forEach { student ->
                                        FirebaseService.getInstance().getTeacherSubmittedMarks(
                                            studentId = student.id,
                                            teacherId = FirebaseService.getInstance().currentUserData?.uid ?: "",
                                            type = selectedType ?: "Grade",
                                            semester = "Semester Two"
                                        ) { marks ->
                                            studentMarks = studentMarks.toMutableMap().apply {
                                                put(student.id, marks)
                                            }
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Semester Two",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Select"
                                )
                            }
                        }
                    }
                }
                // Display Marks
                else {
                    Text(
                        text = "${selectedType} for Grade ${selectedClass} - $selectedSemester",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(classStudents) { student ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = student.name ?: "Unknown Student",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    val studentGradeMap = studentMarks[student.id] ?: emptyMap()
                                    studentGradeMap.forEach { (subject, grade) ->
                                        // Only show the teacher's assigned subject
                                        if (subject == teacherSubject) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = subject,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = grade,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            selectedStudent = student
                                                            selectedSubject = subject
                                                            editedMark = grade
                                                            // Load assessment details
                                                            if (selectedType == "Grade") {
                                                                FirebaseService.getInstance().getAssessmentDetails(
                                                                    studentId = student.id,
                                                                    semester = selectedSemester ?: "Semester One",
                                                                    subject = subject
                                                                ) { details ->
                                                                    assessmentDetails = details
                                                                    editedAssessments = details.associate { it.name to it.score }
                                                                    showEditDialog = true
                                                                }
                                                            } else {
                                                                FirebaseService.getInstance().getProgressAssessmentDetails(
                                                                    studentId = student.id,
                                                                    semester = selectedSemester ?: "Semester One",
                                                                    subject = subject
                                                                ) { details ->
                                                                    assessmentDetails = details
                                                                    editedAssessments = details.associate { it.name to it.score }
                                                                    showEditDialog = true
                                                                }
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Edit"
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            // Delete the mark
                                                            FirebaseService.getInstance().deleteTeacherSubmittedMark(
                                                                studentId = student.id,
                                                                teacherId = FirebaseService.getInstance().currentUserData?.uid ?: "",
                                                                type = selectedType ?: "Grade",
                                                                semester = selectedSemester ?: "Semester One",
                                                                subject = subject
                                                            ) {
                                                                // Update local state
                                                                val updatedMarks = studentMarks[student.id]?.toMutableMap() ?: mutableMapOf()
                                                                updatedMarks.remove(subject)
                                                                studentMarks = studentMarks.toMutableMap().apply {
                                                                    put(student.id, updatedMarks)
                                                                }
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete"
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    if (studentGradeMap.isEmpty()) {
                                        Text(
                                            text = "No ${selectedType?.lowercase()} available",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error
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
    
    // Edit Dialog
    if (showEditDialog && selectedStudent != null && selectedSubject != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit ${selectedType}") },
            text = {
                Column {
                    Text("Student: ${selectedStudent?.name}")
                    Text("Subject: $selectedSubject")
                    
                    // Display and edit assessments
                    assessmentDetails.forEach { assessment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${assessment.name} (${assessment.maxPoints})",
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editedAssessments[assessment.name]?.toString() ?: "",
                                onValueChange = { value ->
                                    val score = value.toIntOrNull() ?: 0
                                    if (score <= assessment.maxPoints) {
                                        editedAssessments = editedAssessments.toMutableMap().apply {
                                            put(assessment.name, score)
                                        }
                                        // Update total mark
                                        editedMark = editedAssessments.values.sum().toString()
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(100.dp)
                            )
                        }
                    }
                    
                    // Display total
                    val totalMarks = assessmentDetails.sumOf { it.maxPoints }
                    val totalScore = editedAssessments.values.sum()
                    Text(
                        "Total: $totalScore/$totalMarks",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Validate total marks
                        val totalMarks = assessmentDetails.sumOf { it.maxPoints }
                        val totalScore = editedAssessments.values.sum()
                        
                        if (totalScore > totalMarks) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Total score cannot exceed $totalMarks")
                            }
                            return@TextButton
                        }
                        
                        // Update the mark in Firebase
                        FirebaseService.getInstance().updateTeacherSubmittedMark(
                            studentId = selectedStudent?.id ?: "",
                            teacherId = FirebaseService.getInstance().currentUserData?.uid ?: "",
                            type = selectedType ?: "Grade",
                            semester = selectedSemester ?: "Semester One",
                            subject = selectedSubject ?: "",
                            newMark = editedMark
                        ) {
                            // Update assessments
                            val assessmentPath = if (selectedType == "Grade") "assessments" else "progress_assessments"
                            val assessmentRef = FirebaseService.getInstance().database.reference
                                .child(assessmentPath)
                                .child(selectedSemester ?: "Semester One")
                                .child(selectedStudent?.id ?: "")
                                .child(selectedSubject ?: "")
                            
                            assessmentRef.setValue(editedAssessments)
                                .addOnSuccessListener {
                                    // Update local state
                                    val updatedMarks = studentMarks[selectedStudent?.id]?.toMutableMap() ?: mutableMapOf()
                                    updatedMarks[selectedSubject ?: ""] = editedMark
                                    studentMarks = studentMarks.toMutableMap().apply {
                                        put(selectedStudent?.id ?: "", updatedMarks)
                                    }
                                    showEditDialog = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Mark updated successfully")
                                    }
                                }
                                .addOnFailureListener { error ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Error updating mark: ${error.message}")
                                    }
                                }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
} 