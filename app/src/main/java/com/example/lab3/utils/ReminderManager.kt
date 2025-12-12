package com.example.lab3.utils

import android.app.Activity
import android.app.AlertDialog
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.widget.Button
import com.example.lab3.R

object ReminderManager {

    fun showReminderDialog(activity: Activity) {
        // Make sure we're not showing multiple dialogs
        if (activity.isFinishing) return

        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_water_reminder, null)

        val dialog = AlertDialog.Builder(activity, R.style.ReminderDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Set dialog window properties
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Play notification sound when dialog appears
        playNotificationSound(activity)

        // Handle Got It button click
        val btnGotIt = dialogView.findViewById<Button>(R.id.btnGotIt)
        btnGotIt.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun playNotificationSound(activity: Activity) {
        try {
            val mediaPlayer = MediaPlayer.create(activity, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            mediaPlayer.setOnCompletionListener { mp ->
                mp.release()
            }
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}