package com.example.lab3.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import android.graphics.BitmapFactory
import android.util.Log
import com.example.lab3.R
import com.example.lab3.MainActivity
import com.example.lab3.utils.AlarmHelper
import com.example.lab3.utils.ReminderManager

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
        private const val CHANNEL_ID = "water_reminder_channel"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_GOT_IT = "GOT_IT_ACTION"
        const val ACTION_SHOW_DIALOG = "SHOW_DIALOG_ACTION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "WATER_REMINDER" -> {
                Log.d(TAG, "Water reminder received from AlarmManager")
                // FIXED: Don't start activity, just show notification
                // This prevents navigation to home screen
                showWaterReminderNotification(context)

                // Cancel reminder after showing notification
                val alarmHelper = AlarmHelper(context)
                alarmHelper.cancelWaterReminder()

                Log.d(TAG, "Notification shown and reminder cancelled")
            }
            ACTION_GOT_IT -> {
                Log.d(TAG, "User acknowledged the reminder")
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID)
            }
        }
    }

    private fun showWaterReminderNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Water Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Get reminders to drink water throughout the day"
                enableVibration(true)
                setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION), null)
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create "Got it!" intent
        val gotItIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_GOT_IT
        }
        val gotItPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            gotItIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // FIXED: Create main app intent WITHOUT flags that navigate to home
        val appIntent = Intent(context, MainActivity::class.java).apply {
            // Don't add flags that clear the back stack or create new task
            // This way user stays on current screen when they click notification
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification with sound
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Time for a hydration break! 💧")
            .setContentText("Staying hydrated is key to a healthy day. Take a moment to drink a glass of water.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Staying hydrated is key to a healthy day. Take a moment to drink a glass of water."))
            .setSmallIcon(R.drawable.ic_water_drop)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_water_drop))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(1000, 1000, 1000)) // Vibrate pattern
            .addAction(
                R.drawable.ic_water_drop,
                "Got it!",
                gotItPendingIntent
            )
            .setContentIntent(appPendingIntent)
            .build()

        // Show notification
        notificationManager.notify(NOTIFICATION_ID, notification)

        Log.d(TAG, "Notification displayed successfully")
    }
}