package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leads")
data class LeadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val normalizedNumber: String,
    val contactName: String,
    val source: String, // "WhatsApp", "WhatsApp Business", "Photo scan"
    val detectedAt: Long = System.currentTimeMillis(),
    val isSaved: Boolean = false,
    val status: String = "NEW LEAD"
)
