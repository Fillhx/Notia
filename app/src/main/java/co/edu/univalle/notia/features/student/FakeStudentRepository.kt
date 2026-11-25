package co.edu.univalle.notia.features.student

import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Implementación en memoria con datos de prueba; filtra borradores igual que lo hará Firestore. */
class FakeStudentRepository(
    private val profile: StudentProfile = FakeStudentData.profile,
    rawSubjects: List<FakeSubject> = FakeStudentData.subjects,
    initialNotices: List<StudentNotice> = FakeStudentData.notices,
) : StudentRepository {

    private val subjects: List<StudentSubjectGrades> = rawSubjects.map { raw ->
        StudentSubjectGrades.fromPublished(
            subject = raw.subject,
            published = raw.activities.mapNotNull { it.toStudentActivityOrNull() },
        )
    }

    private val notices = MutableStateFlow(initialNotices)

    override suspend fun getProfile(): StudentProfile = profile

    override suspend fun getSubjects(): List<StudentSubjectGrades> = subjects

    override suspend fun getSubject(subjectId: String): StudentSubjectGrades? =
        subjects.firstOrNull { it.subject.id == subjectId }

    override fun observeNotices(): Flow<List<StudentNotice>> =
        notices.map { list -> list.sortedByDescending { it.createdAt } }

    override suspend fun markNoticeRead(noticeId: String) {
        notices.update { list -> list.map { if (it.id == noticeId) it.copy(isRead = true) else it } }
    }

    override suspend fun markAllNoticesRead() {
        notices.update { list -> list.map { it.copy(isRead = true) } }
    }
}

/** Actividad tal como viene de la capa de datos (con `estado` en texto). */
data class FakeActivity(
    val id: String,
    val name: String,
    val percentage: Int,
    val estado: String,
    val grade: String? = null,
    val feedback: String? = null,
    val updatedAt: String? = null,
)

data class FakeSubject(val subject: StudentSubject, val activities: List<FakeActivity>)

private fun FakeActivity.toStudentActivityOrNull(): StudentActivity? {
    if (PublicationState.fromRaw(estado) != PublicationState.PUBLISHED) return null
    return StudentActivity(
        id = id,
        name = name,
        percentage = percentage,
        grade = grade?.let { BigDecimal(it) },
        feedback = feedback,
        updatedAt = updatedAt?.let { Instant.parse(it) },
    )
}

/** Datos de prueba: cubren En curso, Aprobado, Reprobado, Sin notas y nota necesaria fuera de alcance. */
object FakeStudentData {

    val profile = StudentProfile(
        id = "est-001",
        name = "Laura Martínez",
        code = "2310452",
        email = "laura.martinez@correounivalle.edu.co",
        program = "Ingeniería de Sistemas",
    )

