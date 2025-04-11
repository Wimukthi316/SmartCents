package com.example.smartcents

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class Screen07 : AppCompatActivity() {

    private lateinit var expenseNameInput: TextInputEditText
    private lateinit var categoryDropdown: AutoCompleteTextView
    private lateinit var amountInput: TextInputEditText
    private lateinit var dateInput: TextInputEditText
    private lateinit var addButton: MaterialButton
    private lateinit var backButton: MaterialButton

    private lateinit var expenseRepository: ExpenseRepository
    private var editingExpenseId: String? = null
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen07)

        expenseRepository = ExpenseRepository(this)
        initializeViews()
        setupBackButton()
        checkEditMode()
        setupCategoryDropdown()
        setupDatePicker()
        setupActionButton()
    }

    private fun initializeViews() {
        expenseNameInput = findViewById(R.id.expenseNameInput)
        categoryDropdown = findViewById(R.id.categoryDropdown)
        amountInput = findViewById(R.id.amountInput)
        dateInput = findViewById(R.id.dateInput)
        addButton = findViewById(R.id.addButton)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun checkEditMode() {
        editingExpenseId = intent.getStringExtra("EXPENSE_ID")
        isEditMode = editingExpenseId != null
        if (isEditMode) {
            addButton.text = "UPDATE EXPENSE"
            loadExpenseData()
        }
    }

    private fun loadExpenseData() {
        editingExpenseId?.let { id ->
            expenseRepository.getAllExpenses().find { it.id == id }?.let { expense ->
                expenseNameInput.setText(expense.name)
                categoryDropdown.setText(expense.category)
                amountInput.setText(expense.amount.toString())
                dateInput.setText(expense.date)
            }
        }
    }

    private fun setupCategoryDropdown() {
        val categories = arrayOf("Food", "Transport", "Bills", "Education", "Grocery")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        categoryDropdown.setAdapter(adapter)
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            dateInput.setText(dateFormat.format(calendar.time))
        }

        dateInput.setOnClickListener {
            DatePickerDialog(
                this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        if (!isEditMode) {
            dateInput.setText(dateFormat.format(calendar.time))
        }
    }

    private fun setupActionButton() {
        addButton.setOnClickListener {
            if (validateInputs()) {
                saveExpense()
                showSuccessMessage()
                finish()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (expenseNameInput.text.toString().trim().isEmpty()) {
            expenseNameInput.error = "Please enter expense name"
            isValid = false
        }

        if (categoryDropdown.text.toString().trim().isEmpty()) {
            categoryDropdown.error = "Please select a category"
            isValid = false
        }

        try {
            val amount = amountInput.text.toString().trim().toDouble()
            if (amount <= 0) {
                amountInput.error = "Amount must be greater than zero"
                isValid = false
            }
        } catch (e: NumberFormatException) {
            amountInput.error = "Please enter a valid amount"
            isValid = false
        }

        if (dateInput.text.toString().trim().isEmpty()) {
            dateInput.error = "Please select a date"
            isValid = false
        }

        return isValid
    }

    private fun saveExpense() {
        val expense = Expense(
            id = editingExpenseId ?: UUID.randomUUID().toString(),
            name = expenseNameInput.text.toString().trim(),
            category = categoryDropdown.text.toString().trim(),
            amount = amountInput.text.toString().trim().toDouble(),
            date = dateInput.text.toString().trim()
        )

        if (isEditMode) {
            expenseRepository.updateExpense(expense)
        } else {
            expenseRepository.addExpense(expense)
        }
    }

    private fun showSuccessMessage() {
        val message = if (isEditMode) "Expense updated!" else "Expense added!"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}