package co.edu.univalle.notia.features.student

import co.edu.univalle.notia.core.grades.RequiredGrade
import co.edu.univalle.notia.core.grades.SubjectStatus
import co.edu.univalle.notia.features.organizations.Roles
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.math.BigDecimal
import java.time.Instant
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val UID = "u1"

private fun doc(id: String, vararg fields: Pair<String, Any?>) = RawDoc(id, mapOf(*fields))

private fun activity(id: String, percentage: Any?, estado: String?, name: String = id) =
    doc(id, "nombre" to name, "porcentaje" to percentage, "estado" to estado)

private fun grade(
    activityId: String,
    nota: Any?,
    student: String = UID,
    estado: String? = "publicada",
    feedback: String? = null,
) = doc(
    "g-$activityId-$student",
    "studentId" to student,
    "activityId" to activityId,
    "estado" to estado,
    "nota" to nota,
    "retroalimentacion" to feedback,
)

private val subjectDoc = doc("s1", "nombre" to "Cálculo", "codigo" to "111045M", "docenteNombre" to "Prof. Gómez")

class FirestoreStudentMappingTest {

    // --- parseGrade ---

    @Test
    fun grade_parses_numbers_and_strings_to_one_decimal() {
        assertEquals(BigDecimal("4.0"), parseGrade(4L))
        assertEquals(BigDecimal("3.5"), parseGrade(3.5))
        assertEquals(BigDecimal("3.5"), parseGrade("3,5"))
        assertEquals(BigDecimal("0.0"), parseGrade(0))
    }

    @Test
    fun grade_rounds_half_up_without_double_error() {
        assertEquals(BigDecimal("3.0"), parseGrade(2.95))
        assertEquals(BigDecimal("2.9"), parseGrade(2.94))
    }

    @Test
    fun grade_is_null_when_invalid_or_out_of_range() {
        assertNull(parseGrade(null))
        assertNull(parseGrade("abc"))
        assertNull(parseGrade(Double.NaN))
        assertNull(parseGrade(5.1))
        assertNull(parseGrade(-0.1))
    }

    // --- parsePercentage ---

    @Test
    fun percentage_accepts_whole_numbers_in_range() {
        assertEquals(25, parsePercentage(25L))
        assertEquals(25, parsePercentage(25.0))
        assertEquals(30, parsePercentage("30"))
        assertEquals(1, parsePercentage(1))
        assertEquals(100, parsePercentage(100))
    }

    @Test
    fun percentage_rejects_invalid_values() {
        assertNull(parsePercentage(null))
        assertNull(parsePercentage(0))
        assertNull(parsePercentage(101))
        assertNull(parsePercentage(25.5))
        assertNull(parsePercentage("abc"))
    }

    // --- buildSubjectGrades: borradores y notas ajenas ---

    @Test
    fun drafts_are_filtered_and_their_weight_is_hidden() {
        val activities = listOf(
            activity("a1", 25L, "publicada", "Parcial 1"),
            activity("a2", 30L, "publicada", "Parcial 2"),
            activity("a3", 45L, "borrador", "Final"),
        )
        val grades = listOf(
            grade("a1", 3.5, feedback = "Bien"),
            grade("a3", 4.5), // nota de una actividad en borrador: no debe verse
            grade("a2", 5.0, student = "u2"), // nota de otro estudiante
        )

        val result = buildSubjectGrades(subjectDoc, activities, grades, UID)

        assertEquals(listOf("a1", "a2"), result.activities.map { it.id })
        assertEquals(BigDecimal("3.5"), result.activities[0].grade)
        assertEquals("Bien", result.activities[0].feedback)
        assertNull(result.activities[1].grade)
        assertEquals(45, result.hiddenWeight)
        assertEquals(BigDecimal("3.5"), result.partial)
        assertNull(result.definitive)
        assertEquals(SubjectStatus.IN_PROGRESS, result.status)
        assertEquals(listOf("a2"), result.simulableActivities.map { it.id })
    }

    @Test
    fun unknown_estado_is_treated_as_draft() {
        val activities = listOf(activity("a1", 50L, "PUBLISHED"), activity("a2", 50L, null))
        val result = buildSubjectGrades(subjectDoc, activities, emptyList(), UID)
        assertTrue(result.activities.isEmpty())
        assertEquals(100, result.hiddenWeight)
    }

    @Test
    fun grade_that_is_not_published_is_ignored() {
        val activities = listOf(activity("a1", 100L, "publicada"))
        val grades = listOf(grade("a1", 4.0, estado = "borrador"))
        val result = buildSubjectGrades(subjectDoc, activities, grades, UID)
        assertNull(result.activities.single().grade)
    }

    @Test
    fun activities_with_invalid_percentage_are_dropped() {
        val activities = listOf(
            activity("a1", 0L, "publicada"),
            activity("a2", 150L, "publicada"),
            activity("a3", "abc", "publicada"),
            activity("a4", null, "publicada"),
            activity("ok", 40L, "publicada"),
        )
        val result = buildSubjectGrades(subjectDoc, activities, emptyList(), UID)
        assertEquals(listOf("ok"), result.activities.map { it.id })
        assertEquals(60, result.hiddenWeight)
    }

    @Test
    fun out_of_range_grade_is_ignored_and_activity_stays_pending() {
        val activities = listOf(activity("a1", 100L, "publicada"))
        val result = buildSubjectGrades(subjectDoc, activities, listOf(grade("a1", 7.5)), UID)
        assertNull(result.activities.single().grade)
    }

