package com.schoolos.android.feature.assignments

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.AiGenerationLoadingOverlay
import com.schoolos.android.core.designsystem.AiGenerationType
import com.schoolos.android.domain.model.AcademicSubject
import com.schoolos.android.domain.model.GeneratedCurriculumQuestion
import com.schoolos.android.domain.model.GeneratedCurriculumResult
import kotlinx.coroutines.launch

private enum class GeneratorStep {
    CONFIGURE,
    GENERATING,
    VERIFY
}

private val EmeraldPrimary = Color(0xFF16A34A)
private val EmeraldBg = Color(0xFFF0FDF4)
private val EmeraldBorder = Color(0xFFBBF7D0)
private val PurplePrimary = Color(0xFF9333EA)
private val PurpleBg = Color(0xFFFAF5FF)
private val PurpleBorder = Color(0xFFE9D5FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentAiGeneratorScreen(
    availableSubjects: List<AcademicSubject>,
    currentSubjectName: String,
    isSubjectLocked: Boolean = false,
    onBack: () -> Unit,
    onApply: (GeneratedCurriculumResult) -> Unit,
    onGenerateRequest: suspend (
        format: String,
        subjectId: String,
        subjectName: String,
        topic: String,
        gradeLevel: String,
        difficulty: String,
        numMcq: Int,
        numEssay: Int,
        includeImages: Boolean,
    ) -> Result<GeneratedCurriculumResult>,
) {
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(GeneratorStep.CONFIGURE) }
    var generatedResult by remember { mutableStateOf<GeneratedCurriculumResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Configuration States
    val gradeOptions = remember {
        listOf(
            "Kelas 1 SD", "Kelas 2 SD", "Kelas 3 SD", "Kelas 4 SD", "Kelas 5 SD", "Kelas 6 SD",
            "Kelas 7 SMP", "Kelas 8 SMP", "Kelas 9 SMP",
            "Kelas 10 SMA", "Kelas 11 SMA", "Kelas 12 SMA",
            "Paket A (Setara SD)", "Paket B (Setara SMP)", "Paket C (Setara SMA)"
        )
    }

    var selectedFormat by remember { mutableStateOf("STRUCTURED_QUESTIONS") }
    var customTopic by remember { mutableStateOf("") }
    var selectedGrade by remember { mutableStateOf("Kelas 5 SD") }
    var selectedDifficulty by remember { mutableStateOf("Sedang") }
    var numMcq by remember { mutableIntStateOf(4) }
    var numEssay by remember { mutableIntStateOf(2) }
    var customMcqInput by remember { mutableStateOf("4") }
    var customEssayInput by remember { mutableStateOf("2") }
    var includeImages by remember { mutableStateOf(true) }

    var selectedSubject by remember(currentSubjectName, availableSubjects) {
        val matched = availableSubjects.find { it.name.equals(currentSubjectName, ignoreCase = true) }
        mutableStateOf(matched ?: availableSubjects.firstOrNull())
    }

    var isSubjectDropdownOpen by remember { mutableStateOf(false) }
    var isGradeDropdownOpen by remember { mutableStateOf(false) }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val emeraldContainerBg = if (isDark) colorScheme.primaryContainer.copy(alpha = 0.35f) else Color(0xFFF0FDF4)
    val purpleContainerBg = if (isDark) colorScheme.secondaryContainer.copy(alpha = 0.35f) else Color(0xFFFAF5FF)
    val context = androidx.compose.ui.platform.LocalContext.current

    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var currentGenJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    androidx.activity.compose.BackHandler(enabled = currentStep == GeneratorStep.GENERATING) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000L) {
            currentGenJob?.cancel()
            currentStep = GeneratorStep.CONFIGURE
            android.widget.Toast.makeText(context, "Proses AI dibatalkan", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            lastBackPressTime = currentTime
            android.widget.Toast.makeText(context, "Tekan sekali lagi untuk membatalkan proses AI", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val handleStartGeneration = {
        val subject = selectedSubject
        if (subject == null) {
            errorMessage = "Silakan pilih mata pelajaran terlebih dahulu."
        } else {
            errorMessage = null
            currentStep = GeneratorStep.GENERATING

            val finalMcq = if (selectedFormat == "HOMEWORK_PR") 0 else numMcq
            val finalEssay = numEssay
            val effectiveTopic = customTopic.trim().ifBlank { subject.name }

            currentGenJob = scope.launch {
                val res = onGenerateRequest(
                    selectedFormat,
                    subject.id,
                    subject.name,
                    effectiveTopic,
                    selectedGrade,
                    selectedDifficulty,
                    finalMcq,
                    finalEssay,
                    includeImages
                )
                res.onSuccess { result ->
                    generatedResult = result
                    currentStep = GeneratorStep.VERIFY
                }.onFailure { err ->
                    if (err !is kotlinx.coroutines.CancellationException) {
                        errorMessage = err.message ?: "Gagal menyusun tugas dengan AI"
                        currentStep = GeneratorStep.CONFIGURE
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentStep) {
            GeneratorStep.CONFIGURE -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        "Studio AI Penugasan Guru",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        "Kurikulum Merdeka • Berbasis Standar Kemendikdasmen",
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
                                    "✨ Mulai Susun Tugas Otomatis",
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
                            // Alert Error if any
                            if (errorMessage != null) {
                                Surface(
                                    color = Color(0xFFFEF2F2),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFDC2626))
                                        Spacer(Modifier.width(8.dp))
                                        Text(errorMessage ?: "", color = Color(0xFF991B1B), fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // 1. Mata Pelajaran
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Mata Pelajaran (Mapel)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = colorScheme.onSurface
                                )
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
                            Spacer(Modifier.height(6.dp))
                            ExposedDropdownMenuBox(
                                expanded = if (isSubjectLocked) false else isSubjectDropdownOpen,
                                onExpandedChange = { if (!isSubjectLocked) isSubjectDropdownOpen = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedSubject?.name ?: "Pilih Mata Pelajaran",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        if (isSubjectLocked) {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary)
                                        } else {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSubjectDropdownOpen)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary
                                    )
                                )
                                if (!isSubjectLocked) {
                                    ExposedDropdownMenu(
                                        expanded = isSubjectDropdownOpen,
                                        onDismissRequest = { isSubjectDropdownOpen = false }
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
                        }

                        // 2. Topik / Materi Spesifik
                        item {
                            Text(
                                "Topik / Materi Penugasan Spesifik (Opsional)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = colorScheme.onSurface
                            )
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customTopic,
                                onValueChange = { customTopic = it },
                                placeholder = {
                                    Text(
                                        "Contoh: Hukum Newton, Rukun Islam, Ekosistem Hutan",
                                        fontSize = 12.sp,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary
                                )
                            )
                        }

                        // 3. Jenjang & Tingkat Kesulitan
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Jenjang / Kelas", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(Modifier.height(6.dp))
                                    ExposedDropdownMenuBox(
                                        expanded = isGradeDropdownOpen,
                                        onExpandedChange = { isGradeDropdownOpen = it }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedGrade,
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGradeDropdownOpen) },
                                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = EmeraldPrimary
                                            )
                                        )
                                        ExposedDropdownMenu(
                                            expanded = isGradeDropdownOpen,
                                            onDismissRequest = { isGradeDropdownOpen = false }
                                        ) {
                                            gradeOptions.forEach { g ->
                                                DropdownMenuItem(
                                                    text = { Text(g, fontSize = 12.sp) },
                                                    onClick = {
                                                        selectedGrade = g
                                                        isGradeDropdownOpen = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text("Tingkat Kesulitan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("Mudah", "Sedang", "HOTS").forEach { diff ->
                                            val isSel = selectedDifficulty == diff
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .clickable { selectedDifficulty = diff },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSel) EmeraldBg else colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    if (isSel) 1.5.dp else 1.dp,
                                                    if (isSel) EmeraldPrimary else colorScheme.outlineVariant
                                                )
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        diff,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSel) EmeraldPrimary else colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Format Tugas (Opsi A vs Opsi B)
                        item {
                            Text("Format Penugasan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Opsi A
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedFormat = "STRUCTURED_QUESTIONS" },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selectedFormat == "STRUCTURED_QUESTIONS") EmeraldBg else colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (selectedFormat == "STRUCTURED_QUESTIONS") 1.5.dp else 1.dp,
                                        if (selectedFormat == "STRUCTURED_QUESTIONS") EmeraldPrimary else colorScheme.outlineVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("📚 Opsi A: Terstruktur", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedFormat == "STRUCTURED_QUESTIONS") EmeraldPrimary else colorScheme.onSurface)
                                        Spacer(Modifier.height(4.dp))
                                        Text("Kombinasi soal PG otomatis dan uraian analitis.", fontSize = 10.sp, color = colorScheme.onSurfaceVariant)
                                    }
                                }
                                // Opsi B
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedFormat = "HOMEWORK_PR" },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selectedFormat == "HOMEWORK_PR") EmeraldBg else colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (selectedFormat == "HOMEWORK_PR") 1.5.dp else 1.dp,
                                        if (selectedFormat == "HOMEWORK_PR") EmeraldPrimary else colorScheme.outlineVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("📝 Opsi B: Tugas Mandiri (PR)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedFormat == "HOMEWORK_PR") EmeraldPrimary else colorScheme.onSurface)
                                        Spacer(Modifier.height(4.dp))
                                        Text("Tugas mandiri, petunjuk foto lembar kerja & rubrik.", fontSize = 10.sp, color = colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // 5. Jumlah Soal PG & Essay
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (selectedFormat == "STRUCTURED_QUESTIONS") {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Jumlah Soal Pilihan Ganda (PG)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Surface(
                                                    color = EmeraldPrimary,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("$numMcq PG", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                listOf(2, 4, 5, 10).forEach { count ->
                                                    val isSel = numMcq == count
                                                    Surface(
                                                        modifier = Modifier.clickable {
                                                            numMcq = count
                                                            customMcqInput = count.toString()
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isSel) EmeraldPrimary else colorScheme.surface,
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) EmeraldPrimary else colorScheme.outlineVariant)
                                                    ) {
                                                        Text(
                                                            "$count PG",
                                                            fontSize = 11.sp,
                                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSel) Color.White else colorScheme.onSurface,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .width(54.dp)
                                                        .height(34.dp)
                                                        .background(colorScheme.surface, RoundedCornerShape(8.dp))
                                                        .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    androidx.compose.foundation.text.BasicTextField(
                                                        value = customMcqInput,
                                                        onValueChange = {
                                                            customMcqInput = it
                                                            it.toIntOrNull()?.let { v -> if (v in 1..20) numMcq = v }
                                                        },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        singleLine = true,
                                                        textStyle = androidx.compose.ui.text.TextStyle(
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                            color = colorScheme.onSurface
                                                        ),
                                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }

                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                if (selectedFormat == "HOMEWORK_PR") "Jumlah Butir Tugas PR" else "Jumlah Soal Uraian / Essay",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Surface(
                                                color = PurplePrimary,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("$numEssay Essay", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            listOf(1, 2, 3, 5).forEach { count ->
                                                val isSel = numEssay == count
                                                Surface(
                                                    modifier = Modifier.clickable {
                                                        numEssay = count
                                                        customEssayInput = count.toString()
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSel) PurplePrimary else colorScheme.surface,
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) PurplePrimary else colorScheme.outlineVariant)
                                                ) {
                                                    Text(
                                                        "$count Essay",
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSel) Color.White else colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .width(54.dp)
                                                    .height(34.dp)
                                                    .background(colorScheme.surface, RoundedCornerShape(8.dp))
                                                    .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                androidx.compose.foundation.text.BasicTextField(
                                                    value = customEssayInput,
                                                    onValueChange = {
                                                        customEssayInput = it
                                                        it.toIntOrNull()?.let { v -> if (v in 1..15) numEssay = v }
                                                    },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    textStyle = androidx.compose.ui.text.TextStyle(
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                        color = colorScheme.onSurface
                                                    ),
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 6. Sertakan Diagram / Gambar
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { includeImages = !includeImages }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = includeImages,
                                        onCheckedChange = { includeImages = it },
                                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Column {
                                        Text("🎨 Sertakan Soal Bergambar / Diagram", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Ilustrasi edukatif Kurikulum Merdeka (sains, bagan, grafik)", fontSize = 10.sp, color = colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }

            GeneratorStep.GENERATING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    AiGenerationLoadingOverlay(
                        isVisible = true,
                        type = AiGenerationType.ASSIGNMENT,
                        subjectName = selectedSubject?.name ?: currentSubjectName,
                        topic = customTopic.ifBlank { selectedSubject?.name ?: "Umum" },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            GeneratorStep.VERIFY -> {
                val res = generatedResult
                if (res != null) {
                    val mcqCount = res.questions.count { it.questionType == "MULTIPLE_CHOICE" }
                    val essayCount = res.questions.count { it.questionType == "ESSAY" }

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            "Verifikasi Hasil AI",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            "Periksa & Verifikasi sebelum diterapkan ke penugasan",
                                            fontSize = 11.sp,
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = { currentStep = GeneratorStep.CONFIGURE }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Kembali ke Pengaturan",
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { currentStep = GeneratorStep.CONFIGURE },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Ubah / Regenerate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onApply(res) },
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimary,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("✓ Terapkan ke Studio", fontSize = 13.sp, fontWeight = FontWeight.Black)
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
                                // Summary Card
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = EmeraldBg,
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.Verified, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                                Text(
                                                    "Paket Tugas Terverifikasi",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = EmeraldPrimary
                                                )
                                            }
                                            Text(
                                                "${res.questions.size} Butir Total",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                color = EmeraldPrimary
                                            )
                                        }

                                        Text(
                                            res.title,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            color = colorScheme.onSurface
                                        )

                                        // Badges row
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (mcqCount > 0) {
                                                Surface(color = EmeraldPrimary, shape = RoundedCornerShape(6.dp)) {
                                                    Text("$mcqCount Soal PG", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                                }
                                            }
                                            if (essayCount > 0) {
                                                Surface(color = PurplePrimary, shape = RoundedCornerShape(6.dp)) {
                                                    Text("$essayCount Soal Essay", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                                }
                                            }
                                            Surface(color = Color(0xFFF59E0B), shape = RoundedCornerShape(6.dp)) {
                                                Text("KKM: ${res.passingScore}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                            }
                                        }

                                        if (!res.instructions.isNullOrBlank()) {
                                            Surface(
                                                color = Color.White.copy(alpha = 0.8f),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    res.instructions ?: "",
                                                    fontSize = 11.sp,
                                                    color = colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(10.dp),
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    "Daftar Butir Soal Hasil Sintesis AI:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = colorScheme.onSurface
                                )
                            }

                            // Items
                            itemsIndexed(res.questions) { idx, q ->
                                QuestionVerificationCard(index = idx + 1, question = q)
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

@Composable
private fun QuestionVerificationCard(
    index: Int,
    question: GeneratedCurriculumQuestion
) {
    val colorScheme = MaterialTheme.colorScheme
    val isMcq = question.questionType == "MULTIPLE_CHOICE"

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isMcq) EmeraldPrimary else PurplePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(index.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Surface(
                        color = if (isMcq) EmeraldBg else PurpleBg,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isMcq) EmeraldBorder else PurpleBorder)
                    ) {
                        Text(
                            if (isMcq) "Pilihan Ganda" else "Uraian / Essay",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMcq) EmeraldPrimary else PurplePrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    "${question.points} Poin",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }

            // Question Text
            Text(
                question.questionText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = colorScheme.onSurface,
                lineHeight = 18.sp
            )

            // Choices for MCQ
            if (isMcq && question.choices.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    question.choices.forEachIndexed { cIdx, choice ->
                        val letter = ('A' + cIdx).toString()
                        val isCorrect = choice.isCorrect
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCorrect) EmeraldBg else colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isCorrect) 1.5.dp else 1.dp,
                                if (isCorrect) EmeraldPrimary else colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (isCorrect) EmeraldPrimary else colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(letter, color = if (isCorrect) Color.White else colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    choice.choiceText,
                                    fontSize = 12.sp,
                                    color = if (isCorrect) EmeraldPrimary else colorScheme.onSurface,
                                    fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isCorrect) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Explanation / Rubric
            if (!question.explanation.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            question.explanation ?: "",
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
