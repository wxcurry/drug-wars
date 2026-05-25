package com.neoncartel.drugwars.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "save_slots")
data class SaveSlotEntity(
    @PrimaryKey val slotId: String,
    val payload: String,
    val updatedAtMillis: Long,
)
