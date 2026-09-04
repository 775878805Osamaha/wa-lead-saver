package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.BlockedPatternEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedPatternDao {

    @Query("SELECT * FROM blocked_patterns ORDER BY createdAt DESC")
    fun getAllBlockedPatterns(): Flow<List<BlockedPatternEntity>>

    @Query("SELECT * FROM blocked_patterns WHERE isEnabled = 1")
    suspend fun getActiveBlockedPatterns(): List<BlockedPatternEntity>

    @Query("SELECT COUNT(*) FROM blocked_patterns WHERE isEnabled = 1")
    fun getActiveCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: BlockedPatternEntity): Long

    @Update
    suspend fun updatePattern(pattern: BlockedPatternEntity)

    @Query("UPDATE blocked_patterns SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setEnabled(id: Long, isEnabled: Boolean)

    @Delete
    suspend fun deletePattern(pattern: BlockedPatternEntity)

    @Query("DELETE FROM blocked_patterns WHERE id = :id")
    suspend fun deletePatternById(id: Long)
}
