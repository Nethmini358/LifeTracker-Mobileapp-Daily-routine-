package com.example.lab3.ui.settings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.lab3.R
import com.example.lab3.data.SharedPrefManager
import com.example.lab3.utils.AlarmHelper
import com.example.lab3.utils.ReminderManager
import android.util.Log

class SettingsFragment : Fragment() {

    private lateinit var switchReminder: SwitchCompat
    private lateinit var seekBarInterval: SeekBar
    private lateinit var tvIntervalValue: TextView
    private lateinit var tvNextReminder: TextView
    private lateinit var alarmHelper: AlarmHelper
    private lateinit var sharedPrefManager: SharedPrefManager

    // Water tracking views
    private lateinit var btnAddWater: Button
    private lateinit var btnResetWater: Button
    private lateinit var tvWaterGoal: TextView
    private lateinit var tvWaterPercentage: TextView
    private lateinit var tvWaterCurrent: TextView
    private lateinit var tvWaterProgress: TextView
    private lateinit var progressWaterCircular: android.widget.ProgressBar
    private lateinit var waterGoalContainer: View

    // Statistics views
    private lateinit var tvTodayHabits: TextView
    private lateinit var tvUniqueMoods: TextView
    private lateinit var tvHydrationRate: TextView

    private var currentInterval = 30L
    private var isReminderEnabled = false
    private var countDownTimer: CountDownTimer? = null
    private var isInitialized = false

    companion object {
        private const val CHANNEL_ID = "water_reminder_channel"
        private const val TAG = "SettingsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        alarmHelper = AlarmHelper(requireContext())
        sharedPrefManager = SharedPrefManager(requireContext())

        initializeViews(view)
        setupNotificationChannel()
        setupListeners()
        updateWaterDisplay()
        updateStatistics()

        // CHANGED: Load reminder state - will be OFF by default
        loadReminderState()
        startCountdownTimer()

        isInitialized = true
        return view
    }

    private fun initializeViews(view: View) {
        switchReminder = view.findViewById(R.id.switch_reminder)
        seekBarInterval = view.findViewById(R.id.seekBar_interval)
        tvIntervalValue = view.findViewById(R.id.tv_interval_value)
        tvNextReminder = view.findViewById(R.id.tv_next_reminder)

        // Water tracking views
        btnAddWater = view.findViewById(R.id.btn_add_water)
        btnResetWater = view.findViewById(R.id.btn_reset_water)
        tvWaterGoal = view.findViewById(R.id.tv_water_goal)
        tvWaterPercentage = view.findViewById(R.id.tv_water_percentage)
        tvWaterCurrent = view.findViewById(R.id.tv_water_current)
        tvWaterProgress = view.findViewById(R.id.tv_water_progress)
        progressWaterCircular = view.findViewById(R.id.progress_water_circular)
        waterGoalContainer = view.findViewById(R.id.water_goal_container)

        // Statistics views
        tvTodayHabits = view.findViewById(R.id.tv_today_habits)
        tvUniqueMoods = view.findViewById(R.id.tv_unique_moods)
        tvHydrationRate = view.findViewById(R.id.tv_hydration_rate)

        // CHANGED: Set up seekbar range for 1-120 min (8 positions)
        seekBarInterval.max = 7 // 8 positions: 0=1, 1=5, 2=15, 3=30, 4=60, 5=90, 6=120
    }

