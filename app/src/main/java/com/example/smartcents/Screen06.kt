package com.example.smartcents

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.NumberFormat
import java.util.*

class Screen06 : AppCompatActivity(), ExpenseRepository.ExpenseChangeListener {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var goalRepository: BudgetGoalRepository
    private lateinit var balanceAmountText: TextView

    companion object {
        private const val CHANNEL_ID = "budget_alerts_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_screen06)

        expenseRepository = ExpenseRepository(this)
        goalRepository = BudgetGoalRepository(this)
        balanceAmountText = findViewById(R.id.balanceAmount)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        createNotificationChannel()
        updateBalance()

        // Set click listeners
        findViewById<View>(R.id.expensesCard).setOnClickListener {
            startActivity(Intent(this, Screen07::class.java))
        }
        findViewById<View>(R.id.allExpensesCard).setOnClickListener {
            startActivity(Intent(this, Screen08::class.java))
        }
        findViewById<View>(R.id.budgetPlanCard).setOnClickListener {
            startActivity(Intent(this, Screen09::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        expenseRepository.addExpenseChangeListener(this)
        updateBalance()
        checkCurrentMonthBudget()
    }

    override fun onPause() {
        super.onPause()
        expenseRepository.removeExpenseChangeListener(this)
    }

    override fun onExpensesChanged(expenses: List<Expense>) {
        runOnUiThread {
            updateBalance()
            checkCurrentMonthBudget()
        }
    }

    private fun updateBalance() {
        val totalExpenses = expenseRepository.getAllExpenses().sumOf { it.amount }
        balanceAmountText.text = formatCurrency(totalExpenses)
    }

    private fun checkCurrentMonthBudget() {
        val currentMonth = SimpleDateFormat("MM/yyyy", Locale.US).format(Date())
        val goal = goalRepository.getBudgetGoalForMonth(currentMonth)

        goal?.let { budgetGoal ->
            val totalSpent = expenseRepository.getExpensesForMonth(currentMonth)
                .sumOf { it.amount }
            if (totalSpent > budgetGoal.amount) {
                sendBudgetExceededNotification(currentMonth, totalSpent, budgetGoal.amount)
            }
        }
    }

    private fun sendBudgetExceededNotification(monthYear: String, spent: Double, limit: Double) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
            .setAutoCancel(true)
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
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance()
        format.maximumFractionDigits = 2
        format.currency = Currency.getInstance("USD")
        return format.format(amount)
    }
}