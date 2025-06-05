package com.example.parent_teacher_engagement.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A utility class to track the application's lifecycle state.
 * This helps determine if the app is in the foreground or background
 * for proper notification handling.
 */
class AppLifecycleTracker private constructor() : DefaultLifecycleObserver {
    
    private val _isAppInForeground = MutableStateFlow(false)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()
    
    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    
    override fun onStart(owner: LifecycleOwner) {
        _isAppInForeground.value = true
    }
    
    override fun onStop(owner: LifecycleOwner) {
        _isAppInForeground.value = false
    }
    
    companion object {
        @Volatile
        private var instance: AppLifecycleTracker? = null
        
        fun getInstance(): AppLifecycleTracker {
            return instance ?: synchronized(this) {
                instance ?: AppLifecycleTracker().also { instance = it }
            }
        }
    }
} 