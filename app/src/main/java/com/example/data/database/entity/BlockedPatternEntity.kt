package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_patterns")
data class BlockedPatternEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pattern: String,
    val matchType: String = MATCH_STARTS_WITH,
    val label: String = "",
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val MATCH_STARTS_WITH = "STARTS_WITH"
        const val MATCH_CONTAINS = "CONTAINS"
        const val MATCH_EXACT = "EXACT"
        const val MATCH_REGEX = "REGEX"
    }
}
