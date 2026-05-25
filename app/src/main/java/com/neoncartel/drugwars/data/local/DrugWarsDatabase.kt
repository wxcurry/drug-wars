package com.neoncartel.drugwars.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SaveSlotEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DrugWarsDatabase : RoomDatabase() {
    abstract fun saveSlotDao(): SaveSlotDao
}
