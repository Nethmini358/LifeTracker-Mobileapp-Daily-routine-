package com.example.lab3.ui.mood

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab3.R
import com.example.lab3.data.SharedPrefManager
import com.example.lab3.model.Mood
import com.example.lab3.model.MoodEmoji
import com.google.android.material.textfield.TextInputEditText

class MoodFragment : Fragment() {

    private lateinit var emojiContainer: LinearLayout
    private lateinit var selectedEmojiContainer: LinearLayout
    private lateinit var tvSelectedEmoji: TextView
    private lateinit var tvSelectedMood: TextView
    private lateinit var etMoodNotes: TextInputEditText
    private lateinit var btnSaveMood: Button
    private lateinit var rvMoodHistory: RecyclerView
    private lateinit var tvClearHistory: TextView

    private var selectedEmoji: String = ""
    private var selectedMood: String = ""
    private val moodEntries = mutableListOf<Mood>()
    private lateinit var moodAdapter: MoodAdapter
    private lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mood, container, false)

        // Initialize SharedPrefManager
        sharedPrefManager = SharedPrefManager(requireContext())

        initViews(view)
        setupEmojiSelector()
        setupRecyclerView()
        loadMoodHistory()
        setupClickListeners()
        return view
    }

    private fun initViews(view: View) {
        emojiContainer = view.findViewById(R.id.emojiContainer)
        selectedEmojiContainer = view.findViewById(R.id.selectedEmojiContainer)
        tvSelectedEmoji = view.findViewById(R.id.tvSelectedEmoji)
        tvSelectedMood = view.findViewById(R.id.tvSelectedMood)
        etMoodNotes = view.findViewById(R.id.etMoodNotes)
        btnSaveMood = view.findViewById(R.id.btnSaveMood)
        rvMoodHistory = view.findViewById(R.id.rvMoodHistory)
        tvClearHistory = view.findViewById(R.id.tvClearHistory)
    }

    private fun setupEmojiSelector() {
        emojiContainer.removeAllViews()

        Mood.availableMoods.forEach { moodEmoji ->
            val emojiView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_emoji, emojiContainer, false)

            val tvEmoji = emojiView.findViewById<TextView>(R.id.tvEmoji)
            val tvMoodLabel = emojiView.findViewById<TextView>(R.id.tvMoodLabel)

            tvEmoji.text = moodEmoji.emoji
            tvMoodLabel.text = moodEmoji.label

            emojiView.setOnClickListener {
                selectMood(moodEmoji.emoji, moodEmoji.label)
            }

            emojiContainer.addView(emojiView)
        }
    }

    private fun selectMood(emoji: String, mood: String) {
        selectedEmoji = emoji
        selectedMood = mood

        tvSelectedEmoji.text = emoji
        tvSelectedMood.text = mood
        selectedEmojiContainer.visibility = View.VISIBLE
        btnSaveMood.isEnabled = true

        // Highlight selected emoji
        for (i in 0 until emojiContainer.childCount) {
            val child = emojiContainer.getChildAt(i)
            val tvEmoji = child.findViewById<TextView>(R.id.tvEmoji)
            val tvMoodLabel = child.findViewById<TextView>(R.id.tvMoodLabel)

            if (tvEmoji.text == emoji) {
                tvEmoji.setBackgroundResource(R.drawable.selected_emoji_background)
                tvMoodLabel.setTextColor(requireContext().getColor(android.R.color.black))
            } else {
                tvEmoji.setBackgroundResource(R.drawable.mood_emoji_background)
                tvMoodLabel.setTextColor(requireContext().getColor(android.R.color.darker_gray))
            }
        }
    }

    private fun setupRecyclerView() {
        moodAdapter = MoodAdapter(moodEntries) { moodEntry ->
            shareMoodEntry(moodEntry)
        }
        rvMoodHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = moodAdapter
        }
    }

    private fun setupClickListeners() {
        btnSaveMood.setOnClickListener {
            saveMoodEntry()
        }

        tvClearHistory.setOnClickListener {
            clearMoodHistory()
        }
    }

    private fun saveMoodEntry() {
        if (selectedEmoji.isEmpty()) {
            showMessage("Please select a mood first")
            return
        }

        val notes = etMoodNotes.text.toString().trim()

        val moodEntry = Mood(
            emoji = selectedEmoji,
            moodType = selectedMood,
            notes = notes
        )

        moodEntries.add(0, moodEntry)
        moodAdapter.notifyItemInserted(0)

        saveMoodHistory()
        resetForm()
        showMessage("Mood saved successfully!")
        rvMoodHistory.smoothScrollToPosition(0)
    }

    private fun resetForm() {
        selectedEmoji = ""
        selectedMood = ""
        selectedEmojiContainer.visibility = View.GONE
        etMoodNotes.text?.clear()
        btnSaveMood.isEnabled = false

        for (i in 0 until emojiContainer.childCount) {
            val child = emojiContainer.getChildAt(i)
            val tvEmoji = child.findViewById<TextView>(R.id.tvEmoji)
            val tvMoodLabel = child.findViewById<TextView>(R.id.tvMoodLabel)
            tvEmoji.setBackgroundResource(R.drawable.mood_emoji_background)
            tvMoodLabel.setTextColor(requireContext().getColor(android.R.color.darker_gray))
        }
    }

    private fun shareMoodEntry(moodEntry: Mood) {
        val shareText = """
            My Mood Entry:
            ${moodEntry.emoji} ${moodEntry.moodType}
            ${moodEntry.notes.takeIf { it.isNotEmpty() } ?: "No additional notes"}
            Recorded on: ${moodEntry.getFullFormattedDateTime()}
        """.trimIndent()

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Mood Entry"))
    }

    private fun clearMoodHistory() {
        if (moodEntries.isEmpty()) {
            showMessage("No mood history to clear")
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Clear Mood History")
            .setMessage("Are you sure you want to clear all mood entries?")
            .setPositiveButton("Clear") { _, _ ->
                moodEntries.clear()
                moodAdapter.notifyDataSetChanged()
                saveMoodHistory()
                showMessage("Mood history cleared")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // FIXED: Use SharedPrefManager instead of separate SharedPreferences
    private fun saveMoodHistory() {
        sharedPrefManager.saveMoods(moodEntries)
    }

    // FIXED: Use SharedPrefManager instead of separate SharedPreferences
    private fun loadMoodHistory() {
        val loadedMoods = sharedPrefManager.getMoods()
        moodEntries.clear()
        moodEntries.addAll(loadedMoods.sortedByDescending { it.timestamp })
        moodAdapter.notifyDataSetChanged()
    }

    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}