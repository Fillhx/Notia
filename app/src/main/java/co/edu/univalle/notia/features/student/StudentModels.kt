package co.edu.univalle.notia.features.student

import co.edu.univalle.notia.core.grades.GradedItem
import co.edu.univalle.notia.core.grades.PASSING_GRADE
import co.edu.univalle.notia.core.grades.RequiredGrade
import co.edu.univalle.notia.core.grades.SubjectStatus
import co.edu.univalle.notia.core.grades.classifySubject
import co.edu.univalle.notia.core.grades.evaluatedWeight
import co.edu.univalle.notia.core.grades.finalGrade
import co.edu.univalle.notia.core.grades.partialGrade
import co.edu.univalle.notia.core.grades.remainingWeight
import co.edu.univalle.notia.core.grades.requiredGrade
import java.math.BigDecimal
import java.time.Instant

/** Valor del campo `estado` de una actividad en la capa de datos. */
enum class PublicationState(val raw: String) {
    PUBLISHED("publicada"),
    DRAFT("borrador");

    companion object {
        /** Cualquier valor desconocido se trata como borrador: nada sin publicar llega al estudiante (RN-06, RNF-05). */
        fun fromRaw(raw: String?): PublicationState = values().firstOrNull { it.raw == raw } ?: DRAFT
    }
}

data class StudentProfile(
    val id: String,
    val name: String,
    val code: String,
    val email: String,
    val program: String,
)

data class StudentSubject(
    val id: String,
    val name: String,
    val code: String,
    val teacherName: String,
)

/**
 * Actividad visible para el estudiante: SIEMPRE publicada (los borradores nunca se modelan aquí).
 * [grade] null = publicada pero aún sin calificar (pendiente / simulable).
 */
data class StudentActivity(
    val id: String,
    val name: String,
    val percentage: Int,
    val grade: BigDecimal? = null,
    val feedback: String? = null,
    val updatedAt: Instant? = null,
)

/**
 * Materia con sus actividades publicadas y los cálculos de `core/grades/` (RF-14, RF-15, RF-16).
 *
 * [hiddenWeight] es el porcentaje que el estudiante no ve (borradores o aún sin asignar):
 * cuenta como pendiente en los cálculos, así la definitiva solo existe con el 100 % publicado y calificado.
 */
data class StudentSubjectGrades(
    val subject: StudentSubject,
    val activities: List<StudentActivity>,
    val hiddenWeight: Int = 0,
) {
    init {
        require(hiddenWeight >= 0) { "hiddenWeight negativo: $hiddenWeight" }
    }

    /** Entrada para `core/grades/`: actividades publicadas + un ítem pendiente por el peso oculto. */
    val gradingItems: List<GradedItem>
        get() = activities.map { GradedItem(it.percentage, it.grade) } +
            listOfNotNull(hiddenWeight.takeIf { it > 0 }?.let { GradedItem(it) })

    /** Definitiva parcial (RF-15), null si no hay notas. */
    val partial: BigDecimal? get() = partialGrade(gradingItems)

    /** Definitiva (RF-15), null mientras falte algo por publicar o calificar. */
    val definitive: BigDecimal? get() = finalGrade(gradingItems)

    /** Clasificación de RF-17 (Aprobado, Reprobado, En curso, Sin notas). */
    val status: SubjectStatus get() = classifySubject(gradingItems)

    /** Porcentaje ya calificado (0–100). */
    val evaluatedPercent: Int get() = evaluatedWeight(gradingItems)

    /** Actividades publicadas sin nota: las que el simulador (RF-16) permite rellenar. */
    val simulableActivities: List<StudentActivity> get() = activities.filter { it.grade == null }

    /** Nota necesaria en lo pendiente para llegar a [goal] (RN-05). */
    fun requiredFor(goal: BigDecimal = PASSING_GRADE): RequiredGrade = requiredGrade(gradingItems, goal)

    companion object {
        /** Construye la materia a partir de las actividades YA filtradas como publicadas. */
        fun fromPublished(subject: StudentSubject, published: List<StudentActivity>): StudentSubjectGrades =
            StudentSubjectGrades(
                subject = subject,
                activities = published,
                hiddenWeight = remainingWeight(published.map { it.percentage }).coerceAtLeast(0),
            )
    }
}

enum class NoticeType { GRADE_PUBLISHED, GRADE_EDITED }

/** Aviso de nota publicada o editada (RF-18). */
data class StudentNotice(
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val activityId: String,
    val activityName: String,
    val type: NoticeType,
    val createdAt: Instant,
    val isRead: Boolean = false,
)
