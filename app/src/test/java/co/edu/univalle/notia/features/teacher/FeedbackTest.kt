package co.edu.univalle.notia.features.teacher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackTest {

    @Test
    fun `quita espacios sobrantes`() {
        assertEquals("Buen trabajo", normalizeFeedback("  Buen trabajo \n"))
    }

    @Test
    fun `vacio o solo espacios significa quitar el comentario`() {
        assertNull(normalizeFeedback(""))
        assertNull(normalizeFeedback("   \n "))
    }

    @Test
    fun `se corta en 500 caracteres`() {
        assertEquals(FEEDBACK_MAX_LENGTH, normalizeFeedback("a".repeat(600))!!.length)
    }

    @Test
    fun `aviso de visibilidad segun el estado de la actividad`() {
        val draft = feedbackVisibilityText("Ana Castaño", "Parcial 2", ActivityStatus.DRAFT)
        assertTrue(draft.startsWith("La nota está en borrador: Ana verá"))
        assertTrue(draft.endsWith("cuando publiques Parcial 2."))

        val published = feedbackVisibilityText("Ana Castaño", "Parcial 1", ActivityStatus.PUBLISHED)
        assertTrue(published.contains("de inmediato"))
        assertTrue(published.contains("notificación"))
    }
}
