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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.abs
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

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = currentPageIndex.coerceIn(0, (pageNumbers.size - 1).coerceAtLeast(0)),
        pageCount = { pageNumbers.size.coerceAtLeast(1) }
    )

    // Keep currentPageIndex and pagerState in sync when document finishes loading
    LaunchedEffect(pageNumbers.size, isLoading) {
        if (!isLoading && pageNumbers.isNotEmpty()) {
            val targetIdx = currentPageIndex.coerceIn(0, pageNumbers.size - 1)
            if (pagerState.currentPage != targetIdx) {
                pagerState.scrollToPage(targetIdx)
            }
        }
    }

    LaunchedEffect(pagerState.currentPage, pageNumbers) {
        if (pageNumbers.isNotEmpty() && pagerState.currentPage in pageNumbers.indices) {
            currentPageIndex = pagerState.currentPage
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            onPageChanged(pageNumbers[pagerState.currentPage])
        }
    }

    val currentPageNumber = pageNumbers.getOrElse(currentPageIndex) { 1 }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            decorFitsSystemWindows = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F19))
                .systemBarsPadding()
        ) {
            // ── TOP BAR ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
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

                if (scale > 1.05f) {
                    IconButton(
                        onClick = { scale = 1f; offsetX = 0f; offsetY = 0f },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.ZoomOutMap,
                            contentDescription = "Reset Zoom",
                            tint = NeonBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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

            // ── MAIN READER BODY (SWIPEABLE HORIZONTAL PAGER) ────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                } else if (activeSession != null && pageNumbers.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = scale <= 1.05f,
                        beyondViewportPageCount = 1,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIdx ->
                        val pageNum = pageNumbers.getOrElse(pageIdx) { 1 }
                        var pageBitmap by remember(activeSession, pageNum) { mutableStateOf<ImageBitmap?>(null) }

                        LaunchedEffect(activeSession, pageNum) {
                            pageBitmap = activeSession.renderPage(pageNum)
                        }

                        val isCurrentPage = pageIdx == pagerState.currentPage
                        val pageScale = if (isCurrentPage) scale else 1f
                        val pageOffsetX = if (isCurrentPage) offsetX else 0f
                        val pageOffsetY = if (isCurrentPage) offsetY else 0f

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                .pointerInput(pageIdx) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (scale > 1.2f) {
                                                scale = 1f
                                                offsetX = 0f
                                                offsetY = 0f
                                            } else {
                                                scale = 2.2f
                                            }
                                        }
                                    )
                                }
                                .pointerInput(pageIdx) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        do {
                                            val event = awaitPointerEvent()
                                            val pointerCount = event.changes.count { it.pressed }
                                            if (pointerCount >= 2) {
                                                val zoomChange = event.calculateZoom()
                                                val panChange = event.calculatePan()
                                                if (zoomChange != 1f || panChange != androidx.compose.ui.geometry.Offset.Zero) {
                                                    scale = (scale * zoomChange).coerceIn(1f, 5f)
                                                    if (scale > 1.05f) {
                                                        offsetX += panChange.x
                                                        offsetY += panChange.y
                                                    } else {
                                                        offsetX = 0f
                                                        offsetY = 0f
                                                    }
                                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                                }
                                            } else if (scale > 1.05f && pointerCount == 1) {
                                                val panChange = event.calculatePan()
                                                offsetX += panChange.x
                                                offsetY += panChange.y
                                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                                            }
                                        } while (event.changes.any { it.pressed })
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .graphicsLayer(
                                        scaleX = pageScale,
                                        scaleY = pageScale,
                                        translationX = pageOffsetX,
                                        translationY = pageOffsetY
                                    )
                            ) {
                                val bmp = pageBitmap
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = "Halaman $pageNum",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(420.dp)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = NeonBlue)
                                    }
                                }
                            }
                        }
                    }

                    // Floating Zoom Controls on Right Edge
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 14.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = { scale = (scale + 0.5f).coerceAtMost(5f) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Perbesar", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "${(scale * 100).toInt()}%",
                            color = NeonBlue,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                scale = (scale - 0.5f).coerceAtLeast(1f)
                                if (scale <= 1.05f) {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Perkecil", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // ── BOTTOM PAGE NAVIGATION BAR ───────────────────────────────────
            if (pageNumbers.isNotEmpty()) {
                val density = LocalDensity.current
                val systemNavBarHeightDp = remember(context) {
                    val resId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
                    if (resId > 0) {
                        with(density) { context.resources.getDimensionPixelSize(resId).toDp() }
                    } else {
                        48.dp
                    }
                }.coerceIn(44.dp, 64.dp)
                val insetNavBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val safeBottomPad = maxOf(insetNavBottom, systemNavBarHeightDp)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                    shadowElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = safeBottomPad + 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val canGoPrev = pagerState.currentPage > 0
                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (canGoPrev) NeonBlue else Color.White.copy(alpha = 0.10f))
                                .clickable(enabled = canGoPrev) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ChevronLeft, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Sebelumnya", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Hal. $currentPageNumber (${pagerState.currentPage + 1} / ${pageNumbers.size})",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Geser kiri/kanan untuk pindah halaman",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.5.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        val canGoNext = pagerState.currentPage < pageNumbers.size - 1
                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (canGoNext) NeonBlue else Color.White.copy(alpha = 0.10f))
                                .clickable(enabled = canGoNext) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Selanjutnya", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
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

