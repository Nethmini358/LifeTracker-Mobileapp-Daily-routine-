package com.example.lab3.ui.habits

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HabitAdapter(private val habits: List<Habit>) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(android.R.id.text1)
        val progressTextView: TextView = itemView.findViewById(android.R.id.text2)
        val progressBar: ProgressBar = itemView.findViewById(android.R.id.progress)
        val addButton: Button = itemView.findViewById(android.R.id.button1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = createHabitItemLayout(parent)
        return HabitViewHolder(view)
    }

    private fun createHabitItemLayout(parent: ViewGroup): View {
        val context = parent.context

        // Main container
        val mainContainer = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
            setBackgroundColor(Color.parseColor("#f8f9fa"))
        }

        // Habit title
        val titleTextView = TextView(context).apply {
            id = android.R.id.text1
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 25
            }
            text = "Habit Title"
            textSize = 18f
            setTextColor(Color.parseColor("#2c3e50"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        mainContainer.addView(titleTextView)

        // Progress container (horizontal layout)
        val progressContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        // Progress text
        val progressTextView = TextView(context).apply {
            id = android.R.id.text2
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = "0/10"
            textSize = 16f
            setTextColor(Color.parseColor("#3498db"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        progressContainer.addView(progressTextView)

        // Progress bar
        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            id = android.R.id.progress
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            ).apply {
                setMargins(25, 0, 25, 0)
                height = 25
            }
            max = 100
            progress = 0
        }
        progressContainer.addView(progressBar)

        // Add button
        val addButton = Button(context).apply {
            id = android.R.id.button1
            layoutParams = LinearLayout.LayoutParams(80, 80)
            text = "+"
            textSize = 20f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#4facfe"))
        }
        progressContainer.addView(addButton)

        mainContainer.addView(progressContainer)

        return mainContainer
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]

        holder.titleTextView.text = habit.title
        holder.progressTextView.text = "${habit.currentCount}/${habit.targetCount}"

        // Update progress bar
        val progress = (habit.currentCount.toFloat() / habit.targetCount.toFloat() * 100).toInt()
        holder.progressBar.progress = progress

        // Set progress bar color based on completion
        val progressColor = when {
            habit.currentCount >= habit.targetCount -> Color.parseColor("#27ae60") // Green
            habit.currentCount > 0 -> Color.parseColor("#3498db") // Blue
            else -> Color.parseColor("#e0e0e0") // Gray
        }

        holder.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(progressColor)

        // Set up add button
        holder.addButton.setOnClickListener {
            if (habit.currentCount < habit.targetCount) {
                habit.currentCount++
                notifyItemChanged(position)

                // Button animation
                holder.addButton.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100)
                    .withEndAction {
                        holder.addButton.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }.start()
            }
        }
    }

    override fun getItemCount(): Int = habits.size
}

// Simple Habit data class
data class Habit(
    val title: String,
    val targetCount: Int,
    var currentCount: Int = 0
)