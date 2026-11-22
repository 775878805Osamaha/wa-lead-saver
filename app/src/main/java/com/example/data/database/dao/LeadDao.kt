package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.database.entity.LeadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeadDao {
    @Transaction
    @Query("SELECT * FROM leads ORDER BY createdAt DESC, detectedAt DESC")
    fun getAllLeads(): Flow<List<LeadEntity>>

    // Flutter bridge queries: bounded keyset pages (createdAt + id), not
    // an unbounded observation of every customer on each UI refresh.
    @Query("""
        SELECT COUNT(*) FROM leads
        WHERE (:reviewOnly = 0 OR isSaved = 0)
          AND (:status = '' OR UPPER(status) = UPPER(:status))
          AND (
            :query = ''
            OR displayName LIKE '%' || :query || '%' ESCAPE '\'
            OR contactName LIKE '%' || :query || '%' ESCAPE '\'
            OR phoneNumber LIKE '%' || :query || '%' ESCAPE '\'
            OR normalizedNumber LIKE '%' || :query || '%' ESCAPE '\'
          )
    """)
    suspend fun getFilteredLeadsCount(
        query: String,
        status: String,
        reviewOnly: Boolean
    ): Int

    @Query("""
        SELECT * FROM leads
        WHERE (:reviewOnly = 0 OR isSaved = 0)
          AND (:status = '' OR UPPER(status) = UPPER(:status))
          AND (
            :query = ''
            OR displayName LIKE '%' || :query || '%' ESCAPE '\'
            OR contactName LIKE '%' || :query || '%' ESCAPE '\'
            OR phoneNumber LIKE '%' || :query || '%' ESCAPE '\'
            OR normalizedNumber LIKE '%' || :query || '%' ESCAPE '\'
          )
          AND (:beforeCreatedAt IS NULL
               OR createdAt < :beforeCreatedAt
               OR (createdAt = :beforeCreatedAt AND id < :beforeId))
        ORDER BY createdAt DESC, id DESC
        LIMIT :pageSize
    """)
    suspend fun getLeadsPage(
        query: String,
        status: String,
        reviewOnly: Boolean,
        beforeCreatedAt: Long?,
        beforeId: Long?,
        pageSize: Int
    ): List<LeadEntity>

    @Query("SELECT * FROM leads WHERE id = :id LIMIT 1")
    suspend fun getLeadById(id: Long): LeadEntity?

    @Query("SELECT * FROM leads WHERE normalizedNumber = :normalizedNumber LIMIT 1")
    suspend fun findLeadByNormalizedNumber(normalizedNumber: String): LeadEntity?

    @Query("SELECT * FROM leads WHERE (normalizedNumber = :normalizedNumber OR phoneNumber = :phoneNumber) AND id != :excludeId LIMIT 1")
    suspend fun findDuplicateExcludingId(normalizedNumber: String, phoneNumber: String, excludeId: Long): LeadEntity?

    @Transaction
    @Query("SELECT * FROM leads WHERE isSaved = 0 ORDER BY createdAt DESC, detectedAt DESC")
    fun getQueuedLeads(): Flow<List<LeadEntity>>

    @Query("SELECT COUNT(*) FROM leads")
    fun getTotalLeadsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM leads WHERE isSaved = 1")
    fun getTotalSavedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM leads WHERE isSaved = 0")
    fun getQueueCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM leads WHERE isSaved = 0 ORDER BY createdAt DESC, detectedAt DESC")
    suspend fun getQueuedLeadsSnapshot(): List<LeadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: LeadEntity): Long

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeads(leads: List<LeadEntity>): List<Long>

    @Update
    suspend fun updateLead(lead: LeadEntity)

    @Transaction
    @Update
    suspend fun updateLeads(leads: List<LeadEntity>)

    @Query("UPDATE leads SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateLeadStatus(id: Long, status: String, updatedAt: Long)

    @Query("UPDATE leads SET notes = :notes, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateLeadNotes(id: Long, notes: String, updatedAt: Long)

    @Query("UPDATE leads SET tags = :tags, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateLeadTags(id: Long, tags: String, updatedAt: Long)

    @Query("UPDATE leads SET lastContactAt = :lastContactAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateLastContactAt(id: Long, lastContactAt: Long, updatedAt: Long)

    @Query("UPDATE leads SET isSaved = 1, status = 'SAVED', updatedAt = :updatedAt WHERE id = :id")
    suspend fun markAsSaved(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE leads SET isSaved = 1, status = 'SAVED', contactName = :contactName, displayName = :contactName, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markAsSavedWithName(id: Long, contactName: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM leads WHERE id = :id")
    suspend fun deleteLeadById(id: Long)

    @Query("DELETE FROM leads WHERE isSaved = 0")
    suspend fun clearQueue()

    @Query("DELETE FROM leads")
    suspend fun clearAllLeads()
}
