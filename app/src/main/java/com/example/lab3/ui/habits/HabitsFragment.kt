package com.example.lab3.ui.habits

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab3.data.SharedPrefManager
import com.example.lab3.model.Habit
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID

class HabitsFragment : Fragment() {

    private lateinit var sharedPrefManager: SharedPrefManager
    private lateinit var habitsAdapter: HabitsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var fabAddHabit: FloatingActionButton
    private val habitsList = mutableListOf<Habit>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Create layout programmatically to avoid XML issues
        return createLayoutProgrammatically(container)
    }

    private fun createLayoutProgrammatically(container: ViewGroup?): View {
        val context = requireContext()

        // Main RelativeLayout
        val mainLayout = androidx.appcompat.widget.LinearLayoutCompat(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = androidx.appcompat.widget.LinearLayoutCompat.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        // RecyclerView
        recyclerView = RecyclerView(context).apply {
            id = android.R.id.list
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        mainLayout.addView(recyclerView)

        // Empty State
        emptyState = LinearLayout(context).apply {
            id = android.R.id.empty
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            visibility = View.GONE
        }

        val emptyIcon = TextView(context).apply {
            text = "📝" // Using emoji as icon
            textSize = 48f
            gravity = android.view.Gravity.CENTER
        }
        emptyState.addView(emptyIcon)

        val emptyTitle = TextView(context).apply {
            text = "No habits yet"
            textSize = 18f
            setTextColor(android.graphics.Color.parseColor("#2c3e50"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
            }
        }
        emptyState.addView(emptyTitle)

        val emptySubtitle = TextView(context).apply {
            text = "Add your first habit to get started"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#7f8c8d"))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
            }
        }
        emptyState.addView(emptySubtitle)

        mainLayout.addView(emptyState)

        // FAB Button
        fabAddHabit = FloatingActionButton(context).apply {
            id = android.R.id.button1
            layoutParams = androidx.appcompat.widget.LinearLayoutCompat.LayoutParams(
                androidx.appcompat.widget.LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
                androidx.appcompat.widget.LinearLayoutCompat.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.END or android.view.Gravity.BOTTOM
                setMargins(0, 0, 0, 16)
            }
            setImageResource(android.R.drawable.ic_input_add)
            setOnClickListener {
                showAddHabitDialog()
            }
        }
        mainLayout.addView(fabAddHabit)

        return mainLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPrefManager = SharedPrefManager(requireContext())
        setupRecyclerView()
        loadHabits()
    }

    private fun setupRecyclerView() {
        habitsAdapter = HabitsAdapter(habitsList) { habit, action ->
            when (action) {
                "increment" -> incrementHabitCompletion(habit)
                "decrement" -> decrementHabitCompletion(habit)
                "delete" -> deleteHabit(habit)
                "edit" -> showEditHabitDialog(habit)
            }
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = habitsAdapter
        }
    }

    private fun loadHabits() {
        habitsList.clear()
        val savedHabits = sharedPrefManager.getHabits()

        if (savedHabits.isEmpty()) {
            // Add sample habits for testing
            habitsList.addAll(listOf(
                Habit(
                    id = UUID.randomUUID().toString(),
                    name = "Drinking water",
                    targetCount = 110,
                    description = "Stay hydrated"
                ),
                Habit(
                    id = UUID.randomUUID().toString(),
                    name = "Eating main 3 meals",
                    targetCount = 13,
                    description = "Weekly meal target"
                ),
                Habit(
                    id = UUID.randomUUID().toString(),
                    name = "Exercise",
                    targetCount = 7,
                    description = "Daily exercise"
                ),
                Habit(
                    id = UUID.randomUUID().toString(),
                    name = "Reading",
                    targetCount = 5,
                    description = "Reading time"
                )
            ))
            sharedPrefManager.saveHabits(habitsList)
        } else {
            habitsList.addAll(savedHabits)
        }

        habitsAdapter.updateHabits(habitsList)
        updateEmptyState()
    }

    private fun showAddHabitDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            com.example.lab3.R.layout.dialog_add_edit_habit,
            null
        )

        val etName = dialogView.findViewById<TextInputEditText>(
            com.example.lab3.R.id.et_habit_name
        )
        val etTarget = dialogView.findViewById<TextInputEditText>(
            com.example.lab3.R.id.et_habit_target
        )
        val etDescription = dialogView.findViewById<TextInputEditText>(
            com.example.lab3.R.id.et_habit_description
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Habit")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val target = etTarget.text.toString().toIntOrNull() ?: 1
                val description = etDescription.text.toString().trim()

                if (name.isNotEmpty()) {
                    val newHabit = Habit(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        targetCount = target,
                        description = description
                    )
                    habitsList.add(newHabit)
                    sharedPrefManager.saveHabits(habitsList)
                    habitsAdapter.updateHabits(habitsList)
                    updateEmptyState()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditHabitDialog(habit: Habit) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            com.example.lab3.R.layout.dialog_add_edit_habit,
            null
        )

        val etName = dialogView.findViewById<TextInputEditText>(
            com.example.lab3.R.id.et_habit_name
        )
        val etTarget = dialogView.findViewById<TextInputEditText>(
            com.example.lab3.R.id.et_habit_target
        )
        val etDescription = dialogView.findViewById<TextInputEditText>(
            com.example.lab3.R.id.et_habit_description
        )

        // Pre-fill the fields with existing habit data
        etName.setText(habit.name)
        etTarget.setText(habit.targetCount.toString())
        etDescription.setText(habit.description)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Habit")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val target = etTarget.text.toString().toIntOrNull() ?: 1
                val description = etDescription.text.toString().trim()

                if (name.isNotEmpty()) {
                    val updatedHabit = habit.copy(
                        name = name,
                        targetCount = target,
                        description = description
                    )
                    val index = habitsList.indexOfFirst { it.id == habit.id }
                    if (index != -1) {
                        habitsList[index] = updatedHabit
                        sharedPrefManager.saveHabits(habitsList)
                        habitsAdapter.updateHabits(habitsList)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun incrementHabitCompletion(habit: Habit) {
        val completions = sharedPrefManager.getTodayCompletions().toMutableMap()
        val current = completions[habit.id] ?: 0
        completions[habit.id] = current + 1
        sharedPrefManager.saveTodayCompletions(completions)
        habitsAdapter.updateHabits(habitsList)
    }

    private fun decrementHabitCompletion(habit: Habit) {
        val completions = sharedPrefManager.getTodayCompletions().toMutableMap()
        val current = completions[habit.id] ?: 0
        if (current > 0) {
            completions[habit.id] = current - 1
            sharedPrefManager.saveTodayCompletions(completions)
            habitsAdapter.updateHabits(habitsList)
        }
    }

    private fun deleteHabit(habit: Habit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Habit")
            .setMessage("Are you sure you want to delete '${habit.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                val index = habitsList.indexOfFirst { it.id == habit.id }
                if (index != -1) {
                    habitsList.removeAt(index)
                    sharedPrefManager.saveHabits(habitsList)

                    // Remove from completions as well
                    val completions = sharedPrefManager.getTodayCompletions().toMutableMap()
                    completions.remove(habit.id)
                    sharedPrefManager.saveTodayCompletions(completions)

                    habitsAdapter.updateHabits(habitsList)
                    updateEmptyState()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateEmptyState() {
        if (habitsList.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}