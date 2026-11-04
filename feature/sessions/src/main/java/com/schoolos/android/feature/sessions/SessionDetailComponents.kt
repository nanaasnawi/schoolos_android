package com.schoolos.android.feature.sessions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.domain.model.LearningSession
import java.time.Duration
import java.time.Instant

@Composable
fun DetailMetricItem(
    label: String,
    value: String,
    icon: ImageVector? = null,
    @androidx.annotation.DrawableRes iconRes: Int? = null,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = textTertiary,
                modifier = Modifier.size(16.dp)
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 10.sp, color = textTertiary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(1.dp))
            Text(value, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        }
    }
}

@Composable
fun ResourceItem(
    label: String,
    icon: ImageVector? = null,
    @androidx.annotation.DrawableRes iconRes: Int? = null,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = if (isDark) 0.12f else 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = if (isDark) 0.35f else 0.20f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        }
    }
}

fun LearningSession.durationText(): String? {
    val start = startedAt?.let { tryParse(it) } ?: return null
    val end = endedAt?.let { tryParse(it) } ?: return null
    val minutes = Duration.between(start, end).toMinutes()
    return when {
        minutes < 60 -> "${minutes}m"
        minutes % 60 == 0L -> "${minutes / 60}h"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}

private fun tryParse(iso: String): Instant? = try { Instant.parse(iso) } catch (_: Exception) { null }
