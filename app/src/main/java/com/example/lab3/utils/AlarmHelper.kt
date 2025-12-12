package com.example.lab3.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.lab3.receiver.NotificationReceiver

class AlarmHelper(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val sharedPreferences = context.getSharedPreferences("WellNestPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "AlarmHelper"
        private const val REQUEST_CODE = 1001
    }

    fun setWaterReminder(intervalMinutes: Long) {
        try {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = "WATER_REMINDER"
                putExtra("interval", intervalMinutes)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + (intervalMinutes * 60 * 1000)

            Log.d(TAG, "Setting SINGLE reminder for $intervalMinutes minutes from now")

            // FIXED: Use simpler alarm method that's more reliable
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                // Fallback to inexact alarm for compatibility
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

            // Save reminder state and next trigger time
            sharedPreferences.edit().putBoolean("reminder_enabled", true).apply()
            sharedPreferences.edit().putLong("reminder_interval", intervalMinutes).apply()
            sharedPreferences.edit().putLong("next_reminder_time", triggerTime).apply()

            Log.d(TAG, "SINGLE reminder set for $intervalMinutes minutes. Next trigger: $triggerTime")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error setting reminder: ${e.message}")
            throw e
        }
    }

    // FIXED: Add back the method to prevent crashes
    fun setRepeatingWaterReminder(intervalMinutes: Long) {
        Log.d(TAG, "setRepeatingWaterReminder called - using single reminder instead")
        // Use single reminder instead of repeating
        setWaterReminder(intervalMinutes)
    }

    fun cancelWaterReminder() {
        try {
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()

            Log.d(TAG, "Cancelled water reminders")

            // Save reminder state and clear next trigger time
            sharedPreferences.edit().putBoolean("reminder_enabled", false).apply()
            sharedPreferences.edit().remove("next_reminder_time").apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling reminder: ${e.message}")
        }
    }

    // CHANGED: Default to false - reminders OFF by default
    fun getReminderState(): Boolean {
        return sharedPreferences.getBoolean("reminder_enabled", false)
    }

    fun getReminderInterval(): Long {
        return sharedPreferences.getLong("reminder_interval", 30L)
    }

    // Get time remaining until next reminder
    fun getTimeRemaining(): Long {
        val nextReminderTime = sharedPreferences.getLong("next_reminder_time", 0L)
        if (nextReminderTime == 0L) return 0L

        val currentTime = System.currentTimeMillis()
        val timeRemaining = nextReminderTime - currentTime

        return if (timeRemaining > 0) timeRemaining else 0L
    }

    // Format time remaining for display
    fun getFormattedTimeRemaining(): String {
        val timeRemainingMs = getTimeRemaining()
        if (timeRemainingMs == 0L) return "No reminder set"

        val minutes = (timeRemainingMs / (1000 * 60)) % 60
        val hours = timeRemainingMs / (1000 * 60 * 60)

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, (timeRemainingMs / 1000) % 60)
        } else {
            String.format("%02d:%02d", minutes, (timeRemainingMs / 1000) % 60)
        }
    }

    // Check if reminder is active and return status
    fun getReminderStatus(): String {
        if (!getReminderState()) return "Reminders disabled"

        val timeRemaining = getTimeRemaining()
        return if (timeRemaining > 0) {
            "Next reminder in: ${getFormattedTimeRemaining()}"
        } else {
            "Setting up next reminder..."
        }
    }

    // Check if we need to reschedule the reminder (for edge cases)
    fun shouldRescheduleReminder(): Boolean {
        if (!getReminderState()) return false

        val timeRemaining = getTimeRemaining()
        // If reminder is enabled but no valid time remaining, reschedule
        return timeRemaining <= 0
    }
}