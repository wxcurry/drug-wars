package com.neoncartel.drugwars.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SaveSlotDao {
    @Query("SELECT * FROM save_slots WHERE slotId = :slotId")
    fun observe(slotId: String = SaveSlotRepository.PRIMARY_SLOT): Flow<SaveSlotEntity?>

    @Query("SELECT * FROM save_slots WHERE slotId = :slotId")
    suspend fun load(slotId: String = SaveSlotRepository.PRIMARY_SLOT): SaveSlotEntity?

    @Upsert
    suspend fun upsert(entity: SaveSlotEntity)

    @Query("DELETE FROM save_slots WHERE slotId = :slotId")
    suspend fun delete(slotId: String = SaveSlotRepository.PRIMARY_SLOT)
}
