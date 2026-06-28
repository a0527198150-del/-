package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM approved_channels ORDER BY addedAt DESC")
    fun getAllApprovedChannels(): Flow<List<ApprovedChannel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ApprovedChannel)

    @Delete
    suspend fun deleteChannel(channel: ApprovedChannel)

    @Query("SELECT * FROM supervisor_settings WHERE id = 1 LIMIT 1")
    suspend fun getSupervisorSettings(): SupervisorSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupervisorSettings(settings: SupervisorSettings)
}
