package com.example.parent_teacher_engagement.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object SettingsManager {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_TEXT_SIZE = "text_size"
    private const val KEY_FONT = "font"
    
    private lateinit var prefs: SharedPreferences
    
    var textSize by mutableStateOf(1.0f)
        private set
    
    var selectedFont by mutableStateOf("Default")
        private set
    
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        textSize = prefs.getFloat(KEY_TEXT_SIZE, 1.0f)
        selectedFont = prefs.getString(KEY_FONT, "Default") ?: "Default"
    }
    
    fun updateTextSize(size: Float) {
        textSize = size
        prefs.edit().putFloat(KEY_TEXT_SIZE, size).apply()
    }
    
    fun updateFont(font: String) {
        selectedFont = font
        prefs.edit().putString(KEY_FONT, font).apply()
    }
    
    fun getFontFamily(): FontFamily {
        return when (selectedFont) {
            "Sans Serif" -> FontFamily.SansSerif
            "Serif" -> FontFamily.Serif
            "Monospace" -> FontFamily.Monospace
            else -> FontFamily.Default
        }
    }
    
    fun getScaledFontSize(baseSize: Int): Int {
        return (baseSize * textSize).toInt()
    }
} 