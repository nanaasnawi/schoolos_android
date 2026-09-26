package com.schoolos.android.core.designsystem

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
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

/**
 * Full-screen in-app book reader dialog for SIBI Kemendikdasmen textbooks and learning modules.
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
    var pages by remember { mutableStateOf<List<SharedRenderedPdfPage>>(emptyList()) }
    var totalDocPages by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPageIndex by remember { mutableIntStateOf(0) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(pdfUrl, startPage, endPage) {
        isLoading = true
        errorMessage = null
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
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to download PDF: $cleanUrl")
                    }
                }

                if (!downloadSuccess || !pdfFile.exists() || pdfFile.length() < 1000) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Gagal mengunduh berkas buku dari server SIBI. Periksa koneksi internet Anda."
                        isLoading = false
                    }
                    return@withContext
                }

                val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                totalDocPages = renderer.pageCount

                val reqStart = startPage ?: 1
                val reqEnd = endPage ?: totalDocPages

                val startIndex = (reqStart - 1).coerceIn(0, totalDocPages - 1)
                val endIndex = (reqEnd - 1).coerceIn(startIndex, totalDocPages - 1)

                val renderedBitmaps = mutableListOf<SharedRenderedPdfPage>()
                val displayMetrics = context.resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels

                for (idx in startIndex..endIndex) {
                    val page = renderer.openPage(idx)
                    val factor = (screenWidth.toFloat() / page.width.toFloat()).coerceIn(1.0f, 1.8f)
                    val bmpWidth = (page.width * factor).toInt()
                    val bmpHeight = (page.height * factor).toInt()
                    val bitmap = createBitmap(bmpWidth, bmpHeight)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    renderedBitmaps.add(
                        SharedRenderedPdfPage(pageNumber = idx + 1, bitmap = bitmap.asImageBitmap())
                    )
                }

                renderer.close()
                pfd.close()

                withContext(Dispatchers.Main) {
                    pages = renderedBitmaps
                    val targetPage = initialPage.coerceIn(reqStart, reqEnd)
                    val initIndex = renderedBitmaps.indexOfFirst { it.pageNumber == targetPage }.coerceAtLeast(0)
                    currentPageIndex = initIndex
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Gagal memproses buku: ${e.localizedMessage}"
                    isLoading = false
                }
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
                            text = if (pages.isNotEmpty()) {
                                "$subject • Halaman ${pages[currentPageIndex].pageNumber} dari ${totalDocPages.coerceAtLeast(pages.size)}"
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
                if (pages.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (currentPageIndex > 0) NeonBlue else Color.White.copy(alpha = 0.12f))
                                .clickable(enabled = currentPageIndex > 0) {
                                    if (currentPageIndex > 0) {
                                        currentPageIndex--
                                        scale = 1f; offsetX = 0f; offsetY = 0f
                                        onPageChanged(pages[currentPageIndex].pageNumber)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ChevronLeft, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Sebelumnya", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "${currentPageIndex + 1} / ${pages.size}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (currentPageIndex < pages.size - 1) NeonBlue else Color.White.copy(alpha = 0.12f))
                                .clickable(enabled = currentPageIndex < pages.size - 1) {
                                    if (currentPageIndex < pages.size - 1) {
                                        currentPageIndex++
                                        scale = 1f; offsetX = 0f; offsetY = 0f
                                        onPageChanged(pages[currentPageIndex].pageNumber)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Berikutnya", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(16.dp))
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
                } else if (pages.isNotEmpty()) {
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
                            Image(
                                bitmap = pages[currentPageIndex].bitmap,
                                contentDescription = "Halaman ${pages[currentPageIndex].pageNumber}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }
    }
}
