package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.LeadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeadDao {
    @Query("SELECT * FROM leads WHERE isSaved = 0 ORDER BY detectedAt DESC")
    fun getQueuedLeads(): Flow<List<LeadEntity>>

    @Query("SELECT * FROM leads ORDER BY detectedAt DESC")
    fun getAllLeads(): Flow<List<LeadEntity>>

    @Query("SELECT COUNT(*) FROM leads WHERE isSaved = 1")
    fun getTotalSavedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM leads WHERE isSaved = 0")
    fun getQueueCount(): Flow<Int>

    @Query("SELECT * FROM leads WHERE isSaved = 0 ORDER BY detectedAt DESC")
    suspend fun getQueuedLeadsSnapshot(): List<LeadEntity>

    @Query("SELECT * FROM leads WHERE normalizedNumber = :normalizedNumber LIMIT 1")
    suspend fun findLeadByNormalizedNumber(normalizedNumber: String): LeadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: LeadEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeads(leads: List<LeadEntity>): List<Long>

    @Update
    suspend fun updateLead(lead: LeadEntity)

    @Query("UPDATE leads SET isSaved = 1, status = 'SAVED' WHERE id = :id")
    suspend fun markAsSaved(id: Long)

    @Query("DELETE FROM leads WHERE id = :id")
    suspend fun deleteLeadById(id: Long)

    @Query("DELETE FROM leads WHERE isSaved = 0")
    suspend fun clearQueue()
}
