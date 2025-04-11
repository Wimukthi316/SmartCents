package com.example.smartcents

import android.content.Context
import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.util.Locale

class ExpenseRepository(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "expense_preferences", Context.MODE_PRIVATE
    )
    private val gson = Gson()
    private val expenseListType = object : TypeToken<List<Expense>>() {}.type

    interface ExpenseChangeListener {
        fun onExpensesChanged(expenses: List<Expense>)
    }

    private val listeners = mutableListOf<ExpenseChangeListener>()

    companion object {
        private const val KEY_EXPENSES = "expenses"
    }

    fun addExpenseChangeListener(listener: ExpenseChangeListener) {
        listeners.add(listener)
    }

    fun removeExpenseChangeListener(listener: ExpenseChangeListener) {
        listeners.remove(listener)
    }

    private fun notifyExpensesChanged(expenses: List<Expense>) {
        listeners.forEach { it.onExpensesChanged(expenses) }
    }

    fun getAllExpenses(): List<Expense> {
        val expensesJson = sharedPreferences.getString(KEY_EXPENSES, null)
        return if (expensesJson != null) {
            gson.fromJson(expensesJson, expenseListType)
        } else {
            emptyList()
        }
    }

    fun getExpensesByCategory(category: String): List<Expense> {
        return if (category == "All Categories") {
            getAllExpenses()
        } else {
            getAllExpenses().filter { it.category == category }
        }
    }

    fun addExpense(expense: Expense) {
        val expenses = getAllExpenses().toMutableList()
        expenses.add(expense)
        saveExpenses(expenses)
    }

    fun updateExpense(updatedExpense: Expense) {
        val expenses = getAllExpenses().toMutableList()
        val index = expenses.indexOfFirst { it.id == updatedExpense.id }
        if (index != -1) {
            expenses[index] = updatedExpense
            saveExpenses(expenses)
        }
    }

    fun deleteExpense(expense: Expense) {
        val expenses = getAllExpenses().toMutableList()
        expenses.removeAll { it.id == expense.id }
        saveExpenses(expenses)
    }

    fun calculateTotalAmount(category: String = "All Categories"): Double {
        val expenses = if (category == "All Categories") {
            getAllExpenses()
        } else {
            getExpensesByCategory(category)
        }
        return expenses.sumOf { it.amount }
    }

    private fun saveExpenses(expenses: List<Expense>) {
        val editor = sharedPreferences.edit()
        val expensesJson = gson.toJson(expenses)
        editor.putString(KEY_EXPENSES, expensesJson)
        editor.apply()
        notifyExpensesChanged(expenses)
    }

    fun getCategories(): List<String> {
        val categories = getAllExpenses()
            .map { it.category }
            .distinct()
            .sorted()
            .toMutableList()
        categories.add(0, "All Categories")
        return categories
    }

    fun getExpensesForMonth(monthYear: String): List<Expense> {
        return getAllExpenses().filter { expense ->
            try {
                val expenseDate = SimpleDateFormat("MM/dd/yyyy", Locale.US).parse(expense.date)
                expenseDate?.let {
                    SimpleDateFormat("MM/yyyy", Locale.US).format(it) == monthYear
                } ?: false
            } catch (e: Exception) {
                false
            }
        }
    }
}