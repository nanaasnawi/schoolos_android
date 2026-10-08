package com.schoolos.android.core.designsystem

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.schoolos.android.core.common.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resolves full avatar URL from a relative or absolute path, pointing directly
 * to the PostgreSQL-backed avatar endpoint or a base64 Data URI.
 */
fun resolveAvatarUrl(avatarUrl: String?): String? {
    if (avatarUrl.isNullOrBlank()) return null
    val raw = avatarUrl.trim()
    return if (raw.startsWith("data:image", ignoreCase = true) ||
        raw.startsWith("http://", ignoreCase = true) ||
        raw.startsWith("https://", ignoreCase = true)
    ) {
        raw
    } else {
        val host = BuildConfig.API_BASE_URL.trimEnd('/')
        "$host/${raw.trimStart('/')}"
    }
}

/**
 * Builds an [ImageRequest] for user profile avatar that completely bypasses
 * local disk storage and cache (diskCachePolicy = DISABLED, memoryCachePolicy = DISABLED),
 * ensuring the photo is always streamed directly from PostgreSQL database.
 */
fun buildNoCacheAvatarImageRequest(
    context: android.content.Context,
    avatarUrl: String?,
): ImageRequest? {
    val fullUrl = resolveAvatarUrl(avatarUrl) ?: return null
    return ImageRequest.Builder(context)
        .data(fullUrl)
        // CRITICAL: Jangan simpan foto profil ke local disk storage atau cache!
        // Always retrieve fresh from the PostgreSQL database directly
        .diskCachePolicy(CachePolicy.DISABLED)
        .memoryCachePolicy(CachePolicy.DISABLED)
        .crossfade(true)
        .build()
}

/**
 * Profile avatar component that strictly avoids local disk storage & cache.
 * Photos are stored directly in PostgreSQL database and loaded on-demand.
 */
@Composable
fun UserProfileAvatar(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Foto Profil",
    contentScale: ContentScale = ContentScale.Crop,
    placeholderIcon: ImageVector = Icons.Default.Person,
    placeholderTint: Color = NeonBlue,
    placeholderBg: Color = CosmicSurface2,
) {
    if (avatarUrl.isNullOrBlank()) {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(placeholderBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = placeholderIcon,
                contentDescription = contentDescription,
                tint = placeholderTint,
            )
        }
        return
    }

    val raw = avatarUrl.trim()

    // 1. Direct PostgreSQL Base64 Data URI: decoded in-memory without any disk storage
    if (raw.startsWith("data:image", ignoreCase = true)) {
        val imageBitmapState = produceState<ImageBitmap?>(initialValue = null, key1 = raw) {
            value = withContext(Dispatchers.Default) {
                try {
                    val base64Data = raw.substringAfter("base64,", "")
                    if (base64Data.isNotEmpty()) {
                        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    } else null
                } catch (_: Exception) {
                    null
                }
            }
        }

        val bitmap = imageBitmapState.value
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = modifier.clip(CircleShape),
                contentScale = contentScale,
            )
        } else {
            Box(
                modifier = modifier
                    .clip(CircleShape)
                    .background(placeholderBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = placeholderIcon,
                    contentDescription = contentDescription,
                    tint = placeholderTint,
                )
            }
        }
        return
    }

    // 2. Direct PostgreSQL Streaming Endpoint (/api/v1/auth/avatar/{id})
    val fullUrl = remember(raw) { resolveAvatarUrl(raw) }
    val context = LocalContext.current
    val imageRequest = remember(fullUrl) {
        if (fullUrl != null) {
            ImageRequest.Builder(context)
                .data(fullUrl)
                // STRICT: Never store photo into local disk storage / cache
                .diskCachePolicy(CachePolicy.DISABLED)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .crossfade(true)
                .build()
        } else null
    }

    if (imageRequest != null) {
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            modifier = modifier.clip(CircleShape),
            contentScale = contentScale,
            error = rememberVectorPainter(placeholderIcon),
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(placeholderBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = placeholderIcon,
                contentDescription = contentDescription,
                tint = placeholderTint,
            )
        }
    }
}
