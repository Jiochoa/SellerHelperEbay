package com.example.sellerhelperebay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.sellerhelperebay.domain.model.ConfidenceLevel

@Composable
fun ConfidenceDot(level: ConfidenceLevel, modifier: Modifier = Modifier) {
    val color = when (level) {
        ConfidenceLevel.GREEN -> Color(0xFF2E7D32)
        ConfidenceLevel.YELLOW -> Color(0xFFF9A825)
        ConfidenceLevel.RED -> Color(0xFFC62828)
        ConfidenceLevel.BLUE -> Color(0xFF1565C0)
        ConfidenceLevel.EMPTY -> MaterialTheme.colorScheme.outlineVariant
    }
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(color)
    )
}
