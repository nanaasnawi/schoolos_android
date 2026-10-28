package com.schoolos.android.feature.profile

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.StudentDocumentItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDossierScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val dossier = state.studentDossier
    val isLoading = state.studentDossierLoading
    val isUploading = state.isUploadingDocument
    val errorMessage = state.studentDossierError

    var activeUploadingDocType by remember { mutableStateOf<String?>(null) }
    var selectedDocForUpload by remember { mutableStateOf<StudentDocumentItem?>(null) }

    // Google Play Services ML Kit Document Scanner (CamScanner AI auto edge-detect, perspective crop, and PDF output)
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            if (scanningResult != null && activeUploadingDocType != null) {
                val docType = activeUploadingDocType!!
                try {
                    val contentResolver = context.contentResolver
                    val pdf = scanningResult.pdf
                    if (pdf != null) {
                        val inputStream = contentResolver.openInputStream(pdf.uri)
                        val bytes = inputStream?.readBytes() ?: ByteArray(0)
                        inputStream?.close()

                        if (bytes.isNotEmpty()) {
                            val filename = "${docType.lowercase()}_scan.pdf"
                            viewModel.uploadStudentDocument(
                                documentType = docType,
                                bytes = bytes,
                                filename = filename,
                                mimeType = "application/pdf",
                                notes = "Dipindai rapi via CamScanner AI Android (${pdf.pageCount} halaman)"
                            ) { success, msg ->
                                if (success) {
                                    Toast.makeText(context, "✅ Dokumen PDF berhasil dipindai dan diunggah!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, msg ?: "Gagal mengunggah dokumen", Toast.LENGTH_LONG).show()
                                }
                            }
                            return@rememberLauncherForActivityResult
                        }
                    }

                    // Fallback to individual page JPEG if PDF format was unavailable
                    val pages = scanningResult.pages
                    if (!pages.isNullOrEmpty()) {
                        val firstPage = pages[0]
                        val inputStream = contentResolver.openInputStream(firstPage.imageUri)
                        val bytes = inputStream?.readBytes() ?: ByteArray(0)
                        inputStream?.close()

                        if (bytes.isNotEmpty()) {
                            val filename = "${docType.lowercase()}_scan.jpg"
                            viewModel.uploadStudentDocument(
                                documentType = docType,
                                bytes = bytes,
                                filename = filename,
                                mimeType = "image/jpeg",
                                notes = "Dipindai rapi via CamScanner AI Android"
                            ) { success, msg ->
                                if (success) {
                                    Toast.makeText(context, "✅ Berkas berhasil dipindai dan diunggah!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, msg ?: "Gagal mengunggah berkas", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal memproses hasil scan: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // System File Picker for Image / PDF
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && activeUploadingDocType != null) {
            val docType = activeUploadingDocType!!
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: ByteArray(0)
                inputStream?.close()

                if (bytes.isEmpty()) {
                    Toast.makeText(context, "Berkas kosong atau tidak dapat dibaca", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }

                val ext = when (mimeType) {
                    "application/pdf" -> "pdf"
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> "jpg"
                }
                val filename = "${docType.lowercase()}_upload.$ext"

                viewModel.uploadStudentDocument(
                    documentType = docType,
                    bytes = bytes,
                    filename = filename,
                    mimeType = mimeType,
                    notes = "Diunggah mandiri oleh Siswa dari Aplikasi Android"
                ) { success, msg ->
                    if (success) {
                        Toast.makeText(context, "✅ Berkas berhasil diunggah!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, msg ?: "Gagal mengunggah berkas", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Kesalahan: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val startDocScanner: (String) -> Unit = { docType ->
        activeUploadingDocType = docType
        val activity = findActivity(context)
        if (activity != null) {
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(10)
                .setResultFormats(
                    GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                    GmsDocumentScannerOptions.RESULT_FORMAT_PDF
                )
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()

            val scanner = GmsDocumentScanning.getClient(options)
            scanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    try {
                        val request = IntentSenderRequest.Builder(intentSender).build()
                        scannerLauncher.launch(request)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Membuka pemilih berkas...", Toast.LENGTH_SHORT).show()
                        filePickerLauncher.launch("*/*")
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Scanner tidak tersedia: ${e.localizedMessage}. Menggunakan pemilih berkas...", Toast.LENGTH_SHORT).show()
                    filePickerLauncher.launch("*/*")
                }
        } else {
            filePickerLauncher.launch("*/*")
        }
    }

    LaunchedEffect(Unit) {
        if (dossier == null) {
            viewModel.loadStudentDossier()
        }
    }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Pemberkasan & E-Arsip",
                subtitle = "Digital Student Vault & Verifikasi",
                onBack = onBack,
                actions = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CosmicNavy)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp))
                            .clickable { viewModel.loadStudentDossier() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang Berkas",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            isLoading && dossier == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = StudentNeon,
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Memuat persyaratan berkas siswa...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            errorMessage != null && dossier == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.loadStudentDossier() },
                            colors = ButtonDefaults.buttonColors(containerColor = StudentNeon)
                        ) {
                            Text("Coba Lagi", color = Color.White)
                        }
                    }
                }
            }

            dossier != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // 1. Overview Progress Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CosmicNavy),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = dossier.fullName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "NISN: ${dossier.nisn} • ${dossier.className ?: "Siswa Aktif"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (dossier.isComplete) Color(0x2210B981) else Color(0x22F59E0B)
                                            )
                                            .border(
                                                1.dp,
                                                if (dossier.isComplete) Color(0xFF10B981) else Color(0xFFF59E0B),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = dossier.programTierLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (dossier.isComplete) Color(0xFF10B981) else Color(0xFFF59E0B)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Kelengkapan Berkas Wajib",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${dossier.verifiedCount} dari ${dossier.requiredCount} Sah (${dossier.completenessPercentage.toInt()}%)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (dossier.isComplete) Color(0xFF10B981) else Color(0xFFF59E0B)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { (dossier.completenessPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (dossier.isComplete) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    trackColor = CosmicNavy
                                )

                                if (dossier.isComplete) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Seluruh berkas wajib telah diverifikasi sah oleh TU.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Section Title
                    item {
                        Text(
                            text = "Dokumen Persyaratan",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    // 3. Document Items
                    items(dossier.documents) { doc ->
                        DocumentCardItem(
                            doc = doc,
                            isUploadingThis = isUploading && activeUploadingDocType == doc.documentType,
                            onUploadClick = {
                                selectedDocForUpload = doc
                            }
                        )
                    }
                }
            }
        }
    }

    // CamScanner AI & File Picker Modal BottomSheet
    if (selectedDocForUpload != null) {
        val targetDoc = selectedDocForUpload!!
        ModalBottomSheet(
            onDismissRequest = { selectedDocForUpload = null },
            containerColor = CosmicNavy,
            dragHandle = { BottomSheetDefaults.DragHandle(color = GlassBorder) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Unggah ${targetDoc.label}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Pilih metode pemindaian atau ambil berkas dari HP:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                )

                // Option 1: AI Scanner (CamScanner Mode -> PDF)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val docType = targetDoc.documentType
                            selectedDocForUpload = null
                            startDocScanner(docType)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicBlack),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudentNeon.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(StudentNeon.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = StudentNeon,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Scan Otomatis (Rapi & PDF)",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "CamScanner AI",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Otomatis merapikan sudut kertas, menghilangkan bayangan, mencerahkan teks, dan langsung membuat format PDF jernih.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: File Picker (Images & PDF)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeUploadingDocType = targetDoc.documentType
                            selectedDocForUpload = null
                            filePickerLauncher.launch("*/*")
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicBlack),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pilih Berkas dari HP (Galeri / PDF)",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Pilih foto dokumen atau file PDF yang sudah tersimpan di memori HP Anda.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun DocumentCardItem(
    doc: StudentDocumentItem,
    isUploadingThis: Boolean,
    onUploadClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicNavy),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (doc.status == "REJECTED") Color(0xFFEF4444) else GlassBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = doc.label,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        if (doc.isRequired) {
                            Text(
                                text = " *Wajib",
                                fontSize = 11.sp,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        } else {
                            Text(
                                text = " (Opsional)",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = doc.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                // Status Badge
                StatusPill(status = doc.status)
            }

            // Rejection reason callout
            val rejectionNote = doc.rejectionReason
            if (doc.status == "REJECTED" && !rejectionNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x22EF4444))
                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Catatan Perbaikan dari TU:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rejectionNote,
                            fontSize = 12.sp,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Upload / Change button
            Button(
                onClick = onUploadClick,
                enabled = !isUploadingThis,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (doc.status) {
                        "VERIFIED" -> CosmicNavy
                        "REJECTED" -> Color(0xFFDC2626)
                        "PENDING" -> CosmicNavy
                        else -> StudentNeon
                    }
                )
            ) {
                if (isUploadingThis) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mengunggah...", fontSize = 12.sp)
                } else {
                    val icon = when (doc.status) {
                        "VERIFIED" -> Icons.Default.Check
                        "REJECTED" -> Icons.Default.Refresh
                        "PENDING" -> Icons.Default.Upload
                        else -> Icons.Default.CameraAlt
                    }
                    val label = when (doc.status) {
                        "VERIFIED" -> "Perbarui Berkas (Jika Ada Perubahan)"
                        "REJECTED" -> "Unggah Ulang Berkas Perbaikan"
                        "PENDING" -> "Ganti Berkas (Sedang Diverifikasi)"
                        else -> "Ambil Foto / Unggah Berkas"
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun StatusPill(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "VERIFIED" -> Triple(Color(0x2210B981), Color(0xFF10B981), "Sah ✅")
        "PENDING" -> Triple(Color(0x22F59E0B), Color(0xFFF59E0B), "Review ⏳")
        "REJECTED" -> Triple(Color(0x22EF4444), Color(0xFFEF4444), "Ditolak ❌")
        else -> Triple(Color(0x11FFFFFF), TextSecondary, "Belum Ada")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

private fun findActivity(context: Context): Activity? {
    var current = context
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
}

