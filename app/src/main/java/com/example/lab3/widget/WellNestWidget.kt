package com.example.lab3.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.lab3.MainActivity
import com.example.lab3.R
import com.example.lab3.data.SharedPrefManager
import java.text.SimpleDateFormat
import java.util.*

class WellNestWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update all widgets
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // When the first widget is created
    }

    override fun onDisabled(context: Context) {
        // When the last widget is disabled
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val sharedPrefManager = SharedPrefManager(context)

            // Get habit progress
            val completions = sharedPrefManager.getTodayCompletions()
            val habits = sharedPrefManager.getHabits()
            val completedHabits = habits.count { habit ->
                val current = completions[habit.id] ?: 0
                current >= habit.targetCount
            }
            val habitsText = "$completedHabits/${habits.size}"

            // Get water progress - USING UPDATED METHODS
            val waterIntake = sharedPrefManager.getCurrentWaterIntake() // Updated method
            val waterGoal = sharedPrefManager.getWaterGoal()
            val waterText = "$waterIntake/$waterGoal"

            // Get today's mood
            val moods = sharedPrefManager.getMoods()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val todayMood = moods.lastOrNull { it.date == today }
            val moodEmoji = todayMood?.emoji ?: "😐"

            // Create timestamp
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val lastUpdated = "Updated: ${timeFormat.format(Date())}"

            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // Update UI elements
            views.setTextViewText(R.id.widget_habits_progress, habitsText)
            views.setTextViewText(R.id.widget_water_progress, waterText)
            views.setTextViewText(R.id.widget_mood_emoji, moodEmoji)
            views.setTextViewText(R.id.widget_last_updated, lastUpdated)

            // Set up click intents
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_layout_root, pendingIntent)

            // Set up add water button
            val addWaterIntent = Intent(context, WellNestWidget::class.java).apply {
                action = "ADD_WATER"
            }
            val addWaterPendingIntent = PendingIntent.getBroadcast(
                context, 1, addWaterIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        // Call this from your app to refresh the widget
        fun refreshWidget(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, WellNestWidget::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            "ADD_WATER" -> {
                // Handle add water from widget - USING UPDATED METHODS
                val sharedPrefManager = SharedPrefManager(context)
                val currentIntake = sharedPrefManager.getCurrentWaterIntake() // Updated method
                val waterGoal = sharedPrefManager.getWaterGoal()

                if (currentIntake < waterGoal) {
                    sharedPrefManager.addWaterGlass() // Updated method - this increments by 1

                    // Refresh widget to show updated data
                    refreshWidget(context)

                    // Also refresh the app data by broadcasting to MainActivity
                    val refreshIntent = Intent("REFRESH_DATA")
                    context.sendBroadcast(refreshIntent)

                    // Show confirmation
                    android.widget.Toast.makeText(
                        context,
                        "✅ Water added from widget!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    android.widget.Toast.makeText(
                        context,
                        "🎉 You've reached your water goal!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            else -> {
                // Handle normal widget updates
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, WellNestWidget::class.java)
                )
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }
}