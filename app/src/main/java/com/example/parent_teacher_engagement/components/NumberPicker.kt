package com.example.parent_teacher_engagement.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { 
                if (value > range.first) {
                    onValueChange(value - 1)
                }
            },
            enabled = value > range.first
        ) {
            Text("-")
        }
        
        Text(
            text = value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        IconButton(
            onClick = { 
                if (value < range.last) {
                    onValueChange(value + 1)
                }
            },
            enabled = value < range.last
        ) {
            Text("+")
        }
    }
}