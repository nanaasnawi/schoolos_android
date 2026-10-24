package com.schoolos.android.feature.profile.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.feature.profile.util.QrCodeGenerator

@Composable
fun ProfileQrCard(
    userName: String,
    roleLabel: String,
    schoolName: String,
    qrToken: String,
    roleNeon: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDarkTheme = LocalIsDarkTheme.current
    val tokenToUse = qrToken.ifBlank { "sch_qr_v1_guest" }
    val qrBitmap = remember(tokenToUse) {
        QrCodeGenerator.generateQrBitmap(tokenToUse, width = 512, height = 512)
    }

    var showEnlargedDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDarkTheme) 0.dp else 1.5.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x10000000)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(20.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Row: Title & Role Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "QR Code Login",
                        tint = roleNeon,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "KARTU QR LOGIN SAYA",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(roleNeon.copy(alpha = 0.12f))
                        .border(0.5.dp, roleNeon.copy(alpha = 0.30f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = roleLabel,
                        color = roleNeon,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Clean QR Code Box (Clickable to open fullscreen view)
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = if (isDarkTheme) 0.dp else 2.dp,
                        shape = RoundedCornerShape(14.dp),
                        spotColor = Color(0x12000000)
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(
                        1.dp,
                        if (isDarkTheme) roleNeon.copy(alpha = 0.35f) else Color(0xFFE2E8F0),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { showEnlargedDialog = true }
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code Login",
                        modifier = Modifier.size(160.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Gagal memuat QR Code",
                            fontSize = 11.sp,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // User Identity & Affiliation
            Text(
                text = userName.ifBlank { "Pengguna Akselerasi-Edu" },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = schoolName.ifBlank { "Akselerasi-Edu Platform" },
                fontSize = 12.sp,
                color = TextSecondary,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Tunjukkan atau pindai QR Code ini untuk login cepat tanpa kata sandi.",
                fontSize = 11.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            Spacer(Modifier.height(18.dp))

            // Single, Clean, High-Contrast Action Button
            val buttonContainer = if (isDarkTheme) roleNeon else Color(0xFF059669)
            val buttonContent = if (isDarkTheme) Color.Black else Color.White

            Button(
                onClick = {
                    saveQrCardToGallery(
                        context = context,
                        userName = userName,
                        roleLabel = roleLabel,
                        schoolName = schoolName,
                        qrToken = tokenToUse,
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonContainer,
                    contentColor = buttonContent,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Unduh QR",
                    modifier = Modifier.size(18.dp),
                    tint = buttonContent,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Unduh Kartu QR Login",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = buttonContent,
                )
            }
        }
    }

    // Fullscreen QR dialog when user taps the QR code
    if (showEnlargedDialog) {
        EnlargedQrPreviewDialog(
            userName = userName,
            roleLabel = roleLabel,
            schoolName = schoolName,
            tokenToUse = tokenToUse,
            onDismissRequest = { showEnlargedDialog = false },
        )
    }
}

@Composable
private fun EnlargedQrPreviewDialog(
    userName: String,
    roleLabel: String,
    schoolName: String,
    tokenToUse: String,
    onDismissRequest: () -> Unit,
) {
    val enlargedBitmap = remember(tokenToUse) {
        QrCodeGenerator.generateQrBitmap(tokenToUse, width = 720, height = 720)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { onDismissRequest() }
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .clickable(enabled = false) {}
                    .padding(24.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "KARTU QR LOGIN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp,
                            )
                            Text(
                                text = schoolName.ifBlank { "Akselerasi-Edu Platform" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                            )
                        }

                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (enlargedBitmap != null) {
                            Image(
                                bitmap = enlargedBitmap.asImageBitmap(),
                                contentDescription = "QR Code Login Besar",
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = userName.ifBlank { "Pengguna Akselerasi-Edu" },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                    )

                    Spacer(Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFECFDF5))
                            .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "ROLE: $roleLabel",
                            color = Color(0xFF059669),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Arahkan layar ke kamera scanner kiosk atau presensi.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F172A),
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Tutup",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Capture and save the QR Login Card as a PNG image in Gallery.
 */
private fun saveQrCardToGallery(
    context: Context,
    userName: String,
    roleLabel: String,
    schoolName: String,
    qrToken: String,
) {
    try {
        val width = 800
        val height = 1050
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply {
            color = AndroidColor.parseColor("#0F172A") // CosmicNavy
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Card Border
        val borderPaint = Paint().apply {
            color = AndroidColor.parseColor("#334155")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        val cardRect = RectF(20f, 20f, width - 20f, height - 20f)
        canvas.drawRoundRect(cardRect, 32f, 32f, borderPaint)

        // Header Text
        val titlePaint = Paint().apply {
            color = AndroidColor.WHITE
            textSize = 36f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("KARTU QR LOGIN RESMI", width / 2f, 100f, titlePaint)

        val subPaint = Paint().apply {
            color = AndroidColor.parseColor("#94A3B8")
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(schoolName.ifBlank { "Akselerasi-Edu Platform" }, width / 2f, 145f, subPaint)

        // White QR Box
        val qrBoxSize = 480f
        val qrBoxLeft = (width - qrBoxSize) / 2f
        val qrBoxTop = 200f
        val qrBgPaint = Paint().apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
        }
        val qrBoxRect = RectF(qrBoxLeft, qrBoxTop, qrBoxLeft + qrBoxSize, qrBoxTop + qrBoxSize)
        canvas.drawRoundRect(qrBoxRect, 24f, 24f, qrBgPaint)

        // Draw ZXing QR Bitmap directly into card
        val qrBmp = QrCodeGenerator.generateQrBitmap(qrToken, width = 420, height = 480)
        if (qrBmp != null) {
            val qrDestRect = RectF(qrBoxLeft + 30f, qrBoxTop + 30f, qrBoxLeft + qrBoxSize - 30f, qrBoxTop + qrBoxSize - 30f)
            canvas.drawBitmap(qrBmp, null, qrDestRect, null)
        }

        // User Name
        val namePaint = Paint().apply {
            color = AndroidColor.WHITE
            textSize = 42f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(userName.ifBlank { "Pengguna Akselerasi-Edu" }, width / 2f, 750f, namePaint)

        // Role Badge
        val rolePaint = Paint().apply {
            color = AndroidColor.parseColor("#38BDF8")
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ROLE: $roleLabel", width / 2f, 805f, rolePaint)

        // Footer Note
        val footerPaint = Paint().apply {
            color = AndroidColor.parseColor("#64748B")
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Pindai QR ini pada layar Login untuk masuk otomatis.", width / 2f, 880f, footerPaint)
        canvas.drawText("Simpan dan jaga kerahasiaan kartu QR Anda.", width / 2f, 920f, footerPaint)

        // Save Bitmap to MediaStore
        val filename = "AkselerasiEdu_QR_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AkselerasiEdu")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (uri != null) {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            Toast.makeText(context, "Kartu QR Login berhasil disimpan ke Galeri (Pictures/AkselerasiEdu)!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Gagal menyimpan Kartu QR ke Galeri.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal mengunduh Kartu QR: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
