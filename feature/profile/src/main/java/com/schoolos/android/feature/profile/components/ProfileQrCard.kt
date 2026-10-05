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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
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
    val tokenToUse = qrToken.ifBlank { "sch_qr_v1_guest" }
    val qrBitmap = remember(tokenToUse) {
        QrCodeGenerator.generateQrBitmap(tokenToUse, width = 512, height = 512)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(20.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Title
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

            Spacer(Modifier.height(16.dp))

            // ZXing Generated Real QR Code Box
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(2.dp, roleNeon.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Real QR Code",
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

            Spacer(Modifier.height(12.dp))

            Text(
                text = userName.ifBlank { "Pengguna SchoolOS" },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )

            Text(
                text = schoolName.ifBlank { "SchoolOS Education" },
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

            Spacer(Modifier.height(16.dp))

            // Download Button
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
                    containerColor = roleNeon,
                    contentColor = CosmicBlack,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Unduh QR",
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Unduh Kartu QR Login",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
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
        canvas.drawText(schoolName.ifBlank { "SchoolOS Platform" }, width / 2f, 145f, subPaint)

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
        canvas.drawText(userName.ifBlank { "Pengguna SchoolOS" }, width / 2f, 750f, namePaint)

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
        val filename = "SchoolOS_QR_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SchoolOS")
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
            Toast.makeText(context, "✅ Kartu QR Login berhasil disimpan ke Galeri (Pictures/SchoolOS)!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Gagal menyimpan Kartu QR ke Galeri.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal mengunduh Kartu QR: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
