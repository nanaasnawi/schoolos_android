package com.schoolos.android.feature.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileFieldBottomSheet(
    fieldKey: String, // "username", "email", "phone", "about"
    currentValue: String,
    roleNeon: Color,
    onSave: (String) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var textValue by remember { mutableStateOf(currentValue) }

    val (title, helperText) = when (fieldKey) {
        "username" -> "Ubah Nama Pengguna" to "Nama pengguna digunakan untuk login dan identitas akun Anda."
        "email" -> "Ubah Alamat Email" to "Email digunakan untuk notifikasi dan pemulihan akun."
        "phone" -> "Ubah Nomor Telepon" to "Nomor telepon aktif untuk komunikasi dan WhatsApp sekolah."
        "about" -> "Ubah Status / Tentang" to "Tuliskan status atau informasi singkat mengenai aktivitas sekolah Anda."
        else -> "Ubah Informasi" to "Masukkan data terbaru Anda."
    }

    val keyboardType = when (fieldKey) {
        "email" -> KeyboardType.Email
        "phone" -> KeyboardType.Phone
        else -> KeyboardType.Text
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = CosmicNavy,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(roleNeon.copy(alpha = 0.4f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(roleNeon.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = roleNeon,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = helperText,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 2,
                        )
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CosmicBlack),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = fieldKey != "about",
                maxLines = if (fieldKey == "about") 3 else 1,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = roleNeon,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = roleNeon,
                ),
                shape = RoundedCornerShape(12.dp),
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicBlack,
                        contentColor = TextSecondary,
                    ),
                ) {
                    Text("Batal", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        onSave(textValue)
                        onDismissRequest()
                    },
                    enabled = textValue.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = roleNeon,
                        contentColor = Color.White,
                        disabledContainerColor = roleNeon.copy(alpha = 0.3f),
                        disabledContentColor = TextTertiary,
                    ),
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileFieldDialog(
    fieldKey: String,
    currentValue: String,
    roleNeon: Color,
    onSave: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    EditProfileFieldBottomSheet(
        fieldKey = fieldKey,
        currentValue = currentValue,
        roleNeon = roleNeon,
        onSave = onSave,
        onDismissRequest = onDismissRequest,
    )
}
