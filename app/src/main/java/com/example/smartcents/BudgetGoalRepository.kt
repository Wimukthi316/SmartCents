package com.example.smartcents

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson

class BudgetGoalRepository(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "budget_goal_preferences", Context.MODE_PRIVATE
    )
    private val gson = Gson()

    private val goalListType = object : TypeToken<List<BudgetGoal>>() {}.type

    companion object {
        private const val KEY_BUDGET_GOALS = "budget_goals"
    }

    fun getAllBudgetGoals(): List<BudgetGoal> {
        val goalsJson = sharedPreferences.getString(KEY_BUDGET_GOALS, null)
        return if (goalsJson != null) {
            gson.fromJson(goalsJson, goalListType)
        } else {
            emptyList()
        }
    }

    fun getBudgetGoalForMonth(monthYear: String): BudgetGoal? {
        return getAllBudgetGoals().find { it.monthYear == monthYear }
    }

    fun saveBudgetGoal(budgetGoal: BudgetGoal) {
        val goals = getAllBudgetGoals().toMutableList()

        // Remove existing goal for the same month/year if exists
        goals.removeAll { it.monthYear == budgetGoal.monthYear }

        // Add the new goal
        goals.add(budgetGoal)

        // Save the updated list
        saveGoals(goals)
    }

    fun deleteBudgetGoal(monthYear: String) {
        val goals = getAllBudgetGoals().toMutableList()
        goals.removeAll { it.monthYear == monthYear }
        saveGoals(goals)
    }

    private fun saveGoals(goals: List<BudgetGoal>) {
        val editor = sharedPreferences.edit()
        val goalsJson = gson.toJson(goals)
        editor.putString(KEY_BUDGET_GOALS, goalsJson)
        editor.apply()
    }
}