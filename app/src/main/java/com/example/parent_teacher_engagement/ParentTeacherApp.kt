package com.example.parent_teacher_engagement

import android.app.Application
import com.google.firebase.FirebaseApp
import com.example.parent_teacher_engagement.utils.AppLifecycleTracker

class ParentTeacherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        
        // Initialize AppLifecycleTracker
        AppLifecycleTracker.getInstance()
    }
}
