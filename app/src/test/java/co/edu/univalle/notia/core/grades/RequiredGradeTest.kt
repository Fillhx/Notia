package co.edu.univalle.notia.core.grades

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

private fun item(percentage: Int, grade: String? = null) =
    GradedItem(percentage, grade?.let { BigDecimal(it) })

class RequiredGradeTest {

    private fun needed(value: String) = RequiredGrade.Needed(BigDecimal(value))

    // --- Needed (RN-05) ---

    @Test
    fun needed_example_from_srs_rounds_up_to_21() {
        // acumulado 2.375 (70 % evaluado), meta 3.0, pendiente 30 % -> 2.083... -> 2.1
        val items = listOf(item(20, "4.0"), item(25, "3.5"), item(25, "2.8"), item(30))
        assertEquals(needed("2.1"), requiredGrade(items, BigDecimal("3.0")))
    }

    @Test
    fun needed_uses_ceiling_not_half_up() {
        // (300 - 198) / 50 = 2.04 -> 2.1 (con HALF_UP sería 2.0)
        val items = listOf(item(30, "4.0"), item(20, "3.9"), item(50))
        assertEquals(needed("2.1"), requiredGrade(items))
    }

    @Test
    fun needed_keeps_exact_value_without_bumping() {
        // (300 - 150) / 50 = 3.0 exacto
        val items = listOf(item(50, "3.0"), item(50))
        assertEquals(needed("3.0"), requiredGrade(items))
    }

    @Test
    fun needed_with_no_grades_equals_the_goal() {
        val items = listOf(item(25), item(25), item(25), item(25))
        assertEquals(needed("3.0"), requiredGrade(items))
    }

    @Test
    fun needed_respects_custom_goal() {
        val items = listOf(item(50, "4.0"), item(50))
        assertEquals(needed("4.0"), requiredGrade(items, BigDecimal("4.0")))
    }

    // --- Achieved ---

    @Test
    fun achieved_when_accumulated_already_exceeds_goal() {
        val items = listOf(item(70, "4.5"), item(30))
        assertEquals(RequiredGrade.Achieved, requiredGrade(items))
    }

    @Test
    fun achieved_when_accumulated_equals_goal() {
        val items = listOf(item(60, "5.0"), item(40))
        assertEquals(RequiredGrade.Achieved, requiredGrade(items))
    }

    // --- OutOfReach ---

    @Test
    fun out_of_reach_when_needed_is_far_above_5() {
        // (300 - 70) / 30 = 7.67
        val items = listOf(item(70, "1.0"), item(30))
        assertEquals(RequiredGrade.OutOfReach, requiredGrade(items))
    }

    @Test
    fun needed_exactly_5_is_still_reachable() {
        // (300 - 50) / 50 = 5.0
        val items = listOf(item(50, "1.0"), item(50))
        assertEquals(needed("5.0"), requiredGrade(items))
    }

    @Test
    fun out_of_reach_when_needed_is_just_above_5() {
        // (300 - 45) / 50 = 5.1
        val items = listOf(item(50, "0.9"), item(50))
        assertEquals(RequiredGrade.OutOfReach, requiredGrade(items))
    }

    @Test
    fun out_of_reach_when_ceiling_pushes_above_5() {
        // (300 - 48) / 50 = 5.04 -> 5.1
        val items = listOf(item(30, "1.0"), item(20, "0.9"), item(50))
        assertEquals(RequiredGrade.OutOfReach, requiredGrade(items))
    }

    // --- NoPending ---

    @Test
    fun no_pending_when_everything_is_graded() {
        val items = listOf(item(50, "3.0"), item(50, "2.0"))
        assertEquals(RequiredGrade.NoPending, requiredGrade(items))
    }

    @Test
    fun no_pending_with_empty_list() {
        assertEquals(RequiredGrade.NoPending, requiredGrade(emptyList()))
    }
}
