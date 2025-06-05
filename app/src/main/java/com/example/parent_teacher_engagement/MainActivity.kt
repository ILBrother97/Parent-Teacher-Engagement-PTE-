package com.example.parent_teacher_engagement

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.parent_teacher_engagement.firebase.FirebaseService
import com.example.parent_teacher_engagement.navigation.Screen
import com.example.parent_teacher_engagement.screens.*
import com.example.parent_teacher_engagement.ui.theme.ParentteacherengagementTheme
import com.example.parent_teacher_engagement.utils.AppLifecycleTracker
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.parent_teacher_engagement.LocalNavController

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var navController: NavHostController
    private lateinit var firebaseService: FirebaseService

    private val CHANNEL_ID = "welcome_channel"
    private val NOTIFICATION_ID = 1
    
    // Variables to store notification data
    private var navigateToMessaging = false
    private var senderId: String? = null
    private var messageId: String? = null
    private var isInitialAuthCheck = true

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Welcome Channel"
            val descriptionText = "Channel for welcome notification"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createMessageNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Message Channel"
            val descriptionText = "Channel for message notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("direct_message_channel", name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            
            Log.d("MainActivity", "Message notification channel created")
        }
    }

    private fun showWelcomeNotification() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Welcome")
            .setContentText("Welcome To PTE")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Auth, Database and Service
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://finalproject-374f5-default-rtdb.europe-west1.firebasedatabase.app")
        firebaseService = FirebaseService.getInstance()
        
        // Set up navigation callback for chat screen
        firebaseService.onNavigateToChatScreen = { partnerId ->
            navController.navigate(Screen.Chat.createRoute(partnerId))
        }
        
        // Initialize AppLifecycleTracker
        AppLifecycleTracker.getInstance()
        
        // Initialize and start NetworkConnectivityMonitor
        com.example.parent_teacher_engagement.utils.NetworkConnectivityMonitor.getInstance(this).startMonitoring()

        // Check if opened from notification
        handleNotificationIntent(intent)
        
        // Create notification channels first
        createNotificationChannel()
        createMessageNotificationChannel()
        
        // Request notification permission for Android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }
        
        // Start listening for new messages
        firebaseService.listenForNewMessages(this)
        
        // Show welcome notification
        showWelcomeNotification()

        setContent {
            navController = rememberNavController()
            
            // Add auth state listener
            LaunchedEffect(Unit) {
                auth.addAuthStateListener { firebaseAuth ->
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        // User is signed in, fetch their data and navigate to appropriate screen
                        firebaseService.getCurrentUserData { userData ->
                            if (userData != null) {
                                // Only navigate if this is the initial auth check or we're on the sign-in screen
                                if (isInitialAuthCheck || navController.currentBackStackEntry?.destination?.route == Screen.SignIn.route) {
                                    navigateToUserScreen(userData.role)
                                }
                                isInitialAuthCheck = false
                            }
                        }
                    } else {
                        // Only navigate to sign-in if this is the initial auth check or we're not already on the sign-in screen
                        if (isInitialAuthCheck || navController.currentBackStackEntry?.destination?.route != Screen.SignIn.route) {
                            navController.navigate(Screen.SignIn.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        isInitialAuthCheck = false
                    }
                }
            }
            
            ParentteacherengagementTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent(navController)
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }
    
    private fun handleNotificationIntent(intent: Intent?) {
        intent?.let {
            navigateToMessaging = it.getBooleanExtra("NAVIGATE_TO_MESSAGING", false)
            senderId = it.getStringExtra("SENDER_ID")
            messageId = it.getStringExtra("MESSAGE_ID")
        }
    }

    @Composable
    private fun AppContent(navController: NavHostController) {
        // Handle navigation from notification
        LaunchedEffect(navigateToMessaging) {
            if (navigateToMessaging) {
                if (senderId != null) {
                    // If we have a sender ID, navigate directly to the chat screen
                    Log.d("MainActivity", "Navigating directly to chat with sender: $senderId")
                    navController.navigate(Screen.Chat.createRoute(senderId!!))
                } else {
                    // Otherwise, navigate to messaging screen
                    Log.d("MainActivity", "Navigating to messaging screen")
                    navController.navigate(Screen.Messaging.route)
                }
                
                // Reset the flags
                navigateToMessaging = false
                senderId = null
                messageId = null
            }
        }
        
        // Provide the NavController to the composition tree
        CompositionLocalProvider(
            LocalNavController provides navController
        ) {
            NavHost(navController = navController, startDestination = Screen.SignIn.route) {
                composable(Screen.SignIn.route) {
                    SignInScreen(
                        onSignInClick = { email, password ->
                            signIn(email, password)
                        },
                        onSignUpClick = {
                            navController.navigate(Screen.SignUp.route)
                        },
                        onForgotPasswordClick = {
                            navController.navigate(Screen.ForgotPassword.route)
                        }
                    )
                }
                composable(Screen.SignUp.route) {
                    SignUpScreen(
                        onSignUpClick = { email, password, userType ->
                            signUp(email, password, userType)
                        },
                        onSignInClick = {
                            navController.navigate(Screen.SignIn.route)
                        }
                    )
                }
                composable(Screen.ForgotPassword.route) {
                    ForgotPasswordScreen(
                        onSendPasswordClick = { email ->
                            auth.sendPasswordResetEmail(email)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(this@MainActivity, getString(R.string.password_reset_sent), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@MainActivity, getString(R.string.reset_email_failed), Toast.LENGTH_SHORT).show()
                                    }
                                }
                        },
                        onBackToSignInClick = {
                            navController.navigate(Screen.SignIn.route) {
                                popUpTo(Screen.SignIn.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.Admin.route) {
                    AdminScreen(
                        userData = firebaseService.currentUserData,
                        onLogout = { handleLogout() },
                        navController = navController
                    )
                }
                composable(Screen.Parent.route) {
                    ParentScreen(
                        onLogout = { handleLogout() },
                        navController = navController
                    )
                }
                composable(Screen.Teacher.route) {
                    TeacherScreen(
                        onLogout = { handleLogout() },
                        navController = navController
                    )
                }
                composable(Screen.Messaging.route) {
                    MessagingScreen(
                        onBack = { navController.popBackStack() },
                        navController = navController
                    )
                }
                composable(Screen.ChangePassword.route) {
                    ChangePasswordScreen(
                        onPasswordChanged = { currentPassword, newPassword ->
                            auth.currentUser?.let { user ->
                                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                                user.reauthenticate(credential)
                                    .addOnSuccessListener {
                                        user.updatePassword(newPassword)
                                            .addOnSuccessListener {
                                                Toast.makeText(this@MainActivity, getString(R.string.password_update_success), Toast.LENGTH_SHORT).show()
                                                navController.popBackStack()
                                            }
                                            .addOnFailureListener { exception ->
                                                Toast.makeText(this@MainActivity, getString(R.string.password_update_failed, exception.message), Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@MainActivity, getString(R.string.current_password_incorrect), Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                
                // Todo Screen Route
                composable(Screen.Todo.route) {
                    TodoScreen(
                        navController = navController
                    )
                }
                
                composable(Screen.Settings.route) {
                    SettingsScreen(navController = navController)
                }
                
                // Marks Screen Route
                composable(Screen.Marks.route) {
                    MarksScreen(navController = navController)
                }
                
                // User Management Screen Route
                composable(Screen.UserManagement.route) {
                    com.example.parent_teacher_engagement.screens.usermanagement.UserManagementScreen(
                        navController = navController
                    )
                }
                
                // Meeting-related routes
                composable(Screen.RequestMeeting.route) {
                    RequestMeetingScreen(
                        navController = navController
                    )
                }
                
                composable(Screen.MeetingsList.route) {
                    MeetingsListScreen(
                        navController = navController
                    )
                }
                
                composable(
                    route = Screen.MeetingDetails.route,
                    arguments = listOf(navArgument("meetingId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val meetingId = backStackEntry.arguments?.getString("meetingId") ?: return@composable
                    MeetingDetailsScreen(
                        meetingId = meetingId,
                        navController = navController
                    )
                }
                
                // Event Details Route
                composable(
                    route = "event_details/{eventId}",
                    arguments = listOf(navArgument("eventId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                    EventDetailsScreen(
                        eventId = eventId,
                        navController = navController
                    )
                }
                
                // Announcements List Route
                composable(Screen.AnnouncementsList.route) {
                    AnnouncementsListScreen(
                        navController = navController
                    )
                }
                
                // Chat Screen Route
                composable(
                    route = "chat/{partnerId}",
                    arguments = listOf(navArgument("partnerId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val partnerId = backStackEntry.arguments?.getString("partnerId") ?: return@composable
                    ChatScreen(
                        partnerId = partnerId,
                        onBack = { navController.popBackStack() }
                    )
                }
                
                // Attendance View Screen Route
                composable(
                    route = "attendance_view/{studentId}",
                    arguments = listOf(navArgument("studentId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val studentId = backStackEntry.arguments?.getString("studentId") ?: return@composable
                    AttendanceViewScreen(
                        studentId = studentId,
                        navController = navController
                    )
                }
                
                // --- Teacher Attendance Flow ---
                composable(Screen.AttendanceDate.route) {
                    AttendanceDateScreen(navController)
                }
                composable(
                    route = Screen.AttendancePeriod.route,
                    arguments = listOf(navArgument("date") { type = NavType.LongType })
                ) { backStackEntry ->
                    val date = backStackEntry.arguments?.getLong("date") ?: return@composable
                    AttendancePeriodScreen(date, navController)
                }
                composable(
                    route = Screen.AttendanceClass.route,
                    arguments = listOf(
                        navArgument("date") { type = NavType.LongType },
                        navArgument("period") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val date = backStackEntry.arguments?.getLong("date") ?: return@composable
                    val period = backStackEntry.arguments?.getInt("period") ?: return@composable
                    AttendanceClassScreen(date, period, navController)
                }
                composable(
                    route = Screen.AttendanceMark.route,
                    arguments = listOf(
                        navArgument("date") { type = NavType.LongType },
                        navArgument("period") { type = NavType.IntType },
                        navArgument("className") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val date = backStackEntry.arguments?.getLong("date") ?: return@composable
                    val period = backStackEntry.arguments?.getInt("period") ?: return@composable
                    val className = backStackEntry.arguments?.getString("className") ?: return@composable
                    AttendanceMarkScreen(date, period, className, navController)
                }
                
                // Problem reporting screens
                composable(Screen.ProblemTypeSelection.route) {
                    ProblemTypeSelectionScreen(
                        navController = navController,
                        userRole = firebaseService.currentUserData?.role ?: ""
                    )
                }
                
                composable(Screen.SystemProblemReport.route) {
                    SystemProblemReportScreen(
                        navController = navController
                    )
                }
                
                composable(
                    route = Screen.UserProblemReport.route,
                    arguments = listOf(navArgument("reportType") { type = NavType.StringType })
                ) { backStackEntry ->
                    val reportType = backStackEntry.arguments?.getString("reportType") ?: return@composable
                    UserProblemReportScreen(
                        navController = navController,
                        reportType = reportType
                    )
                }
                
                // Admin problem management screens
                composable(Screen.ReportedProblemsList.route) {
                    ReportedProblemsListScreen(
                        navController = navController
                    )
                }
                
                composable(
                    route = Screen.ReportedProblemsByRole.route,
                    arguments = listOf(navArgument("role") { type = NavType.StringType })
                ) { backStackEntry ->
                    val role = backStackEntry.arguments?.getString("role") ?: return@composable
                    ReportedProblemsByRoleScreen(
                        navController = navController,
                        role = role
                    )
                }
                
                composable(
                    route = Screen.ReportedProblemsByType.route,
                    arguments = listOf(
                        navArgument("role") { type = NavType.StringType },
                        navArgument("type") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val role = backStackEntry.arguments?.getString("role") ?: return@composable
                    val type = backStackEntry.arguments?.getString("type") ?: return@composable
                    ReportedProblemsByTypeScreen(
                        navController = navController,
                        role = role,
                        type = type
                    )
                }
                
                composable(
                    route = Screen.ReportedProblemDetails.route,
                    arguments = listOf(navArgument("problemId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val problemId = backStackEntry.arguments?.getString("problemId") ?: return@composable
                    ReportedProblemDetailsScreen(
                        navController = navController,
                        problemId = problemId
                    )
                }
            }
        }
    }

    private fun handleLogout() {
        auth.signOut()
        navController.navigate(Screen.SignIn.route) {
            popUpTo(0) { inclusive = true }
        }
        Toast.makeText(this, getString(R.string.logout_success), Toast.LENGTH_SHORT).show()
    }

    private fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, getString(R.string.enter_credentials), Toast.LENGTH_SHORT).show()
            return
        }

        firebaseService.signIn(email, password)
            .addOnSuccessListener { userData ->
                Toast.makeText(this, getString(R.string.welcome_message, userData.name), Toast.LENGTH_SHORT).show()
                navigateToUserScreen(userData.role)
            }
            .addOnFailureListener { exception ->
                val errorMessage = exception.message ?: "Authentication failed"
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
    }

    private fun signUp(email: String, password: String, userType: String) {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, getString(R.string.enter_credentials), Toast.LENGTH_SHORT).show()
            return
        }

        when (userType) {
            "teacher" -> {
                firebaseService.createTeacher(email, email, password, "", emptyList())  // Empty subject and grades for now
                    .addOnSuccessListener { 
                        Toast.makeText(this, getString(R.string.teacher_registration_success), Toast.LENGTH_SHORT).show()
                        navController.navigate(Screen.SignIn.route)
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, getString(R.string.registration_failed, exception.message), Toast.LENGTH_SHORT).show()
                    }
            }
            "parent" -> {
                firebaseService.createParent(email, email, password, emptyList())  // Empty list for child emails
                    .addOnSuccessListener { 
                        Toast.makeText(this, getString(R.string.parent_registration_success), Toast.LENGTH_SHORT).show()
                        navController.navigate(Screen.SignIn.route)
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, getString(R.string.registration_failed, exception.message), Toast.LENGTH_SHORT).show()
                    }
            }
            "admin" -> {
                // Extract name from email (everything before @)
                val adminName = email.substringBefore("@").capitalize()
                firebaseService.createAdmin(adminName, email, password)
                    .addOnSuccessListener { 
                        Toast.makeText(this, getString(R.string.admin_registration_success), Toast.LENGTH_SHORT).show()
                        navController.navigate(Screen.SignIn.route)
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, getString(R.string.registration_failed, exception.message), Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun navigateToUserScreen(userType: String?) {
        when (userType) {
            "admin" -> navController.navigate(Screen.Admin.route) {
                popUpTo(Screen.SignIn.route) { inclusive = true }
            }
            "parent" -> navController.navigate(Screen.Parent.route) {
                popUpTo(Screen.SignIn.route) { inclusive = true }
            }
            "teacher" -> navController.navigate(Screen.Teacher.route) {
                popUpTo(Screen.SignIn.route) { inclusive = true }
            }
        }
    }
}