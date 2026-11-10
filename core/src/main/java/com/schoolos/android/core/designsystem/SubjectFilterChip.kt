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
 * Also intelligently maps aliases and curriculum phases (e.g. IPS <-> IPAS, IPA <-> IPAS).
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
    // If either side is pure "umum", they ONLY match if BOTH are "umum"
    val isItemUmum = cleanItem.equals("umum", ignoreCase = true)
    val isFilterUmum = cleanFilter.equals("umum", ignoreCase = true)
    if (isItemUmum || isFilterUmum) {
        return isItemUmum && isFilterUmum
    }

    // 4. Domain-level curriculum matching (handles Kurikulum Merdeka phases & acronyms)
    if (isSameSubjectDomain(cleanItem, cleanFilter)) {
        return true
    }

    // 5. Standard Indonesian curriculum alias mapping
    val aliases = mapOf(
        "pai" to "pendidikan agama islam",
        "ipa" to "ilmu pengetahuan alam",
        "ips" to "ilmu pengetahuan sosial",
        "ipas" to "ilmu pengetahuan alam dan sosial",
        "ppkn" to "pendidikan pancasila",
        "pkn" to "pendidikan pancasila",
        "pjok" to "pendidikan jasmani",
    )
    val resolvedItem = aliases[cleanItem] ?: cleanItem
    val resolvedFilter = aliases[cleanFilter] ?: cleanFilter

    if (resolvedItem == resolvedFilter) return true

    // 6. Meaningful substring matching (must be at least 4 chars long)
    if (resolvedItem.length >= 4 && resolvedFilter.length >= 4) {
        if (resolvedItem.contains(resolvedFilter, ignoreCase = true) ||
            resolvedFilter.contains(resolvedItem, ignoreCase = true)) {
            return true
        }
    }

    return false
}

/**
 * Checks if two subject representations belong to the same pedagogical domain.
 * Prevents false positives between unrelated subjects while bridging naming variations
 * (e.g. "IPS", "Ilmu Pengetahuan Sosial", "IPAS", "Sosial").
 */
private fun isSameSubjectDomain(c1: String, c2: String): Boolean {
    // Barrier: do not mix pure general "umum"
    if (c1.contains("umum") xor c2.contains("umum")) {
        // Exception: "matematika" vs "matematika umum" is fine
        val isBothMath = c1.contains("matematika") && c2.contains("matematika")
        if (!isBothMath) return false
    }

    // 1. Sosial / IPS / IPAS
    val is1Sosial = c1.contains("sosial") || c1.contains("ips") || c1.contains("ipas")
    val is2Sosial = c2.contains("sosial") || c2.contains("ips") || c2.contains("ipas")
    if (is1Sosial && is2Sosial) {
        return true
    }

    // 2. Alam / IPA / IPAS (only if neither is strictly IPS)
    val is1PureIps = (c1.contains("ips") || c1.contains("sosial")) && !c1.contains("alam") && !c1.contains("ipas")
    val is2PureIps = (c2.contains("ips") || c2.contains("sosial")) && !c2.contains("alam") && !c2.contains("ipas")
    if (!is1PureIps && !is2PureIps) {
        val is1Alam = c1.contains("alam") || c1.contains("ipa") || c1.contains("ipas")
        val is2Alam = c2.contains("alam") || c2.contains("ipa") || c2.contains("ipas")
        if (is1Alam && is2Alam) return true
    }

    // 3. Geografi
    if (c1.contains("geografi") && c2.contains("geografi")) return true

    // 4. Matematika
    val is1Math = c1.contains("matematika") || c1.contains("mtk")
    val is2Math = c2.contains("matematika") || c2.contains("mtk")
    if (is1Math && is2Math) return true

    // 5. Bahasa Indonesia
    if (c1.contains("indonesia") && c2.contains("indonesia")) return true

    // 6. Bahasa Inggris
    val is1Eng = c1.contains("inggris") || c1.contains("english")
    val is2Eng = c2.contains("inggris") || c2.contains("english")
    if (is1Eng && is2Eng) return true

    // 7. Bahasa Daerah / Cirebon
    if (c1.contains("cirebon") && c2.contains("cirebon")) return true

    // 8. Pendidikan Agama Islam / PAI
    val is1Pai = c1.contains("agama") || c1.contains("pai") || c1.contains("islam")
    val is2Pai = c2.contains("agama") || c2.contains("pai") || c2.contains("islam")
    if (is1Pai && is2Pai) return true

    // 9. Pendidikan Pancasila / PKn / PPKn
    val is1Pkn = c1.contains("pancasila") || c1.contains("ppkn") || c1.contains("pkn")
    val is2Pkn = c2.contains("pancasila") || c2.contains("ppkn") || c2.contains("pkn")
    if (is1Pkn && is2Pkn) return true

    // 10. PJOK / Jasmani / Olahraga
    val is1Pjok = c1.contains("jasmani") || c1.contains("pjok") || c1.contains("olahraga")
    val is2Pjok = c2.contains("jasmani") || c2.contains("pjok") || c2.contains("olahraga")
    if (is1Pjok && is2Pjok) return true

    // 11. Seni Budaya
    val is1Seni = c1.contains("seni") || c1.contains("budaya")
    val is2Seni = c2.contains("seni") || c2.contains("budaya")
    if (is1Seni && is2Seni) return true

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
 * Extracts section/parallel class letter (e.g., A in '12A', B in 'PAKET B 8B') from class name.
 */
fun extractSectionLetter(name: String): Char? {
    val match = Regex("""(?:\d+)\s*[-_]?\s*([A-Za-z])(?:\b|$)""").find(name)
    return match?.groupValues?.get(1)?.uppercase()?.firstOrNull()
}

/**
 * Strict class/rombel matching to prevent cross-level material leaks (e.g., Paket A SD materials
 * or Paket C SMA materials leaking into a Paket B SMP class session), and prevents cross-section
 * leaks between parallel classes (e.g. 12A materials appearing in 12B sessions).
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
        if (filterGrade != itemGrade) return false

        // Parallel section isolation: if both define a section (e.g. 12A vs 12B), they must match
        val filterSection = extractSectionLetter(f)
        val itemSection = extractSectionLetter(item)
        if (filterSection != null && itemSection != null && filterSection != itemSection) {
            return false
        }
        return true
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
    canClear: Boolean = false,
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
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            subject,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
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
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            className!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentNeonCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
            }

            if (canClear) {
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
}