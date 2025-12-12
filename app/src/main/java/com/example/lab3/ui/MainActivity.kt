package com.example.lab3

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab3.data.SharedPrefManager
import com.example.lab3.model.Habit
import com.example.lab3.ui.habits.HabitsAdapter
import com.example.lab3.ui.home.HomeFragment
import com.example.lab3.ui.mood.MoodFragment
import com.example.lab3.ui.settings.SettingsFragment
import com.example.lab3.utils.ReminderManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPrefManager: SharedPrefManager
    private lateinit var habitsAdapter: HabitsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddHabit: FloatingActionButton
    private lateinit var messageView: TextView
    private lateinit var habitSection: LinearLayout
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var progressCard: androidx.cardview.widget.CardView
    private val habitsList = mutableListOf<Habit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPrefManager = SharedPrefManager(this)

        setupUI()
        loadHabits()
        setupBottomNavigation()
        setupFAB()

        // Check if activity was started from reminder notification
        if (intent?.action == "SHOW_DIALOG_ACTION") {
            showReminderDialog()
        }

        // Start with Home fragment by default (changed from Habit)
        showHomeFragment()
    }

    private fun setupUI() {
        // Set current time
        try {
            val tvTime = findViewById<TextView>(R.id.tvTime)
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            tvTime.text = currentTime
        } catch (e: Exception) {
            println("Time TextView not found: ${e.message}")
        }

        // Setup views
        habitSection = findViewById(R.id.habit_section)
        fragmentContainer = findViewById(R.id.fragment_container)
        progressCard = findViewById(R.id.progress_card)

        // Setup RecyclerView
        recyclerView = findViewById(R.id.rv_habits)
        habitsAdapter = HabitsAdapter(habitsList) { habit, action ->
            when (action) {
                "increment" -> incrementHabitCompletion(habit)
                "decrement" -> decrementHabitCompletion(habit)
                "delete" -> deleteHabit(habit)
                "edit" -> showEditHabitDialog(habit)
            }
            updateTodayProgress()
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = habitsAdapter
        }

        // Setup FAB
        fabAddHabit = findViewById(R.id.fabAddHabit)

        // Setup Message View
        messageView = findViewById(R.id.message_view)

        updateTodayProgress()
    }

    private fun setupFAB() {
        fabAddHabit.setOnClickListener {
            showAddHabitDialog()
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    showHomeFragment()
                    true
                }
                R.id.navigation_habit -> {
                    showHabitFragment()
                    true
                }
                R.id.navigation_mood -> {
                    showMoodFragment()
                    true
                }
                R.id.navigation_setting -> {
                    showSettingFragment()
                    true
                }
                else -> false
            }
        }
    }

    private fun showHomeFragment() {
        // Hide habit section and show fragment container for Home Fragment
        habitSection.visibility = View.GONE
        fragmentContainer.visibility = View.VISIBLE
        fabAddHabit.visibility = View.GONE

        // Replace with Home Fragment
        val homeFragment = HomeFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, homeFragment)
            .commit()
    }

    private fun showHabitFragment() {
        // Show habit section
        habitSection.visibility = View.VISIBLE
        fragmentContainer.visibility = View.GONE
        fabAddHabit.visibility = View.VISIBLE
        progressCard.visibility = View.VISIBLE

        // Show habits list
        messageView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE

        // Reload habits to ensure data is fresh
        loadHabits()
    }

    private fun showMoodFragment() {
        // Hide habit section and show fragment container
        habitSection.visibility = View.GONE
        fragmentContainer.visibility = View.VISIBLE
        fabAddHabit.visibility = View.GONE

        // Replace with Mood Fragment
        val moodFragment = MoodFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, moodFragment)
            .commit()
    }

    private fun showSettingFragment() {
        // Hide habit section and show fragment container
        habitSection.visibility = View.GONE
        fragmentContainer.visibility = View.VISIBLE
        fabAddHabit.visibility = View.GONE

        // Replace with Settings Fragment
        val settingsFragment = SettingsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, settingsFragment)
            .commit()
    }

    // NEW: Show the reminder dialog
    private fun showReminderDialog() {
        ReminderManager.showReminderDialog(this)
    }

    // Handle back button to show Home fragment when in other fragments
    override fun onBackPressed() {
        if (fragmentContainer.visibility == View.VISIBLE) {
            // If we're in any fragment, go back to Home
            showHomeFragment()
            // Reset bottom navigation to Home tab
            findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.navigation_home
        } else {
            super.onBackPressed()
        }
    }

    private fun sortHabits() {
        val completions = sharedPrefManager.getTodayCompletions()

        habitsList.sortWith(compareBy { habit ->
            val current = completions[habit.id] ?: 0
            if (current >= habit.targetCount) 1 else 0
        })
    }

    private fun loadHabits() {
        habitsList.clear()
        val savedHabits = sharedPrefManager.getHabits()

        if (savedHabits.isEmpty()) {
            habitsList.addAll(listOf(
                Habit(
                    id = UUID.randomUUID().toString(),
                    name = "8 cups of water",
                    targetCount = 8,
                    description = "Daily water intake"
                ),
                Habit(
                    id = UUID.randomUUID().toString(),
                    name = "Eating main 3 meals",
                    targetCount = 3,
                    description = "Daily meals target"
                ),
                Habit(
                    id = UUID.randomUUID().toString(),
                    name = "20 squats 3 times",
                    targetCount = 3,
                    description = "Exercise routine"
                ),
                Habit(
                    id = UUID.randomUUID().toString(),
                    name = "Eat fruits",
                    targetCount = 5,
                    description = "Healthy eating"
                ),
                Habit(
                    id = UUID.randomUUID().toString(),
                    name = "Burn 550 Kcal",
                    targetCount = 550,
                    description = "Calorie burn target"
                ),
                Habit(
                    id = UUID.randomUUID().toString(),
                    name = "Sleep 8 hours",
                    targetCount = 100,
                    description = "Sleep quality"
                )
            ))
            sharedPrefManager.saveHabits(habitsList)
        } else {
            habitsList.addAll(savedHabits)
        }

        sortHabits()
        habitsAdapter.updateHabits(habitsList)
    }

    private fun showAddHabitDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_habit, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_habit_name)
        val etTarget = dialogView.findViewById<EditText>(R.id.et_habit_target)
        val etDescription = dialogView.findViewById<EditText>(R.id.et_habit_description)

        etTarget.setText("")

        AlertDialog.Builder(this)
            .setTitle("Add New Habit")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val targetText = etTarget.text.toString().trim()
                val description = etDescription.text.toString().trim()

                if (name.isNotEmpty() && targetText.isNotEmpty()) {
                    val target = targetText.toIntOrNull()
                    if (target != null && target > 0) {
                        val newHabit = Habit(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            targetCount = target,
                            description = description
                        )
                        habitsList.add(newHabit)
                        sharedPrefManager.saveHabits(habitsList)

                        sortHabits()
                        habitsAdapter.updateHabits(habitsList)
                        updateTodayProgress()

                        val position = habitsList.indexOfFirst { it.id == newHabit.id }
                        if (position != -1) {
                            recyclerView.smoothScrollToPosition(position)
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Please enter a valid positive number for target",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Please enter both habit name and target",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditHabitDialog(habit: Habit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_habit, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_habit_name)
        val etTarget = dialogView.findViewById<EditText>(R.id.et_habit_target)
        val etDescription = dialogView.findViewById<EditText>(R.id.et_habit_description)

        etName.setText(habit.name)
        etTarget.setText(habit.targetCount.toString())
        etDescription.setText(habit.description)

        AlertDialog.Builder(this)
            .setTitle("Edit Habit")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val targetText = etTarget.text.toString().trim()
                val description = etDescription.text.toString().trim()

                if (name.isNotEmpty() && targetText.isNotEmpty()) {
                    val target = targetText.toIntOrNull()
                    if (target != null && target > 0) {
                        val updatedHabit = habit.copy(
                            name = name,
                            targetCount = target,
                            description = description
                        )
                        val index = habitsList.indexOfFirst { it.id == habit.id }
                        if (index != -1) {
                            habitsList[index] = updatedHabit
                            sharedPrefManager.saveHabits(habitsList)

                            sortHabits()
                            habitsAdapter.updateHabits(habitsList)
                            updateTodayProgress()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Please enter a valid positive number for target",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Please enter both habit name and target",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun incrementHabitCompletion(habit: Habit) {
        val completions = sharedPrefManager.getTodayCompletions().toMutableMap()
        val current = completions[habit.id] ?: 0

        when {
            habit.name.contains("Kcal") -> {
                completions[habit.id] = minOf(current + 50, habit.targetCount)
            }
            habit.name.contains("Sleep") -> {
                completions[habit.id] = 91
            }
            else -> {
                completions[habit.id] = minOf(current + 1, habit.targetCount)
            }
        }

        sharedPrefManager.saveTodayCompletions(completions)

        sortHabits()
        habitsAdapter.updateHabits(habitsList)
        updateTodayProgress()
    }

    private fun decrementHabitCompletion(habit: Habit) {
        val completions = sharedPrefManager.getTodayCompletions().toMutableMap()
        val current = completions[habit.id] ?: 0
        if (current > 0) {
            when {
                habit.name.contains("Kcal") -> {
                    completions[habit.id] = maxOf(current - 50, 0)
                }
                else -> {
                    completions[habit.id] = current - 1
                }
            }
            sharedPrefManager.saveTodayCompletions(completions)

            sortHabits()
            habitsAdapter.updateHabits(habitsList)
            updateTodayProgress()
        }
    }

    private fun deleteHabit(habit: Habit) {
        AlertDialog.Builder(this)
            .setTitle("Delete Habit")
            .setMessage("Are you sure you want to delete '${habit.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                val index = habitsList.indexOfFirst { it.id == habit.id }
                if (index != -1) {
                    habitsList.removeAt(index)
                    sharedPrefManager.saveHabits(habitsList)

                    val completions = sharedPrefManager.getTodayCompletions().toMutableMap()
                    completions.remove(habit.id)
                    sharedPrefManager.saveTodayCompletions(completions)

                    sortHabits()
                    habitsAdapter.updateHabits(habitsList)
                    updateTodayProgress()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateTodayProgress() {
        val tvTodayProgress = findViewById<TextView>(R.id.tvTodayProgress)
        val completions = sharedPrefManager.getTodayCompletions()

        if (habitsList.isEmpty()) {
            tvTodayProgress.text = "0%"
            return
        }

        var totalProgress = 0
        habitsList.forEach { habit ->
            val current = completions[habit.id] ?: 0
            val progress = when {
                habit.targetCount > 0 -> (current.toFloat() / habit.targetCount.toFloat() * 100).toInt()
                else -> if (current > 0) 100 else 0
            }
            totalProgress += minOf(progress, 100)
        }

        val averageProgress = totalProgress / habitsList.size
        tvTodayProgress.text = "$averageProgress%"
    }

    private fun debugHabits() {
        println("=== DEBUG: Current Habits ===")
        val completions = sharedPrefManager.getTodayCompletions()
        habitsList.forEach { habit ->
            val current = completions[habit.id] ?: 0
            val isCompleted = current >= habit.targetCount
            println("Habit: ${habit.name}, Target: ${habit.targetCount}, Current: $current, Completed: $isCompleted")
        }
        println("=== END DEBUG ===")
    }

    private fun clearAllData() {
        val sharedPreferences = getSharedPreferences("WellNestPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        habitsList.clear()
        habitsAdapter.updateHabits(habitsList)
        updateTodayProgress()
        println("=== ALL DATA CLEARED ===")
    }
}