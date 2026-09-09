package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leads")
data class LeadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val phoneNumber: String,
    val normalizedNumber: String,
    val contactName: String,
    val source: String,
    val detectedAt: Long = System.currentTimeMillis(),
    val isSaved: Boolean = false,
    val status: String = "NEW LEAD"
) {
    val timestamp: Long get() = detectedAt
}
