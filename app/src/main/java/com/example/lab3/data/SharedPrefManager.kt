package com.example.lab3.data

import android.content.Context
import android.content.SharedPreferences
import com.example.lab3.model.Habit
import com.example.lab3.model.Mood
import com.example.lab3.model.Settings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class SharedPrefManager(private val context: Context) {
    private val sharedPref: SharedPreferences = context.getSharedPreferences("WellNestPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Constants for water tracking
    companion object {
        private const val KEY_WATER_GOAL = "water_goal"
        private const val KEY_CURRENT_WATER_INTAKE = "current_water_intake"
        private const val DEFAULT_WATER_GOAL = 8
    }

    // Habits Management
    fun saveHabits(habits: List<Habit>) {
        val json = gson.toJson(habits)
        sharedPref.edit().putString("habits", json).apply()
    }

    fun getHabits(): List<Habit> {
        val json = sharedPref.getString("habits", null)
        return if (json != null) {
            val type = object : TypeToken<List<Habit>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun saveTodayCompletions(completions: Map<String, Int>) {
        val json = gson.toJson(completions)
        sharedPref.edit().putString("today_completions", json).apply()
    }

    fun getTodayCompletions(): Map<String, Int> {
        val json = sharedPref.getString("today_completions", null)
        return if (json != null) {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } else {
            emptyMap()
        }
    }

    // ==================== MOODS MANAGEMENT ====================

    // Save moods to the main SharedPreferences (same as other data)
    fun saveMoods(moods: List<Mood>) {
        val json = gson.toJson(moods)
        sharedPref.edit().putString("moods", json).apply()
    }

    // Get moods from the main SharedPreferences
    fun getMoods(): List<Mood> {
        val json = sharedPref.getString("moods", null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<Mood>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    // Get today's moods count
    fun getTodayMoodsCount(): Int {
        val moods = getMoods()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return moods.count { it.date == today }
    }

    // Get unique moods count for today
    fun getUniqueMoodsCount(): Int {
        val moods = getMoods()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayMoods = moods.filter { it.date == today }
        return todayMoods.distinctBy { it.moodType }.size
    }

    // ==================== SETTINGS MANAGEMENT ====================

    fun saveSettings(settings: Settings) {
        val json = gson.toJson(settings)
        sharedPref.edit().putString("settings", json).apply()
    }

    fun getSettings(): Settings {
        val json = sharedPref.getString("settings", null)
        return if (json != null) {
            gson.fromJson(json, Settings::class.java)
        } else {
            Settings()
        }
    }

    // ==================== WATER TRACKING METHODS ====================

    // Water Goal Management
    fun getWaterGoal(): Int {
        return sharedPref.getInt(KEY_WATER_GOAL, DEFAULT_WATER_GOAL)
    }

    fun setWaterGoal(goal: Int) {
        sharedPref.edit().putInt(KEY_WATER_GOAL, goal).apply()
    }

    // Current Water Intake Management
    fun getCurrentWaterIntake(): Int {
        return sharedPref.getInt(KEY_CURRENT_WATER_INTAKE, 0)
    }

    fun setCurrentWaterIntake(intake: Int) {
        sharedPref.edit().putInt(KEY_CURRENT_WATER_INTAKE, intake).apply()
    }

    fun resetWaterIntake() {
        setCurrentWaterIntake(0)
    }

    fun addWaterGlass() {
        val current = getCurrentWaterIntake()
        val goal = getWaterGoal()
        if (current < goal) {
            setCurrentWaterIntake(current + 1)
        }
    }

    fun getWaterPercentage(): Int {
        val current = getCurrentWaterIntake()
        val goal = getWaterGoal()
        return if (goal > 0) {
            ((current.toFloat() / goal.toFloat()) * 100).toInt()
        } else {
            0
        }
    }

    // ==================== STATISTICS METHODS ====================

    fun getCompletedHabitsCount(): Int {
        val completions = getTodayCompletions()
        val habits = getHabits()
        var completedCount = 0

        habits.forEach { habit ->
            val current = completions[habit.id] ?: 0
            if (current >= habit.targetCount) {
                completedCount++
            }
        }
        return completedCount
    }

    // FIXED: Use the new method that reads from main SharedPreferences
    fun getUniqueMoodsCountForDisplay(): Int {
        return getUniqueMoodsCount()
    }

    fun getHydrationRate(): Int {
        return getWaterPercentage()
    }

    // ==================== APP STATISTICS ====================

    fun getTotalHabitsCompleted(): Int {
        val completions = getTodayCompletions()
        var total = 0
        completions.values.forEach { count ->
            total += count
        }
        return total
    }

    fun getTotalWaterConsumed(): Int {
        return getCurrentWaterIntake()
    }

    fun getTotalMoodsTracked(): Int {
        return getTodayMoodsCount()
    }

    // Last reset date for habits
    fun getLastResetDate(): Long {
        return sharedPref.getLong("last_reset_date", 0)
    }

    fun setLastResetDate(date: Long) {
        sharedPref.edit().putLong("last_reset_date", date).apply()
    }

    // ==================== DATA MANAGEMENT ====================

    fun clearAllData() {
        sharedPref.edit().clear().apply()
    }

    fun resetDailyData() {
        // Reset daily-specific data but keep settings and goals
        val editor = sharedPref.edit()
        editor.remove("today_completions")
        editor.remove(KEY_CURRENT_WATER_INTAKE)
        // Note: We don't remove water goal, habits, moods, or settings
        editor.apply()
    }
}