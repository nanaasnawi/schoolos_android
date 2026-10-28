package com.schoolos.android.feature.learning.components

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.schoolos.android.core.designsystem.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

private class PdfRenderController(
    private val pfd: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    private val screenWidth: Int,
) {
    val pageCount: Int = renderer.pageCount
    private val mutex = Mutex()
    private val cache = LruCache<Int, ImageBitmap>(5)
    @Volatile
    private var isClosed = false

    suspend fun renderPage(pageNumber1Based: Int): ImageBitmap? {
        cache.get(pageNumber1Based)?.let { return it }
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                if (isClosed) return@withLock null
                cache.get(pageNumber1Based)?.let { return@withLock it }
                val pageIndex = (pageNumber1Based - 1).coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                var page: PdfRenderer.Page? = null
                try {
                    page = renderer.openPage(pageIndex)
                    val scale = (screenWidth.toFloat() / page.width.toFloat()).coerceIn(1.0f, 1.5f)
                    val bmpWidth = (page.width * scale).toInt().coerceAtLeast(1)
                    val bmpHeight = (page.height * scale).toInt().coerceAtLeast(1)
                    // Allocate ARGB_8888 for PdfRenderer compatibility, then convert to RGB_565 to halve RAM footprint
                    val renderBmp = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                    renderBmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(renderBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val compactBmp = renderBmp.copy(Bitmap.Config.RGB_565, false) ?: renderBmp
                    if (compactBmp !== renderBmp) {
                        renderBmp.recycle()
                    }
                    val imageBitmap = compactBmp.asImageBitmap()
                    cache.put(pageNumber1Based, imageBitmap)
                    imageBitmap
                } catch (t: Throwable) {
                    Timber.e(t, "Failed rendering PDF page $pageNumber1Based")
                    null
                } finally {
                    try {
                        page?.close()
                    } catch (_: Throwable) {}
                }
            }
        }
    }

    fun close() {
        isClosed = true
        try {
            cache.evictAll()
        } catch (_: Throwable) {}
        try {
            renderer.close()
        } catch (_: Throwable) {}
        try {
            pfd.close()
        } catch (_: Throwable) {}
    }
}

