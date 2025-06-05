package com.example.parent_teacher_engagement.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.example.parent_teacher_engagement.firebase.FirebaseService
import com.example.parent_teacher_engagement.screens.Message
import com.example.parent_teacher_engagement.service.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReplyReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationService.ACTION_REPLY) {
            // Get the reply text from the RemoteInput
            val remoteInput = RemoteInput.getResultsFromIntent(intent)
            if (remoteInput != null) {
                val replyText = remoteInput.getCharSequence(NotificationService.KEY_TEXT_REPLY)?.toString() ?: ""
                
                // Get the sender ID and name from the intent
                val senderId = intent.getStringExtra(NotificationService.EXTRA_SENDER_ID) ?: return
                val senderName = intent.getStringExtra(NotificationService.EXTRA_SENDER_NAME) ?: "Unknown"
                
                // Update the notification to show the reply
                NotificationService.getInstance(context).updateReplyNotification(
                    senderId = senderId,
                    senderName = senderName,
                    replyText = replyText
                )
                
                // Send the reply message
                CoroutineScope(Dispatchers.IO).launch {
                    val firebaseService = FirebaseService.getInstance()
                    val currentUser = firebaseService.currentUserData
                    
                    if (currentUser != null) {
                        val message = Message(
                            senderId = currentUser.uid,
                            senderName = currentUser.name ?: "",
                            receiverId = senderId,
                            content = replyText
                        )
                        
                        firebaseService.sendMessage(message)
                        Log.d("ReplyReceiver", "Reply sent: $replyText to $senderId")
                    } else {
                        Log.e("ReplyReceiver", "Current user is null, cannot send reply")
                    }
                }
            }
        }
    }
} 