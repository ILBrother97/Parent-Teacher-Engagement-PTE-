package com.example.parent_teacher_engagement.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.parent_teacher_engagement.R
import com.example.parent_teacher_engagement.firebase.FirebaseService
import com.example.parent_teacher_engagement.navigation.Screen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ProblemReport(
    val id: String,
    val userId: String,
    val userName: String,
    val userRole: String,
    val type: String, // "system" or "user"
    val reportType: String? = null, // "teacher" or "parent" (for user reports)
    val reportedUserEmail: String? = null,
    val description: String,
    val timestamp: Long,
    val status: String // "pending", "in_progress", "resolved"
)

/**
 * Initial screen that shows options to view problems reported by teachers or parents
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportedProblemsListScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reported_problems)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = stringResource(R.string.select_role),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { navController.navigate(Screen.ReportedProblemsByRole.createRoute("teacher")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.problems_from_teachers),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            
            Button(
                onClick = { navController.navigate(Screen.ReportedProblemsByRole.createRoute("parent")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.problems_from_parents),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}

/**
 * Screen that shows system problems or user problems based on the selected role
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportedProblemsByRoleScreen(
    navController: NavController,
    role: String
) {
    val title = if (role == "teacher") {
        stringResource(R.string.problems_from_teachers)
    } else {
        stringResource(R.string.problems_from_parents)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = stringResource(R.string.select_role),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { navController.navigate(Screen.ReportedProblemsByType.createRoute(role, "system")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.system_problems),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            
            Button(
                onClick = { navController.navigate(Screen.ReportedProblemsByType.createRoute(role, "user")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.user_problems),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}

/**
 * Screen that lists problems by role and type, allowing the admin to click and view details
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportedProblemsByTypeScreen(
    navController: NavController,
    role: String,
    type: String
) {
    val firebaseService = FirebaseService.getInstance()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // State for problems list
    var problemsList by remember { mutableStateOf<List<ProblemReport>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Title based on role and type
    val title = when {
        role == "teacher" && type == "system" -> "System Problems from Teachers"
        role == "teacher" && type == "user" -> "User Problems from Teachers"
        role == "parent" && type == "system" -> "System Problems from Parents"
        else -> "User Problems from Parents"
    }
    
    // Load problems when screen is first shown
    LaunchedEffect(role, type) {
        // Simulate API call to fetch problems by role and type
        // In a real app, this would be a call to Firebase
        isLoading = true
        
        // Example data loading logic:
        firebaseService.getProblemReportsByRoleAndType(role, type) { problems, error ->
            isLoading = false
            if (error != null) {
                scope.launch {
                    snackbarHostState.showSnackbar("Error loading problems: ${error.message}")
                }
                return@getProblemReportsByRoleAndType
            }
            
            problemsList = problems
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (problemsList.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_problems_found),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(problemsList) { problem ->
                        ProblemReportCard(
                            problem = problem,
                            onClick = {
                                navController.navigate(Screen.ReportedProblemDetails.createRoute(problem.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen that shows the details of a specific problem and allows the admin to mark it as resolved or in progress
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportedProblemDetailsScreen(
    navController: NavController,
    problemId: String
) {
    val firebaseService = FirebaseService.getInstance()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // State for problem details
    var problem by remember { mutableStateOf<ProblemReport?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }
    
    // Load problem details when screen is first shown
    LaunchedEffect(problemId) {
        // Simulate API call to fetch problem details
        // In a real app, this would be a call to Firebase
        isLoading = true
        
        // Example data loading logic:
        firebaseService.getProblemReportById(problemId) { retrievedProblem, error ->
            isLoading = false
            if (error != null) {
                scope.launch {
                    snackbarHostState.showSnackbar("Error loading problem: ${error.message}")
                }
                return@getProblemReportById
            }
            
            problem = retrievedProblem
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.problem_details)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (problem == null) {
                Text(
                    text = "Problem not found",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Problem header
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (problem?.type == "system") "System Problem" else "User Problem",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = stringResource(
                                    R.string.reporter_info, 
                                    problem?.userName ?: "", 
                                    problem?.userRole?.capitalize() ?: ""
                                ),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            Text(
                                text = stringResource(
                                    R.string.reported_date,
                                    formatTimestamp(problem?.timestamp ?: 0)
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            val statusColor = when (problem?.status) {
                                "resolved" -> Color.Green
                                "in_progress" -> Color.Blue
                                else -> Color.Red
                            }
                            
                            Text(
                                text = stringResource(
                                    R.string.problem_status,
                                    problem?.status?.capitalize() ?: ""
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                    
                    // Additional info for user problems
                    if (problem?.type == "user" && !problem?.reportedUserEmail.isNullOrEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.reported_user,
                                        problem?.reportedUserEmail ?: ""
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = "Report Type: ${problem?.reportType?.capitalize() ?: ""}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    
                    // Problem description
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.problem_description),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = problem?.description ?: "",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                // Update status to in_progress
                                firebaseService.updateProblemStatus(
                                    problemId = problemId,
                                    status = "in_progress",
                                    onSuccess = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Status updated to In Progress")
                                            // Update local state
                                            problem = problem?.copy(status = "in_progress")
                                        }
                                    },
                                    onError = { error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Error: ${error.message}")
                                        }
                                    }
                                )
                            },
                            enabled = problem?.status != "in_progress",
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mark In Progress")
                        }
                        
                        Button(
                            onClick = {
                                // Update status to resolved
                                firebaseService.updateProblemStatus(
                                    problemId = problemId,
                                    status = "resolved",
                                    onSuccess = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Status updated to Resolved")
                                            
                                            // Refresh dashboard counts when a problem is resolved
                                            firebaseService.getAllCounts { _ -> }
                                            
                                            // Update local state
                                            problem = problem?.copy(status = "resolved")
                                        }
                                    },
                                    onError = { error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Error: ${error.message}")
                                        }
                                    }
                                )
                            },
                            enabled = problem?.status != "resolved",
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mark Resolved")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemReportCard(
    problem: ProblemReport,
    onClick: () -> Unit
) {
    val statusColor = when (problem.status) {
        "resolved" -> Color.Green
        "in_progress" -> Color.Blue
        else -> Color.Red
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on problem type
            Icon(
                imageVector = if (problem.type == "system") Icons.Default.Computer else Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = problem.userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = formatTimestamp(problem.timestamp),
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = problem.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Status badge
            Surface(
                shape = MaterialTheme.shapes.small,
                color = statusColor.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = problem.status.capitalize(),
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// Helper functions
private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
} 