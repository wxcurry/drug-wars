package com.neoncartel.drugwars.app

import android.content.Context
import androidx.room.Room
import com.neoncartel.drugwars.data.local.DrugWarsDatabase
import com.neoncartel.drugwars.data.local.SaveSlotRepository
import com.neoncartel.drugwars.domain.system.GameEngine

class AppContainer(context: Context) {
    private val database: DrugWarsDatabase = Room.databaseBuilder(
        context.applicationContext,
        DrugWarsDatabase::class.java,
        "drug-wars.db",
    ).build()

    val engine: GameEngine = GameEngine()
    val saveRepository: SaveSlotRepository = SaveSlotRepository(database.saveSlotDao())
}
