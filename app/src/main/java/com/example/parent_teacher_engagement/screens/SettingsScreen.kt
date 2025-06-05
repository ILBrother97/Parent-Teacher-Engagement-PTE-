package com.example.parent_teacher_engagement.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.parent_teacher_engagement.utils.ThemeManager
import com.example.parent_teacher_engagement.utils.SettingsManager
import androidx.navigation.NavController
import com.example.parent_teacher_engagement.navigation.Screen
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    var isDarkMode by remember { mutableStateOf(ThemeManager.isDarkMode) }
    var showTextSizeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var selectedTextSize by remember { mutableStateOf(SettingsManager.textSize) }
    var selectedFont by remember { mutableStateOf(SettingsManager.selectedFont) }

    // Initialize SettingsManager
    LaunchedEffect(Unit) {
        SettingsManager.initialize(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dark Mode Setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dark Mode",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { 
                        isDarkMode = it
                        ThemeManager.toggleTheme()
                    }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Change Password Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { navController.navigate(Screen.ChangePassword.route) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Change Password",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Change Password",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Navigate",
                    modifier = Modifier.rotate(180f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Text Size Setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { showTextSizeDialog = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatSize,
                        contentDescription = "Text Size",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Text Size",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    text = "${(selectedTextSize * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Font Setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { showFontDialog = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Font",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Font",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    text = selectedFont,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Text Size Dialog
        if (showTextSizeDialog) {
            AlertDialog(
                onDismissRequest = { showTextSizeDialog = false },
                title = { Text("Adjust Text Size") },
                text = {
                    Column {
                        Slider(
                            value = selectedTextSize,
                            onValueChange = { 
                                selectedTextSize = it
                                SettingsManager.updateTextSize(it)
                            },
                            valueRange = 0.8f..1.4f,
                            steps = 6
                        )
                        Text(
                            text = "Preview Text",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize * selectedTextSize
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTextSizeDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        // Font Dialog
        if (showFontDialog) {
            AlertDialog(
                onDismissRequest = { showFontDialog = false },
                title = { Text("Select Font") },
                text = {
                    Column {
                        listOf("Default", "Sans Serif", "Serif", "Monospace").forEach { font ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        selectedFont = font
                                        SettingsManager.updateFont(font)
                                        showFontDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedFont == font,
                                    onClick = { 
                                        selectedFont = font
                                        SettingsManager.updateFont(font)
                                        showFontDialog = false
                                    }
                                )
                                Text(
                                    text = font,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFontDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