    val subjects: List<FakeSubject> = listOf(
        // En curso: un borrador oculto (20 %) y un parcial publicado sin calificar.
        FakeSubject(
            StudentSubject("mat-calc1", "Cálculo Diferencial", "111045M", "Prof. Andrés Gómez"),
            listOf(
                FakeActivity(
                    "calc-p1", "Parcial 1", 25, "publicada", "3.5",
                    "Buen manejo de límites; revisa la regla de la cadena en el punto 3.", "2026-10-02T14:00:00Z",
                ),
                FakeActivity("calc-q1", "Quiz 1", 15, "publicada", "4.0", null, "2026-10-09T14:00:00Z"),
                FakeActivity(
                    "calc-t1", "Taller 1", 10, "publicada", "2.8",
                    "Faltó justificar los pasos de las derivadas implícitas.", "2026-10-16T14:00:00Z",
                ),
                FakeActivity("calc-p2", "Parcial 2", 30, "publicada"),
                FakeActivity("calc-pf", "Examen final", 20, "borrador", "4.5", "Borrador interno del docente."),
            ),
        ),
        // Aprobado: 100 % publicado y calificado (definitiva 4.0).
        FakeSubject(
            StudentSubject("mat-ed", "Estructuras de Datos", "750016C", "Prof. Marcela Rojas"),
            listOf(
                FakeActivity(
                    "ed-proy", "Proyecto", 30, "publicada", "4.5",
                    "Muy buena documentación y pruebas.", "2026-11-10T16:00:00Z",
                ),
                FakeActivity("ed-quices", "Quices", 20, "publicada", "3.8", null, "2026-10-20T16:00:00Z"),
                FakeActivity("ed-p1", "Parcial 1", 25, "publicada", "3.5", null, "2026-09-25T16:00:00Z"),
                FakeActivity("ed-p2", "Parcial 2", 25, "publicada", "4.2", null, "2026-11-05T16:00:00Z"),
            ),
        ),
        // Reprobado: 100 % publicado y calificado (definitiva 2.5).
        FakeSubject(
            StudentSubject("mat-fis1", "Física Mecánica", "130002M", "Prof. Jorge Ibarra"),
            listOf(
                FakeActivity(
                    "fis-p1", "Parcial 1", 30, "publicada", "2.0",
                    "Revisa los diagramas de cuerpo libre.", "2026-09-30T15:00:00Z",
                ),
                FakeActivity("fis-p2", "Parcial 2", 30, "publicada", "2.5", null, "2026-11-12T15:00:00Z"),
                FakeActivity("fis-lab", "Laboratorios", 20, "publicada", "3.5", null, "2026-11-01T15:00:00Z"),
                FakeActivity(
                    "fis-final", "Examen final", 20, "publicada", "2.4",
                    "Errores de unidades en los problemas 2 y 4.", "2026-11-18T15:00:00Z",
                ),
            ),
        ),
        // Sin notas: dos actividades publicadas sin calificar y un parcial en borrador (40 % oculto).
        FakeSubject(
            StudentSubject("mat-bd", "Bases de Datos", "750020C", "Prof. Sandra Cifuentes"),
            listOf(
                FakeActivity("bd-t1", "Taller 1", 20, "publicada"),
                FakeActivity("bd-proy", "Proyecto", 40, "publicada"),
                FakeActivity("bd-parcial", "Parcial", 40, "borrador"),
            ),
        ),
        // En curso con nota necesaria fuera de alcance: (3.0 − 0.45) ÷ 0.50 = 5.1.
        FakeSubject(
            StudentSubject("mat-alg", "Álgebra Lineal", "111050M", "Prof. Camilo Paz"),
            listOf(
                FakeActivity(
                    "alg-p1", "Parcial 1", 50, "publicada", "0.9",
                    "Se recomienda asistir a monitoría de espacios vectoriales.", "2026-11-19T13:00:00Z",
                ),
                FakeActivity("alg-p2", "Parcial 2", 50, "publicada"),
            ),
        ),
    )

    val notices: List<StudentNotice> = listOf(
        StudentNotice(
            id = "aviso-1", subjectId = "mat-alg", subjectName = "Álgebra Lineal",
            activityId = "alg-p1", activityName = "Parcial 1",
            type = NoticeType.GRADE_PUBLISHED, createdAt = Instant.parse("2026-11-19T13:05:00Z"),
        ),
        StudentNotice(
            id = "aviso-2", subjectId = "mat-fis1", subjectName = "Física Mecánica",
            activityId = "fis-p2", activityName = "Parcial 2",
            type = NoticeType.GRADE_EDITED, createdAt = Instant.parse("2026-11-12T15:10:00Z"),
        ),
        StudentNotice(
            id = "aviso-3", subjectId = "mat-calc1", subjectName = "Cálculo Diferencial",
            activityId = "calc-t1", activityName = "Taller 1",
            type = NoticeType.GRADE_PUBLISHED, createdAt = Instant.parse("2026-10-16T14:05:00Z"),
        ),
        StudentNotice(
            id = "aviso-4", subjectId = "mat-ed", subjectName = "Estructuras de Datos",
            activityId = "ed-proy", activityName = "Proyecto",
            type = NoticeType.GRADE_PUBLISHED, createdAt = Instant.parse("2026-11-10T16:05:00Z"),
            isRead = true,
        ),
    )
}