    @Test
    fun fully_published_and_graded_subject_has_definitive_grade() {
        val activities = listOf(activity("a1", 50L, "publicada"), activity("a2", 50L, "publicada"))
        val grades = listOf(grade("a1", 3.5), grade("a2", 2.4))
        val result = buildSubjectGrades(subjectDoc, activities, grades, UID)
        assertEquals(0, result.hiddenWeight)
        assertEquals(BigDecimal("3.0"), result.definitive)
        assertEquals(SubjectStatus.PASSED, result.status)
        assertEquals(RequiredGrade.NoPending, result.requiredFor())
    }

    @Test
    fun empty_collections_give_empty_subject_without_errors() {
        val result = buildSubjectGrades(subjectDoc, emptyList(), emptyList(), UID)
        assertTrue(result.activities.isEmpty())
        assertEquals(100, result.hiddenWeight)
        assertEquals(SubjectStatus.NO_GRADES, result.status)
    }

    // --- sujeto e inscripción ---

    @Test
    fun subject_uses_safe_defaults_when_fields_are_missing() {
        val subject = mapSubject(doc("s9"))
        assertEquals("s9", subject.id)
        assertEquals("Materia sin nombre", subject.name)
        assertEquals("—", subject.code)
        assertEquals("Docente sin asignar", subject.teacherName)
    }

    @Test
    fun enrollment_requires_uid_in_student_ids() {
        assertTrue(isEnrolled(mapOf("studentIds" to listOf("u1", "u2")), "u1"))
        assertFalse(isEnrolled(mapOf("studentIds" to listOf("u2")), "u1"))
        assertFalse(isEnrolled(mapOf("studentIds" to null), "u1"))
        assertFalse(isEnrolled(emptyMap(), "u1"))
    }

    // --- avisos ---

    @Test
    fun notice_maps_valid_document() {
        val notice = mapNotice(
            doc(
                "n1",
                "studentId" to UID,
                "subjectId" to "s1",
                "subjectName" to "Cálculo",
                "activityId" to "a1",
                "activityName" to "Parcial 1",
                "type" to "GRADE_EDITED",
                "createdAt" to Date(1_700_000_000_000L),
                "read" to true,
            ),
            UID,
        )!!
        assertEquals(NoticeType.GRADE_EDITED, notice.type)
        assertEquals(Instant.ofEpochMilli(1_700_000_000_000L), notice.createdAt)
        assertTrue(notice.isRead)
        assertEquals("s1", notice.subjectId)
    }

    @Test
    fun notice_defaults_to_unread() {
        val notice = mapNotice(
            doc("n1", "studentId" to UID, "subjectId" to "s1", "type" to "publicada", "createdAt" to 1_000L),
            UID,
        )!!
        assertFalse(notice.isRead)
        assertEquals(NoticeType.GRADE_PUBLISHED, notice.type)
    }

    @Test
    fun notice_is_null_for_other_student_or_malformed_documents() {
        val base = arrayOf<Pair<String, Any?>>(
            "studentId" to UID, "subjectId" to "s1", "type" to "GRADE_PUBLISHED", "createdAt" to 1_000L,
        )
        assertNull(mapNotice(doc("n", *base), "otro"))
        assertNull(mapNotice(doc("n", *base, "type" to "otra-cosa"), UID))
        assertNull(mapNotice(doc("n", *base, "createdAt" to null), UID))
        assertNull(mapNotice(doc("n", *base, "subjectId" to null), UID))
    }

    // --- perfil ---

    @Test
    fun profile_prefers_user_document_and_falls_back_to_auth() {
        val full = buildProfile(
            uid = UID,
            authEmail = "auth@correo.co",
            authName = "Auth Name",
            userData = mapOf("nombre" to "Laura", "correo" to "laura@correo.co", "programa" to "Sistemas"),
            memberData = mapOf("codigo" to "2310452", "nombre" to "Otro"),
        )
        assertEquals("Laura", full.name)
        assertEquals("laura@correo.co", full.email)
        assertEquals("2310452", full.code)
        assertEquals("Sistemas", full.program)
        assertEquals(UID, full.id)

        val minimal = buildProfile(UID, "auth@correo.co", "Auth Name", null, null)
        assertEquals("Auth Name", minimal.name)
        assertEquals("auth@correo.co", minimal.email)
        assertEquals("", minimal.code)
        assertEquals("", minimal.program)
    }

    @Test
    fun profile_survives_missing_everything() {
        val profile = buildProfile(UID, null, null, emptyMap(), emptyMap())
        assertEquals("", profile.name)
        assertEquals("", profile.email)
    }

    // --- organización activa ---

    @Test
    fun missing_organization_gives_clear_error() {
        val error = runCatching { requireStudentOrgId(null, null) }.exceptionOrNull()
        assertTrue(error is StudentRepositoryException)
        assertEquals("Selecciona una organización para ver tus materias.", error?.message)
        assertTrue(runCatching { requireStudentOrgId("  ", Roles.STUDENT) }.isFailure)
    }

    @Test
    fun non_student_role_is_rejected() {
        val error = runCatching { requireStudentOrgId("org1", "Coordinación") }.exceptionOrNull()
        assertTrue(error is StudentRepositoryException)
    }

    @Test
    fun student_role_returns_org_id() {
        assertEquals("org1", requireStudentOrgId("org1", Roles.STUDENT))
    }

    // --- mensajes de error ---
    @Test
    fun errors_become_clear_spanish_messages() {
        assertEquals(
            "Ocurrió un error inesperado. Inténtalo de nuevo.",
            friendlyErrorMessage(IllegalStateException("x")),
        )
        assertEquals("Hola", friendlyErrorMessage(StudentRepositoryException("Hola")))
    }
}
