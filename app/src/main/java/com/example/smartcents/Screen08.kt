package com.example.smartcents

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.*

class Screen08 : AppCompatActivity() {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var categorySpinner: Spinner
    private lateinit var expensesRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: View
    private lateinit var totalAmountText: TextView
    private lateinit var addNewButton: MaterialButton
    private lateinit var backButton: MaterialButton
    private var currentCategory = "All Categories"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen08)

        expenseRepository = ExpenseRepository(this)
        initializeViews()
        setupBackButton()
        setupRecyclerView()
        setupCategorySpinner()
        setupAddButton()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the category spinner to include any new categories
        setupCategorySpinner()
        // Then load expenses with the current category
        loadExpenses()
    }

    private fun initializeViews() {
        categorySpinner = findViewById(R.id.categorySpinner)
        expensesRecyclerView = findViewById(R.id.expensesRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        totalAmountText = findViewById(R.id.totalAmountText)
        addNewButton = findViewById(R.id.addNewButton)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            navigateToScreen06()
        }
    }

    private fun navigateToScreen06() {
        val intent = Intent(this, Screen06::class.java)
        // Clear the back stack so user can't return to this screen with back button
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish() // Finish current activity
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(
            emptyList(),
            onEditClick = { expense -> navigateToEditScreen(expense) },
            onDeleteClick = { expense -> showDeleteDialog(expense) }
        )
        expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        expensesRecyclerView.adapter = expenseAdapter
    }

    private fun setupCategorySpinner() {
        // Get the current selection if there is one
        val currentSelection = if (::categorySpinner.isInitialized && categorySpinner.selectedItem != null) {
            categorySpinner.selectedItem.toString()
        } else {
            "All Categories"
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            expenseRepository.getCategories()
        )
        categorySpinner.adapter = adapter

        // Restore the selection if it still exists in the list
        val position = (0 until adapter.count).find {
            adapter.getItem(it).toString() == currentSelection
        } ?: 0
        categorySpinner.setSelection(position)

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                currentCategory = parent?.getItemAtPosition(pos).toString()
                loadExpenses()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupAddButton() {
        addNewButton.setOnClickListener {
            startActivity(Intent(this, Screen07::class.java))
        }
    }

    private fun loadExpenses() {
        val expenses = expenseRepository.getExpensesByCategory(currentCategory)
        expenseAdapter.updateExpenses(expenses)
        updateEmptyState(expenses.isEmpty())
        updateTotalAmount()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        expensesRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun updateTotalAmount() {
        val total = expenseRepository.calculateTotalAmount(currentCategory)
        totalAmountText.text = NumberFormat.getCurrencyInstance(Locale.US).format(total)
    }

    private fun navigateToEditScreen(expense: Expense) {
        startActivity(Intent(this, Screen07::class.java).apply {
            putExtra("EXPENSE_ID", expense.id)
        })
    }

    private fun showDeleteDialog(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Delete ${expense.name}?")
            .setPositiveButton("Delete") { _, _ ->
                expenseRepository.deleteExpense(expense)
                loadExpenses()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}