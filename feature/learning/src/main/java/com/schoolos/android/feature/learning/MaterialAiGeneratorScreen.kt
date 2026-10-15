package com.schoolos.android.feature.learning

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.AiGenerationLoadingOverlay
import com.schoolos.android.core.designsystem.AiGenerationType
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AcademicSubject
import com.schoolos.android.domain.model.GeneratedMaterialResult
import com.schoolos.android.domain.model.UnifiedAcademicClass
import com.schoolos.android.domain.model.groupParallelClasses
import com.schoolos.android.domain.model.findClass
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private enum class MaterialGenStep {
    CONFIGURE,
    GENERATING,
    VERIFY
}

private val EmeraldPrimary = Color(0xFF16A34A)
private val CyanPrimary = Color(0xFF0284C7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialAiGeneratorScreen(
    availableSubjects: List<AcademicSubject>,
    availableClasses: List<AcademicClass>,
    currentSubjectName: String,
    currentClassName: String,
    isSubjectLocked: Boolean = false,
    onBack: () -> Unit,
    onApply: (GeneratedMaterialResult) -> Unit,
    onGenerateRequest: suspend (
        mode: String,
        subjectName: String,
        topic: String,
        gradeLevel: String,
    ) -> Result<GeneratedMaterialResult>,
) {
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val primaryHighlightBg = if (isDark) colorScheme.primaryContainer.copy(alpha = 0.35f) else Color(0xFFF0FDF4)
    val secondaryHighlightBg = if (isDark) colorScheme.secondaryContainer.copy(alpha = 0.35f) else Color(0xFFF0F9FF)

    var currentStep by remember { mutableStateOf(MaterialGenStep.CONFIGURE) }
    var generatedResult by remember { mutableStateOf<GeneratedMaterialResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Configuration States
    var selectedMode by remember { mutableStateOf("INFOGRAPHIC") } // "INFOGRAPHIC" | "ARTICLE"
    var customTopic by remember { mutableStateOf("") }

    var selectedSubject by remember(currentSubjectName, availableSubjects) {
        val matched = availableSubjects.find { it.name.equals(currentSubjectName, ignoreCase = true) }
        mutableStateOf(matched ?: availableSubjects.firstOrNull())
    }

    val unifiedClasses = remember(availableClasses) {
        availableClasses.groupParallelClasses()
    }

    var selectedClass by remember(currentClassName, unifiedClasses) {
        val matched = unifiedClasses.findClass(currentClassName)
        mutableStateOf(matched ?: unifiedClasses.firstOrNull())
    }

    var isSubjectDropdownOpen by remember { mutableStateOf(false) }
    var isClassDropdownOpen by remember { mutableStateOf(false) }

    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var currentGenJob by remember { mutableStateOf<Job?>(null) }

    // BackHandler: Double back press confirmation during AI generation
    BackHandler(enabled = currentStep == MaterialGenStep.GENERATING) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000L) {
            currentGenJob?.cancel()
            currentStep = MaterialGenStep.CONFIGURE
            Toast.makeText(context, "Proses AI dibatalkan", Toast.LENGTH_SHORT).show()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, "Tekan sekali lagi untuk membatalkan proses AI", Toast.LENGTH_SHORT).show()
        }
    }

    val handleStartGeneration = {
        val subject = selectedSubject
        val topic = customTopic.trim()
        if (subject == null && currentSubjectName.isBlank()) {
            errorMessage = "Silakan pilih mata pelajaran terlebih dahulu."
        } else if (topic.isBlank()) {
            errorMessage = "Silakan tuliskan topik atau konsep materi yang ingin disusun."
        } else {
            errorMessage = null
            currentStep = MaterialGenStep.GENERATING

            val effectiveSubName = subject?.name ?: currentSubjectName
            val effectiveGrade = selectedClass?.memberClassNames?.firstOrNull() ?: selectedClass?.displayName ?: currentClassName

            currentGenJob = scope.launch {
                val res = onGenerateRequest(
                    selectedMode,
                    effectiveSubName,
                    topic,
                    effectiveGrade
                )
                res.onSuccess { result ->
                    generatedResult = result
                    currentStep = MaterialGenStep.VERIFY
                }.onFailure { err ->
                    if (err !is kotlinx.coroutines.CancellationException) {
                        errorMessage = err.message ?: "Gagal menyusun materi pembelajaran dengan AI"
                        currentStep = MaterialGenStep.CONFIGURE
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentStep) {
            MaterialGenStep.CONFIGURE -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        "Studio AI Materi & Modul Ajar",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        "Kurikulum Merdeka • Sintesis Materi Terstruktur",
                                        fontSize = 11.sp,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Kembali",
                                        tint = colorScheme.onSurface
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        Surface(
                            tonalElevation = 8.dp,
                            shadowElevation = 8.dp,
                            color = colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { handleStartGeneration() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "✨ Generate Materi Modul Sekarang",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            if (errorMessage != null) {
                                Surface(
                                    color = colorScheme.errorContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = colorScheme.error, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(errorMessage ?: "", color = colorScheme.onErrorContainer, fontSize = 12.sp)
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }

                            // 1. FORMAT SELECTION
                            Text("Format Materi Pembelajaran", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Infografis Majalah
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedMode = "INFOGRAPHIC" },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selectedMode == "INFOGRAPHIC") primaryHighlightBg else colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.5.dp, if (selectedMode == "INFOGRAPHIC") EmeraldPrimary else colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("🖼️ Infografis Majalah", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedMode == "INFOGRAPHIC") EmeraldPrimary else colorScheme.onSurface)
                                        Spacer(Modifier.height(4.dp))
                                        Text("Visual dinamis, card intisari & ilustrasi kontekstual.", fontSize = 10.sp, color = colorScheme.onSurfaceVariant, lineHeight = 14.sp)
                                    }
                                }

                                // Artikel Berita
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedMode = "ARTICLE" },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selectedMode == "ARTICLE") secondaryHighlightBg else colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.5.dp, if (selectedMode == "ARTICLE") CyanPrimary else colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("📰 Naskah Berita / Modul", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedMode == "ARTICLE") CyanPrimary else colorScheme.onSurface)
                                        Spacer(Modifier.height(4.dp))
                                        Text("Sub-bab rinci, penjelasan ilmiah & rangkuman.", fontSize = 10.sp, color = colorScheme.onSurfaceVariant, lineHeight = 14.sp)
                                    }
                                }
                            }
                        }

                        // 2. MATA PELAJARAN
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Mata Pelajaran", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (isSubjectLocked) {
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        color = EmeraldPrimary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "🔒 Terkunci (Guru Pengampu)",
                                            fontSize = 10.sp,
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isSubjectLocked) { isSubjectDropdownOpen = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedSubject?.name ?: currentSubjectName.ifBlank { "Pilih Mata Pelajaran..." },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (selectedSubject != null || currentSubjectName.isNotBlank()) colorScheme.onSurface else colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            if (isSubjectLocked) Icons.Default.Lock else Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (!isSubjectLocked) {
                                    DropdownMenu(
                                        expanded = isSubjectDropdownOpen,
                                        onDismissRequest = { isSubjectDropdownOpen = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        availableSubjects.forEach { sub ->
                                            DropdownMenuItem(
                                                text = { Text(sub.name, fontSize = 13.sp) },
                                                onClick = {
                                                    selectedSubject = sub
                                                    isSubjectDropdownOpen = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Quick subject chips
                            if (!isSubjectLocked && availableSubjects.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    availableSubjects.take(3).forEach { sub ->
                                        val isSel = selectedSubject?.id == sub.id
                                        Surface(
                                            modifier = Modifier.clickable { selectedSubject = sub },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) EmeraldPrimary else colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, if (isSel) EmeraldPrimary else colorScheme.outlineVariant)
                                        ) {
                                            Text(
                                                sub.name,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) Color.White else colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. TARGET ROMBEL
                        item {
                            Text("Target Rombel *", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isClassDropdownOpen = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedClass?.displayName ?: currentClassName.ifBlank { "Pilih Rombel / Kelas" },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colorScheme.onSurfaceVariant)
                                    }
                                }
                                DropdownMenu(
                                    expanded = isClassDropdownOpen,
                                    onDismissRequest = { isClassDropdownOpen = false }
                                ) {
                                    unifiedClasses.forEach { cls ->
                                        DropdownMenuItem(
                                            text = { Text(cls.displayName, fontSize = 13.sp) },
                                            onClick = {
                                                selectedClass = cls
                                                isClassDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 4. TOPIK / KONSEP MATERI
                        item {
                            Text("Topik / Konsep Materi *", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customTopic,
                                onValueChange = { customTopic = it },
                                placeholder = {
                                    Text(
                                        "Contoh: Siklus Air & Hidrologi, Struktur Sel Tumbuhan, Sejarah Kemerdekaan...",
                                        fontSize = 12.sp,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 3,
                                maxLines = 5
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "AI akan menyusun judul, intisari, poin penting, analogi, serta ilustrasi sesuai silabus Kurikulum Merdeka.",
                                fontSize = 11.sp,
                                color = colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }

            MaterialGenStep.GENERATING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    AiGenerationLoadingOverlay(
                        isVisible = true,
                        type = AiGenerationType.MATERIAL,
                        subjectName = selectedSubject?.name ?: currentSubjectName,
                        topic = customTopic.ifBlank { selectedSubject?.name ?: "Materi Pembelajaran" },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            MaterialGenStep.VERIFY -> {
                val res = generatedResult
                if (res != null) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            "Verifikasi Modul Materi Ajar",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            "Periksa konten modul sebelum diterapkan ke editor",
                                            fontSize = 11.sp,
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = { currentStep = MaterialGenStep.CONFIGURE }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali ke Konfigurasi", tint = colorScheme.onSurface)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.surface)
                            )
                        },
                        bottomBar = {
                            Surface(
                                tonalElevation = 8.dp,
                                shadowElevation = 8.dp,
                                color = colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { currentStep = MaterialGenStep.CONFIGURE },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Susun Ulang", fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { onApply(res) },
                                        modifier = Modifier
                                            .weight(1.6f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimary,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Terapkan ke Editor", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    ) { padding ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Spacer(Modifier.height(4.dp))
                                // SUMMARY HERO CARD
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = primaryHighlightBg,
                                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = EmeraldPrimary,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    if (res.mode == "INFOGRAPHIC") "🖼️ INFOGRAFIS MAJALAH" else "📰 ARTIKEL BERITA",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Surface(
                                                color = colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    "${res.blocks.size} Blok Konten",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = res.title,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = colorScheme.onSurface
                                        )

                                        if (res.description.isNotBlank()) {
                                            Text(
                                                text = res.description,
                                                fontSize = 12.sp,
                                                color = colorScheme.onSurfaceVariant,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    "Pratinjau Butir Blok Konten (${res.blocks.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = colorScheme.onSurface
                                )
                            }

                            itemsIndexed(res.blocks) { idx, block ->
                                val isImg = block.type.equals("IMAGE", ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colorScheme.surface,
                                    border = BorderStroke(1.dp, colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = if (isImg) CyanPrimary.copy(alpha = 0.12f) else EmeraldPrimary.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (isImg) "🎨 GAMBAR / DIAGRAM" else "📝 TEKS / NARASI",
                                                    color = if (isImg) CyanPrimary else EmeraldPrimary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = "Blok #${idx + 1}",
                                                fontSize = 11.sp,
                                                color = colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Text(
                                            text = block.content,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp,
                                            color = colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            item {
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
