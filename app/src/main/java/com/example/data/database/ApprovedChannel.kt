package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "approved_channels")
data class ApprovedChannel(
    @PrimaryKey val channelId: String, // e.g. "UC..."
    val customName: String,            // Custom Hebrew/English name for the channel
    val officialName: String,          // Official YouTube channel name
    val avatarUrl: String = "",        // Avatar image URL (optional)
    val description: String = "",      // Short description of the content
    val addedAt: Long = System.currentTimeMillis()
)
