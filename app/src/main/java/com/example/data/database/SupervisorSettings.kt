package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supervisor_settings")
data class SupervisorSettings(
    @PrimaryKey val id: Int = 1,
    val supervisorPin: String = "1234", // Default supervisor passcode is 1234
    val isFilterStrict: Boolean = true
)
