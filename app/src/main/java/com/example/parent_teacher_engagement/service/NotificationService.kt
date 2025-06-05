package com.example.parent_teacher_engagement.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.parent_teacher_engagement.MainActivity
import com.example.parent_teacher_engagement.R
import com.example.parent_teacher_engagement.firebase.FirebaseService
import com.example.parent_teacher_engagement.receivers.ReplyReceiver
import com.example.parent_teacher_engagement.screens.Message
import com.example.parent_teacher_engagement.utils.AppLifecycleTracker

class NotificationService(private val context: Context) {
    
    private val notificationManager: NotificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    private val CHANNEL_ID = "direct_message_channel"
    private val NOTIFICATION_ID_BASE = 1000
    
    companion object {
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val ACTION_REPLY = "com.example.parent_teacher_engagement.ACTION_REPLY"
        const val EXTRA_SENDER_ID = "sender_id"
        const val EXTRA_SENDER_NAME = "sender_name"
        
        @Volatile
        private var instance: NotificationService? = null
        
        fun getInstance(context: Context): NotificationService {
            return instance ?: synchronized(this) {
                instance ?: NotificationService(context.applicationContext).also { instance = it }
            }
        }
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Direct Messages"
            val descriptionText = "Channel for direct message notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showMessageNotification(message: Message) {
        Log.d("NotificationService", "Showing message notification from: ${message.senderName}, to: ${message.receiverId}, content: ${message.content}")
        
        // Check if app is in foreground - temporarily disabled to ensure notifications appear
        val isAppInForeground = AppLifecycleTracker.getInstance().isAppInForeground.value
        Log.d("NotificationService", "App is in foreground: $isAppInForeground")
        
        // Get sender and receiver roles
        val firebaseService = FirebaseService.getInstance()
        firebaseService.getUserRole(message.senderId) { senderRole ->
            firebaseService.getUserRole(message.receiverId) { receiverRole ->
                // Skip showing notification if both users are parent and teacher
                if ((senderRole == "parent" && receiverRole == "teacher") || 
                    (senderRole == "teacher" && receiverRole == "parent")) {
                    Log.d("NotificationService", "Skipping notification for parent-teacher message")
                    return@getUserRole
                }
                
                // Create an intent to open the messaging screen
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("NAVIGATE_TO_MESSAGING", true)
                    putExtra("SENDER_ID", message.senderId)
                    putExtra("MESSAGE_ID", message.id)
                }
                
                val pendingIntent = PendingIntent.getActivity(
                    context, 
                    0, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                // Create reply action
                val replyLabel = "Reply"
                val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
                    .setLabel(replyLabel)
                    .build()
                
                // Create reply intent
                val replyIntent = Intent(context, ReplyReceiver::class.java).apply {
                    action = ACTION_REPLY
                    putExtra(EXTRA_SENDER_ID, message.senderId)
                    putExtra(EXTRA_SENDER_NAME, message.senderName)
                }
                
                val replyPendingIntent = PendingIntent.getBroadcast(
                    context,
                    message.senderId.hashCode(),
                    replyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                
                // Create reply action
                val replyAction = NotificationCompat.Action.Builder(
                    R.drawable.ic_reply,
                    "Reply",
                    replyPendingIntent
                ).addRemoteInput(remoteInput).build()
                
                // Build the notification
                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("New message from ${message.senderName}")
                    .setContentText(message.content)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .addAction(replyAction)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setVibrate(longArrayOf(0, 500, 1000))
                
                // Show the notification
                try {
                    val notificationId = NOTIFICATION_ID_BASE + message.senderId.hashCode()
                    Log.d("NotificationService", "Showing notification with ID: $notificationId")
                    notificationManager.notify(notificationId, builder.build())
                    Log.d("NotificationService", "Successfully showed notification for message from ${message.senderName}")
                } catch (e: Exception) {
                    Log.e("NotificationService", "Error showing notification", e)
                }
            }
        }
    }
    
    fun showMessagesNotification(senderId: String, senderName: String, messages: List<Message>) {
        Log.d("NotificationService", "Showing messages notification from: $senderName, count: ${messages.size}, messages: ${messages.map { it.content }}")
        
        // Check if app is in foreground - temporarily disabled to ensure notifications appear
        val isAppInForeground = AppLifecycleTracker.getInstance().isAppInForeground.value
        Log.d("NotificationService", "App is in foreground: $isAppInForeground")
        
        // Create an intent to open the messaging screen
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO_MESSAGING", true)
            putExtra("SENDER_ID", senderId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create reply action
        val replyLabel = "Reply"
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel(replyLabel)
            .build()
        
        // Create reply intent
        val replyIntent = Intent(context, ReplyReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra(EXTRA_SENDER_ID, senderId)
            putExtra(EXTRA_SENDER_NAME, senderName)
        }
        
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            senderId.hashCode(),
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        
        // Create reply action
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_reply,
            "Reply",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()
        
        // Create notification style for multiple messages
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("${messages.size} new messages from $senderName")
        
        // Add up to 5 messages to the inbox style
        messages.take(5).forEach { message ->
            inboxStyle.addLine(message.content)
        }
        
        if (messages.size > 5) {
            inboxStyle.setSummaryText("+ ${messages.size - 5} more")
        }
        
        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("${messages.size} new messages from $senderName")
            .setContentText(messages.first().content)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(replyAction)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 500, 1000))
        
        // Show the notification
        try {
            val notificationId = NOTIFICATION_ID_BASE + senderId.hashCode()
            Log.d("NotificationService", "Showing notification with ID: $notificationId")
            notificationManager.notify(notificationId, builder.build())
            Log.d("NotificationService", "Successfully showed notification for messages from $senderName")
        } catch (e: Exception) {
            Log.e("NotificationService", "Error showing notification", e)
        }
    }
    
    fun showNewEventNotification() {
        // Create an intent to open the main activity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("New School Event")
            .setContentText("There is a new event at our school! Check it out!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 500, 1000))
        
        // Show the notification
        notificationManager.notify(NOTIFICATION_ID_BASE + 1, builder.build())
    }
    
    fun updateReplyNotification(senderId: String, senderName: String, replyText: String) {
        // Create a notification to show the reply was sent
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Reply to $senderName")
            .setContentText(replyText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        
        // Show the notification
        notificationManager.notify(NOTIFICATION_ID_BASE + senderId.hashCode(), builder.build())
    }
}
