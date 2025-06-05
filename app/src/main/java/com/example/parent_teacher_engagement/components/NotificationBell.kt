package com.example.parent_teacher_engagement.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parent_teacher_engagement.firebase.FirebaseService
import kotlinx.coroutines.delay

@Composable
fun NotificationBell(
    unreadCount: Int,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "notification_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (unreadCount > 0) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_animation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                modifier = Modifier
                    .size(28.dp)
                    .scale(scale),
                tint = if (unreadCount > 0) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }
    }
}

@Composable
fun UnreadMessageNotification(
    userId: String,
    onUnreadMessagesFound: (Int) -> Unit
) {
    var unreadCount by remember { mutableStateOf(0) }
    
    LaunchedEffect(userId) {
        FirebaseService.getInstance().getUnreadMessagesCount(userId) { count ->
            unreadCount = count
            onUnreadMessagesFound(count)
        }
        
        // Periodically check for new unread messages
        while (true) {
            delay(30000) // Check every 30 seconds
            FirebaseService.getInstance().getUnreadMessagesCount(userId) { count ->
                unreadCount = count
                onUnreadMessagesFound(count)
            }
        }
    }
} 