package co.edu.univalle.notia.features.student

import kotlinx.coroutines.flow.Flow

/**
 * Contrato de datos del estudiante. La UI solo depende de esta interfaz;
 * `FakeStudentRepository` hoy, `FirestoreStudentRepository` después (Commit 7).
 *
 * Reglas que toda implementación debe cumplir:
 * - Solo actividades con `estado == "publicada"`; los borradores nunca salen del repositorio (RN-06, RNF-05).
 * - Solo materias en las que el estudiante autenticado está inscrito y solo sus propias notas (RN-06, RNF-04).
 * - Construir cada materia con [StudentSubjectGrades.fromPublished] para que `hiddenWeight` sea consistente.
 */
interface StudentRepository {

    suspend fun getProfile(): StudentProfile

    /** Materias inscritas con sus actividades publicadas (RF-14, RF-15). */
    suspend fun getSubjects(): List<StudentSubjectGrades>

    /** Una materia por id, o null si no existe o el estudiante no está inscrito. */
    suspend fun getSubject(subjectId: String): StudentSubjectGrades?

    /** Avisos de notas publicadas o editadas, del más reciente al más antiguo (RF-18). */
    fun observeNotices(): Flow<List<StudentNotice>>

    suspend fun markNoticeRead(noticeId: String)

    suspend fun markAllNoticesRead()
}
