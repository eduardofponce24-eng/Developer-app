package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

enum class TimeFilter(val label: String) {
    THIS_MONTH("Este Mes"),
    THIS_WEEK("Esta Semana"),
    ALL("Todo")
}

data class CategoryShare(
    val category: ExpenseCategory,
    val totalAmount: Double,
    val percentage: Float,
    val count: Int,
    val sweepAngle: Float
)

data class ExpenseUiState(
    val allExpenses: List<Expense> = emptyList(),
    val filteredExpenses: List<Expense> = emptyList(),
    val totalAmount: Double = 0.0,
    val categoryShares: List<CategoryShare> = emptyList(),
    val topExpenses: List<Expense> = emptyList(),
    val selectedTimeFilter: TimeFilter = TimeFilter.THIS_MONTH,
    val selectedCategory: ExpenseCategory? = null,
    val searchQuery: String = "",
    val editingExpense: Expense? = null,
    val isAddEditDialogOpen: Boolean = false,
    val message: String? = null
)

private data class FilterState(
    val timeFilter: TimeFilter = TimeFilter.THIS_MONTH,
    val selectedCategory: ExpenseCategory? = null,
    val searchQuery: String = ""
)

private data class DialogState(
    val editingExpense: Expense? = null,
    val isAddEditDialogOpen: Boolean = false,
    val message: String? = null
)

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val _filterState = MutableStateFlow(FilterState())
    private val _dialogState = MutableStateFlow(DialogState())

    val uiState: StateFlow<ExpenseUiState> = combine(
        repository.allExpenses,
        _filterState,
        _dialogState
    ) { allExpenses, filterState, dialogState ->
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val timeFiltered = allExpenses.filter { expense ->
            when (filterState.timeFilter) {
                TimeFilter.THIS_MONTH -> {
                    calendar.timeInMillis = now
                    val currentMonth = calendar.get(Calendar.MONTH)
                    val currentYear = calendar.get(Calendar.YEAR)

                    calendar.timeInMillis = expense.timestamp
                    calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
                }
                TimeFilter.THIS_WEEK -> {
                    calendar.timeInMillis = now
                    val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
                    val currentYear = calendar.get(Calendar.YEAR)

                    calendar.timeInMillis = expense.timestamp
                    calendar.get(Calendar.WEEK_OF_YEAR) == currentWeek && calendar.get(Calendar.YEAR) == currentYear
                }
                TimeFilter.ALL -> true
            }
        }

        val totalAmount = timeFiltered.sumOf { it.amount }

        // Category breakdown for the wheel chart
        val categoryGroups = timeFiltered.groupBy { ExpenseCategory.fromId(it.category) }
        val categoryShares = categoryGroups.map { (cat, expenses) ->
            val sum = expenses.sumOf { it.amount }
            val pct = if (totalAmount > 0) (sum / totalAmount).toFloat() else 0f
            CategoryShare(
                category = cat,
                totalAmount = sum,
                percentage = pct,
                count = expenses.size,
                sweepAngle = pct * 360f
            )
        }.sortedByDescending { it.totalAmount }

        // Mayores gastos (Top expenses sorted by amount descending)
        val topExpenses = timeFiltered.sortedByDescending { it.amount }.take(5)

        // Further filter by selectedCategory and search query for list view
        val finalFiltered = timeFiltered.filter { expense ->
            val matchesCategory = filterState.selectedCategory == null ||
                ExpenseCategory.fromId(expense.category) == filterState.selectedCategory
            val matchesSearch = filterState.searchQuery.isBlank() ||
                expense.title.contains(filterState.searchQuery, ignoreCase = true) ||
                expense.note.contains(filterState.searchQuery, ignoreCase = true) ||
                ExpenseCategory.fromId(expense.category).displayName.contains(filterState.searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }

        ExpenseUiState(
            allExpenses = allExpenses,
            filteredExpenses = finalFiltered,
            totalAmount = totalAmount,
            categoryShares = categoryShares,
            topExpenses = topExpenses,
            selectedTimeFilter = filterState.timeFilter,
            selectedCategory = filterState.selectedCategory,
            searchQuery = filterState.searchQuery,
            editingExpense = dialogState.editingExpense,
            isAddEditDialogOpen = dialogState.isAddEditDialogOpen,
            message = dialogState.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpenseUiState()
    )

    fun setTimeFilter(filter: TimeFilter) {
        _filterState.update { it.copy(timeFilter = filter) }
    }

    fun selectCategory(category: ExpenseCategory?) {
        _filterState.update {
            it.copy(selectedCategory = if (it.selectedCategory == category) null else category)
        }
    }

    fun setSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun openAddExpenseDialog() {
        _dialogState.update {
            it.copy(editingExpense = null, isAddEditDialogOpen = true)
        }
    }

    fun openEditExpenseDialog(expense: Expense) {
        _dialogState.update {
            it.copy(editingExpense = expense, isAddEditDialogOpen = true)
        }
    }

    fun closeAddEditDialog() {
        _dialogState.update {
            it.copy(editingExpense = null, isAddEditDialogOpen = false)
        }
    }

    fun saveExpense(
        id: Long = 0,
        title: String,
        amount: Double,
        category: ExpenseCategory,
        timestamp: Long,
        note: String
    ) {
        viewModelScope.launch {
            val expense = Expense(
                id = id,
                title = title.trim(),
                amount = amount,
                category = category.id,
                timestamp = timestamp,
                note = note.trim()
            )
            if (id == 0L) {
                repository.insertExpense(expense)
                _dialogState.update {
                    it.copy(
                        editingExpense = null,
                        isAddEditDialogOpen = false,
                        message = "Gasto registrado con éxito"
                    )
                }
            } else {
                repository.updateExpense(expense)
                _dialogState.update {
                    it.copy(
                        editingExpense = null,
                        isAddEditDialogOpen = false,
                        message = "Gasto actualizado"
                    )
                }
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _dialogState.update { it.copy(message = "Gasto eliminado") }
        }
    }

    fun clearMessage() {
        _dialogState.update { it.copy(message = null) }
    }

    companion object {
        fun formatAmount(amount: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
            return try {
                format.format(amount)
            } catch (e: Exception) {
                String.format(Locale.getDefault(), "$%.2f", amount)
            }
        }
    }
}

class ExpenseViewModelFactory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
