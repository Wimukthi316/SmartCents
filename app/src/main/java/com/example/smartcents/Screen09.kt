package com.example.smartcents


import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class Screen09 : AppCompatActivity() {

    private lateinit var monthYearInput: TextInputEditText
    private lateinit var budgetGoalInput: TextInputEditText
    private lateinit var monthDropdown: AutoCompleteTextView
    private lateinit var saveGoalButton: MaterialButton
    private lateinit var styledBackButton: MaterialButton
    private lateinit var chartContainer: LinearLayout

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var goalRepository: BudgetGoalRepository

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MM/yyyy", Locale.US)

    companion object {
        private const val CHANNEL_ID = "budget_alerts_channel"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen09)

        expenseRepository = ExpenseRepository(this)
        goalRepository = BudgetGoalRepository(this)
        createNotificationChannel()
        checkNotificationPermission()

        initializeViews()
        setupMonthPicker()
        setupMonthDropdown()
        setupSaveButton()
        setupBackButton()
        loadExistingGoal()
        updateChart()
    }

    private fun initializeViews() {
        monthYearInput = findViewById(R.id.monthYearInput)
        budgetGoalInput = findViewById(R.id.budgetGoalInput)
        monthDropdown = findViewById(R.id.monthDropdown)
        saveGoalButton = findViewById(R.id.saveGoalButton)
        styledBackButton = findViewById(R.id.styledBackButton)
        chartContainer = findViewById(R.id.chartContainer)
    }

    private fun setupBackButton() {
        styledBackButton.setOnClickListener {
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

    private fun setupMonthPicker() {
        monthYearInput.setText(dateFormat.format(calendar.time))
        monthYearInput.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, _ ->
                    calendar.set(year, month, 1)
                    monthYearInput.setText(dateFormat.format(calendar.time))
                    loadExistingGoal()
                    updateChart()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                1
            ).show()
        }
    }

    private fun setupMonthDropdown() {
        val months = resources.getStringArray(R.array.months_array)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, months)
        monthDropdown.setAdapter(adapter)
        monthDropdown.setText(months[calendar.get(Calendar.MONTH)], false)

        monthDropdown.setOnItemClickListener { _, _, position, _ ->
            calendar.set(Calendar.MONTH, position)
            monthYearInput.setText(dateFormat.format(calendar.time))
            loadExistingGoal()
            updateChart()
        }
    }

    private fun setupSaveButton() {
        saveGoalButton.setOnClickListener {
            if (validateInput()) {
                saveGoal()
                updateChart()
                checkBudgetExceeded()
            }
        }
    }

    private fun validateInput(): Boolean {
        return when {
            budgetGoalInput.text.isNullOrEmpty() -> {
                budgetGoalInput.error = "Please enter amount"
                false
            }
            budgetGoalInput.text.toString().toDoubleOrNull() == null -> {
                budgetGoalInput.error = "Invalid amount"
                false
            }
            budgetGoalInput.text.toString().toDouble() <= 0 -> {
                budgetGoalInput.error = "Amount must be positive"
                false
            }
            else -> true
        }
    }

    private fun saveGoal() {
        goalRepository.saveBudgetGoal(
            BudgetGoal(
                monthYear = monthYearInput.text.toString(),
                amount = budgetGoalInput.text.toString().toDouble()
            )
        )
        Toast.makeText(this, "Budget saved!", Toast.LENGTH_SHORT).show()
    }

    private fun loadExistingGoal() {
        goalRepository.getBudgetGoalForMonth(monthYearInput.text.toString())?.let {
            budgetGoalInput.setText(it.amount.toString())
        } ?: run {
            budgetGoalInput.setText("")
        }
    }

    private fun updateChart() {
        chartContainer.removeAllViews()

        val monthYear = monthYearInput.text.toString()
        val expenses = getExpensesForMonth(monthYear)
        val goal = goalRepository.getBudgetGoalForMonth(monthYear)

        if (expenses.isEmpty() && goal == null) {
            val emptyText = TextView(this).apply {
                text = "No data available for this month"
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
            }
            chartContainer.addView(emptyText)
            return
        }

        val categoryMap = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { expense -> expense.amount } }

        val maxAmount = (categoryMap.values.maxOrNull() ?: 0.0).coerceAtLeast(goal?.amount ?: 0.0)

        categoryMap.entries.forEach { (category, amount) ->
            addBarToChart(category, amount, maxAmount, false)
        }

        goal?.let {
            addBarToChart("Budget Goal", it.amount, maxAmount, true)
        }
    }

    private fun addBarToChart(label: String, value: Double, maxValue: Double, isGoal: Boolean) {
        val barLayout = layoutInflater.inflate(R.layout.item_chart_bar, chartContainer, false)

        val labelView = barLayout.findViewById<TextView>(R.id.barLabel)
        val barView = barLayout.findViewById<View>(R.id.barView)
        val valueView = barLayout.findViewById<TextView>(R.id.barValue)

        labelView.text = label
        valueView.text = "$${"%.2f".format(value)}"

        val params = barView.layoutParams as LinearLayout.LayoutParams
        params.weight = (value / maxValue).toFloat()
        barView.layoutParams = params

        barView.setBackgroundColor(
            if (isGoal) ContextCompat.getColor(this, R.color.green)
            else Color.rgb(64, 89, 128)
        )

        chartContainer.addView(barLayout)
    }

    private fun getExpensesForMonth(monthYear: String): List<Expense> {
        return expenseRepository.getAllExpenses().filter { expense ->
            try {
                val expenseDate = SimpleDateFormat("MM/dd/yyyy", Locale.US).parse(expense.date)
                expenseDate?.let { dateFormat.format(it) == monthYear } ?: false
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun checkBudgetExceeded() {
        val monthYear = monthYearInput.text.toString()
        val goal = goalRepository.getBudgetGoalForMonth(monthYear) ?: return
        val totalSpent = getExpensesForMonth(monthYear).sumOf { it.amount }

        if (totalSpent > goal.amount) {
            sendBudgetExceededNotification(monthYear, totalSpent, goal.amount)
        }
    }

    private fun sendBudgetExceededNotification(monthYear: String, spent: Double, limit: Double) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, Screen09::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val overAmount = spent - limit

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setColor(ContextCompat.getColor(this, R.color.green))
            .setContentTitle("Budget Exceeded!")
            .setContentText("You've spent ${"%.2f".format(spent)} of ${"%.2f".format(limit)}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("""
                    You exceeded your $monthYear budget by ${"%.2f".format(overAmount)}!
                    
                    Monthly limit: $${"%.2f".format(limit)}
                    Total spent: $${"%.2f".format(spent)}
                    """.trimIndent()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when you exceed your budget goals"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 100, 200)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showPermissionRationale()
                }
                else -> {
                    requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_CODE
                    )
                }
            }
        }
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Notification Permission Needed")
            .setMessage("BudgetFit needs notification permission to alert you when you exceed your budget.")
            .setPositiveButton("Allow") { _, _ ->
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications disabled - budget alerts won't work", Toast.LENGTH_LONG).show()
            }
        }
    }
}