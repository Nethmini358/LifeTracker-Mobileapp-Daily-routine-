package com.example.lab3.model

import java.util.Date

data class Habit(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val description: String,
    val targetCount: Int = 1, // How many times per day
    val currentCount: Int = 0,
    val completed: Boolean = false,
    val date: Date = Date(),
    val color: Int = 0 // For UI coloring
) {
    fun getProgressPercentage(): Int {
        return if (targetCount > 0) {
            (currentCount * 100 / targetCount).coerceAtMost(100)
        } else 0
    }

    fun incrementCount(): Habit {
        return this.copy(
            currentCount = (currentCount + 1).coerceAtMost(targetCount),
            completed = currentCount + 1 >= targetCount
        )
    }

    fun resetForNewDay(): Habit {
        return this.copy(
            currentCount = 0,
            completed = false
        )
    }
}