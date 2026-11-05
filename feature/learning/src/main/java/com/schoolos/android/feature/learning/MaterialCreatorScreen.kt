package com.schoolos.android.feature.learning

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.LibraryBook
import com.schoolos.android.domain.model.MaterialType
import com.schoolos.android.feature.learning.components.InAppVideoPlayer
import androidx.compose.ui.window.Dialog
import android.content.Intent

import com.schoolos.android.domain.model.groupParallelClasses
import com.schoolos.android.domain.model.findClass

private fun queryFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = it.getString(index)
                }
            }
        }
    }
    if (result == null) {
        val path = uri.path
        val cut = path?.lastIndexOf('/') ?: -1
        if (cut != -1 && path != null) {
            result = path.substring(cut + 1)
        }
    }
    return result
}

private const val MAX_MATERIAL_UPLOAD_BYTES = 25L * 1024L * 1024L // 25 MB

private suspend fun readMaterialUriBytes(
    context: android.content.Context,
    uri: Uri,
    maxBytes: Long = MAX_MATERIAL_UPLOAD_BYTES
): ByteArray = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                    val fileSize = cursor.getLong(sizeIdx)
                    if (fileSize > maxBytes) {
                        throw IllegalArgumentException("Ukuran berkas melebihi batas maksimal 25 MB.")
                    }
                }
            }
        }
    }
    val bytes = context.contentResolver.openInputStream(uri)?.use { stream -> stream.readBytes() }
        ?: throw IllegalArgumentException("Gagal membaca berkas yang dipilih.")
    if (bytes.size > maxBytes) {
        throw IllegalArgumentException("Ukuran berkas melebihi batas maksimal 25 MB.")
    }
    bytes
}

