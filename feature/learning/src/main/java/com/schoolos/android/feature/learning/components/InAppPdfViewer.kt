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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.schoolos.android.core.designsystem.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    isTeacher: Boolean = false,
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

                val candidateUrls = resolvePdfDownloadUrls(cleanUrl)
                if (candidateUrls.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Tautan dokumen PDF buku belum disematkan."
                        isLoading = false
                    }
                    return@withContext
                }

                val cacheFileName = "book_pdf_${cleanUrl.hashCode().toString().replace("-", "n")}.pdf"
                val pdfFile = File(context.cacheDir, cacheFileName)

                var downloadSuccess = pdfFile.exists() && pdfFile.length() > 200

                if (!downloadSuccess) {
                    for (targetUrl in candidateUrls) {
                        if (downloadSuccess) break
                        var currentTarget = targetUrl
                        var redirectCount = 0
                        while (redirectCount < 5) {
                            var connection: HttpURLConnection? = null
                            try {
                                val urlObj = URL(currentTarget)
                                connection = (urlObj.openConnection() as HttpURLConnection).apply {
                                    connectTimeout = 15000
                                    readTimeout = 60000
                                    instanceFollowRedirects = true
                                    setRequestProperty("User-Agent", "Akselerasi-Edu-Android-Client/1.0")
                                    setRequestProperty("Accept", "application/pdf,application/octet-stream,*/*")
                                }
                                connection.connect()

                                val code = connection.responseCode
                                if (code in 300..399) {
                                    val redirectLocation = connection.getHeaderField("Location")
                                    connection.disconnect()
                                    if (!redirectLocation.isNullOrBlank()) {
                                        currentTarget = if (redirectLocation.startsWith("http://") || redirectLocation.startsWith("https://")) {
                                            redirectLocation
                                        } else {
                                            URL(urlObj, redirectLocation).toString()
                                        }
                                        redirectCount++
                                        continue
                                    }
                                }

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

                                    if (tempFile.length() > 200) {
                                        if (pdfFile.exists()) pdfFile.delete()
                                        tempFile.renameTo(pdfFile)
                                        downloadSuccess = true
                                        break
                                    }
                                } else {
                                    Timber.w("PDF download responded with HTTP code $code from $currentTarget")
                                    break
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to download PDF from $currentTarget")
                                break
                            } finally {
                                try {
                                    connection?.disconnect()
                                } catch (_: Throwable) {}
                            }
                        }
                    }
                }

                if (!downloadSuccess || !pdfFile.exists() || pdfFile.length() < 200) {
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
    val isPageRestricted = (startPage != null || endPage != null)
    val pageNumbers = remember(totalDocPages, startPage, endPage, showFullDocument, isTeacher) {
        if (totalDocPages <= 0) {
            emptyList()
        } else if (isTeacher && showFullDocument) {
            (1..totalDocPages).toList()
        } else if (!isPageRestricted) {
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
                            if (isPageRestricted) {
                                if (isTeacher && showFullDocument) {
                                    "Seluruh Dokumen ($totalDocPages Hal.)"
                                } else {
                                    "Hal. $sPage — $ePage (${pageNumbers.size} Halaman Ditugaskan)"
                                }
                            } else {
                                "Dokumen Buku ($totalDocPages Hal.)"
                            },
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Strict Student Privacy & Pedagogical Focus:
                        // Only teachers can toggle full document; students can NEVER view unassigned pages!
                        if (isTeacher && isPageRestricted && totalDocPages > pageNumbers.size) {
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

                        // External opening is restricted to teachers or when the whole document is assigned
                        // to prevent students from bypassing the page restriction outside the app.
                        val resolvedExternalUrl = remember(pdfUrl) {
                            resolvePdfDownloadUrls(pdfUrl).firstOrNull() ?: pdfUrl
                        }
                        if (resolvedExternalUrl.isNotBlank() && (isTeacher || !isPageRestricted)) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(resolvedExternalUrl), "application/pdf")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(resolvedExternalUrl))
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

                val inlineScope = rememberCoroutineScope()
                val inlinePagerState = rememberPagerState(
                    initialPage = 0,
                    pageCount = { pageNumbers.size.coerceAtLeast(1) }
                )

                // Swipeable HorizontalPager for smooth left/right page sliding
                HorizontalPager(
                    state = inlinePagerState,
                    beyondViewportPageCount = 1,
                    pageSpacing = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { pageIdx ->
                    val pageNum = pageNumbers.getOrElse(pageIdx) { 1 }
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
                                    .heightIn(min = 340.dp),
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
                                    text = if (isPageRestricted) {
                                        "Buku Resmi • Halaman $pageNum (Lembar ${pageIdx + 1}/${pageNumbers.size})"
                                    } else {
                                        "Buku Resmi • Halaman $pageNum${if (totalDocPages > 0) " dari $totalDocPages" else ""}"
                                    },
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Inline Page Navigation Bar (Slide + Prev/Next Buttons)
                if (pageNumbers.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CosmicSurface2)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val canPrev = inlinePagerState.currentPage > 0
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (canPrev) NeonBlue else CosmicNavy)
                                .clickable(enabled = canPrev) {
                                    inlineScope.launch {
                                        inlinePagerState.animateScrollToPage(inlinePagerState.currentPage - 1)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    contentDescription = null,
                                    tint = if (canPrev) Color.White else TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    "Sebelumnya",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canPrev) Color.White else TextTertiary
                                )
                            }
                        }

                        Text(
                            text = "Lembar ${inlinePagerState.currentPage + 1} / ${pageNumbers.size}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        val canNext = inlinePagerState.currentPage < pageNumbers.size - 1
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (canNext) NeonBlue else CosmicNavy)
                                .clickable(enabled = canNext) {
                                    inlineScope.launch {
                                        inlinePagerState.animateScrollToPage(inlinePagerState.currentPage + 1)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Selanjutnya",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canNext) Color.White else TextTertiary
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = if (canNext) Color.White else TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
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

                        val resolvedBrowserUrl = remember(pdfUrl) {
                            resolvePdfDownloadUrls(pdfUrl).firstOrNull() ?: pdfUrl
                        }
                        if (resolvedBrowserUrl.isNotBlank() && (isTeacher || !isPageRestricted)) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resolvedBrowserUrl))
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

    // ── FULLSCREEN PDF PAGE LIGHTBOX (SWIPEABLE HORIZONTAL PAGER) ─────────────
    val fsPageNum = fullscreenPageNumber
    if (fsPageNum != null && activeCtrl != null && pageNumbers.isNotEmpty()) {
        val initialIdx = pageNumbers.indexOf(fsPageNum).coerceAtLeast(0)
        val fsScope = rememberCoroutineScope()
        val fsPagerState = rememberPagerState(
            initialPage = initialIdx,
            pageCount = { pageNumbers.size.coerceAtLeast(1) }
        )
        val currentIndex = fsPagerState.currentPage
        val activePageNum = pageNumbers.getOrElse(currentIndex) { pageNumbers.first() }

        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(currentIndex) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }

        Dialog(
            onDismissRequest = { fullscreenPageNumber = null },
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { fullscreenPageNumber = null },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Halaman $activePageNum (Lembar ${currentIndex + 1}/${pageNumbers.size})",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (scale > 1.05f) {
                        IconButton(
                            onClick = { scale = 1f; offsetX = 0f; offsetY = 0f },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(NeonBlue.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Reset Zoom", tint = Color.White)
                        }
                    } else {
                        Spacer(Modifier.size(38.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalPager(
                        state = fsPagerState,
                        userScrollEnabled = scale <= 1.05f,
                        beyondViewportPageCount = 1,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIdx ->
                        val pageNum = pageNumbers.getOrElse(pageIdx) { pageNumbers.first() }
                        var fullscreenBitmap by remember(activeCtrl, pageNum) { mutableStateOf<ImageBitmap?>(null) }

                        LaunchedEffect(activeCtrl, pageNum) {
                            fullscreenBitmap = activeCtrl.renderPage(pageNum)
                        }

                        val isCurrent = pageIdx == fsPagerState.currentPage
                        val pageScale = if (isCurrent) scale else 1f
                        val pageOffsetX = if (isCurrent) offsetX else 0f
                        val pageOffsetY = if (isCurrent) offsetY else 0f

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                .pointerInput(pageIdx) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (scale > 1.2f) {
                                                scale = 1f; offsetX = 0f; offsetY = 0f
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
                                                    scale = (scale * zoomChange).coerceIn(1f, 6f)
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
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
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
                                val bmp = fullscreenBitmap
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
                            onClick = {
                                scale = (scale - 0.5f).coerceAtLeast(1f)
                                if (scale <= 1.05f) {
                                    offsetX = 0f; offsetY = 0f
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Perkecil", tint = Color.White)
                        }
                    }
                }

                val density = androidx.compose.ui.platform.LocalDensity.current
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
                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (currentIndex > 0) NeonBlue else Color.White.copy(alpha = 0.10f))
                                .clickable(enabled = currentIndex > 0) {
                                    fsScope.launch {
                                        fsPagerState.animateScrollToPage(currentIndex - 1)
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
                                text = "${currentIndex + 1} / ${pageNumbers.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (currentIndex < pageNumbers.size - 1) NeonBlue else Color.White.copy(alpha = 0.10f))
                                .clickable(enabled = currentIndex < pageNumbers.size - 1) {
                                    fsScope.launch {
                                        fsPagerState.animateScrollToPage(currentIndex + 1)
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
    }
}

/**
 * Resolves any raw PDF URL (whether relative /uploads/uuid.pdf, uploads/uuid.pdf, or full HTTPS)
 * into a ordered list of download URLs with domain fallback (production web & direct backend).
 */
fun resolvePdfDownloadUrls(rawUrl: String): List<String> {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return emptyList()

    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        val urls = mutableListOf(trimmed)
        if (trimmed.contains("akselerasi-edu.id/uploads/")) {
            urls.add(trimmed.replace("://akselerasi-edu.id", "://www.akselerasi-edu.id"))
            urls.add(trimmed.replace("www.akselerasi-edu.id", "schoolosbackend-production.up.railway.app").replace("akselerasi-edu.id", "schoolosbackend-production.up.railway.app"))
        } else if (trimmed.contains("schoolosbackend-production.up.railway.app/uploads/")) {
            urls.add(trimmed.replace("schoolosbackend-production.up.railway.app", "www.akselerasi-edu.id"))
            urls.add(trimmed.replace("schoolosbackend-production.up.railway.app", "akselerasi-edu.id"))
        }
        return urls.distinct()
    }

    val path = when {
        trimmed.startsWith("/uploads/") -> trimmed
        trimmed.startsWith("uploads/") -> "/$trimmed"
        trimmed.startsWith("/") -> trimmed
        else -> "/uploads/$trimmed"
    }

    return listOf(
        "https://schoolosbackend-production.up.railway.app$path",
        "https://www.akselerasi-edu.id$path",
        "https://akselerasi-edu.id$path"
    )
}

