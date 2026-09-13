package com.example.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.LeadSource
import com.example.data.model.LeadStatus

@Entity(
    tableName = "leads",
    indices = [
        Index(value = ["phoneNumber"]),
        Index(value = ["status"]),
        Index(value = ["source"]),
        Index(value = ["createdAt"])
    ]
)
data class LeadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "phoneNumber")
    val phoneNumber: String,

    @ColumnInfo(name = "normalizedNumber")
    val normalizedNumber: String = "",

    @ColumnInfo(name = "displayName")
    val displayName: String = "",

    @ColumnInfo(name = "contactName")
    val contactName: String = displayName,

    @ColumnInfo(name = "source")
    val source: String = LeadSource.MANUAL.name,

    @ColumnInfo(name = "status")
    val status: String = LeadStatus.NEW.name,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "tags")
    val tags: String = "",

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = createdAt,

    @ColumnInfo(name = "lastContactAt")
    val lastContactAt: Long? = null,

    @ColumnInfo(name = "detectedAt")
    val detectedAt: Long = createdAt,

    @ColumnInfo(name = "isSaved")
    val isSaved: Boolean = false
) {
    val timestamp: Long get() = createdAt

    val effectiveName: String
        get() = displayName.ifBlank { contactName.ifBlank { phoneNumber } }

    fun getTagList(): List<String> {
        if (tags.isBlank()) return emptyList()
        return tags.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
