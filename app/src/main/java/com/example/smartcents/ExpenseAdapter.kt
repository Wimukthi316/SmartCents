package com.example.smartcents

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartcents.Expense
import com.example.smartcents.R
import java.text.NumberFormat
import java.util.*

class ExpenseAdapter(
    private var expenses: List<Expense>,
    private val onEditClick: (Expense) -> Unit,
    private val onDeleteClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    class ExpenseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.expenseNameText)
        val categoryText: TextView = view.findViewById(R.id.categoryText)
        val dateText: TextView = view.findViewById(R.id.dateText)
        val amountText: TextView = view.findViewById(R.id.amountText)
        val editButton: ImageButton = view.findViewById(R.id.editButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]

        holder.nameText.text = expense.name
        holder.categoryText.text = expense.category
        holder.dateText.text = expense.date

        // Format currency
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
        holder.amountText.text = currencyFormat.format(expense.amount)

        // Set click listeners
        holder.editButton.setOnClickListener { onEditClick(expense) }
        holder.deleteButton.setOnClickListener { onDeleteClick(expense) }
    }

    override fun getItemCount() = expenses.size

    fun updateExpenses(newExpenses: List<Expense>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }
}