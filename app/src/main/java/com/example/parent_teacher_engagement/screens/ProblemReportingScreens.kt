package com.example.parent_teacher_engagement.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.parent_teacher_engagement.R
import com.example.parent_teacher_engagement.firebase.FirebaseService
import com.example.parent_teacher_engagement.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemTypeSelectionScreen(
    navController: NavController,
    userRole: String
) {
    val firebaseService = FirebaseService.getInstance()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.problem_type_selection)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = stringResource(R.string.problem_type_selection),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // System Problem Button
            Button(
                onClick = { navController.navigate(Screen.SystemProblemReport.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = stringResource(R.string.report_system_problem),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            
            // User Problem Button (Teachers or Parents depending on role)
            Button(
                onClick = {
                    val reportType = if (userRole == "parent") "teacher" else "parent"
                    navController.navigate(Screen.UserProblemReport.createRoute(reportType))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    val reportText = if (userRole == "parent") {
                        stringResource(R.string.report_teacher_problem)
                    } else {
                        stringResource(R.string.report_parent_problem)
                    }
                    Text(
                        text = reportText,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemProblemReportScreen(
    navController: NavController
) {
    val firebaseService = FirebaseService.getInstance()
    val currentUser = firebaseService.currentUserData
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    
    // Store string resources in variables
    val reportSubmittedMessage = stringResource(R.string.report_submitted)
    val reportSubmissionFailedMessage = stringResource(R.string.report_submission_failed)
    
    // For handling success and error messages
    var submissionSuccessful by remember { mutableStateOf(false) }
    var submissionError by remember { mutableStateOf<Exception?>(null) }
    
    // Effect to show messages after submission
    LaunchedEffect(submissionSuccessful, submissionError) {
        if (submissionSuccessful) {
            snackbarHostState.showSnackbar(reportSubmittedMessage)
            submissionSuccessful = false
            // Navigate back after showing success message
            navController.popBackStack()
        }
        
        submissionError?.let { error ->
            snackbarHostState.showSnackbar(
                "$reportSubmissionFailedMessage: ${error.message}"
            )
            submissionError = null
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_system_problem)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.system_problem),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.problem_description)) },
                placeholder = { Text(stringResource(R.string.enter_problem_description)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                maxLines = 10
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (description.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please enter a description")
                        }
                        return@Button
                    }
                    
                    if (currentUser == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar("User information not available")
                        }
                        return@Button
                    }
                    
                    isSubmitting = true
                    
                    firebaseService.submitSystemProblemReport(
                        userId = currentUser.uid,
                        userRole = currentUser.role ?: "",
                        userName = currentUser.name ?: "",
                        description = description,
                        onSuccess = {
                            isSubmitting = false
                            submissionSuccessful = true
                        },
                        onError = { exception ->
                            isSubmitting = false
                            submissionError = exception
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.submit_report))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProblemReportScreen(
    navController: NavController,
    reportType: String
) {
    val firebaseService = FirebaseService.getInstance()
    val currentUser = firebaseService.currentUserData
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var userEmail by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    
    // Store string resources in variables
    val reportSubmittedMessage = stringResource(R.string.report_submitted)
    val reportSubmissionFailedMessage = stringResource(R.string.report_submission_failed)
    
    // For handling success and error messages
    var submissionSuccessful by remember { mutableStateOf(false) }
    var submissionError by remember { mutableStateOf<Exception?>(null) }
    
    // Effect to show messages after submission
    LaunchedEffect(submissionSuccessful, submissionError) {
        if (submissionSuccessful) {
            snackbarHostState.showSnackbar(reportSubmittedMessage)
            submissionSuccessful = false
            // Navigate back after showing success message
            navController.popBackStack()
        }
        
        submissionError?.let { error ->
            snackbarHostState.showSnackbar(
                "$reportSubmissionFailedMessage: ${error.message}"
            )
            submissionError = null
        }
    }
    
    // Set title based on report type
    val titleRes = if (reportType == "teacher") {
        R.string.report_teacher_problem
    } else {
        R.string.report_parent_problem
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = userEmail,
                onValueChange = { userEmail = it },
                label = { Text(stringResource(R.string.problem_user_email)) },
                placeholder = { Text(stringResource(R.string.enter_user_email)) },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.problem_description)) },
                placeholder = { Text(stringResource(R.string.enter_problem_description)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                maxLines = 10
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (userEmail.isBlank() || description.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please fill all fields")
                        }
                        return@Button
                    }
                    
                    if (!userEmail.contains("@") || !userEmail.contains(".")) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please enter a valid email")
                        }
                        return@Button
                    }
                    
                    if (currentUser == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar("User information not available")
                        }
                        return@Button
                    }
                    
                    isSubmitting = true
                    
                    firebaseService.submitUserProblemReport(
                        userId = currentUser.uid,
                        userRole = currentUser.role ?: "",
                        userName = currentUser.name ?: "",
                        reportedUserEmail = userEmail,
                        reportType = reportType,
                        description = description,
                        onSuccess = {
                            isSubmitting = false
                            submissionSuccessful = true
                        },
                        onError = { exception ->
                            isSubmitting = false
                            submissionError = exception
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.submit_report))
            }
        }
    }
} 