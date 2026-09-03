package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val contactName: String,
    val source: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "Saved", "Duplicate", "Phone number unavailable", "Removed", "Failed"
    val details: String = ""
)
