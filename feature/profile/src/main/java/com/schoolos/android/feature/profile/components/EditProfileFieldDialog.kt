package com.schoolos.android.feature.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileFieldDialog(
    fieldKey: String, // "username", "email", "phone", "about"
    currentValue: String,
    roleNeon: Color,
    onSave: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var textValue by remember { mutableStateOf(currentValue) }

    val (title, helperText) = when (fieldKey) {
        "username" -> "Masukkan Nama Pengguna" to "Nama pengguna dapat digunakan untuk login dan identitas akun SchoolOS."
        "email" -> "Masukkan Alamat Email" to "Email ini digunakan untuk notifikasi dan pemulihan akun."
        "phone" -> "Masukkan Nomor Telepon" to "Nomor telepon aktif untuk komunikasi dan WhatsApp sekolah."
        "about" -> "Ubah Status / Tentang" to "Tuliskan status atau informasi singkat mengenai aktivitas sekolah Anda."
        else -> "Ubah Informasi" to "Masukkan informasi terbaru Anda."
    }

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CosmicNavy)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = helperText,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp,
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = fieldKey != "about",
                maxLines = if (fieldKey == "about") 3 else 1,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = roleNeon,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = roleNeon,
                ),
                shape = RoundedCornerShape(10.dp),
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))

                TextButton(onClick = onDismissRequest) {
                    Text(
                        text = "Batal",
                        color = TextTertiary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(Modifier.width(8.dp))

                TextButton(
                    onClick = {
                        onSave(textValue)
                        onDismissRequest()
                    },
                    enabled = textValue.isNotBlank(),
                ) {
                    Text(
                        text = "Simpan",
                        color = if (textValue.isNotBlank()) roleNeon else TextTertiary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
