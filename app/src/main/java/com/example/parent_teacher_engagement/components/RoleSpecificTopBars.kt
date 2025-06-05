package com.example.parent_teacher_engagement.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.parent_teacher_engagement.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTopBar(
    title: String,
    onMenuClick: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                // Add Users Section
                DropdownMenuItem(
                    text = { Text("Add Teacher") },
                    leadingIcon = { Icon(Icons.Default.Add, "Add Teacher") },
                    onClick = {
                        onMenuClick("AddTeacher")
                        showMenu = false
                    }
                )
                
                DropdownMenuItem(
                    text = { Text("Add Parent") },
                    leadingIcon = { Icon(Icons.Default.Add, "Add Parent") },
                    onClick = {
                        onMenuClick("AddParent")
                        showMenu = false
                    }
                )
                
                DropdownMenuItem(
                    text = { Text("Add Student") },
                    leadingIcon = { Icon(Icons.Default.Add, "Add Student") },
                    onClick = {
                        onMenuClick("AddStudent")
                        showMenu = false
                    }
                )
                
                HorizontalDivider()
                
                // View Lists Section
                DropdownMenuItem(
                    text = { Text("List of Teachers") },
                    leadingIcon = { Icon(Icons.Default.List, "List Teachers") },
                    onClick = {
                        onMenuClick("ListTeachers")
                        showMenu = false
                    }
                )
                
                DropdownMenuItem(
                    text = { Text("List of Parents") },
                    leadingIcon = { Icon(Icons.Default.List, "List Parents") },
                    onClick = {
                        onMenuClick("ListParents")
                        showMenu = false
                    }
                )
                
                DropdownMenuItem(
                    text = { Text("List of Students") },
                    leadingIcon = { Icon(Icons.Default.List, "List Students") },
                    onClick = {
                        onMenuClick("ListStudents")
                        showMenu = false
                    }
                )
                
                HorizontalDivider()
                
                // Events Section
                DropdownMenuItem(
                    text = { Text("Post Upcoming Events") },
                    leadingIcon = { Icon(Icons.Default.DateRange, "Post Events") },
                    onClick = {
                        onMenuClick("PostEvents")
                        showMenu = false
                    }
                )
                
                HorizontalDivider()
                
                // Change Password
                DropdownMenuItem(
                    text = { Text("Change Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, "Change Password") },
                    onClick = {
                        onMenuClick("ChangePassword")
                        showMenu = false
                    }
                )
                
                HorizontalDivider()
                
                // Logout
                DropdownMenuItem(
                    text = { Text("Logout") },
                    leadingIcon = { Icon(Icons.Default.ExitToApp, "Logout") },
                    onClick = {
                        onLogoutClick()
                        showMenu = false
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentTopBar(
    title: String,
    onMenuClick: (String) -> Unit,
    onLogoutClick: () -> Unit,
    notificationBell: @Composable () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                // View Grades
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.view_grades)) },
                    leadingIcon = { Icon(Icons.Default.Star, stringResource(R.string.view_grades)) },
                    onClick = {
                        onMenuClick("ViewGrades")
                        showMenu = false
                    }
                )
                
                // View Attendance
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.view_attendance)) },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, stringResource(R.string.view_attendance)) },
                    onClick = {
                        onMenuClick("ViewAttendance")
                        showMenu = false
                    }
                )
                
                // View Events
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.view_events)) },
                    leadingIcon = { Icon(Icons.Default.DateRange, stringResource(R.string.view_events)) },
                    onClick = {
                        onMenuClick("ViewEvents")
                        showMenu = false
                    }
                )
                
                // Contact Teacher
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.contact_teacher)) },
                    leadingIcon = { Icon(Icons.Default.Email, stringResource(R.string.contact_teacher)) },
                    onClick = {
                        onMenuClick("ContactTeacher")
                        showMenu = false
                    }
                )
                
                HorizontalDivider()
                
                // Change Password
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.change_password)) },
                    leadingIcon = { Icon(Icons.Default.Lock, stringResource(R.string.change_password)) },
                    onClick = {
                        onMenuClick("ChangePassword")
                        showMenu = false
                    }
                )
                
                HorizontalDivider()
                
                // Logout
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.logout)) },
                    leadingIcon = { Icon(Icons.Default.ExitToApp, stringResource(R.string.logout)) },
                    onClick = {
                        onLogoutClick()
                        showMenu = false
                    }
                )
            }
        },
        actions = {
            // Add notification bell
            notificationBell()
            
            // Language button
            IconButton(onClick = { onMenuClick("Language") }) {
                Icon(Icons.Default.Language, contentDescription = "Change Language")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherTopBar(
    title: String,
    onMenuClick: (String) -> Unit,
    onLogoutClick: () -> Unit,
    notificationBell: @Composable () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                // Class Management Section
                DropdownMenuItem(
                    text = { Text("Take Attendance") },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, "Take Attendance") },
                    onClick = {
                        onMenuClick("TakeAttendance")
                        showMenu = false
                    }
                )
                
                DropdownMenuItem(
                    text = { Text("Insert Grade") },
                    leadingIcon = { Icon(Icons.Default.Star, "Insert Grade") },
                    onClick = {
                        onMenuClick("InsertGrade")
                        showMenu = false
                    }
                )
                
                DropdownMenuItem(
                    text = { Text("View Class List") },
                    leadingIcon = { Icon(Icons.Default.List, "View Class List") },
                    onClick = {
                        onMenuClick("ViewClassList")
                        showMenu = false
                    }
                )
                
                HorizontalDivider()
                
                // Schedule & Announcements
                DropdownMenuItem(
                    text = { Text("Post Announcement") },
                    leadingIcon = { Icon(Icons.Default.Edit, "Post Announcement") },
                    onClick = {
                        onMenuClick("PostAnnouncement")
                        showMenu = false
                    }
                )
                
                HorizontalDivider()
                
                // Communication
                DropdownMenuItem(
                    text = { Text("Messages") },
                    leadingIcon = { Icon(Icons.Default.Email, "Messages") },
                    onClick = {
                        onMenuClick("Messages")
                        showMenu = false
                    }
                )
                
                DropdownMenuItem(
                    text = { Text("Schedule Parent Meeting") },
                    leadingIcon = { Icon(Icons.Default.DateRange, "Schedule Parent Meeting") },
                    onClick = {
                        onMenuClick("ScheduleMeeting")
                        showMenu = false
                    }
                )
                
                HorizontalDivider()
                
                // Reports
                DropdownMenuItem(
                    text = { Text("View Reports") },
                    leadingIcon = { Icon(Icons.Default.List, "View Reports") },
                    onClick = {
                        onMenuClick("ViewReports")
                        showMenu = false
                    }
                )
                
                HorizontalDivider()
                
                // Change Password
                DropdownMenuItem(
                    text = { Text("Change Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, "Change Password") },
                    onClick = {
                        onMenuClick("ChangePassword")
                        showMenu = false
                    }
                )
                
                HorizontalDivider()
                
                // Logout
                DropdownMenuItem(
                    text = { Text("Logout") },
                    leadingIcon = { Icon(Icons.Default.ExitToApp, "Logout") },
                    onClick = {
                        onLogoutClick()
                        showMenu = false
                    }
                )
            }
        },
        actions = {
            // Add notification bell
            notificationBell()
            
            // Logout button
            IconButton(onClick = onLogoutClick) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}