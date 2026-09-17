package com.schoolos.android.feature.auth.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextTertiary

@Composable
fun LoginErrorToast(
    errorMessage: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = errorMessage != null,
        enter = fadeIn(tween(300)) + slideInVertically(
            animationSpec = tween(350, easing = FastOutSlowInEasing),
            initialOffsetY = { it },
        ),
        exit = fadeOut(tween(250)) + slideOutVertically(
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            targetOffsetY = { it },
        ),
        modifier = modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 32.dp)
            .fillMaxWidth(),
    ) {
        if (errorMessage != null) {
            // 100% Solid Opaque Background (NO translucency / NO bleeding underneath)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(18.dp),
                        spotColor = NeonError.copy(alpha = 0.5f),
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(CosmicNavy) // Completely solid opaque
                    .border(1.5.dp, NeonError, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(NeonError.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = NeonError,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = errorMessage,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        lineHeight = 18.sp,
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Tutup",
                            tint = TextTertiary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
