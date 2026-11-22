package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.database.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

data class HistoryCounts(
    val total: Int,
    val duplicates: Int,
    val groupExcluded: Int,
    val invalidOrExcluded: Int
)

@Dao
interface HistoryDao {
    // Legacy export is intentionally separate from the on-screen paged UI.
    @Transaction
    @Query("SELECT * FROM history ORDER BY timestamp DESC, id DESC")
    suspend fun getAllHistoryList(): List<HistoryEntity>

    @Query("SELECT * FROM history ORDER BY timestamp DESC, id DESC LIMIT 6")
    fun observeRecentHistory(): Flow<List<HistoryEntity>>

    @Query("""
        SELECT COUNT(*) AS total,
               COALESCE(SUM(CASE WHEN LOWER(status) = 'duplicate' THEN 1 ELSE 0 END), 0) AS duplicates,
               COALESCE(SUM(CASE WHEN LOWER(status) LIKE '%group%' THEN 1 ELSE 0 END), 0) AS groupExcluded,
               COALESCE(SUM(CASE WHEN LOWER(status) IN ('excluded', 'invalid number')
                        OR LOWER(status) LIKE '%system%' THEN 1 ELSE 0 END), 0) AS invalidOrExcluded
        FROM history
    """)
    fun observeCaptureCounts(): Flow<HistoryCounts>

    @Query("""
        SELECT COUNT(*) FROM history WHERE
            :filter = 'ALL'
            OR (:filter = 'SAVED' AND LOWER(status) = 'saved')
            OR (:filter = 'NEEDS_REVIEW' AND LOWER(status) = 'needs review')
            OR (:filter = 'GROUP_EXCLUDED' AND LOWER(status) LIKE '%group%')
            OR (:filter = 'DUPLICATE' AND LOWER(status) LIKE '%duplicate%')
            OR (:filter = 'EXCLUDED' AND (
                 LOWER(status) IN ('excluded', 'invalid number')
                 OR LOWER(status) LIKE '%system%'
                 OR LOWER(status) LIKE '%unavailable%'))
    """)
    suspend fun getFilteredCount(filter: String): Int

    // Keyset pagination: stable when multiple events have identical timestamps.
    @Query("""
        SELECT * FROM history
        WHERE (
            :filter = 'ALL'
            OR (:filter = 'SAVED' AND LOWER(status) = 'saved')
            OR (:filter = 'NEEDS_REVIEW' AND LOWER(status) = 'needs review')
            OR (:filter = 'GROUP_EXCLUDED' AND LOWER(status) LIKE '%group%')
            OR (:filter = 'DUPLICATE' AND LOWER(status) LIKE '%duplicate%')
            OR (:filter = 'EXCLUDED' AND (
                 LOWER(status) IN ('excluded', 'invalid number')
                 OR LOWER(status) LIKE '%system%'
                 OR LOWER(status) LIKE '%unavailable%'))
        )
        AND (:beforeTimestamp IS NULL
             OR timestamp < :beforeTimestamp
             OR (timestamp = :beforeTimestamp AND id < :beforeId))
        ORDER BY timestamp DESC, id DESC
        LIMIT :pageSize
    """)
    suspend fun getHistoryPage(
        filter: String,
        beforeTimestamp: Long?,
        beforeId: Long?,
        pageSize: Int
    ): List<HistoryEntity>

    @Query("SELECT COUNT(*) FROM history WHERE status = 'Saved'")
    fun getSavedHistoryCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryEntity): Long

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryList(items: List<HistoryEntity>): List<Long>

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}
