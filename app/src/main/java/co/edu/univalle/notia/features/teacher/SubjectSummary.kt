package co.edu.univalle.notia.features.teacher

/** Lo pendiente de una materia en "Mis materias" del docente, en orden de prioridad. */
sealed interface SubjectPending {
    /** Sin actividades o porcentajes que no suman 100 % (RN-03): hay que completar el esquema. */
    data class IncompleteScheme(val total: Int) : SubjectPending

    /** Actividad en borrador que ya tiene notas: falta publicarla. */
    data class DraftWithGrades(val activityName: String) : SubjectPending

    /** Actividad en borrador sin ninguna nota todavía. */
    data class NoGrades(val activityName: String) : SubjectPending

    data object AllPublished : SubjectPending
}

fun subjectPending(activities: List<GradeActivity>, gradeCounts: Map<String, Int>): SubjectPending {
    val total = activities.sumOf { it.percentage }
    if (total != 100) return SubjectPending.IncompleteScheme(total)
    val drafts = activities.filter { it.status == ActivityStatus.DRAFT }
    drafts.firstOrNull { (gradeCounts[it.id] ?: 0) > 0 }?.let { return SubjectPending.DraftWithGrades(it.name) }
    drafts.firstOrNull()?.let { return SubjectPending.NoGrades(it.name) }
    return SubjectPending.AllPublished
}

/** Porcentaje del esquema que ya ven los estudiantes (suma de actividades publicadas). */
fun publishedPercent(activities: List<GradeActivity>): Int =
    activities.filter { it.status == ActivityStatus.PUBLISHED }.sumOf { it.percentage }.coerceAtMost(100)

/** "40 % publicado · 4 actividades", "Sin notas publicadas · 1 actividad". */
fun subjectMetaText(publishedPercent: Int, activityCount: Int): String {
    val published = if (publishedPercent == 0) "Sin notas publicadas" else "$publishedPercent % publicado"
    val count = if (activityCount == 1) "1 actividad" else "$activityCount actividades"
    return "$published · $count"
}

private val NAME_STOPWORDS = setOf("de", "del", "la", "las", "el", "los", "y")

/** Iniciales de la organización para su insignia: "Curso de Programación" → "CP". */
fun organizationInitials(name: String): String =
    name.split(" ")
        .filter { it.isNotBlank() && it.lowercase() !in NAME_STOPWORDS }
        .take(2)
        .joinToString("") { it.first().uppercase() }
