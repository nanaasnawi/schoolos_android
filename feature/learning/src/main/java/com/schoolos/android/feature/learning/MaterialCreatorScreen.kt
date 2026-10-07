package com.schoolos.android.feature.learning

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

private val EmeraldGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF059669), Color(0xFF0891B2), Color(0xFF1D4ED8))
)
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

    val classOptions = remember(state.availableClasses) {
        state.availableClasses.map { it.name }
    }
    val subjectOptions = remember(state.availableSubjects) {
        state.availableSubjects.map { it.name }
    }

    var selectedClass by remember { mutableStateOf(initialClass ?: "") }
    var selectedSubject by remember { mutableStateOf(initialSubject ?: "") }
    var isClassMenuExpanded by remember { mutableStateOf(false) }
    var isSubjectMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(classOptions) {
        if (selectedClass.isEmpty() && classOptions.isNotEmpty()) {
            selectedClass = classOptions.first()
        }
    }

    LaunchedEffect(subjectOptions) {
        if (selectedSubject.isEmpty() && subjectOptions.isNotEmpty()) {
            selectedSubject = subjectOptions.first()
        }
    }

    LaunchedEffect(selectedClass, selectedSubject, state.availableBooks) {
        viewModel.updateRecommendation(selectedClass, selectedSubject)
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

    val clipboardManager = LocalClipboardManager.current

    val applySelectedBook: (LibraryBook, String, String) -> Unit = { book, sPage, ePage ->
        selectedBook = book
        startPageInput = sPage
        endPageInput = ePage
        val sInt = sPage.toIntOrNull() ?: 1
        val eInt = ePage.toIntOrNull() ?: minOf(sInt + 10, book.totalPages)
        title = "Materi Bacaan: ${book.title} (Hal. $sInt–$eInt)"
        description = "Buku Teks Kurikulum SIBI: ${book.title}. Diterbitkan oleh ${book.author ?: book.publisher ?: "Kemdikdasmen"}. Pelajari materi pada halaman $sInt sampai $eInt."
        mediaUrl = book.fileUrl ?: ""
        book.subjectName?.let { subj ->
            if (subjectOptions.any { it.equals(subj, ignoreCase = true) }) {
                selectedSubject = subjectOptions.first { it.equals(subj, ignoreCase = true) }
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
                Toast.makeText(context, "✓ Video dipilih! Judul dan deskripsi otomatis terisi.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showPrePublishDialog) {
        val targetClassId = state.availableClasses.find { it.name == selectedClass }?.id ?: selectedClass
        val targetSubjectId = state.availableSubjects.find { it.name == selectedSubject }?.id
        val startP = startPageInput.toIntOrNull() ?: 1
        val endP = endPageInput.toIntOrNull() ?: minOf(startP + 10, selectedBook?.totalPages ?: 100)

        PrePublishMaterialReviewDialog(
            title = title.trim(),
            description = description.trim(),
            selectedType = selectedType,
            mediaUrl = mediaUrl.trim(),
            contentBody = contentBody.trim(),
            selectedClass = selectedClass,
            selectedSubject = selectedSubject,
            selectedBook = selectedBook,
            startPage = startP.toString(),
            endPage = endP.toString(),
            isLoading = state.isLoading,
            onDismiss = { showPrePublishDialog = false },
            onConfirmPublish = {
                if (selectedType == MaterialType.DOCUMENT && pdfSourceMode == "SIBI" && selectedBook != null) {
                    viewModel.assignReadingBook(
                        book = selectedBook!!,
                        startPage = startP,
                        endPage = endP,
                        instructions = description.trim().ifBlank { null },
                        classId = targetClassId,
                        subjectId = targetSubjectId
                    )
                } else {
                    val fullDesc = "$selectedSubject • $selectedClass • $description"
                    viewModel.createMaterial(
                        title = title.trim(),
                        description = fullDesc.trim(),
                        materialType = selectedType,
                        mediaUrl = mediaUrl.trim().ifBlank { null },
                        subject = selectedSubject,
                        classId = targetClassId
                    )
                }
            }
        )
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val rawName = queryFileName(context, it) ?: "modul_ajar_${System.currentTimeMillis()}.pdf"
            val fileName = if (!rawName.contains(".")) "$rawName.pdf" else rawName
            val bytes = context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
            if (bytes != null) {
                viewModel.uploadFile(bytes, fileName, "application/pdf")
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val mime = context.contentResolver.getType(it) ?: "image/jpeg"
            val ext = when (mime) {
                "image/png" -> ".png"
                "image/webp" -> ".webp"
                "image/gif" -> ".gif"
                else -> ".jpg"
            }
            val rawName = queryFileName(context, it) ?: "gambar_materi_${System.currentTimeMillis()}$ext"
            val fileName = if (!rawName.contains(".")) "$rawName$ext" else rawName
            val bytes = context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
            if (bytes != null) {
                viewModel.uploadFile(bytes, fileName, mime)
            }
        }
    }

    LaunchedEffect(state.uploadedFileUrl) {
        state.uploadedFileUrl?.let { mediaUrl = it }
    }

    LaunchedEffect(state.success) {
        if (state.success) {
            Toast.makeText(context, "✓ Materi ajar berhasil diterbitkan ke siswa!", Toast.LENGTH_SHORT).show()
            onSuccess()
            viewModel.resetState()
        }
    }
    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Studio Materi Guru",
                subtitle = "Publikasikan Modul Digital ke Kelas",
                onBack = onBack,
                actions = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (showPreview) EmeraldGlow.copy(alpha = 0.2f) else CosmicSurface2)
                            .border(
                                0.5.dp,
                                if (showPreview) EmeraldGlow else GlassBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { showPreview = !showPreview }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (showPreview) Icons.Default.Edit else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (showPreview) EmeraldGlow else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                if (showPreview) "Tutup" else "Preview",
                                color = if (showPreview) EmeraldGlow else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = CosmicNavy,
                tonalElevation = 10.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .border(1.dp, GlassBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showPreview = !showPreview },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGlow),
                        border = BorderStroke(1.dp, EmeraldGlow.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            if (showPreview) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (showPreview) "Tutup Preview" else "Preview",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGlow,
                            disabledContainerColor = EmeraldGlow.copy(alpha = 0.35f)
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Pratinjau & Terbitkan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── LIVE STUDENT FEED PREVIEW CARD ──
            AnimatedVisibility(visible = showPreview) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CosmicNavy)
                        .border(1.dp, EmeraldGlow.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(NeonSuccess)
                                )
                                Text("Preview Modul di HP Siswa", color = NeonSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("In-App Reader", color = TextTertiary, fontSize = 10.sp)
                        }
                        Text(
                            text = title.ifBlank { "Judul Modul Materi Pembelajaran" },
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = description.ifBlank { "Deskripsi pengantar dan capaian kompetensi materi..." },
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 3
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Chip(label = selectedSubject.ifBlank { "Mata Pelajaran" }, color = EmeraldGlow)
                            Chip(label = selectedType.name, color = TealAccent)
                            if (selectedClass.isNotBlank()) {
                                Chip(label = selectedClass, color = NeonBlue)
                            }
                        }
                    }
                }
            }

            // ── SECTION 1: SASARAN ROMBEL & MAPEL ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicNavy)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionHeader(
                        number = 1,
                        title = "Sasaran Rombel & Mapel",
                        subtitle = "Tentukan kelas dan mata pelajaran target penerima modul",
                        accentColor = EmeraldGlow
                    )

                    ModernDropdown(
                        value = selectedClass,
                        label = "Rombel Target",
                        icon = Icons.Default.Group,
                        expanded = isClassMenuExpanded,
                        accentColor = EmeraldGlow,
                        onExpand = { isClassMenuExpanded = true },
                        onDismiss = { isClassMenuExpanded = false },
                        options = classOptions,
                        onSelect = { selectedClass = it }
                    )

                    ModernDropdown(
                        value = selectedSubject,
                        label = "Mata Pelajaran",
                        icon = Icons.Default.School,
                        expanded = isSubjectMenuExpanded,
                        accentColor = EmeraldGlow,
                        onExpand = { isSubjectMenuExpanded = true },
                        onDismiss = { isSubjectMenuExpanded = false },
                        options = subjectOptions,
                        onSelect = { selectedSubject = it }
                    )
                }
            }

            // ── SECTION 2: FORMAT MODUL PEMBELAJARAN ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicNavy)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionHeader(
                        number = 2,
                        title = "Format Modul Pembelajaran",
                        subtitle = "Pilih jenis konten media yang akan diunggah",
                        accentColor = TealAccent
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(MaterialType.DOCUMENT, "PDF", Icons.Default.PictureAsPdf),
                            Triple(MaterialType.VIDEO, "Video", Icons.Default.PlayCircle),
                            Triple(MaterialType.IMAGE, "Infografis", Icons.Default.Image),
                            Triple(MaterialType.ARTICLE, "Artikel", Icons.Default.Article)
                        ).forEach { (type, label, icon) ->
                            val isSelected = selectedType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) EmeraldGlow.copy(alpha = 0.15f) else CosmicSurface2
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) EmeraldGlow else GlassBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedType = type
                                        if (type != MaterialType.DOCUMENT) {
                                            selectedBook = null
                                        }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = label,
                                        tint = if (isSelected) EmeraldGlow else TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        label,
                                        color = if (isSelected) EmeraldGlow else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── SECTION 3: KONTEN SESUAI FORMAT MATERI (DYNAMIC & CONDITIONALLY FILTERED) ──
            when (selectedType) {
                MaterialType.DOCUMENT -> {
                    // ── 1. MODUL DOKUMEN / PDF ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CosmicNavy)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SectionHeader(
                                number = 3,
                                title = "Sumber Modul Dokumen (PDF)",
                                subtitle = "Pilih antara Buku Teks Kurikulum SIBI atau Unggah Berkas PDF Mandiri",
                                accentColor = EmeraldGlow
                            )

                            // Sub-mode Switcher: SIBI vs Upload Mandiri
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isSibiMode = pdfSourceMode == "SIBI"
                                Surface(
                                    color = if (isSibiMode) EmeraldGlow.copy(alpha = 0.18f) else CosmicSurface2,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(if (isSibiMode) 1.5.dp else 0.5.dp, if (isSibiMode) EmeraldGlow else GlassBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            pdfSourceMode = "SIBI"
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("📚", fontSize = 16.sp)
                                        Column {
                                            Text(
                                                "Katalog Buku SIBI",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSibiMode) EmeraldGlow else TextPrimary
                                            )
                                            Text(
                                                "Kemdikdasmen Resmi",
                                                fontSize = 9.sp,
                                                color = if (isSibiMode) EmeraldGlow.copy(alpha = 0.8f) else TextSecondary
                                            )
                                        }
                                    }
                                }

                                val isUploadMode = pdfSourceMode == "UPLOAD"
                                Surface(
                                    color = if (isUploadMode) TealAccent.copy(alpha = 0.18f) else CosmicSurface2,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(if (isUploadMode) 1.5.dp else 0.5.dp, if (isUploadMode) TealAccent else GlassBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            pdfSourceMode = "UPLOAD"
                                            selectedBook = null
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("📤", fontSize = 16.sp)
                                        Column {
                                            Text(
                                                "Upload Berkas PDF",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isUploadMode) TealAccent else TextPrimary
                                            )
                                            Text(
                                                "Dokumen Guru Mandiri",
                                                fontSize = 9.sp,
                                                color = if (isUploadMode) TealAccent.copy(alpha = 0.8f) else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            if (pdfSourceMode == "SIBI") {
                                // ── SIBI BOOK CATALOG MODE ──
                                // Upload Berkas PDF HIDDEN. Judul & Deskripsi terisi otomatis.
                                if (selectedBook != null) {
                                    Surface(
                                        color = CosmicSurface2,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, EmeraldGlow.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(EmeraldGlow.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("📚", fontSize = 18.sp)
                                                }
                                                Spacer(Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = selectedBook!!.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = EmeraldGlow,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = "${selectedBook!!.publisher ?: "Kemendikbudristek"} • Total ${selectedBook!!.totalPages} Hal.",
                                                        fontSize = 10.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                                if (!selectedBook!!.fileUrl.isNullOrBlank()) {
                                                    OutlinedButton(
                                                        onClick = { previewingSelectedBook = true },
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGlow),
                                                        border = BorderStroke(1.dp, EmeraldGlow.copy(alpha = 0.6f))
                                                    ) {
                                                        Icon(Icons.Default.Visibility, null, modifier = Modifier.size(12.dp))
                                                        Spacer(Modifier.width(4.dp))
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
                                                    label = { Text("Hal. Mulai", fontSize = 10.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = EmeraldGlow,
                                                        unfocusedBorderColor = GlassBorder,
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
                                                    label = { Text("Hal. Selesai", fontSize = 10.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = EmeraldGlow,
                                                        unfocusedBorderColor = GlassBorder,
                                                        focusedTextColor = TextPrimary,
                                                        unfocusedTextColor = TextPrimary
                                                    )
                                                )
                                            }

                                            // Auto-fill confirmation card
                                            Surface(
                                                color = EmeraldGlow.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(0.5.dp, EmeraldGlow.copy(alpha = 0.3f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, null, tint = EmeraldGlow, modifier = Modifier.size(14.dp))
                                                    Text(
                                                        "Judul & Deskripsi terisi otomatis dari buku SIBI. Tidak perlu upload PDF manual.",
                                                        color = EmeraldGlow,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else if (state.recommendedBooks.isNotEmpty()) {
                                    val primaryBook = state.recommendedBooks.first()
                                    val otherBooks = state.recommendedBooks.drop(1).take(3)

                                    Surface(
                                        color = CosmicSurface2,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(0.5.dp, GlassBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("💡", fontSize = 15.sp)
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = "Rekomendasi Buku SIBI Resmi:",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = EmeraldGlow
                                                )
                                            }
                                            Text(
                                                text = primaryBook.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "${primaryBook.publisher ?: "Kemendikbudristek"} • ${primaryBook.totalPages} Halaman",
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                            Button(
                                                onClick = {
                                                    applySelectedBook(primaryBook, "1", minOf(20, primaryBook.totalPages).toString())
                                                },
                                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
                                            ) {
                                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Gunakan Buku SIBI Ini", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }

                                            if (otherBooks.isNotEmpty()) {
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = "Pilihan Terkait Lainnya:",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextTertiary
                                                )
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    otherBooks.forEach { altBook ->
                                                        Surface(
                                                            color = CosmicNavy,
                                                            shape = RoundedCornerShape(8.dp),
                                                            border = BorderStroke(0.5.dp, GlassBorder),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    applySelectedBook(altBook, "1", minOf(20, altBook.totalPages).toString())
                                                                }
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = altBook.title,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    modifier = Modifier.weight(1f),
                                                                    maxLines = 1,
                                                                    color = TextPrimary
                                                                )
                                                                Text("Pilih →", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldGlow)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { showBookCatalogSheet = true },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGlow),
                                    border = BorderStroke(0.5.dp, EmeraldGlow.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedBook != null) "Ganti Buku dari Katalog SIBI" else "Jelajahi Katalog Lengkap (${state.availableBooks.size} Buku)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                // ── UPLOAD BERKAS PDF MANDIRI MODE ──
                                // Katalog Buku SIBI HIDDEN! Tampilkan upload PDF & input manual judul & deskripsi
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "Unggah Berkas PDF Mandiri",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    if (state.isUploadingFile) {
                                        UploadingIndicator("Mengunggah dokumen PDF ke server...", EmeraldGlow)
                                    } else if (!state.uploadedFileName.isNullOrBlank() || mediaUrl.isNotBlank()) {
                                        val displayName = state.uploadedFileName ?: mediaUrl.substringAfterLast("/")
                                        FileUploadedCard(
                                            fileName = displayName,
                                            statusText = "✓ Berkas PDF siap diterbitkan",
                                            icon = Icons.Default.PictureAsPdf,
                                            iconTint = Color(0xFFEF4444),
                                            accentColor = NeonSuccess,
                                            onReplace = { documentPickerLauncher.launch("application/pdf") }
                                        )
                                    } else {
                                        FileDropzone(
                                            label = "Pilih Dokumen PDF dari Perangkat",
                                            hint = "Format PDF (Modul Ajar, Lembar Kerja, Handout)",
                                            icon = Icons.Default.UploadFile,
                                            accentColor = TealAccent,
                                            onClick = { documentPickerLauncher.launch("application/pdf") }
                                        )
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    StudioTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        label = "Judul Modul PDF *",
                                        placeholder = "Contoh: Modul Ajar Bab 2: Hukum Newton & Penerapannya",
                                        icon = Icons.Default.Title,
                                        accentColor = TealAccent
                                    )

                                    StudioTextField(
                                        value = description,
                                        onValueChange = { description = it },
                                        label = "Deskripsi & Panduan Belajar *",
                                        placeholder = "Tuliskan panduan atau ringkasan capaian materi untuk siswa...",
                                        icon = Icons.Default.Notes,
                                        accentColor = TealAccent,
                                        minHeight = 90.dp
                                    )
                                }
                            }
                        }
                    }
                }

                MaterialType.VIDEO -> {
                    // ── 2. VIDEO YOUTUBE (KATALOG BUKU TIDAK DITAMPILKAN!) ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CosmicNavy)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
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
                                Toast.makeText(context, "✓ Video dipilih! Judul dan deskripsi otomatis terisi.", Toast.LENGTH_SHORT).show()
                            },
                            onPreviewVideo = { video ->
                                previewingYoutubeVideo = video
                            },
                            onChangeVideo = {
                                isChangingYoutubeVideo = true
                            }
                        )
                    }
                }

                MaterialType.IMAGE -> {
                    // ── 3. INFOGRAFIS (KATALOG BUKU TIDAK DITAMPILKAN!) ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CosmicNavy)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SectionHeader(
                                number = 3,
                                title = "Studio Infografis Interaktif",
                                subtitle = "Susun infografis dengan gambar dan teks yang dapat dipratinjau secara langsung",
                                accentColor = EmeraldGlow
                            )

                            StudioTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = "Judul Infografis *",
                                placeholder = "Contoh: Infografis Anatomi & Siklus Hidup Tumbuhan",
                                icon = Icons.Default.Title,
                                accentColor = EmeraldGlow
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
                }

                MaterialType.ARTICLE -> {
                    // ── 4. ARTIKEL (KATALOG BUKU TIDAK DITAMPILKAN!) ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CosmicNavy)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SectionHeader(
                                number = 3,
                                title = "Studio Artikel Pembelajaran",
                                subtitle = "Tulis materi artikel dengan paragraf terstruktur dan opsi salin-tempel teks",
                                accentColor = TealAccent
                            )

                            StudioTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = "Judul Artikel *",
                                placeholder = "Contoh: Artikel: Sejarah & Dampak Revolusi Industri 4.0",
                                icon = Icons.Default.Title,
                                accentColor = TealAccent
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

            Spacer(Modifier.height(40.dp))
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
                    description = "Buku Teks: ${book.title} oleh ${book.author ?: book.publisher ?: "Kemendikbudristek"}. Silakan pelajari dan baca halaman $sInt sampai $eInt."
                    selectedType = MaterialType.DOCUMENT
                    mediaUrl = book.fileUrl ?: ""
                    book.subjectName?.let { subj ->
                        if (subjectOptions.any { it.equals(subj, ignoreCase = true) }) {
                            selectedSubject = subjectOptions.first { it.equals(subj, ignoreCase = true) }
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
    accentColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (number != null) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(number.toString(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        } else {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.linearGradient(listOf(accentColor, accentColor.copy(alpha = 0.3f))))
            )
        }
        Column {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = TextTertiary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun Chip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernDropdown(
    value: String,
    label: String,
    icon: ImageVector,
    expanded: Boolean,
    accentColor: Color,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (!expanded) onExpand() else onDismiss() }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
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
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.background(CosmicNavy)
        ) {
            options.forEach { option ->
                val isSelected = option == value
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            color = if (isSelected) accentColor else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = { onSelect(option); onDismiss() },
                    leadingIcon = if (isSelected) ({
                        Icon(Icons.Default.Check, null, tint = accentColor, modifier = Modifier.size(16.dp))
                    }) else null
                )
            }
        }
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
            border = ButtonDefaults.outlinedButtonBorder().copy(
                brush = Brush.linearGradient(listOf(NeonBlue, NeonBlue))
            ),
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
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.05f))
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(listOf(accentColor.copy(alpha = 0.5f), accentColor.copy(alpha = 0.2f))),
                shape = RoundedCornerShape(16.dp)
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
                    Text("📖", fontSize = 36.sp)
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
                                    Text("📕", fontSize = 20.sp)
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
                                        text = "${book.author ?: "Tim Penulis"} • ${book.publisher ?: "Kemendikbudristek"}",
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
private fun PrePublishMaterialReviewDialog(
    title: String,
    description: String,
    selectedType: MaterialType,
    mediaUrl: String,
    contentBody: String,
    selectedClass: String,
    selectedSubject: String,
    selectedBook: LibraryBook?,
    startPage: String,
    endPage: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirmPublish: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = CosmicNavy,
            border = BorderStroke(1.dp, EmeraldGlow.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldGlow.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = EmeraldGlow, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Pratinjau Tampilan Siswa", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text("Realtime tampilan modul sebelum diterbitkan", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = onDismiss, enabled = !isLoading) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextPrimary)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Scrollable Simulated Student View
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Target Rombel & Subject Banner
                    Surface(
                        color = CosmicSurface2,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(0.5.dp, GlassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎯", fontSize = 18.sp)
                            Column {
                                Text("Akan Diterbitkan Untuk:", fontSize = 10.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Kelas $selectedClass • $selectedSubject",
                                    color = EmeraldGlow,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Student Material Card Simulator
                    Surface(
                        color = CosmicBlack,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Badge Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = EmeraldGlow.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = when (selectedType) {
                                            MaterialType.VIDEO -> "VIDEO PEMBELAJARAN"
                                            MaterialType.DOCUMENT -> if (selectedBook != null) "BUKU SIBI KEMENDIKBUD" else "MODUL PDF"
                                            MaterialType.IMAGE -> "INFOGRAFIS / GAMBAR"
                                            MaterialType.ARTICLE -> "ARTIKEL BACAAN"
                                        },
                                        color = EmeraldGlow,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text("Status: Segera Terbit", color = NeonSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            // Title & Description
                            Text(
                                text = title.ifBlank { "(Tanpa Judul)" },
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp
                            )

                            if (description.isNotBlank()) {
                                Text(
                                    text = description,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }

                            HorizontalDivider(color = GlassBorder)

                            // Media preview
                            if (selectedBook != null) {
                                Surface(
                                    color = CosmicSurface2,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(0.5.dp, EmeraldGlow.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("📕", fontSize = 24.sp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(selectedBook.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(
                                                "Siswa membaca Hal. $startPage s/d $endPage",
                                                color = EmeraldGlow,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            } else if (selectedType == MaterialType.VIDEO && mediaUrl.isNotBlank()) {
                                Surface(
                                    color = CosmicSurface2,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(0.5.dp, GlassBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("▶️", fontSize = 16.sp)
                                            Text("Tautan Video Pembelajaran:", fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
                                        }
                                        Text(mediaUrl, color = NeonBlue, fontSize = 11.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    }
                                }
                            } else if (selectedType == MaterialType.ARTICLE && contentBody.isNotBlank()) {
                                Surface(
                                    color = CosmicSurface2,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        contentBody,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else if (mediaUrl.isNotBlank()) {
                                Surface(
                                    color = CosmicSurface2,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("📎", fontSize = 18.sp)
                                        Text("Berkas Lampiran Digital Tersedia", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Koreksi / Edit", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onConfirmPublish,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Terbitkan Sekarang", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }
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
            number = null,
            title = "Integrasi Video YouTube",
            subtitle = "Cari video materi langsung dari YouTube tanpa perlu copy-paste link manual",
            accentColor = NeonBlue
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
                    placeholder = { Text("Cari materi di YouTube (Mis: $defaultSubject)", color = TextTertiary, fontSize = 12.sp) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
                Button(
                    onClick = { if (searchQuery.isNotBlank()) viewModel.searchYoutube(searchQuery) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Cari", tint = Color.White)
                }
            }

            // Quick suggestion chips
            val quickChips = remember(defaultSubject) {
                listOfNotNull(
                    defaultSubject.takeIf { it.isNotBlank() },
                    "Kurikulum Merdeka",
                    "Eksperimen",
                    "Rangkuman Materi"
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(chip, color = TextSecondary, fontSize = 10.sp)
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
    var activeBlockId by remember { mutableStateOf<String?>(null) }
    var showUrlInputDialog by remember { mutableStateOf(false) }
    var inputImageUrl by remember { mutableStateOf("") }

    val localImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val mime = context.contentResolver.getType(it) ?: "image/jpeg"
            val bytes = context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
            if (bytes != null) {
                viewModel.uploadFile(bytes, "infografis_${System.currentTimeMillis()}.jpg", mime, onUploaded = { url ->
                    if (activeBlockId != null) {
                        onUpdateBlock(activeBlockId!!, url)
                        activeBlockId = null
                    } else {
                        onAddBlockWithContent(ContentBlockType.IMAGE, url)
                    }
                })
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
