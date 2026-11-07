package com.schoolos.android.feature.learning

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.schoolos.android.core.common.toHumanReadableError
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AcademicSubject
import com.schoolos.android.domain.model.GeneratedMaterialResult
import com.schoolos.android.domain.model.findClass
import com.schoolos.android.domain.model.groupParallelClasses
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private enum class MaterialGenStep {
    CONFIGURE,
    GENERATING,
    VERIFY
}

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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current

    val pageBg = if (isDark) Color(0xFF09090B) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val subSurfaceBg = if (isDark) Color(0xFF18181B) else Color(0xFFF1F5F9)
    val subSurfaceBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    val emeraldAccent = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val emeraldButton = Color(0xFF059669)
    val emeraldSoftBg = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5)
    val emeraldSoftBorder = if (isDark) Color(0xFF059669).copy(alpha = 0.45f) else Color(0xFFA7F3D0)

    val cyanAccent = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val cyanSoftBg = if (isDark) Color(0xFF0C243B).copy(alpha = 0.45f) else Color(0xFFE0F2FE)
    val cyanSoftBorder = if (isDark) Color(0xFF0284C7).copy(alpha = 0.45f) else Color(0xFFBAE6FD)

    var currentStep by remember { mutableStateOf(MaterialGenStep.CONFIGURE) }
    var generatedResult by remember { mutableStateOf<GeneratedMaterialResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var currentGenJob by remember { mutableStateOf<Job?>(null) }

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
                    if (err !is CancellationException) {
                        errorMessage = err.toHumanReadableError(fallback = "Gagal menyusun materi pembelajaran dengan AI. Silakan periksa koneksi dan coba lagi.")
                        currentStep = MaterialGenStep.CONFIGURE
                    }
                }
            }
        }
    }

    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Box(modifier = Modifier.fillMaxSize().background(pageBg)) {
        when (currentStep) {
            MaterialGenStep.CONFIGURE -> {
                Scaffold(
                    containerColor = pageBg,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        ExecutiveTopBar(
                            title = "Studio AI Materi & Modul",
                            subtitle = "Kurikulum Merdeka • Sintesis Infografis & Naskah",
                            onBack = onBack
                        )
                    },
                    bottomBar = {
                        if (!isImeVisible) {
                            Surface(
                                color = cardBg,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Button(
                                        onClick = { handleStartGeneration() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = emeraldButton,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Mulai Susun Modul Otomatis",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = padding.calculateTopPadding(),
                                bottom = if (isImeVisible) 0.dp else padding.calculateBottomPadding()
                            )
                            .imePadding()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (errorMessage != null) {
                            item {
                                Surface(
                                    color = NeonError.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, NeonError.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = NeonError, modifier = Modifier.size(18.dp))
                                        Text(errorMessage ?: "", color = NeonError, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        // ── 1. FORMAT MATERI PEMBELAJARAN ──
                        item {
                            Surface(
                                color = cardBg,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(emeraldAccent)
                                        )
                                        Text(
                                            "FORMAT MODUL DIGITAL",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = emeraldAccent,
                                            letterSpacing = 0.6.sp
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val isInfo = selectedMode == "INFOGRAPHIC"
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(13.dp))
                                                .clickable { selectedMode = "INFOGRAPHIC" },
                                            shape = RoundedCornerShape(13.dp),
                                            color = if (isInfo) emeraldSoftBg else subSurfaceBg,
                                            border = BorderStroke(
                                                width = if (isInfo) 1.5.dp else 1.dp,
                                                color = if (isInfo) emeraldAccent else subSurfaceBorder
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "Infografis Visual",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = if (isInfo) emeraldAccent else textPrimary
                                                    )
                                                    if (isInfo) {
                                                        Icon(Icons.Default.CheckCircle, null, tint = emeraldAccent, modifier = Modifier.size(15.dp))
                                                    }
                                                }
                                                Text(
                                                    "Visual dinamis, poin intisari & diagram kontekstual.",
                                                    fontSize = 10.5.sp,
                                                    color = textSecondary,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }

                                        val isArticle = selectedMode == "ARTICLE"
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(13.dp))
                                                .clickable { selectedMode = "ARTICLE" },
                                            shape = RoundedCornerShape(13.dp),
                                            color = if (isArticle) cyanSoftBg else subSurfaceBg,
                                            border = BorderStroke(
                                                width = if (isArticle) 1.5.dp else 1.dp,
                                                color = if (isArticle) cyanAccent else subSurfaceBorder
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "Artikel Naskah",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = if (isArticle) cyanAccent else textPrimary
                                                    )
                                                    if (isArticle) {
                                                        Icon(Icons.Default.CheckCircle, null, tint = cyanAccent, modifier = Modifier.size(15.dp))
                                                    }
                                                }
                                                Text(
                                                    "Sub-bab terstruktur, uraian konsep & kesimpulan.",
                                                    fontSize = 10.5.sp,
                                                    color = textSecondary,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── 2. SASARAN ROMBEL & MATA PELAJARAN (BOTTOM SHEET PICKER) ──
                        item {
                            Surface(
                                color = cardBg,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        "Sasaran Kelas & Mata Pelajaran",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = textPrimary
                                    )

                                    val classNames = remember(unifiedClasses, currentClassName) {
                                        val list = unifiedClasses.map { it.displayName }
                                        if (list.isEmpty() && currentClassName.isNotBlank()) listOf(currentClassName) else list
                                    }

                                    ExecutiveBottomSheetSelector(
                                        value = selectedClass?.displayName ?: currentClassName.ifBlank { "Pilih Rombel" },
                                        label = "Rombel / Kelas Target",
                                        icon = Icons.Default.Group,
                                        options = classNames,
                                        onSelect = { chosen ->
                                            selectedClass = unifiedClasses.findClass(chosen)
                                        },
                                        sheetTitle = "Pilih Rombel Target",
                                        sheetSubtitle = "Kedalaman materi akan disesuaikan dengan jenjang kelas ini",
                                        accentColor = emeraldAccent
                                    )

                                    val effectiveSubjectLocked = isSubjectLocked && availableSubjects.size <= 1
                                    val subNames = remember(availableSubjects, currentSubjectName) {
                                        val list = availableSubjects.map { it.name }
                                        if (list.isEmpty() && currentSubjectName.isNotBlank()) listOf(currentSubjectName) else list
                                    }

                                    ExecutiveBottomSheetSelector(
                                        value = selectedSubject?.name ?: currentSubjectName.ifBlank { "Pilih Mata Pelajaran" },
                                        label = "Mata Pelajaran",
                                        icon = Icons.Default.School,
                                        options = subNames,
                                        onSelect = { chosen ->
                                            selectedSubject = availableSubjects.find { it.name.equals(chosen, ignoreCase = true) }
                                        },
                                        sheetTitle = "Pilih Mata Pelajaran",
                                        sheetSubtitle = "Pilih mata pelajaran yang sesuai dengan modul ajar",
                                        accentColor = emeraldAccent,
                                        isLocked = effectiveSubjectLocked,
                                        lockedBadgeText = "Tutor Pengampu",
                                        onLockedClick = {
                                            Toast.makeText(context, "Mata pelajaran dikunci sesuai penugasan tutor", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }

                        // ── 3. TOPIK / KONSEP MATERI ──
                        item {
                            Surface(
                                color = cardBg,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        "Topik / Konsep Materi *",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = textPrimary
                                    )

                                    OutlinedTextField(
                                        value = customTopic,
                                        onValueChange = { customTopic = it },
                                        placeholder = {
                                            Text(
                                                "Contoh: Siklus Air & Hidrologi, Struktur Sel Tumbuhan, Pergerakan Nasional...",
                                                fontSize = 12.sp,
                                                color = textTertiary
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        minLines = 3,
                                        maxLines = 5,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = emeraldAccent,
                                            unfocusedBorderColor = subSurfaceBorder,
                                            focusedTextColor = textPrimary,
                                            unfocusedTextColor = textPrimary,
                                            cursorColor = emeraldAccent
                                        )
                                    )

                                    Text(
                                        "AI akan menyusun judul, intisari materi, analogi kontekstual, serta ilustrasi visual yang siap diterbitkan.",
                                        fontSize = 11.sp,
                                        color = textSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            MaterialGenStep.GENERATING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(pageBg),
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
                        containerColor = pageBg,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        topBar = {
                            ExecutiveTopBar(
                                title = "Verifikasi Modul AI",
                                subtitle = "Tinjau hasil sintesis sebelum diterapkan ke Studio Materi",
                                onBack = { currentStep = MaterialGenStep.CONFIGURE }
                            )
                        },
                        bottomBar = {
                            Surface(
                                color = cardBg,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { currentStep = MaterialGenStep.CONFIGURE },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                                        border = BorderStroke(1.dp, cardBorder)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Susun Ulang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onApply(res) },
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = emeraldButton,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Terapkan ke Studio", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                            contentPadding = PaddingValues(vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = cardBg,
                                    border = BorderStroke(1.dp, cardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(emeraldSoftBg)
                                                    .border(0.5.dp, emeraldSoftBorder, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    if (res.mode == "INFOGRAPHIC") "INFOGRAFIS VISUAL" else "ARTIKEL NASKAH",
                                                    color = emeraldAccent,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(subSurfaceBg)
                                                    .border(0.5.dp, subSurfaceBorder, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    "${res.blocks.size} Bagian Konten",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textSecondary
                                                )
                                            }
                                        }

                                        Text(
                                            text = res.title,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = textPrimary,
                                            lineHeight = 22.sp
                                        )

                                        if (res.description.isNotBlank()) {
                                            Text(
                                                text = res.description,
                                                fontSize = 12.sp,
                                                color = textSecondary,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    "Pratinjau Isi Modul (${res.blocks.size} Bagian)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = textPrimary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            itemsIndexed(res.blocks) { idx, block ->
                                val isImg = block.type.equals("IMAGE", ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = cardBg,
                                    border = BorderStroke(1.dp, cardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isImg) cyanSoftBg else emeraldSoftBg)
                                                    .border(0.5.dp, if (isImg) cyanSoftBorder else emeraldSoftBorder, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = if (isImg) "ILUSTRASI / DIAGRAM" else "TEKS / PENJELASAN",
                                                    color = if (isImg) cyanAccent else emeraldAccent,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = "Bagian #${idx + 1}",
                                                fontSize = 11.sp,
                                                color = textTertiary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (isImg) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(190.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(subSurfaceBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                SubcomposeAsyncImage(
                                                    model = block.content.trim(),
                                                    contentDescription = "Visual #${idx + 1}",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize(),
                                                    loading = {
                                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(24.dp),
                                                                strokeWidth = 2.dp,
                                                                color = cyanAccent
                                                            )
                                                        }
                                                    },
                                                    error = {
                                                        Column(
                                                            modifier = Modifier.fillMaxSize().padding(12.dp),
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            Icon(Icons.Default.BrokenImage, null, tint = textTertiary, modifier = Modifier.size(26.dp))
                                                            Spacer(Modifier.height(4.dp))
                                                            Text("Visual belum dapat dimuat", fontSize = 11.sp, color = textSecondary)
                                                        }
                                                    }
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = block.content,
                                                fontSize = 12.5.sp,
                                                color = textPrimary,
                                                lineHeight = 19.sp
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
