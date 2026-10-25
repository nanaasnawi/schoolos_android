package com.schoolos.android.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Normalizes a subject name by stripping parenthesized qualifiers like (Umum), (Wajib), (Peminatan),
 * cleaning non-alphanumeric characters, and lowercasing.
 */
fun cleanSubjectName(raw: String): String {
    return raw
        .replace(Regex("(?i)\\s*\\((umum|wajib|peminatan)\\)"), "")
        .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .lowercase()
}

/**
 * Strict and tolerant subject matcher that prevents umbrella "Umum" contamination.
 * A specific subject like "Matematika" or "Matematika (Umum)" will NEVER match a generic "Umum" item,
 * and vice-versa.
 */
fun matchesSubjectFilter(itemSubject: String?, filter: String?): Boolean {
    if (filter.isNullOrBlank()) return true
    if (itemSubject.isNullOrBlank()) return false

    val rawItem = itemSubject.trim()
    val rawFilter = filter.trim()

    // 1. Direct case-insensitive equality
    if (rawItem.equals(rawFilter, ignoreCase = true)) return true

    // 2. Normalized comparison
    val cleanItem = cleanSubjectName(rawItem)
    val cleanFilter = cleanSubjectName(rawFilter)

    if (cleanItem == cleanFilter) return true

    // 3. Strict barrier for "Umum":
    // If either side is "umum", they ONLY match if BOTH are "umum"
    val isItemUmum = cleanItem.equals("umum", ignoreCase = true)
    val isFilterUmum = cleanFilter.equals("umum", ignoreCase = true)
    if (isItemUmum || isFilterUmum) {
        return isItemUmum && isFilterUmum
    }

    // 4. Standard Indonesian curriculum alias mapping
    val aliases = mapOf(
        "pai" to "pendidikan agama islam",
        "ipa" to "ilmu pengetahuan alam",
        "ips" to "ilmu pengetahuan sosial",
        "ppkn" to "pendidikan pancasila",
        "pkn" to "pendidikan pancasila",
        "pjok" to "pendidikan jasmani",
    )
    val resolvedItem = aliases[cleanItem] ?: cleanItem
    val resolvedFilter = aliases[cleanFilter] ?: cleanFilter

    if (resolvedItem == resolvedFilter) return true

    // 5. Meaningful substring matching (must be at least 4 chars long)
    if (resolvedItem.length >= 4 && resolvedFilter.length >= 4) {
        if (resolvedItem.contains(resolvedFilter, ignoreCase = true) ||
            resolvedFilter.contains(resolvedItem, ignoreCase = true)) {
            return true
        }
    }

    return false
}

/**
 * Extracts education package (Paket A, B, or C) from class/rombel name.
 */
fun extractPackageLetter(name: String): Char? {
    val upper = name.uppercase()
    return when {
        upper.contains("PAKET A") || upper.contains("PKBM A") || upper.contains("SD") -> 'A'
        upper.contains("PAKET B") || upper.contains("PKBM B") || upper.contains("SMP") -> 'B'
        upper.contains("PAKET C") || upper.contains("PKBM C") || upper.contains("SMA") || upper.contains("SMK") || upper.contains("KKA C") -> 'C'
        else -> null
    }
}

/**
 * Extracts grade level number (1..12) from class/rombel name.
 */
fun extractGradeNumber(name: String): Int? {
    val match = Regex("(?<![a-zA-Z])([1-9]|1[0-2])(?![0-9])").find(name)
    return match?.value?.toIntOrNull()
}

/**
 * Strict class/rombel matching to prevent cross-level material leaks (e.g., Paket A SD materials
 * or Paket C SMA materials leaking into a Paket B SMP class session).
 */
fun matchesClassFilter(itemClass: String?, filterClass: String?): Boolean {
    if (filterClass.isNullOrBlank()) return true
    if (itemClass.isNullOrBlank()) return false

    val f = filterClass.trim()
    val item = itemClass.trim()

    // 1. Direct case-insensitive equality
    if (item.equals(f, ignoreCase = true)) return true

    // 2. Package isolation (Paket A vs Paket B vs Paket C)
    val filterPackage = extractPackageLetter(f)
    val itemPackage = extractPackageLetter(item)

    if (filterPackage != null && itemPackage != null && filterPackage != itemPackage) {
        return false // Strictly reject cross-package materials
    }

    // 3. Grade isolation if both have numbers
    val filterGrade = extractGradeNumber(f)
    val itemGrade = extractGradeNumber(item)

    if (filterGrade != null && itemGrade != null) {
        return filterGrade == itemGrade
    }

    // 4. If same package and item is package-wide (no grade number), allow it
    if (filterPackage != null && itemPackage != null && filterPackage == itemPackage) {
        if (itemGrade == null) return true
    }

    return false
}

/**
 * Modern, theme-aware filter chip displaying active subject and/or class filters.
 */
@Composable
fun SubjectFilterChip(
    subject: String,
    className: String? = null,
    onClear: (() -> Unit)? = null,
    onClearSubject: (() -> Unit)? = null,
    onClearClass: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val hasSubject = subject.isNotBlank()
    val hasClass = !className.isNullOrBlank()

    if (!hasSubject && !hasClass) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NeonBlue.copy(alpha = 0.08f))
            .border(1.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.FilterList,
                contentDescription = null,
                tint = NeonBlue,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (hasSubject) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Mata Pelajaran: ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                        )
                        Text(
                            subject,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (hasClass) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Kelas / Rombel: ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                        )
                        Text(
                            className!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentNeonCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            val clearAction = onClear ?: onClearSubject ?: onClearClass
            if (clearAction != null) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Hapus filter",
                    tint = TextTertiary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = clearAction),
                )
            }
        }
    }
}