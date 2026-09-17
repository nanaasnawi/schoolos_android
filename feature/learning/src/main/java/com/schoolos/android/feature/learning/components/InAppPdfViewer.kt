package com.schoolos.android.feature.learning.components

import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.createBitmap
import com.schoolos.android.core.designsystem.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class RenderedPdfPage(
    val pageNumber: Int,
    val bitmap: ImageBitmap
)

/**
 * Native in-app PDF viewer rendering real multi-page images using PdfRenderer
 * directly from Kemendikdasmen CDN books or uploaded documents.
 */
@Composable
fun InAppPdfViewer(
    pdfUrl: String,
    title: String,
    subject: String,
    description: String = "",
    startPage: Int? = null,
    endPage: Int? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var pages by remember { mutableStateOf<List<RenderedPdfPage>>(emptyList()) }
    var totalDocPages by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(pdfUrl, title, startPage, endPage, retryTrigger) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val cleanUrl = pdfUrl.trim()
                if (cleanUrl.isBlank()) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Tautan dokumen PDF buku belum disematkan."
                        isLoading = false
                    }
                    return@withContext
                }

                // Deterministic cache file based on URL
                val cacheFileName = "book_pdf_${cleanUrl.hashCode().toString().replace("-", "n")}.pdf"
                val pdfFile = File(context.cacheDir, cacheFileName)

                var downloadSuccess = pdfFile.exists() && pdfFile.length() > 5000

                if (!downloadSuccess && (cleanUrl.startsWith("http://", ignoreCase = true) || cleanUrl.startsWith("https://", ignoreCase = true))) {
                    try {
                        val connection = URL(cleanUrl).openConnection() as HttpURLConnection
                        connection.connectTimeout = 15000
                        connection.readTimeout = 60000
                        connection.instanceFollowRedirects = true
                        connection.setRequestProperty("User-Agent", "SchoolOS-Android-Client/1.0")
                        connection.connect()

                        val code = connection.responseCode
                        if (code in 200..299) {
                            val tempFile = File(context.cacheDir, "${cacheFileName}.tmp")
                            val inputStream = connection.inputStream
                            val outputStream = FileOutputStream(tempFile)
                            val buffer = ByteArray(16384)
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                            }
                            outputStream.flush()
                            outputStream.close()
                            inputStream.close()

                            if (tempFile.length() > 5000) {
                                if (pdfFile.exists()) pdfFile.delete()
                                tempFile.renameTo(pdfFile)
                                downloadSuccess = true
                            }
                        } else {
                            Timber.w("PDF download responded with HTTP code $code from $cleanUrl")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to download PDF: $cleanUrl")
                        downloadSuccess = false
                    }
                }

                if (!downloadSuccess || !pdfFile.exists() || pdfFile.length() < 1000) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Gagal mengunduh berkas PDF buku dari server. Periksa koneksi internet Anda."
                        isLoading = false
                    }
                    return@withContext
                }

                // Render real PDF pages via native Android PdfRenderer
                val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val totalPages = renderer.pageCount

                // Compute page slice according to teacher assignment
                val reqStart = startPage ?: 1
                val reqEnd = endPage ?: minOf(totalPages, reqStart + 14)

                val startIndex = (reqStart - 1).coerceIn(0, totalPages - 1)
                val endIndex = (reqEnd - 1).coerceIn(startIndex, totalPages - 1)

                val renderedBitmaps = mutableListOf<RenderedPdfPage>()
                val displayMetrics = context.resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels

                for (idx in startIndex..endIndex) {
                    val page = renderer.openPage(idx)
                    val scale = (screenWidth.toFloat() / page.width.toFloat()).coerceIn(1.0f, 1.6f)
                    val bmpWidth = (page.width * scale).toInt()
                    val bmpHeight = (page.height * scale).toInt()
                    val bitmap = createBitmap(bmpWidth, bmpHeight)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    renderedBitmaps.add(RenderedPdfPage(pageNumber = idx + 1, bitmap = bitmap.asImageBitmap()))
                    page.close()
                }

                renderer.close()
                pfd.close()

                withContext(Dispatchers.Main) {
                    pages = renderedBitmaps
                    totalDocPages = totalPages
                    isLoading = false
                }
            } catch (e: Exception) {
                Timber.e(e, "Error opening PDF document")
                withContext(Dispatchers.Main) {
                    errorMessage = "Gagal memproses dokumen PDF: ${e.localizedMessage}"
                    isLoading = false
                }
            }
        }
    }

    // Fullscreen state for single page lightbox
    var fullscreenPageIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier) {
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicDark),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = NeonBlue, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Mengunduh & Merender Buku Digital...",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (startPage != null && endPage != null) "Halaman $startPage — $endPage" else "Menyiapkan lembar bacaan resmi",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
        } else if (pages.isNotEmpty()) {
            val totalPages = pages.size
            val sPage = startPage ?: pages.first().pageNumber
            val ePage = endPage ?: pages.last().pageNumber

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header Bar with Full PDF Reader Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = NeonBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Hal. $sPage — $ePage ($totalPages Lembar)",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (pdfUrl.isNotBlank()) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(pdfUrl), "application/pdf")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
                                    context.startActivity(webIntent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Buka Dokumen Penuh", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Rendered Real Pages
                pages.forEachIndexed { index: Int, item: RenderedPdfPage ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { fullscreenPageIndex = index }
                    ) {
                        Column {
                            Box {
                                Image(
                                    bitmap = item.bitmap,
                                    contentDescription = "Halaman ${item.pageNumber}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(),
                                    contentScale = ContentScale.FillWidth
                                )

                                // Fullscreen Hint Pill Overlay
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.70f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Fullscreen, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("Perbesar", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A))
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Buku Resmi • Halaman ${item.pageNumber}${if (totalDocPages > 0) " dari $totalDocPages" else ""}",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Error Card with Retry and Direct Browser View
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.WarningAmber, null, tint = NeonWarning, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "Dokumen belum dapat ditampilkan.",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { retryTrigger++ },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Coba Lagi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (pdfUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Buka di Browser", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── FULLSCREEN PDF PAGE LIGHTBOX ─────────────────────────────────────────
    val fsIndex = fullscreenPageIndex
    if (fsIndex != null && pages.isNotEmpty() && fsIndex in pages.indices) {
        var currentPage by remember(fsIndex) { mutableIntStateOf(fsIndex) }
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        Dialog(
            onDismissRequest = { fullscreenPageIndex = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B0F19))
            ) {
                // Page container with pinch-to-zoom & gestures
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .pointerInput(currentPage) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 6f)
                                scale = newScale
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 64.dp)
                            .fillMaxSize()
                    ) {
                        Image(
                            bitmap = pages[currentPage].bitmap,
                            contentDescription = "Halaman ${pages[currentPage].pageNumber}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White)
                        )
                    }
                }

                // Top bar overlay: close + page indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { fullscreenPageIndex = null },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Halaman ${pages[currentPage].pageNumber} (Lembar ${currentPage + 1}/${pages.size})",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (scale > 1f) {
                        IconButton(
                            onClick = { scale = 1f; offsetX = 0f; offsetY = 0f },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NeonBlue.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Reset Zoom", tint = Color.White)
                        }
                    } else {
                        Spacer(Modifier.size(40.dp))
                    }
                }

                // Floating Zoom Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 14.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { scale = (scale + 0.5f).coerceAtMost(6f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Perbesar", tint = Color.White)
                    }

                    Text(
                        text = "${(scale * 100).toInt()}%",
                        color = NeonBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { scale = (scale - 0.5f).coerceAtLeast(1f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Perkecil", tint = Color.White)
                    }
                }

                // Bottom bar overlay: page switching
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (currentPage > 0) NeonBlue.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.3f))
                            .clickable(enabled = currentPage > 0) {
                                currentPage--
                                scale = 1f; offsetX = 0f; offsetY = 0f
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ChevronLeft, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sebelumnya", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (currentPage < pages.size - 1) NeonBlue.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.3f))
                            .clickable(enabled = currentPage < pages.size - 1) {
                                currentPage++
                                scale = 1f; offsetX = 0f; offsetY = 0f
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Berikutnya", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
