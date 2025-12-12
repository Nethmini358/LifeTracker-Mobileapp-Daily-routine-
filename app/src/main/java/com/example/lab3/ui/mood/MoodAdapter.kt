package com.example.lab3.ui.mood

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lab3.R
import com.example.lab3.model.Mood

class MoodAdapter(
    private val moodEntries: List<Mood>,
    private val onShareClick: (Mood) -> Unit
) : RecyclerView.Adapter<MoodAdapter.MoodViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mood_entry, parent, false)
        return MoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoodViewHolder, position: Int) {
        val moodEntry = moodEntries[position]
        holder.bind(moodEntry)
    }

    override fun getItemCount(): Int = moodEntries.size

    inner class MoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMoodEmoji: TextView = itemView.findViewById(R.id.tvMoodEmoji)
        private val tvMoodType: TextView = itemView.findViewById(R.id.tvMoodType)
        private val tvMoodDate: TextView = itemView.findViewById(R.id.tvMoodDate)
        private val tvMoodNotes: TextView = itemView.findViewById(R.id.tvMoodNotes)
        private val btnShareMood: ImageButton = itemView.findViewById(R.id.btnShareMood)

        fun bind(moodEntry: Mood) {
            tvMoodEmoji.text = moodEntry.emoji
            tvMoodType.text = moodEntry.moodType
            tvMoodDate.text = moodEntry.getDisplayDate()

            // Handle empty notes
            if (moodEntry.notes.isNotEmpty()) {
                tvMoodNotes.text = moodEntry.notes
                tvMoodNotes.visibility = View.VISIBLE
            } else {
                tvMoodNotes.visibility = View.GONE
            }

            btnShareMood.setOnClickListener {
                onShareClick(moodEntry)
            }

            // Add click listener to the entire item if needed
            itemView.setOnClickListener {
                // You can add more actions when a mood entry is clicked
            }
        }
    }
}