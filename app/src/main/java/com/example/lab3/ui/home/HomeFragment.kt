package com.example.lab3.ui.home

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.lab3.R
import com.example.lab3.data.SharedPrefManager
import com.example.lab3.utils.AlarmHelper
import com.example.lab3.widget.WellNestWidget
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var sharedPrefManager: SharedPrefManager
    private lateinit var alarmHelper: AlarmHelper

    // Views
    private lateinit var tvGreeting: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvHabitsProgress: TextView
    private lateinit var tvWaterProgress: TextView
    private lateinit var tvMoodsCount: TextView
    private lateinit var tvMotivationalQuote: TextView
    private lateinit var tvReminderStatus: TextView

    // Progress views
    private lateinit var progressHabits: android.widget.ProgressBar
    private lateinit var progressWater: android.widget.ProgressBar

    // Chart Views
    private lateinit var chartBarsContainer: LinearLayout
    private lateinit var tvCompletedToday: TextView
    private lateinit var tvPendingToday: TextView
    private lateinit var tvCompletionRate: TextView
    private lateinit var btnRefreshChart: ImageView

    private var countDownTimer: CountDownTimer? = null
    private val chartHandler = Handler(Looper.getMainLooper())
    private var chartRefreshRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        initViews(view)
        setupData()
        return view
    }

    private fun initViews(view: View) {
        sharedPrefManager = SharedPrefManager(requireContext())
        alarmHelper = AlarmHelper(requireContext())

        tvGreeting = view.findViewById(R.id.tv_greeting)
        tvDate = view.findViewById(R.id.tv_date)
        tvHabitsProgress = view.findViewById(R.id.tv_habits_progress)
        tvWaterProgress = view.findViewById(R.id.tv_water_progress)
        tvMoodsCount = view.findViewById(R.id.tv_moods_count)
        tvMotivationalQuote = view.findViewById(R.id.tv_motivational_quote)
        tvReminderStatus = view.findViewById(R.id.tv_reminder_status)

        progressHabits = view.findViewById(R.id.progress_habits)
        progressWater = view.findViewById(R.id.progress_water)

        // Initialize chart views
        chartBarsContainer = view.findViewById(R.id.chart_bars_container)
        tvCompletedToday = view.findViewById(R.id.tv_completed_today)
        tvPendingToday = view.findViewById(R.id.tv_pending_today)
        tvCompletionRate = view.findViewById(R.id.tv_completion_rate)
        btnRefreshChart = view.findViewById(R.id.btn_refresh_chart)
    }

    private fun setupData() {
        // Set greeting based on time
        setGreeting()

        // Set current date
        setCurrentDate()

        // Update progress data
        updateProgressData()

        // Set motivational quote
        setMotivationalQuote()

        // Start countdown timer for reminder status
        startCountdownTimer()

        // Set up chart functionality
        setupChart()
    }

    private fun setGreeting() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val greeting = when (hour) {
            in 5..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            in 18..21 -> "Good Evening"
            else -> "Hello"
        }
        tvGreeting.text = greeting
    }

    private fun setCurrentDate() {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        tvDate.text = currentDate
    }

    private fun updateProgressData() {
        // Habits progress
        val completedHabits = sharedPrefManager.getCompletedHabitsCount()
        val totalHabits = sharedPrefManager.getHabits().size
        val habitsProgress = if (totalHabits > 0) {
            (completedHabits.toFloat() / totalHabits.toFloat() * 100).toInt()
        } else {
            0
        }

        progressHabits.progress = habitsProgress
        tvHabitsProgress.text = "$completedHabits/$totalHabits habits"

        // Water progress
        val waterIntake = sharedPrefManager.getCurrentWaterIntake()
        val waterGoal = sharedPrefManager.getWaterGoal()
        val waterProgress = sharedPrefManager.getWaterPercentage()

        progressWater.progress = waterProgress
        tvWaterProgress.text = "$waterIntake/$waterGoal glasses"

        // FIXED: Use the correct method for moods count
        val todayMoodsCount = sharedPrefManager.getTotalMoodsTracked()
        tvMoodsCount.text = "$todayMoodsCount moods logged"

        // Refresh widget when data changes
        WellNestWidget.refreshWidget(requireContext())

        // Debug log to verify data is updating
        println("HomeFragment - Water: $waterIntake/$waterGoal ($waterProgress%)")
        println("HomeFragment - Habits: $completedHabits/$totalHabits completed")
        println("HomeFragment - Moods: $todayMoodsCount tracked today")
    }

    // Countdown timer for reminder status on home screen
    private fun startCountdownTimer() {
        stopCountdownTimer()

        if (!alarmHelper.getReminderState()) {
            tvReminderStatus.text = "💧 Water reminders are off"
            return
        }

        val timeRemaining = alarmHelper.getTimeRemaining()
        if (timeRemaining <= 0) {
            tvReminderStatus.text = "💧 Setting up next reminder..."
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

                tvReminderStatus.text = "💧 Next reminder in: $timeText"
            }

            override fun onFinish() {
                tvReminderStatus.text = "💧 Reminder coming soon! 🔔"
                // Restart timer to check for next reminder
                startCountdownTimer()
            }
        }.start()
    }

    private fun stopCountdownTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private fun setMotivationalQuote() {
        val quotes = listOf(
            "Small daily improvements are the key to staggering long-term results.",
            "The only bad workout is the one that didn't happen.",
            "Your body can stand almost anything. It's your mind you have to convince.",
            "Don't stop when you're tired. Stop when you're done.",
            "Success is the sum of small efforts repeated day in and day out.",
            "The hardest part is getting started. You're already here!",
            "Every day is a new opportunity to become a better version of yourself.",
            "Progress, not perfection. Every step counts!",
            "Your future self will thank you for the choices you make today.",
            "Wellness is not a destination, it's a journey of small consistent steps."
        )

        val randomQuote = quotes.random()
        tvMotivationalQuote.text = "💫 \"$randomQuote\""
    }

    // Chart functionality Today Progress
    private fun setupChart() {
        // Set up refresh button
        btnRefreshChart.setOnClickListener {
            updateLiveChart()
        }

        // Initial chart update
        updateLiveChart()

        // Set up auto-refresh every 30 seconds
        setupAutoRefresh()
    }

    private fun updateLiveChart() {
        val habits = sharedPrefManager.getHabits()
        val completions = sharedPrefManager.getTodayCompletions()

        // Calculate today's stats
        val completedToday = habits.count { habit ->
            val current = completions[habit.id] ?: 0
            current >= habit.targetCount
        }
        val pendingToday = habits.size - completedToday
        val completionRate = if (habits.isNotEmpty()) {
            (completedToday * 100 / habits.size)
        } else {
            0
        }

        //--- Update real-time stats
        tvCompletedToday.text = completedToday.toString()
        tvPendingToday.text = pendingToday.toString()
        tvCompletionRate.text = "$completionRate%"

        //--- Update weekly chart
        updateWeeklyChart()
    }

    private fun updateWeeklyChart() {
        chartBarsContainer.removeAllViews()

        val weeklyData = getWeeklyHabitData()

        val days = listOf("M", "T", "W", "T", "F", "S", "S")
        val currentDayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2 // Adjust for Monday start
        val maxHeight = 100 // Maximum bar height in dp

        days.forEachIndexed { index, dayLabel ->
            val dayData = weeklyData.getOrElse(index) { 0 }
            val maxHabits = 10 // Maximum expected habits per day

            val barHeight = if (maxHabits > 0) {
                (dayData * maxHeight) / maxHabits
            } else {
                0
            }.coerceAtLeast(20) // Minimum height for visibility

            val isToday = index == currentDayIndex

            val dayLayout = LayoutInflater.from(requireContext())
                .inflate(R.layout.chart_bar_item, chartBarsContainer, false)

            val tvValue = dayLayout.findViewById<TextView>(R.id.tv_bar_value)
            val barView = dayLayout.findViewById<View>(R.id.bar_view)
            val tvDay = dayLayout.findViewById<TextView>(R.id.tv_day_label)

            tvValue.text = dayData.toString()
            tvDay.text = dayLabel

            // Set bar height
            val params = barView.layoutParams as LinearLayout.LayoutParams
            params.height = barHeight.dpToPx(requireContext())
            barView.layoutParams = params

            // Set bar color based on whether it's today
            barView.background = ContextCompat.getDrawable(
                requireContext(),
                if (isToday) R.drawable.progress_bar_fill_active else R.drawable.progress_bar_fill
            )

            // Highlight today's label
            if (isToday) {
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                tvDay.setTypeface(tvDay.typeface, android.graphics.Typeface.BOLD)
            }

            chartBarsContainer.addView(dayLayout)
        }
    }

    private fun getWeeklyHabitData(): List<Int> {
        // This is a placeholder - you'll need to implement actual weekly data retrieval
        // For now, returning mock data
        return listOf(1, 2, 3, 4, 5, 6, 7)
    }

    private fun setupAutoRefresh() {
        chartRefreshRunnable = object : Runnable {
            override fun run() {
                updateLiveChart()
                chartHandler.postDelayed(this, 30000) // Refresh every 30 seconds
            }
        }
        chartHandler.postDelayed(chartRefreshRunnable!!, 30000)
    }

    private fun stopAutoRefresh() {
        chartRefreshRunnable?.let {
            chartHandler.removeCallbacks(it)
        }
        chartRefreshRunnable = null
    }

    // Extension function to convert dp to pixels
    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    override fun onResume() {
        super.onResume()
        updateProgressData()
        startCountdownTimer()
        updateLiveChart() // Refresh chart when fragment resumes
    }

    override fun onPause() {
        super.onPause()
        stopCountdownTimer()
        stopAutoRefresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCountdownTimer()
        stopAutoRefresh()
        chartHandler.removeCallbacksAndMessages(null)
    }
}