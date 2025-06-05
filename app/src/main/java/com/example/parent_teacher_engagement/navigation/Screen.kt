package com.example.parent_teacher_engagement.navigation

import androidx.compose.ui.window.DialogProperties

sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")
    object SignUp : Screen("sign_up")
    object ForgotPassword : Screen("forgot_password")
    object Admin : Screen("admin")
    object Parent : Screen("parent")
    object Teacher : Screen("teacher")
    object Messaging : Screen("messaging")
    object ChangePassword : Screen("change_password")
    object Todo : Screen("todo")
    object UserManagement : Screen("user_management")
    object Settings : Screen("settings")
    object Marks : Screen("marks")
    
    // Meeting-related screens
    object RequestMeeting : Screen("request_meeting")
    object MeetingsList : Screen("meetings_list")
    object MeetingDetails : Screen("meeting_details/{meetingId}") {
        fun createRoute(meetingId: String) = "meeting_details/$meetingId"
    }
    
    // Problem reporting screens
    object ProblemTypeSelection : Screen("problem_type_selection")
    object SystemProblemReport : Screen("system_problem_report")
    object UserProblemReport : Screen("user_problem_report/{reportType}") {
        fun createRoute(reportType: String) = "user_problem_report/$reportType"
    }
    
    // Admin problem viewing screens
    object ReportedProblemsList : Screen("reported_problems_list")
    object ReportedProblemsByRole : Screen("reported_problems_by_role/{role}") {
        fun createRoute(role: String) = "reported_problems_by_role/$role"
    }
    object ReportedProblemsByType : Screen("reported_problems_by_type/{role}/{type}") {
        fun createRoute(role: String, type: String) = "reported_problems_by_type/$role/$type"
    }
    object ReportedProblemDetails : Screen("reported_problem_details/{problemId}") {
        fun createRoute(problemId: String) = "reported_problem_details/$problemId"
    }
    
    object Chat : Screen("chat/{partnerId}") {
        fun createRoute(partnerId: String) = "chat/$partnerId"
    }
    object EventDetails : Screen("event_details/{eventId}") {
        fun createRoute(eventId: String) = "event_details/$eventId"
    }
    
    // Announcements screen
    object AnnouncementsList : Screen("announcements_list")
    
    object AttendanceView : Screen("attendance_view/{studentId}") {
        fun createRoute(studentId: String) = "attendance_view/$studentId"
    }
    
    object AttendanceDate : Screen("attendance_date")
    object AttendancePeriod : Screen("attendance_period/{date}") {
        fun createRoute(date: Long) = "attendance_period/$date"
    }
    object AttendanceClass : Screen("attendance_class/{date}/{period}") {
        fun createRoute(date: Long, period: Int) = "attendance_class/$date/$period"
    }
    object AttendanceMark : Screen("attendance_mark/{date}/{period}/{className}") {
        fun createRoute(date: Long, period: Int, className: String) = "attendance_mark/$date/$period/$className"
    }
}