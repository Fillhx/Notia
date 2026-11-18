package co.edu.univalle.notia.features.teacher

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId

enum class ActivityStatus { DRAFT, PUBLISHED }

data class TeacherSubject(
    val id: String,
    val name: String,
    val organizationName: String,
    val periodName: String,
)

/** Actividad evaluativa del esquema de una materia (RF-10). */
data class GradeActivity(
    val id: String,
    val name: String,
    val percentage: Int,
    val status: ActivityStatus,
)

data class EnrolledStudent(
    val id: String,
    val name: String,
    val code: String,
) {
    val initials: String
        get() = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
}

/** Actividad tal como queda al guardar el esquema (RF-10). `id = null` es una actividad nueva. */
data class SchemeEntry(
    val id: String?,
    val name: String,
    val percentage: Int,
)

/** Entrada del historial de cambios de notas publicadas (RF-19, RN-08). */
data class GradeChange(
    val activityId: String,
    val studentId: String,
    val oldValue: BigDecimal?,
    val newValue: BigDecimal?,
    val author: String,
    val timestamp: Long,
)

/**
 * Datos de ejemplo en memoria mientras no esté Firebase.
 * Cuando el paquete A conecte Firestore, este objeto se reemplaza por uno con las mismas funciones.
 */
object FakeTeacherRepository {
    const val DEMO_SUBJECT_ID = "prog-movil"
    const val CURRENT_TEACHER = "Laura Gómez"

    private val subjects = mapOf(
        DEMO_SUBJECT_ID to TeacherSubject(DEMO_SUBJECT_ID, "Programación Móvil", "Curso de Programación", "2026-II")
    )

    private val students = listOf(
        EnrolledStudent("s1", "Ana Castaño", "2380412"),
        EnrolledStudent("s2", "Brayan Mejía", "2380377"),
        EnrolledStudent("s3", "Camila Rojas", "2380590"),
        EnrolledStudent("s4", "David López", "2380121"),
        EnrolledStudent("s5", "Esteban Vargas", "2380266"),
        EnrolledStudent("s6", "Fernanda Ortiz", "2380455"),
        EnrolledStudent("s7", "Gabriel Torres", "2380319"),
        EnrolledStudent("s8", "Isabela Muñoz", "2380502"),
        EnrolledStudent("s9", "Juan Pablo Ríos", "2380188"),
        EnrolledStudent("s10", "Laura Cardona", "2380634"),
    )

    private val activities = mutableListOf(
        GradeActivity("p1", "Parcial 1", 25, ActivityStatus.PUBLISHED),
        GradeActivity("taller", "Taller", 15, ActivityStatus.PUBLISHED),
        GradeActivity("p2", "Parcial 2", 30, ActivityStatus.DRAFT),
        GradeActivity("final", "Examen final", 30, ActivityStatus.DRAFT),
    )

    /** grades[activityId][studentId] */
    private val grades: MutableMap<String, MutableMap<String, BigDecimal>> = mutableMapOf(
        // Valores actuales; los anteriores están en el historial de ejemplo (seedHistory).
        "p1" to seed("4.0", "3.6", "2.8", "4.6", "3.1", "3.9", "2.4", "4.2", "3.3", "4.8").apply { remove("s5") },
        "taller" to seed("4.2", "3.6", "3.4", "3.8", "2.7", "4.4", "3.2", "4.0", "3.8", "4.5"),
        "p2" to mutableMapOf("s1" to BigDecimal("4.5"), "s2" to BigDecimal("3.2"), "s5" to BigDecimal("2.8")),
        "final" to mutableMapOf(),
    )

    /** feedback[activityId][studentId] (RF-13) */
    private val feedback: MutableMap<String, MutableMap<String, String>> = mutableMapOf(
        "p2" to mutableMapOf(
            "s1" to "Buen manejo de corrutinas y del estado de la pantalla. Te faltó mostrar un mensaje cuando no hay conexión."
        ),
    )

    private val history = seedHistory().toMutableList()
    private var nextActivityNumber = 1

