package com.example.lab3.model

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

data class Mood(
    val id: Long = System.currentTimeMillis(),
    val emoji: String,
    val moodType: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
) : Serializable {

    fun getFormattedTime(): String {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return timeFormat.format(Date(timestamp))
    }

    fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    fun getFullFormattedDateTime(): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    fun isToday(): Boolean {
        val today = Calendar.getInstance()
        val moodDate = Calendar.getInstance().apply { time = Date(timestamp) }
        return today.get(Calendar.YEAR) == moodDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == moodDate.get(Calendar.DAY_OF_YEAR)
    }

    fun isYesterday(): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val moodDate = Calendar.getInstance().apply { time = Date(timestamp) }
        return yesterday.get(Calendar.YEAR) == moodDate.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == moodDate.get(Calendar.DAY_OF_YEAR)
    }

    fun getDisplayDate(): String {
        return when {
            isToday() -> "Today, ${getFormattedTime()}"
            isYesterday() -> "Yesterday, ${getFormattedTime()}"
            else -> "${getFormattedDate()}, ${getFormattedTime()}"
        }
    }

    companion object {
        val availableMoods = listOf(
            MoodEmoji("😊", "Happy"),
            MoodEmoji("😢", "Sad"),
            MoodEmoji("😡", "Angry"),
            MoodEmoji("😴", "Tired"),
            MoodEmoji("😃", "Excited"),
            MoodEmoji("😌", "Calm"),
            MoodEmoji("😰", "Anxious"),
            MoodEmoji("🤩", "Amazed")
        )
    }
}

data class MoodEmoji(
    val emoji: String,
    val label: String
) : Serializable