private val EmeraldGlow     = Color(0xFF059669)
private val TealAccent      = Color(0xFF0891B2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialCreatorScreen(
    initialClass: String? = null,
    initialSubject: String? = null,
    onBack: () -> Unit = {},
    onSuccess: () -> Unit = {},
    viewModel: MaterialCreatorViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    val unifiedClasses = remember(state.availableClasses) {
        state.availableClasses.groupParallelClasses()
    }
    val classOptions = remember(unifiedClasses) {
        unifiedClasses.map { it.displayName }
    }
    val subjectOptions = remember(state.availableSubjects) {
        state.availableSubjects.map { it.name }
    }

    var selectedClass by remember {
        mutableStateOf(
            unifiedClasses.findClass(initialClass)?.displayName
                ?: initialClass
                ?: ""
        )
    }
    var selectedSubject by remember { mutableStateOf(initialSubject ?: "") }
    var isClassMenuExpanded by remember { mutableStateOf(false) }
    var isSubjectMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(classOptions) {
        if (selectedClass.isEmpty() && classOptions.isNotEmpty()) {
            selectedClass = classOptions.first()
        } else if (selectedClass.isNotEmpty()) {
            unifiedClasses.findClass(selectedClass)?.let {
                selectedClass = it.displayName
            }
        }
    }

    LaunchedEffect(subjectOptions, state.isSubjectLocked, state.teacherSubject) {
        if (state.isSubjectLocked && !state.teacherSubject.isNullOrBlank()) {
            val matched = subjectOptions.firstOrNull { 
                it.equals(state.teacherSubject!!, ignoreCase = true) ||
                it.contains(state.teacherSubject!!, ignoreCase = true) || 
                state.teacherSubject!!.contains(it, ignoreCase = true) 
            }
            selectedSubject = matched ?: subjectOptions.firstOrNull() ?: state.teacherSubject!!
        } else if (selectedSubject.isEmpty() && subjectOptions.isNotEmpty()) {
            selectedSubject = subjectOptions.first()
        }
    }

    LaunchedEffect(selectedClass, selectedSubject, state.availableBooks) {
        val resolvedName = unifiedClasses.findClass(selectedClass)?.memberClassNames?.firstOrNull() ?: selectedClass
        viewModel.updateRecommendation(resolvedName, selectedSubject)
    }

    var selectedType by remember { mutableStateOf(MaterialType.DOCUMENT) }
    var pdfSourceMode by remember { mutableStateOf("SIBI") } // "SIBI" vs "UPLOAD"
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var contentBody by remember { mutableStateOf("") }
    var mediaUrl by remember { mutableStateOf("") }
    var showPreview by remember { mutableStateOf(false) }
    var showBookCatalogSheet by remember { mutableStateOf(false) }
    var selectedBook by remember { mutableStateOf<LibraryBook?>(null) }
    var startPageInput by remember { mutableStateOf("1") }
    var endPageInput by remember { mutableStateOf("10") }
    var showPrePublishDialog by remember { mutableStateOf(false) }
    var previewingSelectedBook by remember { mutableStateOf(false) }
    var previewingYoutubeVideo by remember { mutableStateOf<YoutubeVideoResult?>(null) }
    var isChangingYoutubeVideo by remember { mutableStateOf(false) }
    var infographicViewMode by remember { mutableStateOf("EDIT") } // "EDIT" vs "CANVAS"
    var articleViewMode by remember { mutableStateOf("EDIT") } // "EDIT" vs "PREVIEW"
    var showAiGenerateDialog by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val rawName = withContext(Dispatchers.IO) { queryFileName(context, it) }
                        ?: "modul_ajar_${System.currentTimeMillis()}.pdf"
                    val fileName = if (!rawName.contains(".")) "$rawName.pdf" else rawName
                    val bytes = readMaterialUriBytes(context, it)
                    viewModel.uploadFile(bytes, fileName, "application/pdf")
                } catch (err: Exception) {
                    Toast.makeText(context, err.message ?: "Gagal membaca dokumen.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val mime = context.contentResolver.getType(it) ?: "image/jpeg"
                    val ext = when (mime) {
                        "image/png" -> ".png"
                        "image/webp" -> ".webp"
                        "image/gif" -> ".gif"
                        else -> ".jpg"
                    }
                    val rawName = withContext(Dispatchers.IO) { queryFileName(context, it) }
                        ?: "gambar_materi_${System.currentTimeMillis()}$ext"
                    val fileName = if (!rawName.contains(".")) "$rawName$ext" else rawName
                    val bytes = readMaterialUriBytes(context, it)
                    viewModel.uploadFile(bytes, fileName, mime)
                } catch (err: Exception) {
                    Toast.makeText(context, err.message ?: "Gagal membaca gambar.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(state.uploadedFileUrl) {
        state.uploadedFileUrl?.let { mediaUrl = it }
    }

    LaunchedEffect(state.success) {
        if (state.success) {
            Toast.makeText(context, "Materi ajar berhasil diterbitkan ke siswa!", Toast.LENGTH_SHORT).show()
            showPrePublishDialog = false
            onSuccess()
            viewModel.resetState()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
        }
    }

    val applySelectedBook: (LibraryBook, String, String) -> Unit = { book, sPage, ePage ->
        selectedBook = book
        startPageInput = sPage
        endPageInput = ePage
        val sInt = sPage.toIntOrNull() ?: 1
        val eInt = ePage.toIntOrNull() ?: minOf(sInt + 10, book.totalPages)
        title = "Materi Bacaan: ${book.title} (Hal. $sInt–$eInt)"
        description = "Buku Teks Kurikulum SIBI: ${book.title}. Diterbitkan oleh ${book.author ?: book.publisher ?: "Kemdikdasmen"}. Pelajari materi pada halaman $sInt sampai $eInt."
        mediaUrl = book.fileUrl ?: ""
        if (!state.isSubjectLocked) {
            book.subjectName?.let { subj ->
                if (subjectOptions.any { it.equals(subj, ignoreCase = true) }) {
                    selectedSubject = subjectOptions.first { it.equals(subj, ignoreCase = true) }
                }
            }
        }
    }

    if (previewingSelectedBook && selectedBook != null && !selectedBook!!.fileUrl.isNullOrBlank()) {
        BookReaderDialog(
            title = selectedBook!!.title,
            pdfUrl = selectedBook!!.fileUrl ?: "",
            subject = selectedBook!!.subjectName ?: selectedSubject,
            startPage = startPageInput.toIntOrNull(),
            endPage = endPageInput.toIntOrNull(),
            onDismiss = { previewingSelectedBook = false }
        )
    }

    if (previewingYoutubeVideo != null) {
        YoutubePreviewDialog(
            video = previewingYoutubeVideo!!,
            onDismiss = { previewingYoutubeVideo = null },
            onUseVideo = {
                val v = previewingYoutubeVideo!!
                title = v.title
                description = v.description
                mediaUrl = "https://www.youtube.com/watch?v=${v.videoId}"
                isChangingYoutubeVideo = false
                previewingYoutubeVideo = null
                Toast.makeText(context, "Video dipilih! Judul dan deskripsi otomatis terisi.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAiGenerateDialog) {
        MaterialAiGeneratorScreen(
            availableSubjects = state.availableSubjects,
            availableClasses = state.availableClasses,
            currentSubjectName = selectedSubject,
            currentClassName = selectedClass,
            isSubjectLocked = state.isSubjectLocked,
            onBack = { showAiGenerateDialog = false },
            onApply = { generated ->
                title = generated.title
                description = generated.description
                selectedType = if (generated.mode == "INFOGRAPHIC") MaterialType.IMAGE else MaterialType.ARTICLE
                val newBlocks = generated.blocks.map { b ->
                    ContentBlock(
                        id = b.id,
                        type = if (b.type.equals("IMAGE", ignoreCase = true)) ContentBlockType.IMAGE else ContentBlockType.TEXT,
                        content = b.content
                    )
                }
                if (newBlocks.isNotEmpty()) {
                    viewModel.contentBlocks.value = newBlocks
                }
                showAiGenerateDialog = false
                Toast.makeText(context, "Materi \"${generated.title}\" berhasil diterapkan!", Toast.LENGTH_SHORT).show()
            },
            onGenerateRequest = { mode, subName, topic, gradeLevel ->
                viewModel.generateMaterialAsync(
                    mode = mode,
                    topic = topic,
                    gradeLevel = gradeLevel,
                    subjectName = subName
                )
            }
        )
        return
    }

    if (showPrePublishDialog) {
        val matchedUnified = unifiedClasses.findClass(selectedClass)
        val targetClassIds = if (matchedUnified != null && matchedUnified.memberClassIds.isNotEmpty()) {
            matchedUnified.memberClassIds
        } else {
            val singleId = state.availableClasses.find { it.name == selectedClass }?.id ?: selectedClass
            listOf(singleId)
        }
        val targetClassId = targetClassIds.firstOrNull() ?: selectedClass
        val targetSubjectId = state.availableSubjects.find { it.name == selectedSubject }?.id
        val startP = startPageInput.toIntOrNull() ?: 1
        val endP = endPageInput.toIntOrNull() ?: minOf(startP + 10, selectedBook?.totalPages ?: 100)

        val currentBlocks by viewModel.contentBlocks.collectAsState()
        MaterialReviewAndPublishScreen(
            title = title.trim(),
            description = description.trim(),
            selectedType = selectedType,
            mediaUrl = mediaUrl.trim(),
            contentBody = contentBody.trim(),
            contentBlocks = currentBlocks,
            selectedClass = selectedClass,
            selectedSubject = selectedSubject,
            selectedBook = selectedBook,
            startPage = startP.toString(),
            endPage = endP.toString(),
            isLoading = state.isLoading,
            onOpenBookPreview = { previewingSelectedBook = true },
            onBackToEdit = { showPrePublishDialog = false },
            onConfirmPublish = { isScheduled, releaseAtIso ->
                if (selectedType == MaterialType.DOCUMENT && pdfSourceMode == "SIBI" && selectedBook != null) {
                    viewModel.assignReadingBook(
                        book = selectedBook!!,
                        startPage = startP,
                        endPage = endP,
                        instructions = description.trim().ifBlank { null },
                        classId = targetClassId,
                        classIds = targetClassIds,
                        subjectId = targetSubjectId,
                        releaseAt = releaseAtIso,
                        visibility = if (isScheduled) "draft" else "published",
                    )
                } else {
                    val fullDesc = "$selectedSubject • $selectedClass • $description"
                    viewModel.createMaterial(
                        title = title.trim(),
                        description = fullDesc.trim(),
                        materialType = selectedType,
                        mediaUrl = mediaUrl.trim().ifBlank { null },
                        subject = selectedSubject,
                        classId = targetClassId,
                        classIds = targetClassIds,
                        subjectId = targetSubjectId,
                        visibility = if (isScheduled) "draft" else "published",
                        releaseAt = releaseAtIso,
                    )
                }
            }
        )
        return
    }

    val isImeVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
    val isDark = LocalIsDarkTheme.current
    val pageBg = if (isDark) Color(0xFF09090B) else Color(0xFFF8FAFC)
    val surfaceBg = if (isDark) Color(0xFF111113) else Color.White
    val surfaceBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val subSurfaceBg = if (isDark) Color(0xFF18181B) else Color(0xFFF1F5F9)
    val subSurfaceBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val emeraldAccent = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val emeraldSoftBg = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5)
    val emeraldSoftBorder = if (isDark) Color(0xFF059669).copy(alpha = 0.45f) else Color(0xFFA7F3D0)

    Scaffold(
        containerColor = pageBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ExecutiveTopBar(
                title = "Studio Materi Guru",
                subtitle = "Publikasikan Modul Digital ke Kelas",
                onBack = onBack
            )
        },
        bottomBar = {
            if (!isImeVisible) {
                Surface(
                    color = surfaceBg,
                    tonalElevation = 0.dp,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, surfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (selectedType == MaterialType.DOCUMENT) {
                                    if (pdfSourceMode == "SIBI") {
                                        if (selectedBook == null) {
                                            Toast.makeText(context, "Silakan pilih salah satu buku dari katalog SIBI!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (title.isBlank()) {
                                            val sInt = startPageInput.toIntOrNull() ?: 1
                                            val eInt = endPageInput.toIntOrNull() ?: minOf(sInt + 10, selectedBook!!.totalPages)
                                            title = "Materi Bacaan: ${selectedBook!!.title} (Hal. $sInt–$eInt)"
                                        }
                                    } else {
                                        if (title.isBlank()) {
                                            Toast.makeText(context, "Judul modul PDF tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (mediaUrl.isBlank() && state.uploadedFileName.isNullOrBlank()) {
                                            Toast.makeText(context, "Silakan unggah berkas PDF dari perangkat!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                    }
                                } else if (selectedType == MaterialType.VIDEO) {
                                    if (mediaUrl.isBlank()) {
                                        Toast.makeText(context, "Silakan cari dan pilih video dari YouTube!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (title.isBlank()) {
                                        Toast.makeText(context, "Judul video tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                } else {
                                    if (title.isBlank()) {
                                        Toast.makeText(context, "Judul materi tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                }
                                showPrePublishDialog = true
                            },
                            enabled = !state.isLoading && !state.isUploadingFile,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGlow,
                                contentColor = Color.White,
                                disabledContainerColor = EmeraldGlow.copy(alpha = 0.35f)
                            )
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Pratinjau & Terbitkan Materi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = if (isImeVisible) 0.dp else padding.calculateBottomPadding()
                )
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. SLEEK AI MATERIAL GENERATOR BAR ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(emeraldSoftBg)
                    .border(1.dp, emeraldSoftBorder, RoundedCornerShape(16.dp))
                    .clickable { showAiGenerateDialog = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldGlow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Susun Materi Otomatis (AI)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(EmeraldGlow)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Asisten Guru",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Sintesis infografis visual & artikel pembelajaran otomatis",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = emeraldAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            // ── 2. UNIFIED STUDIO WORKSPACE (SASARAN, FORMAT, DAN KONTEN AKTIF) ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = surfaceBg,
                border = BorderStroke(1.dp, surfaceBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // A. Sasaran Rombel & Mapel (BottomSheet Pickers — Zero Vanilla Dropdown!)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "SASARAN ROMBEL & MATA PELAJARAN",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = emeraldAccent,
                            letterSpacing = 0.6.sp
                        )

                        ExecutiveBottomSheetSelector(
                            value = selectedClass,
                            label = "Rombel / Kelas Target *",
                            icon = Icons.Default.Group,
                            options = classOptions,
                            onSelect = { selectedClass = it },
                            sheetTitle = "Pilih Rombel Target",
                            sheetSubtitle = "Modul pembelajaran akan dibagikan ke rombel ini",
                            accentColor = emeraldAccent
                        )

                        val isSubLocked = state.isSubjectLocked && subjectOptions.size <= 1
                        ExecutiveBottomSheetSelector(
                            value = selectedSubject,
                            label = "Mata Pelajaran *",
                            icon = Icons.Default.School,
                            options = subjectOptions,
                            onSelect = { selectedSubject = it },
                            sheetTitle = "Pilih Mata Pelajaran",
                            sheetSubtitle = "Pilih mata pelajaran yang Anda ampu",
                            accentColor = emeraldAccent,
                            isLocked = isSubLocked,
                            lockedBadgeText = "Guru Pengampu",
                            onLockedClick = {
                                Toast.makeText(context, "Mata pelajaran dikunci untuk guru pengampu: $selectedSubject", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    HorizontalDivider(color = surfaceBorder)

                    // B. Segmented Format Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "FORMAT MODUL PEMBELAJARAN",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextSecondary,
                            letterSpacing = 0.6.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(subSurfaceBg)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                Triple(MaterialType.DOCUMENT, "PDF / Buku", Icons.Default.PictureAsPdf),
                                Triple(MaterialType.VIDEO, "Video", Icons.Default.PlayCircle),
                                Triple(MaterialType.IMAGE, "Infografis", Icons.Default.Image),
                                Triple(MaterialType.ARTICLE, "Artikel", Icons.Default.Article)
                            ).forEach { (type, label, icon) ->
                                val isSelected = selectedType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (isSelected) surfaceBg else Color.Transparent)
                                        .border(
                                            width = if (isSelected) 1.dp else 0.dp,
                                            color = if (isSelected) emeraldSoftBorder else Color.Transparent,
                                            shape = RoundedCornerShape(9.dp)
                                        )
                                        .clickable {
                                            selectedType = type
                                            if (type != MaterialType.DOCUMENT) {
                                                selectedBook = null
                                            }
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = if (isSelected) emeraldAccent else TextTertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = label,
                                            color = if (isSelected) emeraldAccent else TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 10.5.sp,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = surfaceBorder)

                    // C. Active Format Workspace (Flat inside unified container — Zero Extra Outer Box!)
                    when (selectedType) {
                        MaterialType.DOCUMENT -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "SUMBER DOKUMEN MODUL",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextSecondary,
                                    letterSpacing = 0.6.sp
                                )

                                // Sub-mode Switcher: SIBI vs Upload Mandiri
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isSibiMode = pdfSourceMode == "SIBI"
                                    Surface(
                                        color = if (isSibiMode) emeraldSoftBg else subSurfaceBg,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(if (isSibiMode) 1.5.dp else 1.dp, if (isSibiMode) emeraldAccent else subSurfaceBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { pdfSourceMode = "SIBI" }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 11.dp, horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = if (isSibiMode) emeraldAccent else TextSecondary, modifier = Modifier.size(18.dp))
                                            Column {
                                                Text(
                                                    "Katalog Buku SIBI",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSibiMode) emeraldAccent else TextPrimary
                                                )
                                                Text(
                                                    "Kemdikdasmen Resmi",
                                                    fontSize = 9.5.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                    }

                                    val isUploadMode = pdfSourceMode == "UPLOAD"
                                    Surface(
                                        color = if (isUploadMode) TealAccent.copy(alpha = 0.14f) else subSurfaceBg,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(if (isUploadMode) 1.5.dp else 1.dp, if (isUploadMode) TealAccent else subSurfaceBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                pdfSourceMode = "UPLOAD"
                                                selectedBook = null
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 11.dp, horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = if (isUploadMode) TealAccent else TextSecondary, modifier = Modifier.size(18.dp))
                                            Column {
                                                Text(
                                                    "Upload PDF Mandiri",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isUploadMode) TealAccent else TextPrimary
                                                )
                                                Text(
                                                    "Dokumen Guru",
                                                    fontSize = 9.5.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }

                                if (pdfSourceMode == "SIBI") {
                                    if (selectedBook != null) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(subSurfaceBg)
                                                .border(1.dp, emeraldSoftBorder, RoundedCornerShape(14.dp))
                                                .padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(emeraldSoftBg),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = emeraldAccent, modifier = Modifier.size(20.dp))
                                                }
                                                Spacer(Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = selectedBook!!.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = TextPrimary,
                                                        maxLines = 2
                                                    )
                                                    Text(
                                                        text = "${selectedBook!!.publisher ?: "Kemendikdasmen"} • Total ${selectedBook!!.totalPages} Hal.",
                                                        fontSize = 10.5.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                                if (!selectedBook!!.fileUrl.isNullOrBlank()) {
                                                    OutlinedButton(
                                                        onClick = { previewingSelectedBook = true },
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(30.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = emeraldAccent),
                                                        border = BorderStroke(1.dp, emeraldSoftBorder)
                                                    ) {
                                                        Text("Baca", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(Modifier.width(4.dp))
                                                }
                                                IconButton(
                                                    onClick = { selectedBook = null },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, "Batal", tint = NeonError, modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = startPageInput,
                                                    onValueChange = {
                                                        startPageInput = it.filter { ch -> ch.isDigit() }
                                                        selectedBook?.let { b ->
                                                            applySelectedBook(b, startPageInput, endPageInput)
                                                        }
                                                    },
                                                    label = { Text("Hal. Mulai", fontSize = 10.5.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = emeraldAccent,
                                                        unfocusedBorderColor = subSurfaceBorder,
                                                        focusedTextColor = TextPrimary,
                                                        unfocusedTextColor = TextPrimary
                                                    )
                                                )
                                                OutlinedTextField(
                                                    value = endPageInput,
                                                    onValueChange = {
                                                        endPageInput = it.filter { ch -> ch.isDigit() }
                                                        selectedBook?.let { b ->
                                                            applySelectedBook(b, startPageInput, endPageInput)
                                                        }
                                                    },
                                                    label = { Text("Hal. Selesai", fontSize = 10.5.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = emeraldAccent,
                                                        unfocusedBorderColor = subSurfaceBorder,
                                                        focusedTextColor = TextPrimary,
                                                        unfocusedTextColor = TextPrimary
                                                    )
                                                )
                                            }
                                        }
                                    } else if (state.recommendedBooks.isNotEmpty()) {
                                        val primaryBook = state.recommendedBooks.first()
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(subSurfaceBg)
                                                .border(1.dp, subSurfaceBorder, RoundedCornerShape(14.dp))
                                                .padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.AutoStories, contentDescription = null, tint = emeraldAccent, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = "Rekomendasi Buku SIBI Sesuai Mapel",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp,
                                                    color = emeraldAccent
                                                )
                                            }
                                            Text(
                                                text = primaryBook.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "${primaryBook.publisher ?: "Kemendikdasmen"} • ${primaryBook.totalPages} Halaman",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                            Button(
                                                onClick = {
                                                    applySelectedBook(primaryBook, "1", minOf(20, primaryBook.totalPages).toString())
                                                },
                                                modifier = Modifier.fillMaxWidth().height(38.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
                                            ) {
                                                Text("Gunakan Buku SIBI Ini", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                            }
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { showBookCatalogSheet = true },
                                        modifier = Modifier.fillMaxWidth().height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = emeraldAccent),
                                        border = BorderStroke(1.dp, emeraldSoftBorder)
                                    ) {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (selectedBook != null) "Ganti Buku dari Katalog SIBI" else "Buka Katalog Buku SIBI (${state.availableBooks.size} Buku)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        if (state.isUploadingFile) {
                                            UploadingIndicator("Mengunggah dokumen PDF ke server...", emeraldAccent)
                                        } else if (!state.uploadedFileName.isNullOrBlank() || mediaUrl.isNotBlank()) {
                                            val displayName = state.uploadedFileName ?: mediaUrl.substringAfterLast("/")
                                            FileUploadedCard(
                                                fileName = displayName,
                                                statusText = "Berkas PDF siap diterbitkan",
                                                icon = Icons.Default.PictureAsPdf,
                                                iconTint = Color(0xFFEF4444),
                                                accentColor = NeonSuccess,
                                                onReplace = { documentPickerLauncher.launch("application/pdf") }
                                            )
                                        } else {
                                            FileDropzone(
                                                label = "Pilih Dokumen PDF dari Perangkat",
                                                hint = "Format PDF (Maks. 25 MB)",
                                                icon = Icons.Default.UploadFile,
                                                accentColor = TealAccent,
                                                onClick = { documentPickerLauncher.launch("application/pdf") }
                                            )
                                        }

                                        StudioTextField(
                                            value = title,
                                            onValueChange = { title = it },
                                            label = "Judul Modul PDF *",
                                            placeholder = "Contoh: Modul Ajar Bab 2: Hukum Newton",
                                            icon = Icons.Default.Title,
                                            accentColor = emeraldAccent
                                        )

                                        StudioTextField(
                                            value = description,
                                            onValueChange = { description = it },
                                            label = "Deskripsi & Panduan Belajar *",
                                            placeholder = "Tuliskan panduan atau ringkasan capaian materi untuk siswa...",
                                            icon = Icons.Default.Notes,
                                            accentColor = emeraldAccent,
                                            minHeight = 84.dp
                                        )
                                    }
                                }
                            }
                        }

                        MaterialType.VIDEO -> {
                            YoutubeSearchSection(
                                viewModel = viewModel,
                                state = state,
                                currentMediaUrl = mediaUrl,
                                currentTitle = title,
                                currentDescription = description,
                                defaultSubject = selectedSubject,
                                onVideoSelected = { video ->
                                    title = video.title
                                    description = video.description
                                    mediaUrl = "https://www.youtube.com/watch?v=${video.videoId}"
                                    isChangingYoutubeVideo = false
                                    Toast.makeText(context, "Video dipilih! Judul dan deskripsi otomatis terisi.", Toast.LENGTH_SHORT).show()
                                },
                                onPreviewVideo = { video ->
                                    previewingYoutubeVideo = video
                                },
                                onChangeVideo = {
                                    isChangingYoutubeVideo = true
                                }
                            )
                        }

                        MaterialType.IMAGE, MaterialType.INFOGRAPHIC -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                StudioTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = "Judul Infografis *",
                                    placeholder = "Contoh: Infografis Anatomi & Siklus Hidup Tumbuhan",
                                    icon = Icons.Default.Title,
                                    accentColor = emeraldAccent
                                )

                                val contentBlocks by viewModel.contentBlocks.collectAsState()

                                RichBlockEditorSection(
                                    blocks = contentBlocks,
                                    onAddBlock = viewModel::addBlock,
                                    onAddBlockWithContent = viewModel::addBlockWithContent,
                                    onUpdateBlock = viewModel::updateBlock,
                                    onRemoveBlock = viewModel::removeBlock,
                                    onMoveUp = viewModel::moveBlockUp,
                                    onMoveDown = viewModel::moveBlockDown,
                                    viewModel = viewModel,
                                    materialType = MaterialType.IMAGE,
                                    currentTitle = title,
                                    viewMode = infographicViewMode,
                                    onToggleViewMode = { infographicViewMode = if (infographicViewMode == "EDIT") "CANVAS" else "EDIT" }
                                )
                            }
                        }

                        MaterialType.ARTICLE -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                StudioTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = "Judul Artikel *",
                                    placeholder = "Contoh: Artikel: Sejarah & Dampak Revolusi Industri 4.0",
                                    icon = Icons.Default.Title,
                                    accentColor = emeraldAccent
                                )

                                val contentBlocks by viewModel.contentBlocks.collectAsState()

                                RichBlockEditorSection(
                                    blocks = contentBlocks,
                                    onAddBlock = viewModel::addBlock,
                                    onAddBlockWithContent = viewModel::addBlockWithContent,
                                    onUpdateBlock = viewModel::updateBlock,
                                    onRemoveBlock = viewModel::removeBlock,
                                    onMoveUp = viewModel::moveBlockUp,
                                    onMoveDown = viewModel::moveBlockDown,
                                    viewModel = viewModel,
                                    materialType = MaterialType.ARTICLE,
                                    currentTitle = title,
                                    viewMode = articleViewMode,
                                    onToggleViewMode = { articleViewMode = if (articleViewMode == "EDIT") "CANVAS" else "EDIT" }
                                )
                            }
                        }
                    }
                }
            }

            if (!state.error.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonError.copy(alpha = 0.12f))
                        .border(1.dp, NeonError.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = NeonError, modifier = Modifier.size(18.dp))
                        Text(state.error!!, color = NeonError, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showBookCatalogSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBookCatalogSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CosmicNavy,
        ) {
            LibraryBookCatalogSheetContent(
                books = state.availableBooks,
                isLoading = state.isLoadingBooks,
                onDismiss = { showBookCatalogSheet = false },
                onSelectBook = { book, startP, endP ->
                    selectedBook = book
                    startPageInput = startP
                    endPageInput = endP
                    val sInt = startP.toIntOrNull() ?: 1
                    val eInt = endP.toIntOrNull() ?: minOf(sInt + 10, book.totalPages)
                    title = "Materi Bacaan: ${book.title} (Hal. $sInt–$eInt)"
                    description = "Buku Teks: ${book.title} oleh ${book.author ?: book.publisher ?: "Kemendikdasmen"}. Silakan pelajari dan baca halaman $sInt sampai $eInt."
                    selectedType = MaterialType.DOCUMENT
                    mediaUrl = book.fileUrl ?: ""
                    if (!state.isSubjectLocked) {
                        book.subjectName?.let { subj ->
                            if (subjectOptions.any { it.equals(subj, ignoreCase = true) }) {
                                selectedSubject = subjectOptions.first { it.equals(subj, ignoreCase = true) }
                            }
                        }
                    }
                    showBookCatalogSheet = false
                    Toast.makeText(context, "Buku dipilih: ${book.title} (Hal. $sInt–$eInt)", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// ─── Shared Studio Components ──────────────────────────────────────────

@Composable
private fun SectionHeader(
    number: Int?,
    title: String,
    subtitle: String,
    accentColor: Color = EmeraldGlow
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (number != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CosmicSurface2)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    String.format(java.util.Locale.US, "%02d", number),
                    color = EmeraldGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun Chip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun StudioTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    accentColor: Color,
    minHeight: androidx.compose.ui.unit.Dp = 56.dp
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, fontSize = 12.sp, color = TextTertiary) },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = minHeight),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            unfocusedBorderColor = GlassBorder,
            focusedLabelColor = accentColor,
            focusedLeadingIconColor = accentColor,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            unfocusedLabelColor = TextTertiary
        )
    )
}

@Composable
private fun UploadingIndicator(message: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.08f))
            .border(1.5.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = accentColor, strokeWidth = 2.5.dp)
            Text(message, color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FileUploadedCard(
    fileName: String,
    statusText: String,
    icon: ImageVector,
    iconTint: Color,
    accentColor: Color,
    onReplace: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.07f))
            .border(1.5.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(fileName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            Text(statusText, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(
            onClick = onReplace,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue),
            border = BorderStroke(1.dp, NeonBlue),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Ganti", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FileDropzone(
    label: String,
    hint: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.05f))
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
            }
            Text(label, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
            Text(hint, color = TextTertiary, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun LibraryBookCatalogSheetContent(
    books: List<LibraryBook>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSelectBook: (book: LibraryBook, startPage: String, endPage: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedBookId by remember { mutableStateOf<String?>(null) }
    var tempStartPage by remember { mutableStateOf("1") }
    var tempEndPage by remember { mutableStateOf("10") }
    var previewingBook by remember { mutableStateOf<LibraryBook?>(null) }

    if (previewingBook != null && !previewingBook!!.fileUrl.isNullOrBlank()) {
        BookReaderDialog(
            title = previewingBook!!.title,
            pdfUrl = previewingBook!!.fileUrl ?: "",
            subject = previewingBook!!.subjectName ?: "Buku SIBI",
            startPage = tempStartPage.toIntOrNull(),
            endPage = tempEndPage.toIntOrNull(),
            onDismiss = { previewingBook = null }
        )
    }

    val filtered = remember(books, searchQuery) {
        if (searchQuery.isBlank()) books
        else books.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            (it.author ?: "").contains(searchQuery, ignoreCase = true) ||
            (it.publisher ?: "").contains(searchQuery, ignoreCase = true) ||
            (it.subjectName ?: "").contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Katalog Buku Kurikulum",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Pilih buku teks & rentang halaman yang akan dibaca siswa",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Tutup", tint = TextPrimary)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari judul buku, mapel, atau penerbit...", fontSize = 12.sp, color = TextTertiary) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = EmeraldGlow, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldGlow,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = EmeraldGlow
            )
        )

        Spacer(Modifier.height(14.dp))

        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldGlow)
            }
        } else if (filtered.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = EmeraldGlow, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Tidak ada buku ditemukan", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Coba cari dengan kata kunci lain", fontSize = 12.sp, color = TextTertiary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { book ->
                    val isExpanded = expandedBookId == book.id

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExpanded) CosmicSurface2 else CosmicNavy
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isExpanded) EmeraldGlow else GlassBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isExpanded) {
                                    expandedBookId = null
                                } else {
                                    expandedBookId = book.id
                                    tempStartPage = "1"
                                    tempEndPage = minOf(15, book.totalPages).toString()
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EmeraldGlow.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = EmeraldGlow, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = book.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${book.author ?: "Tim Penulis"} • ${book.publisher ?: "Kemendikdasmen"}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        val subj = book.subjectName
                                        if (subj != null) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = EmeraldGlow.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    subj,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldGlow,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = CosmicSurface2
                                        ) {
                                            Text(
                                                "${book.totalPages} Halaman",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextTertiary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (isExpanded) {
                                HorizontalDivider(color = GlassBorder)
                                if (!book.fileUrl.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = { previewingBook = book },
                                        modifier = Modifier.fillMaxWidth().height(38.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGlow),
                                        border = BorderStroke(1.dp, EmeraldGlow.copy(alpha = 0.6f))
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Buka & Baca Isi Buku SIBI Ini", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                Text(
                                    text = "Tentukan Halaman Bacaan Siswa:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = EmeraldGlow
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = tempStartPage,
                                        onValueChange = { tempStartPage = it.filter { c -> c.isDigit() } },
                                        label = { Text("Dari Hal.", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldGlow,
                                            unfocusedBorderColor = GlassBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedLabelColor = EmeraldGlow
                                        )
                                    )
                                    OutlinedTextField(
                                        value = tempEndPage,
                                        onValueChange = { tempEndPage = it.filter { c -> c.isDigit() } },
                                        label = { Text("Sampai Hal.", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldGlow,
                                            unfocusedBorderColor = GlassBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedLabelColor = EmeraldGlow
                                        )
                                    )
                                }
                                Button(
                                    onClick = {
                                        onSelectBook(book, tempStartPage, tempEndPage)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
                                ) {
                                    Text("Gunakan Buku & Rentang Halaman Ini", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MaterialReviewAndPublishScreen(
    title: String,
    description: String,
    selectedType: MaterialType,
    mediaUrl: String,
    contentBody: String,
    contentBlocks: List<ContentBlock> = emptyList(),
    selectedClass: String,
    selectedSubject: String,
    selectedBook: LibraryBook?,
    startPage: String,
    endPage: String,
    isLoading: Boolean,
    onBackToEdit: () -> Unit,
    onOpenBookPreview: () -> Unit = {},
    onConfirmPublish: (isScheduled: Boolean, releaseAtIso: String?) -> Unit,
) {
    androidx.activity.compose.BackHandler(onBack = onBackToEdit)
    val isDark = LocalIsDarkTheme.current
    val pageBg = if (isDark) Color(0xFF09090B) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val itemBg = if (isDark) Color(0xFF18181B) else Color(0xFFF1F5F9)
    val textPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569)
    val textMuted = if (isDark) Color(0xFF71717A) else Color(0xFF64748B)
    val emerald = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    val blue = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    var isScheduled by remember { mutableStateOf(false) }
    var scheduledDate by remember { mutableStateOf(java.time.LocalDate.now().plusDays(1)) }
    var scheduledTime by remember { mutableStateOf(java.time.LocalTime.of(7, 0)) }

    Scaffold(
        containerColor = pageBg,
        topBar = {
            ExecutiveTopBar(
                title = "Pratinjau Materi & Modul",
                subtitle = "Peninjauan Akhir Sebelum Resmi Diterbitkan",
                onBack = onBackToEdit
            )
        },
        bottomBar = {
            Surface(
                color = cardBg,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBackToEdit,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Koreksi / Edit", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val releaseAtIso = if (isScheduled) {
                                buildScheduledUtcIso(scheduledDate, scheduledTime)
                            } else null
                            onConfirmPublish(isScheduled, releaseAtIso)
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.4f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScheduled) Color(0xFFF59E0B) else emerald,
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isScheduled) "Menjadwalkan..." else "Menerbitkan...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                if (isScheduled) Icons.Default.Schedule else Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isScheduled) "Jadwalkan Materi" else "Terbitkan Sekarang",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── UNIFIED MASTER PREVIEW & SCHEDULE WORKSPACE ─────────────────────
            Surface(
                color = cardBg,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Metadata Pills Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = emerald.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = selectedClass,
                                    color = emerald,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = blue.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = selectedSubject,
                                    color = blue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Surface(
                            color = if (isScheduled) Color(0xFFF59E0B).copy(alpha = 0.14f) else emerald.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isScheduled) "Terjadwal" else "Siap Terbit",
                                color = if (isScheduled) Color(0xFFF59E0B) else emerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Format Sub-label + Title
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = when (selectedType) {
                                MaterialType.VIDEO -> "VIDEO PEMBELAJARAN INTERAKTIF"
                                MaterialType.DOCUMENT -> if (selectedBook != null) "BUKU TEKS KURIKULUM SIBI" else "MODUL DOKUMEN PDF"
                                MaterialType.IMAGE, MaterialType.INFOGRAPHIC -> "INFOGRAFIS & VISUAL"
                                MaterialType.ARTICLE -> "ARTIKEL & MODUL BACAAN"
                            },
                            color = textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = title.ifBlank { "(Tanpa Judul Materi)" },
                            color = textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 24.sp
                        )
                    }

                    // Live Interactive Media Preview
                    if (selectedType == MaterialType.VIDEO && mediaUrl.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black)
                                .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                        ) {
                            InAppVideoPlayer(
                                videoUrl = mediaUrl,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else if (selectedType == MaterialType.DOCUMENT && selectedBook != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(emerald.copy(alpha = 0.08f))
                                .border(1.dp, emerald.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(emerald.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = emerald, modifier = Modifier.size(22.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(selectedBook.title, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2)
                                Text(
                                    "Bacaan: Halaman $startPage – $endPage (Total ${selectedBook.totalPages} Hal)",
                                    color = emerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (!selectedBook.fileUrl.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = onOpenBookPreview,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    border = BorderStroke(1.dp, emerald.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = emerald)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Baca", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if ((selectedType == MaterialType.INFOGRAPHIC || selectedType == MaterialType.ARTICLE || selectedType == MaterialType.IMAGE) && contentBlocks.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(itemBg)
                                .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            contentBlocks.forEach { block ->
                                if (block.type == ContentBlockType.IMAGE && block.content.isNotBlank()) {
                                    SubcomposeAsyncImage(
                                        model = block.content,
                                        contentDescription = "Visual Materi",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 140.dp, max = 260.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (block.type == ContentBlockType.TEXT && block.content.isNotBlank()) {
                                    Text(
                                        text = block.content,
                                        color = textPrimary,
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    } else if (mediaUrl.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(itemBg)
                                .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = emerald, modifier = Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Berkas Dokumen Siap Diterbitkan", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(mediaUrl, color = textMuted, fontSize = 10.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                        }
                    }

                    // Clean Description / Learning Guide (no ugly gray inner box)
                    if (description.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Ringkasan & Petunjuk Belajar",
                                color = textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = description,
                                color = textSecondary,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    HorizontalDivider(color = cardBorder)

                    // Embedded Publishing Schedule Selector
                    PublishingScheduleSelector(
                        isScheduled = isScheduled,
                        onScheduleModeChange = { isScheduled = it },
                        scheduledDate = scheduledDate,
                        onDateChange = { scheduledDate = it },
                        scheduledTime = scheduledTime,
                        onTimeChange = { scheduledTime = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun YoutubePreviewDialog(
    video: YoutubeVideoResult,
    onDismiss: () -> Unit,
    onUseVideo: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = CosmicNavy,
            border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pratinjau Video YouTube", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(video.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                    }
                }

                // InApp Player for preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                ) {
                    InAppVideoPlayer(
                        videoUrl = "https://www.youtube.com/watch?v=${video.videoId}",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Text(video.channelTitle, color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                if (video.description.isNotBlank()) {
                    Text(video.description, color = TextSecondary, fontSize = 11.sp, maxLines = 3)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Tutup", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onUseVideo,
                        modifier = Modifier.weight(1.4f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gunakan Video Ini", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun YoutubeSearchSection(
    viewModel: MaterialCreatorViewModel,
    state: MaterialCreatorUiState,
    currentMediaUrl: String,
    currentTitle: String,
    currentDescription: String,
    defaultSubject: String,
    onVideoSelected: (YoutubeVideoResult) -> Unit,
    onPreviewVideo: (YoutubeVideoResult) -> Unit,
    onChangeVideo: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val isVideoReady = currentMediaUrl.isNotBlank() && currentMediaUrl.contains("youtube")

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(
            number = 3,
            title = "Integrasi Video YouTube",
            subtitle = "Cari video materi langsung dari YouTube tanpa perlu copy-paste link manual",
            accentColor = EmeraldGlow
        )

        if (isVideoReady) {
            // Selected Active Video Showcase Card
            Surface(
                color = CosmicSurface2,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, EmeraldGlow.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = EmeraldGlow, modifier = Modifier.size(16.dp))
                            Text(
                                "Video Terpilih (Judul & Deskripsi Terisi Otomatis)",
                                color = EmeraldGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = onChangeVideo,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Ganti Video", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // InApp Player
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black)
                    ) {
                        InAppVideoPlayer(
                            videoUrl = currentMediaUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = currentTitle.ifBlank { "Video Pembelajaran" },
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (currentDescription.isNotBlank()) {
                            Text(
                                text = currentDescription,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 3
                            )
                        }
                    }
                }
            }
        } else {
            // Search Bar & Results
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Cari video materi di YouTube...",
                            color = TextTertiary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGlow,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
                Button(
                    onClick = { if (searchQuery.isNotBlank()) viewModel.searchYoutube(searchQuery) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                    modifier = Modifier.height(54.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Cari", tint = Color.White)
                }
            }

            // Quick suggestion chips (Horizontally Scrollable)
            val quickChips = remember(defaultSubject) {
                listOfNotNull(
                    defaultSubject.takeIf { it.isNotBlank() },
                    "Kurikulum Merdeka",
                    "Eksperimen",
                    "Rangkuman Materi"
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickChips.forEach { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicSurface2)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                searchQuery = chip
                                viewModel.searchYoutube(chip)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = chip,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            if (state.isSearchingYoutube) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = NeonBlue, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("Mencari video pembelajaran di YouTube...", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            } else if (!state.youtubeSearchError.isNullOrBlank()) {
                Surface(
                    color = NeonError.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, NeonError.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        state.youtubeSearchError ?: "",
                        color = NeonError,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else if (state.youtubeSearchResults.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Hasil Pencarian YouTube (${state.youtubeSearchResults.size} Video):",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    state.youtubeSearchResults.forEach { video ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CosmicSurface2),
                            border = BorderStroke(0.5.dp, GlassBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(110.dp)
                                            .height(68.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black)
                                    ) {
                                        SubcomposeAsyncImage(
                                            model = video.thumbnailUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(24.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = video.title,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2
                                        )
                                        Text(
                                            text = video.channelTitle,
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onPreviewVideo(video) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue),
                                        border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.5f))
                                    ) {
                                        Icon(Icons.Default.Visibility, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Pratinjau", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { onVideoSelected(video) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.4f).height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Gunakan Video Ini", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RichBlockEditorSection(
    blocks: List<ContentBlock>,
    onAddBlock: (ContentBlockType) -> Unit,
    onAddBlockWithContent: (ContentBlockType, String) -> Unit,
    onUpdateBlock: (String, String) -> Unit,
    onRemoveBlock: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    viewModel: MaterialCreatorViewModel,
    materialType: MaterialType,
    currentTitle: String,
    viewMode: String,
    onToggleViewMode: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val blockScope = rememberCoroutineScope()
    var activeBlockId by remember { mutableStateOf<String?>(null) }
    var showUrlInputDialog by remember { mutableStateOf(false) }
    var inputImageUrl by remember { mutableStateOf("") }

    val localImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            blockScope.launch {
                try {
                    val mime = context.contentResolver.getType(it) ?: "image/jpeg"
                    val bytes = readMaterialUriBytes(context, it)
                    viewModel.uploadFile(bytes, "infografis_${System.currentTimeMillis()}.jpg", mime, onUploaded = { url ->
                        if (activeBlockId != null) {
                            onUpdateBlock(activeBlockId!!, url)
                            activeBlockId = null
                        } else {
                            onAddBlockWithContent(ContentBlockType.IMAGE, url)
                        }
                    })
                } catch (err: Exception) {
                    Toast.makeText(context, err.message ?: "Gagal membaca gambar.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (showUrlInputDialog) {
        Dialog(onDismissRequest = { showUrlInputDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CosmicNavy,
                border = BorderStroke(1.dp, EmeraldGlow.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tempel URL Gambar Infografis", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    OutlinedTextField(
                        value = inputImageUrl,
                        onValueChange = { inputImageUrl = it },
                        label = { Text("URL Gambar (https://...)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGlow,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text ?: ""
                                if (clip.isNotBlank()) inputImageUrl = clip
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Text("Tempel Clipboard", fontSize = 10.sp)
                        }
                        Button(
                            onClick = {
                                if (inputImageUrl.isNotBlank()) {
                                    if (activeBlockId != null) {
                                        onUpdateBlock(activeBlockId!!, inputImageUrl.trim())
                                        activeBlockId = null
                                    } else {
                                        onAddBlockWithContent(ContentBlockType.IMAGE, inputImageUrl.trim())
                                    }
                                    showUrlInputDialog = false
                                    inputImageUrl = ""
                                }
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
                        ) {
                            Text("Gunakan", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Mode Switcher Tab: Susun Blok vs Pratinjau Kanvas Dokumen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (materialType == MaterialType.IMAGE) "Komponen Infografis (${blocks.size} Blok)" else "Struktur Artikel (${blocks.size} Bagian)",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (viewMode == "CANVAS") EmeraldGlow.copy(alpha = 0.2f) else CosmicSurface2)
                    .border(0.5.dp, if (viewMode == "CANVAS") EmeraldGlow else GlassBorder, RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleViewMode)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        if (viewMode == "CANVAS") Icons.Default.Edit else Icons.Default.Visibility,
                        null,
                        tint = if (viewMode == "CANVAS") EmeraldGlow else TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        if (viewMode == "CANVAS") "Mode Susun / Edit" else "Pratinjau Kanvas Dokumen",
                        color = if (viewMode == "CANVAS") EmeraldGlow else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (viewMode == "CANVAS") {
            // ── LIVE DOCUMENT / CANVA CANVAS PREVIEW ──
            Surface(
                color = CosmicBlack,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, EmeraldGlow.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldGlow))
                        Text("Pratinjau Kanvas Tampilan Siswa", color = EmeraldGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = currentTitle.ifBlank { "Judul Materi" },
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    blocks.forEachIndexed { index, block ->
                        if (block.type == ContentBlockType.IMAGE && block.content.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CosmicNavy)
                            ) {
                                SubcomposeAsyncImage(
                                    model = block.content,
                                    contentDescription = "Infografis $index",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else if (block.type == ContentBlockType.TEXT && block.content.isNotBlank()) {
                            Surface(
                                color = CosmicNavy.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(0.5.dp, GlassBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = block.content,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // ── BLOCK BUILDER EDIT MODE ──
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                blocks.forEachIndexed { index, block ->
                    Card(
                        modifier = Modifier.fillMaxWidth().border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface2)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Block Card Header (Type badge, reorder, delete)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Chip(
                                        label = if (block.type == ContentBlockType.TEXT) "Blok #${index + 1} Teks" else "Blok #${index + 1} Gambar",
                                        color = if (block.type == ContentBlockType.TEXT) TealAccent else EmeraldGlow
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (index > 0) {
                                        IconButton(onClick = { onMoveUp(block.id) }, modifier = Modifier.size(26.dp)) {
                                            Icon(Icons.Default.ArrowUpward, "Naik", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    if (index < blocks.size - 1) {
                                        IconButton(onClick = { onMoveDown(block.id) }, modifier = Modifier.size(26.dp)) {
                                            Icon(Icons.Default.ArrowDownward, "Turun", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    IconButton(onClick = { onRemoveBlock(block.id) }, modifier = Modifier.size(26.dp)) {
                                        Icon(Icons.Default.Close, "Hapus Blok", tint = NeonError, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            if (block.type == ContentBlockType.TEXT) {
                                OutlinedTextField(
                                    value = block.content,
                                    onValueChange = { onUpdateBlock(block.id, it) },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                                    placeholder = { Text("Ketik atau tempel teks materi di sini...", color = TextTertiary, fontSize = 12.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldGlow,
                                        unfocusedBorderColor = GlassBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = {
                                            val clip = clipboardManager.getText()?.text ?: ""
                                            if (clip.isNotBlank()) {
                                                val combined = if (block.content.isBlank()) clip else "${block.content}\n$clip"
                                                onUpdateBlock(block.id, combined)
                                                Toast.makeText(context, "Teks ditempel dari clipboard!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, null, tint = EmeraldGlow, modifier = Modifier.size(12.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Tempel dari Clipboard", color = EmeraldGlow, fontSize = 10.sp)
                                    }
                                    if (block.content.isNotBlank()) {
                                        TextButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(block.content))
                                                Toast.makeText(context, "Teks disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Salin Teks", color = TextSecondary, fontSize = 10.sp)
                                        }
                                    }
                                }
                            } else {
                                // IMAGE BLOCK
                                if (block.content.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SubcomposeAsyncImage(
                                            model = block.content,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    activeBlockId = block.id
                                                    localImagePickerLauncher.launch("image/*")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Ganti Berkas", fontSize = 10.sp, color = Color.White)
                                            }
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                activeBlockId = block.id
                                                localImagePickerLauncher.launch("image/*")
                                            },
                                            modifier = Modifier.weight(1f).height(68.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, EmeraldGlow.copy(alpha = 0.5f))
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.UploadFile, null, tint = EmeraldGlow, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.height(2.dp))
                                                Text("Unggah dari Galeri", color = EmeraldGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                activeBlockId = block.id
                                                showUrlInputDialog = true
                                            },
                                            modifier = Modifier.weight(1f).height(68.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, GlassBorder)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Link, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.height(2.dp))
                                                Text("Tempel URL Gambar", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Add Block Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (materialType == MaterialType.IMAGE) {
                        Button(
                            onClick = {
                                activeBlockId = null
                                localImagePickerLauncher.launch("image/*")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicSurface2),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.5.dp, GlassBorder),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = EmeraldGlow, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tambah Gambar", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                activeBlockId = null
                                showUrlInputDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicSurface2),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.5.dp, GlassBorder),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tempel URL", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { onAddBlock(ContentBlockType.TEXT) },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicSurface2),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, GlassBorder),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = TealAccent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (materialType == MaterialType.ARTICLE) "Tambah Paragraf" else "Tambah Teks", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
