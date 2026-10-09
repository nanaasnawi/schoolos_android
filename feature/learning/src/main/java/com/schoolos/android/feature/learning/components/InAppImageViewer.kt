package com.schoolos.android.feature.learning.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.schoolos.android.core.designsystem.*
import kotlin.math.absoluteValue

val EDUCATIONAL_FALLBACK_IMAGES = listOf(
    "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&q=80",
    "https://images.unsplash.com/photo-1509228468518-180dd4864904?w=800&q=80",
    "https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=800&q=80",
    "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=800&q=80",
    "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=800&q=80"
)

/**
 * In-App High Resolution Image & Infographic Viewer with Pinch-to-Zoom, Lightbox,
 * and automatic educational fallback so images never show broken icons.
 */
@Composable
fun InAppImageViewer(
    imageUrl: String,
    title: String,
    modifier: Modifier = Modifier,
    fallbackIndex: Int = 0,
) {
    val context = LocalContext.current
    var showFullscreenLightbox by remember { mutableStateOf(false) }
    var useFallback by remember(imageUrl) { mutableStateOf(false) }

    val fallbackUrl = remember(title, fallbackIndex) {
        val idx = (title.hashCode().absoluteValue + fallbackIndex) % EDUCATIONAL_FALLBACK_IMAGES.size
        EDUCATIONAL_FALLBACK_IMAGES[idx]
    }

    val activeUrl = remember(imageUrl, useFallback) {
        if (useFallback || imageUrl.isBlank()) {
            fallbackUrl
        } else {
            imageUrl
        }
    }

    val imageRequest = remember(activeUrl) {
        ImageRequest.Builder(context)
            .data(activeUrl)
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
            .crossfade(true)
            .build()
    }

    Box(modifier = modifier) {
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            loading = {
                ShimmerBox(modifier = Modifier.fillMaxSize())
            },
            error = {
                if (!useFallback && imageUrl.isNotBlank()) {
                    LaunchedEffect(imageUrl) {
                        useFallback = true
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CosmicNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Ilustrasi Materi Digital",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = title,
                            color = TextTertiary,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clickable { showFullscreenLightbox = true }
        )

        // Overlay hint button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable { showFullscreenLightbox = true }
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Perbesar (Pinch-to-Zoom)",
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showFullscreenLightbox) {
        Dialog(
            onDismissRequest = { showFullscreenLightbox = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            var scale by remember { mutableStateOf(1f) }
            var offsetX by remember { mutableStateOf(0f) }
            var offsetY by remember { mutableStateOf(0f) }

            val lightboxRequest = remember(activeUrl) {
                ImageRequest.Builder(context)
                    .data(activeUrl)
                    .addHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    )
                    .crossfade(true)
                    .build()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                SubcomposeAsyncImage(
                    model = lightboxRequest,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NeonBlue)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                )

                // Close button
                IconButton(
                    onClick = { showFullscreenLightbox = false },
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }

                // Reset zoom button
                if (scale > 1f) {
                    Button(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                    ) {
                        Text("Reset Zoom (1x)", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
