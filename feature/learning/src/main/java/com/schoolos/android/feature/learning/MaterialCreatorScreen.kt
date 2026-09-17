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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.LibraryBook
import com.schoolos.android.domain.model.MaterialType

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

    var selectedType by remember { mutableStateOf(MaterialType.DOCUMENT) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var contentBody by remember { mutableStateOf("") }
    var mediaUrl by remember { mutableStateOf("") }
    var showPreview by remember { mutableStateOf(false) }
    var showBookCatalogSheet by remember { mutableStateOf(false) }
    var selectedBook by remember { mutableStateOf<LibraryBook?>(null) }
    var startPageInput by remember { mutableStateOf("1") }
    var endPageInput by remember { mutableStateOf("10") }

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
            // Hero Header — flat bottom, emerald-teal gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EmeraldGradient)
                    .statusBarsPadding()
            ) {
                // Decorative circles
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .offset(x = 260.dp, y = (-30).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.07f))
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .offset(x = 290.dp, y = 20.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomBackButton(
                        onClick = onBack,
                        onHero = true,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Studio Materi Guru",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            "Publikasikan Modul Digital ke Kelas",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    // Badge icon
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = CosmicNavy,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showPreview = !showPreview },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGlow),
                        border = ButtonDefaults.outlinedButtonBorder().copy(
                            brush = Brush.linearGradient(listOf(EmeraldGlow, TealAccent))
                        )
                    ) {
                        Icon(
                            if (showPreview) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (showPreview) "Tutup Preview" else "Preview Siswa",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                Toast.makeText(context, "Judul materi tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val targetClassId = state.availableClasses.find { it.name == selectedClass }?.id ?: selectedClass
                            if (selectedBook != null) {
                                val startP = startPageInput.toIntOrNull() ?: 1
                                val endP = endPageInput.toIntOrNull() ?: minOf(startP + 10, selectedBook!!.totalPages)
                                val targetSubjectId = state.availableSubjects.find { it.name == selectedSubject }?.id
                                viewModel.assignReadingBook(
                                    book = selectedBook!!,
                                    startPage = startP,
                                    endPage = endP,
                                    instructions = description.trim().ifBlank { null },
                                    classId = targetClassId,
                                    subjectId = targetSubjectId
                                )
                                return@Button
                            }
                            if ((selectedType == MaterialType.DOCUMENT || selectedType == MaterialType.IMAGE) && mediaUrl.isBlank()) {
                                Toast.makeText(context, "Silakan pilih berkas dari perangkat terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val fullDesc = "$selectedSubject • $selectedClass • $description"
                            viewModel.createMaterial(
                                title = title.trim(),
                                description = fullDesc.trim(),
                                materialType = selectedType,
                                mediaUrl = mediaUrl.trim().ifBlank { null },
                                contentBody = if (selectedType == MaterialType.ARTICLE) contentBody.trim() else null,
                                subject = selectedSubject,
                                classId = targetClassId
                            )
                        },
                        enabled = title.isNotBlank() && !state.isLoading && !state.isUploadingFile,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.4f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Terbitkan Materi", fontWeight = FontWeight.Black, fontSize = 13.sp)
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // ── LIVE STUDENT FEED PREVIEW CARD ──
            AnimatedVisibility(visible = showPreview) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CosmicNavy),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(EmeraldGlow.copy(alpha = 0.08f), TealAccent.copy(alpha = 0.05f))))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                        modifier = Modifier.size(8.dp).clip(CircleShape)
                                            .background(NeonSuccess)
                                    )
                                    Text("Preview Modul di HP Siswa", color = NeonSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("In-App Reader", color = TextTertiary, fontSize = 10.sp)
                            }
                            Text(
                                text = title.ifBlank { "Judul Modul Materi Pembelajaran" },
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = description.ifBlank { "Deskripsi pengantar dan capaian kompetensi materi..." },
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Chip(label = selectedSubject, color = EmeraldGlow)
                                Chip(label = selectedType.name, color = TealAccent)
                            }
                        }
                    }
                }
            }

            // ── PERPUSTAKAAN GURU / KATALOG BUKU SELEKTOR ──
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📚", fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Katalog Buku Perpustakaan Guru",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (selectedBook != null)
                                    "Buku dipilih: ${selectedBook!!.title}"
                                else
                                    "Gunakan buku teks resmi Kurikulum Merdeka yang siap dibaca siswa",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (selectedBook != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedBook!!.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${selectedBook!!.publisher ?: "Kemendikbudristek"} • Total ${selectedBook!!.totalPages} Hal.",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { selectedBook = null },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, "Batal", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = startPageInput,
                                        onValueChange = { startPageInput = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Hal. Mulai", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    OutlinedTextField(
                                        value = endPageInput,
                                        onValueChange = { endPageInput = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Hal. Selesai", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showBookCatalogSheet = true },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (selectedBook != null) "Ganti Buku dari Katalog" else "Pilih Buku dari Katalog Perpustakaan (${state.availableBooks.size} Buku)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── SECTION 1: FORMAT MATERI ──
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(4.dp))
                SectionHeader(
                    number = null,
                    title = "Format Modul Pembelajaran",
                    subtitle = "Pilih jenis konten yang akan diunggah",
                    accentColor = EmeraldGlow
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        Triple(MaterialType.DOCUMENT, "PDF", Icons.Default.PictureAsPdf),
                        Triple(MaterialType.VIDEO, "Video", Icons.Default.PlayCircle),
                        Triple(MaterialType.IMAGE, "Infografis", Icons.Default.Image),
                        Triple(MaterialType.ARTICLE, "Artikel", Icons.Default.Article)
                    ).forEach { (type, label, icon) ->
                        val isSelected = selectedType == type
                        val bgAlpha by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0f,
                            animationSpec = tween(200), label = ""
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected)
                                        Brush.linearGradient(listOf(EmeraldGlow, TealAccent))
                                    else
                                        Brush.linearGradient(listOf(CosmicNavy, CosmicNavy))
                                )
                                .border(
                                    width = if (isSelected) 0.dp else 1.5.dp,
                                    color = if (isSelected) Color.Transparent else GlassBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedType = type }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    tint = if (isSelected) Color.White else EmeraldGlow,
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    label,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── SECTION 2: SASARAN ROMBEL & MAPEL ──
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicNavy),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionHeader(
                        number = null,
                        title = "Sasaran Rombel & Mapel",
                        subtitle = "Tentukan kelas dan mata pelajaran target",
                        accentColor = TealAccent
                    )

                    // Rombel Dropdown
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

                    // Mapel Dropdown
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

            Spacer(Modifier.height(4.dp))

            // ── SECTION 3: KONTEN & BERKAS ──
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicNavy),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionHeader(
                        number = null,
                        title = "Konten & Berkas Materi",
                        subtitle = "Isi detail dan lampirkan dokumen",
                        accentColor = EmeraldGlow
                    )

                    StudioTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Judul Materi / Modul *",
                        placeholder = "Contoh: Bab 1: Eksperimen Gaya & Gerak Sains",
                        icon = Icons.Default.Title,
                        accentColor = EmeraldGlow
                    )

                    StudioTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Deskripsi & Tujuan Pembelajaran",
                        placeholder = "Uraian singkat materi untuk panduan siswa...",
                        icon = Icons.Default.Notes,
                        accentColor = EmeraldGlow,
                        minHeight = 90.dp
                    )

                    when (selectedType) {
                        MaterialType.DOCUMENT -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Berkas Dokumen PDF",
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
                                        hint = "Sentuh di sini untuk memilih file PDF",
                                        icon = Icons.Default.UploadFile,
                                        accentColor = EmeraldGlow,
                                        onClick = { documentPickerLauncher.launch("application/pdf") }
                                    )
                                }
                            }
                        }
                        MaterialType.VIDEO -> {
                            StudioTextField(
                                value = mediaUrl,
                                onValueChange = { mediaUrl = it },
                                label = "Tautan Video (YouTube / MP4) *",
                                placeholder = "https://www.youtube.com/watch?v=...",
                                icon = Icons.Default.Link,
                                accentColor = EmeraldGlow
                            )
                        }
                        MaterialType.IMAGE -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Berkas Gambar / Infografis", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                if (state.isUploadingFile) {
                                    UploadingIndicator("Mengunggah berkas gambar ke server...", EmeraldGlow)
                                } else if (!state.uploadedFileName.isNullOrBlank() || mediaUrl.isNotBlank()) {
                                    val displayName = state.uploadedFileName ?: mediaUrl.substringAfterLast("/")
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FileUploadedCard(
                                            fileName = displayName,
                                            statusText = "✓ Gambar siap diterbitkan",
                                            icon = Icons.Default.Image,
                                            iconTint = TealAccent,
                                            accentColor = NeonSuccess,
                                            onReplace = { imagePickerLauncher.launch("image/*") }
                                        )
                                        if (mediaUrl.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(160.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(CosmicBlack)
                                            ) {
                                                SubcomposeAsyncImage(
                                                    model = mediaUrl,
                                                    contentDescription = "Preview",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    FileDropzone(
                                        label = "Pilih Gambar dari Galeri",
                                        hint = "Format: JPG, PNG, WEBP (maks. 25MB)",
                                        icon = Icons.Default.AddPhotoAlternate,
                                        accentColor = EmeraldGlow,
                                        onClick = { imagePickerLauncher.launch("image/*") }
                                    )
                                }
                            }
                        }
                        MaterialType.ARTICLE -> {
                            StudioTextField(
                                value = contentBody,
                                onValueChange = { contentBody = it },
                                label = "Isi Teks Artikel Pembelajaran *",
                                placeholder = "Tuliskan naskah materi lengkap di sini...",
                                icon = Icons.Default.Edit,
                                accentColor = EmeraldGlow,
                                minHeight = 160.dp
                            )
                        }
                    }
                }
            }

            if (!state.error.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
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

            Spacer(Modifier.height(30.dp))
        }
    }

    if (showBookCatalogSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBookCatalogSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
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
                    if (book.subjectName != null && subjectOptions.any { it.equals(book.subjectName, ignoreCase = true) }) {
                        selectedSubject = subjectOptions.first { it.equals(book.subjectName, ignoreCase = true) }
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
            .padding(20.dp),
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
            .padding(24.dp),
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
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .navigationBarsPadding()
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
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Pilih buku teks & rentang halaman yang akan dibaca siswa",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Tutup", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari judul buku, mapel, atau penerbit...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(14.dp))

        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (filtered.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📖", fontSize = 36.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Tidak ada buku ditemukan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Coba cari dengan kata kunci lain", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            containerColor = if (isExpanded)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isExpanded) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
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
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📕", fontSize = 22.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = book.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${book.author ?: "Tim Penulis"} • ${book.publisher ?: "Kemendikbudristek"}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        if (book.subjectName != null) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    book.subjectName,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                "${book.totalPages} Halaman",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (isExpanded) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                Text(
                                    text = "Tentukan Halaman Bacaan Siswa:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = tempStartPage,
                                        onValueChange = { tempStartPage = it.filter { c -> c.isDigit() } },
                                        label = { Text("Dari Hal.") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    OutlinedTextField(
                                        value = tempEndPage,
                                        onValueChange = { tempEndPage = it.filter { c -> c.isDigit() } },
                                        label = { Text("Sampai Hal.") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                                Button(
                                    onClick = {
                                        onSelectBook(book, tempStartPage, tempEndPage)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
