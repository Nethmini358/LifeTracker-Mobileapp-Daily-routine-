package com.example.lab3.model

data class Settings(
    val waterReminderEnabled: Boolean = false,
    val waterReminderInterval: Int = 60, // minutes
    val enableShakeDetection: Boolean = true,
    val theme: String = "light", // light, dark, system
    val notificationEnabled: Boolean = true
)