    /** Cambios de ejemplo de hoy y de ayer sobre notas publicadas (RF-19). */
    private fun seedHistory(): List<GradeChange> {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val yesterday = now.toLocalDate().minusDays(1)
        fun at(time: LocalDateTime) = time.atZone(zone).toInstant().toEpochMilli()
        fun grade(value: String?) = value?.let(::BigDecimal)
        return listOf(
            GradeChange("p1", "s1", grade("3.5"), grade("4.0"), CURRENT_TEACHER, at(now.minusMinutes(25))),
            GradeChange("p1", "s2", grade("4.0"), grade("3.6"), CURRENT_TEACHER, at(now.minusMinutes(27))),
            GradeChange("taller", "s4", null, grade("3.8"), CURRENT_TEACHER, at(now.minusHours(2))),
            GradeChange("taller", "s3", grade("3.0"), grade("3.4"), CURRENT_TEACHER, at(yesterday.atTime(16, 5))),
            GradeChange("p1", "s5", grade("3.1"), null, CURRENT_TEACHER, at(yesterday.atTime(15, 58))),
        )
    }

    private fun seed(vararg values: String): MutableMap<String, BigDecimal> =
        students.zip(values).associate { (student, value) -> student.id to BigDecimal(value) }.toMutableMap()

    fun subject(subjectId: String): TeacherSubject = subjects.getValue(subjectId)

    fun students(subjectId: String): List<EnrolledStudent> = students

    fun activities(subjectId: String): List<GradeActivity> = activities.toList()

    fun grades(subjectId: String, activityId: String): Map<String, BigDecimal> =
        grades[activityId].orEmpty().toMap()

    fun history(subjectId: String): List<GradeChange> = history.toList()

    fun feedback(subjectId: String, activityId: String): Map<String, String> =
        feedback[activityId].orEmpty().toMap()

    /** Guarda o quita (`text = null`) la retroalimentación de un estudiante en una actividad (RF-13). */
    fun saveFeedback(subjectId: String, activityId: String, studentId: String, text: String?) {
        val target = feedback.getOrPut(activityId) { mutableMapOf() }
        if (text == null) target.remove(studentId) else target[studentId] = text
    }

    fun gradeCount(subjectId: String, activityId: String): Int = grades[activityId]?.size ?: 0

    /**
     * Reemplaza el esquema de la materia (RF-10). Las actividades nuevas quedan en borrador.
     * RN-03: los porcentajes deben sumar 100. No se borra una actividad que tenga notas.
     */
    fun saveScheme(subjectId: String, entries: List<SchemeEntry>) {
        require(entries.sumOf { it.percentage } == 100) { "RN-03: el esquema debe sumar 100 %" }
        val keptIds = entries.mapNotNull { it.id }.toSet()
        activities.filter { it.id !in keptIds }.forEach { removed ->
            check(gradeCount(subjectId, removed.id) == 0) { "${removed.name} tiene notas y no se puede eliminar" }
            grades.remove(removed.id)
            feedback.remove(removed.id)
        }
        val updated = entries.map { entry ->
            if (entry.id == null) {
                GradeActivity("nueva-${nextActivityNumber++}", entry.name.trim(), entry.percentage, ActivityStatus.DRAFT)
            } else {
                activity(entry.id).copy(name = entry.name.trim(), percentage = entry.percentage)
            }
        }
        activities.clear()
        activities.addAll(updated)
    }

    /** Guarda notas en borrador; `null` borra la nota de ese estudiante. */
    fun saveDraft(subjectId: String, activityId: String, values: Map<String, BigDecimal?>) {
        check(activity(activityId).status == ActivityStatus.DRAFT) { "La actividad ya está publicada" }
        applyGrades(activityId, values)
    }

    fun publish(subjectId: String, activityId: String) {
        val index = activities.indexOfFirst { it.id == activityId }
        activities[index] = activities[index].copy(status = ActivityStatus.PUBLISHED)
    }

    /** Edita notas ya publicadas y deja cada cambio en el historial (RF-11, RN-08). */
    fun updatePublished(subjectId: String, activityId: String, values: Map<String, BigDecimal?>, author: String) {
        val current = grades[activityId].orEmpty()
        val now = System.currentTimeMillis()
        values.forEach { (studentId, newValue) ->
            val oldValue = current[studentId]
            if (!sameGrade(oldValue, newValue)) {
                history += GradeChange(activityId, studentId, oldValue, newValue, author, now)
            }
        }
        applyGrades(activityId, values)
    }

    private fun activity(activityId: String) = activities.first { it.id == activityId }

    /** 4.5 y 4.50 son la misma nota; por eso compareTo y no equals. */
    private fun sameGrade(a: BigDecimal?, b: BigDecimal?): Boolean =
        if (a == null || b == null) a == b else a.compareTo(b) == 0

    private fun applyGrades(activityId: String, values: Map<String, BigDecimal?>) {
        val target = grades.getOrPut(activityId) { mutableMapOf() }
        values.forEach { (studentId, value) ->
            if (value == null) target.remove(studentId) else target[studentId] = value
        }
    }
}
