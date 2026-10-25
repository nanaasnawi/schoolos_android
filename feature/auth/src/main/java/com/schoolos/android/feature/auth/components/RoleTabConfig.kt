package com.schoolos.android.feature.auth.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.ParentNeon
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon

data class RoleTabConfig(
    val title: String,
    val subtitle: String = "",
    val icon: ImageVector,
    val accentColor: Color,
    val secondaryColor: Color,
    val inputLabel: String,
    val inputPlaceholder: String = "",
    val hintBadge: String = "",
    val hintExample: String = "",
)

@Composable
fun rememberRoleTabs(): List<RoleTabConfig> {
    val neonBlue = NeonBlue
    val studentNeon = StudentNeon
    val teacherNeon = TeacherNeon
    val parentNeon = ParentNeon
    return remember(neonBlue, studentNeon, teacherNeon, parentNeon) {
        listOf(
            RoleTabConfig(
                title = "Semua",
                icon = Icons.Default.Person,
                accentColor = neonBlue,
                secondaryColor = Color(0xFF38BDF8),
                inputLabel = "Username / Identitas",
                inputPlaceholder = "Masukkan username atau identitas",
            ),
            RoleTabConfig(
                title = "Siswa",
                icon = Icons.Default.School,
                accentColor = studentNeon,
                secondaryColor = Color(0xFFA855F7),
                inputLabel = "NISN / Username",
                inputPlaceholder = "Masukkan 10 digit NISN atau username",
                hintBadge = "Siswa",
                hintExample = "10 Digit NISN Dapodik",
            ),
            RoleTabConfig(
                title = "Guru",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                accentColor = teacherNeon,
                secondaryColor = Color(0xFF34D399),
                inputLabel = "NIP / NUPTK / Username",
                inputPlaceholder = "Masukkan NIP, NUPTK, atau username",
                hintBadge = "Guru",
                hintExample = "NIP / NUPTK Terdaftar",
            ),
            RoleTabConfig(
                title = "Wali",
                icon = Icons.Default.People,
                accentColor = parentNeon,
                secondaryColor = Color(0xFFFB7185),
                inputLabel = "Username Akun Ibu",
                inputPlaceholder = "Contoh: ibu_<nisn> atau username",
                hintBadge = "Akun Ibu",
                hintExample = "Gunakan username akun ibu siswa",
            ),
        )
    }
}
