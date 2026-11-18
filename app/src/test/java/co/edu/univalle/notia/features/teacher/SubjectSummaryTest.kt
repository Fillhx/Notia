package co.edu.univalle.notia.features.teacher

import org.junit.Assert.assertEquals
import org.junit.Test

class SubjectSummaryTest {

    private fun activity(id: String, pct: Int, status: ActivityStatus) = GradeActivity(id, "Act $id", pct, status)

    private val programacionMovil = listOf(
        GradeActivity("p1", "Parcial 1", 25, ActivityStatus.PUBLISHED),
        GradeActivity("taller", "Taller", 15, ActivityStatus.PUBLISHED),
        GradeActivity("p2", "Parcial 2", 30, ActivityStatus.DRAFT),
        GradeActivity("final", "Examen final", 30, ActivityStatus.DRAFT),
    )

    @Test
    fun `ejemplo del diseno - Parcial 2 en borrador con notas`() {
        val pending = subjectPending(programacionMovil, mapOf("p1" to 10, "taller" to 10, "p2" to 3))
        assertEquals(SubjectPending.DraftWithGrades("Parcial 2"), pending)
        assertEquals(40, publishedPercent(programacionMovil))
    }

    @Test
    fun `esquema incompleto tiene prioridad`() {
        val web = listOf(
            activity("a", 30, ActivityStatus.DRAFT),
            activity("b", 25, ActivityStatus.DRAFT),
            activity("c", 30, ActivityStatus.DRAFT),
        )
        assertEquals(SubjectPending.IncompleteScheme(85), subjectPending(web, emptyMap()))
        assertEquals(SubjectPending.IncompleteScheme(0), subjectPending(emptyList(), emptyMap()))
    }

    @Test
    fun `borrador sin notas cuando no hay borradores con notas`() {
        val pending = subjectPending(programacionMovil, mapOf("p1" to 10, "taller" to 10))
        assertEquals(SubjectPending.NoGrades("Parcial 2"), pending)
    }

    @Test
    fun `todo publicado`() {
        val all = listOf(activity("a", 60, ActivityStatus.PUBLISHED), activity("b", 40, ActivityStatus.PUBLISHED))
        assertEquals(SubjectPending.AllPublished, subjectPending(all, mapOf("a" to 5, "b" to 5)))
        assertEquals(100, publishedPercent(all))
    }

    @Test
    fun `texto de la tarjeta`() {
        assertEquals("40 % publicado · 4 actividades", subjectMetaText(40, 4))
        assertEquals("Sin notas publicadas · 1 actividad", subjectMetaText(0, 1))
    }

    @Test
    fun `iniciales de la organizacion`() {
        assertEquals("CP", organizationInitials("Curso de Programación"))
        assertEquals("UT", organizationInitials("Univalle · Tuluá".replace("·", "")))
        assertEquals("AM", organizationInitials("Academia Matemática"))
    }
}
