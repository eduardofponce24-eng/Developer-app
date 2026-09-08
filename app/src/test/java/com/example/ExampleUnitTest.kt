package com.example

import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun expense_category_matching() {
        val cat = ExpenseCategory.fromId("comida")
        assertEquals(ExpenseCategory.FOOD, cat)

        val unknown = ExpenseCategory.fromId("desconocido")
        assertEquals(ExpenseCategory.OTHER, unknown)
    }

    @Test
    fun expense_calculation_top_expenses() {
        val expenses = listOf(
            Expense(id = 1, title = "Café", amount = 3.50, category = "comida"),
            Expense(id = 2, title = "Alquiler", amount = 600.00, category = "hogar"),
            Expense(id = 3, title = "Supermercado", amount = 120.00, category = "comida"),
            Expense(id = 4, title = "Transporte", amount = 25.00, category = "transporte")
        )

        val total = expenses.sumOf { it.amount }
        assertEquals(748.50, total, 0.001)

        val topExpenses = expenses.sortedByDescending { it.amount }
        assertEquals("Alquiler", topExpenses[0].title)
        assertEquals(600.00, topExpenses[0].amount, 0.001)
        assertEquals("Supermercado", topExpenses[1].title)

        val foodTotal = expenses.filter { it.category == "comida" }.sumOf { it.amount }
        assertEquals(123.50, foodTotal, 0.001)
        val foodPercentage = (foodTotal / total).toFloat()
        assertTrue(foodPercentage > 0.16f && foodPercentage < 0.17f)
    }
}
