package co.edu.univalle.notia.core.grades

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun item(percentage: Int, grade: String? = null) =
    GradedItem(percentage, grade?.let { BigDecimal(it) })

class FinalGradeTest {

    // --- evaluatedWeight / accumulatedGrade ---

    @Test
    fun evaluated_weight_counts_only_items_with_grade() {
        val items = listOf(item(25, "4.0"), item(15), item(30, "0.0"), item(30))
        assertEquals(55, evaluatedWeight(items))
    }

    @Test
    fun accumulated_grade_is_exact_and_unrounded() {
        val items = listOf(item(50, "3.5"), item(50, "2.4"))
        assertEquals(0, BigDecimal("2.95").compareTo(accumulatedGrade(items)))
    }

    // --- partialGrade (RN-04 / RF-15) ---

    @Test
    fun partial_with_single_evaluated_item_is_its_grade() {
        assertEquals(BigDecimal("3.5"), partialGrade(listOf(item(50, "3.5"), item(50))))
    }

    @Test
    fun partial_divides_by_evaluated_weight() {
        // (4.0*25 + 3.0*15) / 40 = 3.625 -> 3.6
        val items = listOf(item(25, "4.0"), item(15, "3.0"), item(30), item(30))
        assertEquals(BigDecimal("3.6"), partialGrade(items))
    }

    @Test
    fun partial_rounds_half_up_with_bigdecimal() {
        // (3.0*20 + 2.9*20) / 40 = 2.95 -> 3.0
        val items = listOf(item(20, "3.0"), item(20, "2.9"), item(60))
        assertEquals(BigDecimal("3.0"), partialGrade(items))
    }

    @Test
    fun partial_counts_zero_grade_as_evaluated() {
        assertEquals(BigDecimal("0.0"), partialGrade(listOf(item(50, "0.0"), item(50))))
    }

    @Test
    fun partial_is_null_without_grades() {
        assertNull(partialGrade(emptyList()))
        assertNull(partialGrade(listOf(item(50), item(50))))
    }

    @Test
    fun partial_equals_final_when_everything_is_evaluated() {
        val items = listOf(item(40, "3.0"), item(60, "2.9"))
        assertEquals(finalGrade(items), partialGrade(items))
    }

    // --- finalGrade (RN-04 / RF-15) ---

    @Test
    fun final_rounds_295_up_to_30() {
        val items = listOf(item(50, "3.5"), item(50, "2.4"))
        assertEquals(BigDecimal("3.0"), finalGrade(items))
    }

    @Test
    fun final_rounds_294_down_to_29() {
        val items = listOf(item(40, "3.0"), item(60, "2.9"))
        assertEquals(BigDecimal("2.9"), finalGrade(items))
    }

    @Test
    fun final_with_four_activities() {
        // 4.0*25 + 3.0*15 + 2.5*30 + 3.5*30 = 325 -> 3.25 -> 3.3
        val items = listOf(item(25, "4.0"), item(15, "3.0"), item(30, "2.5"), item(30, "3.5"))
        assertEquals(BigDecimal("3.3"), finalGrade(items))
    }

    @Test
    fun final_is_null_when_something_is_pending() {
        val items = listOf(item(50, "3.5"), item(50))
        assertNull(finalGrade(items))
    }

    @Test
    fun final_is_null_without_grades() {
        assertNull(finalGrade(emptyList()))
        assertNull(finalGrade(listOf(item(100))))
    }

    @Test
    fun final_is_null_when_weights_do_not_sum_100_even_if_all_graded() {
        assertNull(finalGrade(listOf(item(50, "3.0"), item(40, "4.0"))))
        assertNull(finalGrade(listOf(item(60, "3.0"), item(60, "4.0"))))
    }
}
