package com.schoolos.android.domain.model

data class AcademicClass(
    val id: String,
    val name: String,
    val gradeLevelId: String? = null,
    val homeroomTeacherId: String? = null,
)

data class AcademicSubject(
    val id: String,
    val name: String,
    val code: String? = null,
)

data class UnifiedAcademicClass(
    val primaryId: String,
    val displayName: String,
    val memberClassIds: List<String>,
    val memberClassNames: List<String>
)

fun List<AcademicClass>.groupParallelClasses(): List<UnifiedAcademicClass> {
    val regex = Regex("""^(.*?)\s*([A-Za-z])$""")
    val groups = linkedMapOf<String, MutableList<AcademicClass>>()
    val standalone = mutableListOf<AcademicClass>()

    for (cls in this) {
        val match = regex.find(cls.name.trim())
        if (match != null) {
            val baseName = match.groupValues[1].trim()
            groups.getOrPut(baseName) { mutableListOf() }.add(cls)
        } else {
            standalone.add(cls)
        }
    }

    val result = mutableListOf<UnifiedAcademicClass>()

    for ((baseName, members) in groups) {
        if (members.size > 1) {
            val sections = members.mapNotNull { m ->
                regex.find(m.name.trim())?.groupValues?.get(2)?.uppercase()
            }.distinct().sorted()
            val sectionLabel = sections.joinToString(" & ")
            val displayName = "$baseName (Kelas $sectionLabel)"
            result.add(
                UnifiedAcademicClass(
                    primaryId = members.first().id,
                    displayName = displayName,
                    memberClassIds = members.map { it.id },
                    memberClassNames = members.map { it.name }
                )
            )
        } else {
            val single = members.first()
            result.add(
                UnifiedAcademicClass(
                    primaryId = single.id,
                    displayName = single.name,
                    memberClassIds = listOf(single.id),
                    memberClassNames = listOf(single.name)
                )
            )
        }
    }

    for (cls in standalone) {
        result.add(
            UnifiedAcademicClass(
                primaryId = cls.id,
                displayName = cls.name,
                memberClassIds = listOf(cls.id),
                memberClassNames = listOf(cls.name)
            )
        )
    }

    return result
}

fun List<UnifiedAcademicClass>.findClass(nameOrId: String?): UnifiedAcademicClass? {
    if (nameOrId.isNullOrBlank()) return null
    return find { unified ->
        unified.displayName.equals(nameOrId, ignoreCase = true)
            || unified.primaryId == nameOrId
            || unified.memberClassIds.contains(nameOrId)
            || unified.memberClassNames.any { it.equals(nameOrId, ignoreCase = true) }
    }
}