/**
 * Native in-app PDF viewer rendering pages lazily on-demand using PdfRenderer
 * with bounded LruCache (max 5 pages) and RGB_565 bitmaps to prevent OOM.
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
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var controller by remember { mutableStateOf<PdfRenderController?>(null) }
    var totalDocPages by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }
    var showFullDocument by remember { mutableStateOf(false) }

    // Download PDF file on IO thread with proper stream & connection cleanup
    LaunchedEffect(pdfUrl, retryTrigger) {
        isLoading = true
        errorMessage = null
        downloadedFile = null
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

                val cacheFileName = "book_pdf_${cleanUrl.hashCode().toString().replace("-", "n")}.pdf"
                val pdfFile = File(context.cacheDir, cacheFileName)

                var downloadSuccess = pdfFile.exists() && pdfFile.length() > 5000

                if (!downloadSuccess && (cleanUrl.startsWith("http://", ignoreCase = true) || cleanUrl.startsWith("https://", ignoreCase = true))) {
                    var connection: HttpURLConnection? = null
                    try {
                        connection = (URL(cleanUrl).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 15000
                            readTimeout = 60000
                            instanceFollowRedirects = true
                            setRequestProperty("User-Agent", "Akselerasi-Edu-Android-Client/1.0")
                        }
                        connection.connect()

                        val code = connection.responseCode
                        if (code in 200..299) {
                            val tempFile = File(context.cacheDir, "${cacheFileName}.tmp")
                            connection.inputStream.use { inputStream ->
                                FileOutputStream(tempFile).use { outputStream ->
                                    val buffer = ByteArray(16384)
                                    var bytesRead: Int
                                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                        outputStream.write(buffer, 0, bytesRead)
                                    }
                                    outputStream.flush()
                                }
                            }

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
                    } finally {
                        try {
                            connection?.disconnect()
                        } catch (_: Throwable) {}
                    }
                }

                if (!downloadSuccess || !pdfFile.exists() || pdfFile.length() < 1000) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Gagal mengunduh berkas PDF buku dari server. Periksa koneksi internet Anda."
                        isLoading = false
                    }
                    return@withContext
                }

                withContext(Dispatchers.Main) {
                    downloadedFile = pdfFile
                }
            } catch (e: Exception) {
                Timber.e(e, "Error preparing PDF document")
                withContext(Dispatchers.Main) {
                    errorMessage = "Gagal memproses dokumen PDF: ${e.localizedMessage}"
                    isLoading = false
                }
            }
        }
    }

    // Manage ParcelFileDescriptor and PdfRenderer lifecycle safely via DisposableEffect
    DisposableEffect(downloadedFile) {
        val file = downloadedFile
        var activeController: PdfRenderController? = null
        if (file != null && file.exists()) {
            try {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val screenWidth = context.resources.displayMetrics.widthPixels
                activeController = PdfRenderController(pfd, renderer, screenWidth)
                controller = activeController
                totalDocPages = activeController.pageCount
                isLoading = false
            } catch (t: Throwable) {
                Timber.e(t, "Error opening PdfRenderer")
                try { file.delete() } catch (_: Throwable) {}
                errorMessage = "Gagal membuka dokumen PDF: ${t.localizedMessage}"
                isLoading = false
            }
        }
        onDispose {
            activeController?.close()
            if (controller === activeController) {
                controller = null
            }
        }
    }

    val activeCtrl = controller
    val pageNumbers = remember(totalDocPages, startPage, endPage, showFullDocument) {
        if (totalDocPages <= 0) {
            emptyList()
        } else if (showFullDocument || (startPage == null && endPage == null)) {
            (1..totalDocPages).toList()
        } else {
            val s = (startPage ?: 1).coerceIn(1, totalDocPages)
            val e = (endPage ?: totalDocPages).coerceIn(s, totalDocPages)
            (s..e).toList()
        }
    }

    var fullscreenPageNumber by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicNavy)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .height(150.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(14.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(10.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }
        } else if (activeCtrl != null && pageNumbers.isNotEmpty()) {
            val sPage = pageNumbers.first()
            val ePage = pageNumbers.last()

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = NeonBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Hal. $sPage — $ePage (${pageNumbers.size} dari $totalDocPages Hal.)",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if ((startPage != null || endPage != null) && totalDocPages > pageNumbers.size) {
                            OutlinedButton(
                                onClick = { showFullDocument = !showFullDocument },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    if (showFullDocument) "Halaman Tugas" else "Semua ($totalDocPages Hal)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                                Text("Buka Eksternal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Lazy on-demand page rendering inside bounded LazyColumn
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 380.dp, max = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(
                        items = pageNumbers,
                        key = { _, pageNum -> pageNum }
                    ) { _, pageNum ->
                        var pageBitmap by remember(activeCtrl, pageNum) { mutableStateOf<ImageBitmap?>(null) }
                        LaunchedEffect(activeCtrl, pageNum) {
                            pageBitmap = activeCtrl.renderPage(pageNum)
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { fullscreenPageNumber = pageNum }
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 260.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val bmp = pageBitmap
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp,
                                            contentDescription = "Halaman $pageNum",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight(),
                                            contentScale = ContentScale.FillWidth
                                        )
                                    } else {
                                        CircularProgressIndicator(
                                            color = NeonBlue,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                    }

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
                                        .background(Color(0xFF000000))
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Buku Resmi • Halaman $pageNum${if (totalDocPages > 0) " dari $totalDocPages" else ""}",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
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
    val fsPageNum = fullscreenPageNumber
    if (fsPageNum != null && activeCtrl != null && pageNumbers.isNotEmpty()) {
        val initialIdx = pageNumbers.indexOf(fsPageNum).coerceAtLeast(0)
        var currentIndex by remember(fsPageNum) { mutableIntStateOf(initialIdx) }
        val activePageNum = pageNumbers.getOrElse(currentIndex) { pageNumbers.first() }
        var fullscreenBitmap by remember(activeCtrl, activePageNum) { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(activeCtrl, activePageNum) {
            fullscreenBitmap = activeCtrl.renderPage(activePageNum)
        }

        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        Dialog(
            onDismissRequest = { fullscreenPageNumber = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B0F19))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .pointerInput(currentIndex) {
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
                        val bmp = fullscreenBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = "Halaman $activePageNum",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = NeonBlue)
                            }
                        }
                    }
                }

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
                        onClick = { fullscreenPageNumber = null },
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
                            "Halaman $activePageNum (Lembar ${currentIndex + 1}/${pageNumbers.size})",
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

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    color = Color(0xFF0F172A).copy(alpha = 0.96f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                    shadowElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (currentIndex > 0) NeonBlue else Color.White.copy(alpha = 0.10f))
                                .clickable(enabled = currentIndex > 0) {
                                    currentIndex--
                                    scale = 1f; offsetX = 0f; offsetY = 0f
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ChevronLeft, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Sebelumnya", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${currentIndex + 1} / ${pageNumbers.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (currentIndex < pageNumbers.size - 1) NeonBlue else Color.White.copy(alpha = 0.10f))
                                .clickable(enabled = currentIndex < pageNumbers.size - 1) {
                                    currentIndex++
                                    scale = 1f; offsetX = 0f; offsetY = 0f
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Selanjutnya", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

