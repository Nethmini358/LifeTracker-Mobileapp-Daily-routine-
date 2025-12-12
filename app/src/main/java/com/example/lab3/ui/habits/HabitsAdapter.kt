package com.example.lab3.ui.habits

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lab3.R
import com.example.lab3.data.SharedPrefManager
import com.example.lab3.model.Habit

class HabitsAdapter(
    private var habits: List<Habit>,
    private val onHabitAction: (Habit, String) -> Unit
) : RecyclerView.Adapter<HabitsAdapter.HabitViewHolder>() {

    private lateinit var sharedPrefManager: SharedPrefManager

    class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.habitTitle)
        val progressTextView: TextView = itemView.findViewById(R.id.progressText)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        val addButton: Button = itemView.findViewById(R.id.addButton)
        val subtitleTextView: TextView = itemView.findViewById(R.id.tvSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]

        if (!::sharedPrefManager.isInitialized) {
            sharedPrefManager = SharedPrefManager(holder.itemView.context)
        }

        val completions = sharedPrefManager.getTodayCompletions()
        val currentCount = completions[habit.id] ?: 0

        holder.titleTextView.text = habit.name

        // Format progress text based on habit type
        val progressText = when {
            habit.name.contains("Kcal") -> "$currentCount/${habit.targetCount}"
            habit.name.contains("Sleep") -> "${currentCount}%"
            else -> "$currentCount/${habit.targetCount}"
        }
        holder.progressTextView.text = progressText

        // Calculate progress percentage
        val progress = when {
            habit.name.contains("Sleep") -> currentCount
            habit.targetCount > 0 -> (currentCount.toFloat() / habit.targetCount.toFloat() * 100).toInt()
            else -> 0
        }
        holder.progressBar.progress = minOf(progress, 100)

        // Set progress bar color
        val progressColor = when {
            progress >= 100 -> android.graphics.Color.parseColor("#27ae60") // Green
            progress > 0 -> android.graphics.Color.parseColor("#4facfe") // Blue
            else -> android.graphics.Color.parseColor("#e0e0e0") // Gray
        }
        holder.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(progressColor)

        // Set up button click listener
        holder.addButton.setOnClickListener {
            onHabitAction(habit, "increment")
        }

        // Long press for decrement
        holder.addButton.setOnLongClickListener {
            onHabitAction(habit, "decrement")
            true
        }

        // Click on item for edit
        holder.itemView.setOnClickListener {
            onHabitAction(habit, "edit")
        }

        // Long press on item for delete
        holder.itemView.setOnLongClickListener {
            onHabitAction(habit, "delete")
            true
        }
    }

    override fun getItemCount(): Int = habits.size

    fun updateHabits(newHabits: List<Habit>) {
        habits = newHabits
        notifyDataSetChanged()
    }
}