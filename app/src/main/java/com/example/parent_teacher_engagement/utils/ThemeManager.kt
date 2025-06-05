package com.example.parent_teacher_engagement.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeManager {
    var isDarkMode by mutableStateOf(true)
        private set

    fun toggleTheme() {
        isDarkMode = !isDarkMode
    }
} 