package com.schoolos.android.core.designsystem

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class SharedRenderedPdfPage(
    val pageNumber: Int,
    val bitmap: ImageBitmap
)

private class BookPdfRenderSession(
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
                val idx = (pageNumber1Based - 1).coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                var page: PdfRenderer.Page? = null
                try {
                    page = renderer.openPage(idx)
                    val factor = (screenWidth.toFloat() / page.width.toFloat()).coerceIn(1.0f, 1.6f)
                    val bmpWidth = (page.width * factor).toInt().coerceAtLeast(1)
                    val bmpHeight = (page.height * factor).toInt().coerceAtLeast(1)
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
                    Timber.e(t, "Failed rendering book PDF page $pageNumber1Based")
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
 * Full-screen in-app book reader dialog for SIBI Kemendikdasmen textbooks and learning modules.
 * Renders pages lazily on-demand with a bounded 5-page LruCache and RGB_565 bitmaps.
 */
@Composable
fun BookReaderDialog(
    title: String,
    pdfUrl: String,
    subject: String = "Buku SIBI",
    startPage: Int? = null,
    endPage: Int? = null,
    initialPage: Int = 1,
    onPageChanged: (Int) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var session by remember { mutableStateOf<BookPdfRenderSession?>(null) }
    var totalDocPages by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPageIndex by remember { mutableIntStateOf(0) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(pdfUrl) {
        isLoading = true
        errorMessage = null
        downloadedFile = null
        withContext(Dispatchers.IO) {
            try {
                val cleanUrl = pdfUrl.trim()
                if (cleanUrl.isBlank()) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Tautan dokumen PDF buku belum tersedia."
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
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to download PDF: $cleanUrl")
                    } finally {
                        try {
                            connection?.disconnect()
                        } catch (_: Throwable) {}
                    }
                }

                if (!downloadSuccess || !pdfFile.exists() || pdfFile.length() < 1000) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Gagal mengunduh berkas buku dari server SIBI. Periksa koneksi internet Anda."
                        isLoading = false
                    }
                    return@withContext
                }

                withContext(Dispatchers.Main) {
                    downloadedFile = pdfFile
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Gagal memproses buku: ${e.localizedMessage}"
                    isLoading = false
                }
            }
        }
    }

    DisposableEffect(downloadedFile) {
        val file = downloadedFile
        var activeSession: BookPdfRenderSession? = null
        if (file != null && file.exists()) {
            try {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val screenWidth = context.resources.displayMetrics.widthPixels
                activeSession = BookPdfRenderSession(pfd, renderer, screenWidth)
                session = activeSession
                totalDocPages = activeSession.pageCount
                val reqStart = (startPage ?: 1).coerceIn(1, totalDocPages.coerceAtLeast(1))
                val reqEnd = (endPage ?: totalDocPages).coerceIn(reqStart, totalDocPages.coerceAtLeast(1))
                val targetPage = initialPage.coerceIn(reqStart, reqEnd)
                currentPageIndex = (targetPage - reqStart).coerceAtLeast(0)
                isLoading = false
            } catch (t: Throwable) {
                Timber.e(t, "Failed opening PdfRenderer in BookReaderDialog")
                errorMessage = "Gagal membuka buku: ${t.localizedMessage}"
                isLoading = false
            }
        }
        onDispose {
            activeSession?.close()
            if (session === activeSession) {
                session = null
            }
        }
    }

    val activeSession = session
    val pageNumbers = remember(totalDocPages, startPage, endPage) {
        if (totalDocPages <= 0) {
            emptyList()
        } else {
            val s = (startPage ?: 1).coerceIn(1, totalDocPages)
            val e = (endPage ?: totalDocPages).coerceIn(s, totalDocPages)
            (s..e).toList()
        }
    }

    val currentPageNumber = pageNumbers.getOrElse(currentPageIndex) { 1 }
    var currentBitmap by remember(activeSession, currentPageNumber) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(activeSession, currentPageNumber) {
        if (activeSession != null && pageNumbers.isNotEmpty()) {
            currentBitmap = activeSession.renderPage(currentPageNumber)
            // Pre-warm next page in background into bounded LruCache(5)
            val nextPageIdx = currentPageIndex + 1
            if (nextPageIdx in pageNumbers.indices) {
                activeSession.renderPage(pageNumbers[nextPageIdx])
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Scaffold(
            containerColor = Color(0xFF0B0F19),
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (pageNumbers.isNotEmpty()) {
                                "$subject • Halaman $currentPageNumber dari ${totalDocPages.coerceAtLeast(pageNumbers.size)}"
                            } else {
                                subject
                            },
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (pdfUrl.isNotBlank()) {
                        IconButton(
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
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.OpenInBrowser,
                                contentDescription = "Buka PDF Eksternal",
                                tint = NeonBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            },
            bottomBar = {
                if (pageNumbers.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
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
                                    .background(if (currentPageIndex > 0) NeonBlue else Color.White.copy(alpha = 0.10f))
                                    .clickable(enabled = currentPageIndex > 0) {
                                        if (currentPageIndex > 0) {
                                            currentPageIndex--
                                            scale = 1f; offsetX = 0f; offsetY = 0f
                                            onPageChanged(pageNumbers[currentPageIndex])
                                        }
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
                                    text = "${currentPageIndex + 1} / ${pageNumbers.size}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (currentPageIndex < pageNumbers.size - 1) NeonBlue else Color.White.copy(alpha = 0.10f))
                                    .clickable(enabled = currentPageIndex < pageNumbers.size - 1) {
                                        if (currentPageIndex < pageNumbers.size - 1) {
                                            currentPageIndex++
                                            scale = 1f; offsetX = 0f; offsetY = 0f
                                            onPageChanged(pageNumbers[currentPageIndex])
                                        }
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
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp),
                    ) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(320.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(16.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.35f)
                                .height(12.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                } else if (errorMessage != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = NeonError, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "Terjadi kesalahan",
                            color = Color.White,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else if (pageNumbers.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                            .pointerInput(currentPageIndex) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1f, 5f)
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
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            val bmp = currentBitmap
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = "Halaman $currentPageNumber",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White)
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = NeonBlue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

