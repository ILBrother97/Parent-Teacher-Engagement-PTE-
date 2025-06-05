package com.example.parent_teacher_engagement.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
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
                // Add Teacher
                DropdownMenuItem(
                    text = { Text("Add Teacher") },
                    leadingIcon = { Icon(Icons.Default.Add, "Add Teacher") },
                    onClick = {
                        onMenuClick("AddTeacher")
                        showMenu = false
                    }
                )
                
                // Add Parent
                DropdownMenuItem(
                    text = { Text("Add Parent") },
                    leadingIcon = { Icon(Icons.Default.Add, "Add Parent") },
                    onClick = {
                        onMenuClick("AddParent")
                        showMenu = false
                    }
                )
                
                // Add Student
                DropdownMenuItem(
                    text = { Text("Add Student") },
                    leadingIcon = { Icon(Icons.Default.Add, "Add Student") },
                    onClick = {
                        onMenuClick("AddStudent")
                        showMenu = false
                    }
                )
                
                HorizontalDivider()
                
                // List of Teachers
                DropdownMenuItem(
                    text = { Text("List of Teachers") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, "List Teachers") },
                    onClick = {
                        onMenuClick("ListTeachers")
                        showMenu = false
                    }
                )
                
                // List of Parents
                DropdownMenuItem(
                    text = { Text("List of Parents") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, "List Parents") },
                    onClick = {
                        onMenuClick("ListParents")
                        showMenu = false
                    }
                )
                
                // List of Students
                DropdownMenuItem(
                    text = { Text("List of Students") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, "List Students") },
                    onClick = {
                        onMenuClick("ListStudents")
                        showMenu = false
                    }
                )
                
                HorizontalDivider()
                
                // Post Upcoming Events
                DropdownMenuItem(
                    text = { Text("Post Upcoming Events") },
                    leadingIcon = { Icon(Icons.Default.DateRange, "Post Events") },
                    onClick = {
                        onMenuClick("PostEvents")
                        showMenu = false
                    }
                )
                
                HorizontalDivider()
                
                // Logout
                DropdownMenuItem(
                    text = { Text("Logout") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout") },
                    onClick = {
                        onLogoutClick()
                        showMenu = false
                    }
                )
            }
        }
    )
}