    private fun loadReminderState() {
        // CHANGED: Load saved state - will be false by default
        isReminderEnabled = alarmHelper.getReminderState()
        currentInterval = alarmHelper.getReminderInterval()

        // Update UI based on saved state
        switchReminder.isChecked = isReminderEnabled

        // CHANGED: Set seekbar position based on current interval for 1-120 min range
        val progress = when (currentInterval) {
            1L -> 0
            5L -> 1
            15L -> 2
            30L -> 3
            60L -> 4
            90L -> 5
            120L -> 6
            else -> 6 // default to 30 minutes (position 3)
        }
        seekBarInterval.progress = progress

        updateIntervalDisplay()

        // Update status text
        if (!isReminderEnabled) {
            tvNextReminder.text = "Reminders are disabled"
        }
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Water Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Get reminders to drink water"
                enableVibration(true)
                setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION), null)
            }

            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupListeners() {
        // Reminder toggle switch
        switchReminder.setOnCheckedChangeListener { _, isChecked ->
            isReminderEnabled = isChecked
            if (isChecked) {
                // Only update reminder if we have a valid interval
                if (currentInterval > 0) {
                    updateReminder()
                    startCountdownTimer()
                    Toast.makeText(requireContext(), "Water reminders enabled", Toast.LENGTH_SHORT).show()
                } else {
                    // If no interval set, revert the switch
                    switchReminder.isChecked = false
                    isReminderEnabled = false
                    Toast.makeText(requireContext(), "Please select a reminder interval first", Toast.LENGTH_SHORT).show()
                }
            } else {
                cancelReminder()
                stopCountdownTimer()
                Toast.makeText(requireContext(), "Water reminders disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // CHANGED: Interval seekbar for 1-120 min range
        seekBarInterval.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentInterval = when (progress) {
                    0 -> 1L    // 1 minute
                    1 -> 5L    // 5 minutes
                    2 -> 15L   // 15 minutes
                    3 -> 30L   // 30 minutes
                    4 -> 60L   // 60 minutes
                    5 -> 90L   // 90 minutes
                    6 -> 120L  // 120 minutes
                    else -> 30L
                }
                updateIntervalDisplay()

                // Only update reminder if switch is enabled AND user is changing AND we're initialized
                if (isReminderEnabled && fromUser && isInitialized) {
                    updateReminder()
                    startCountdownTimer()
                    Toast.makeText(requireContext(), "Reminder interval updated to $currentInterval minutes", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Water tracking buttons
        btnAddWater.setOnClickListener {
            addWaterGlass()
        }

        btnResetWater.setOnClickListener {
            resetWaterIntake()
        }

        // Water goal editor
        waterGoalContainer.setOnClickListener {
            showWaterGoalDialog()
        }
    }

    private fun startCountdownTimer() {
        stopCountdownTimer()

        if (!isReminderEnabled) {
            tvNextReminder.text = "Reminders are disabled"
            return
        }

        val timeRemaining = alarmHelper.getTimeRemaining()
        if (timeRemaining <= 0) {
            tvNextReminder.text = "Setting up next reminder..."
            return
        }

        countDownTimer = object : CountDownTimer(timeRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / (1000 * 60)) % 60
                val hours = millisUntilFinished / (1000 * 60 * 60)
                val seconds = (millisUntilFinished / 1000) % 60

                val timeText = if (hours > 0) {
                    String.format("%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format("%02d:%02d", minutes, seconds)
                }

                tvNextReminder.text = "Next reminder in: $timeText ⏱️"
            }

            override fun onFinish() {
                tvNextReminder.text = "Reminder should appear soon! 🔔"
                // FIXED: Show notification IMMEDIATELY when countdown finishes
                showReminderNotificationImmediately()
            }
        }.start()
    }

    private fun stopCountdownTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    // FIXED: Show reminder notification IMMEDIATELY without any delay
    private fun showReminderNotificationImmediately() {
        Log.d(TAG, "Showing reminder notification IMMEDIATELY")

        try {
            // Show the dialog on current screen IMMEDIATELY
            if (isAdded && !requireActivity().isFinishing) {
                ReminderManager.showReminderDialog(requireActivity())
                Log.d(TAG, "Dialog shown successfully")
            } else {
                Log.d(TAG, "Fragment not attached or activity finishing")
            }

            // Also show system notification with sound IMMEDIATELY
            showSystemNotification()

            // FIXED: Auto-disable after showing notification - don't auto-schedule next reminder
            switchReminder.isChecked = false
            isReminderEnabled = false
            cancelReminder()

            Log.d(TAG, "Notification shown immediately and reminder auto-disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing reminder notification: ${e.message}")
        }
    }

    // FIXED: Show system notification with sound
    private fun showSystemNotification() {
        try {
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Build the notification with sound
            val notification = androidx.core.app.NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setContentTitle("Time for a hydration break! 💧")
                .setContentText("Staying hydrated is key to a healthy day. Take a moment to drink a glass of water.")
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle()
                    .bigText("Staying hydrated is key to a healthy day. Take a moment to drink a glass of water."))
                .setSmallIcon(R.drawable.ic_water_drop)
                .setAutoCancel(true)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(longArrayOf(1000, 1000, 1000)) // Vibrate pattern
                .build()

            // Show notification IMMEDIATELY
            notificationManager.notify(1001, notification)
            Log.d(TAG, "System notification displayed with sound IMMEDIATELY")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing system notification: ${e.message}")
        }
    }

    private fun addWaterGlass() {
        val current = sharedPrefManager.getCurrentWaterIntake()
        val goal = sharedPrefManager.getWaterGoal()

        if (current < goal) {
            sharedPrefManager.addWaterGlass()
            updateWaterDisplay()
            updateStatistics()

            if (sharedPrefManager.getCurrentWaterIntake() == goal) {
                tvWaterProgress.text = "🎉 Amazing! You've reached your water goal! 🎉"
                Toast.makeText(requireContext(), "Congratulations! You've reached your daily water goal!", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "You've already reached your daily water goal!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetWaterIntake() {
        sharedPrefManager.resetWaterIntake()
        updateWaterDisplay()
        updateStatistics()
        Toast.makeText(requireContext(), "Water intake reset to 0", Toast.LENGTH_SHORT).show()
    }

    private fun showWaterGoalDialog() {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Set Water Goal")
        builder.setMessage("Enter your daily water goal (in glasses):")

        val input = android.widget.EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.setText(sharedPrefManager.getWaterGoal().toString())
        builder.setView(input)

        builder.setPositiveButton("Set") { dialog, which ->
            val newGoal = input.text.toString().toIntOrNull()
            if (newGoal != null && newGoal > 0 && newGoal <= 20) {
                sharedPrefManager.setWaterGoal(newGoal)
                updateWaterDisplay()
                updateStatistics()
                Toast.makeText(requireContext(), "Water goal updated to $newGoal glasses", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Please enter a valid number between 1 and 20", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun updateWaterDisplay() {
        val currentIntake = sharedPrefManager.getCurrentWaterIntake()
        val goal = sharedPrefManager.getWaterGoal()
        val percentage = sharedPrefManager.getWaterPercentage()

        progressWaterCircular.progress = percentage
        tvWaterPercentage.text = "$percentage%"
        tvWaterCurrent.text = "$currentIntake/$goal glasses"
        tvWaterGoal.text = goal.toString()
        updateProgressMessage(currentIntake, goal)
    }

    private fun updateProgressMessage(current: Int, goal: Int) {
        val message = when {
            current == 0 -> "Let's start hydrating! 💧"
            current < goal / 3 -> "Great start! Keep going! 💪"
            current < goal / 2 -> "You're doing well! Stay hydrated! 😊"
            current < goal -> "Almost there! Keep it up! 🌟"
            else -> "🎉 Amazing! You've reached your water goal! 🎉"
        }
        tvWaterProgress.text = message
    }

    private fun updateStatistics() {
        val completedHabits = sharedPrefManager.getCompletedHabitsCount()
        val uniqueMoods = sharedPrefManager.getUniqueMoodsCount()
        val hydrationRate = sharedPrefManager.getHydrationRate()

        tvTodayHabits.text = completedHabits.toString()
        tvUniqueMoods.text = uniqueMoods.toString()
        tvHydrationRate.text = "$hydrationRate%"
    }

    private fun updateIntervalDisplay() {
        tvIntervalValue.text = "$currentInterval min"
    }

    private fun updateReminder() {
        if (isReminderEnabled) {
            // FIXED: Only set single reminder, not repeating
            alarmHelper.cancelWaterReminder()
            alarmHelper.setWaterReminder(currentInterval) // Set single reminder only
            updateIntervalDisplay()
            startCountdownTimer()
            Log.d(TAG, "Single reminder set for $currentInterval minutes")
        }
    }

    private fun cancelReminder() {
        alarmHelper.cancelWaterReminder()
        tvNextReminder.text = "Reminders are disabled"
    }

    override fun onResume() {
        super.onResume()
        updateWaterDisplay()
        updateStatistics()

        // FIXED: Only refresh the display, don't reload the state which might reset the timer
        // Just update the countdown timer if reminder is enabled
        if (isReminderEnabled) {
            startCountdownTimer()
        } else {
            tvNextReminder.text = "Reminders are disabled"
        }
    }

    override fun onPause() {
        super.onPause()
        stopCountdownTimer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCountdownTimer()
    }
}