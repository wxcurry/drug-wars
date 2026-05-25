package com.neoncartel.drugwars.data.local

import com.neoncartel.drugwars.domain.model.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SaveSlotRepository(
    private val dao: SaveSlotDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    val savedGame: Flow<GameState?> = dao.observe().map { entity ->
        entity?.let { GameStateCodec.decode(it.payload) }
    }

    suspend fun load(): GameState? = dao.load()?.let { GameStateCodec.decode(it.payload) }

    suspend fun save(state: GameState) {
        dao.upsert(
            SaveSlotEntity(
                slotId = PRIMARY_SLOT,
                payload = GameStateCodec.encode(state),
                updatedAtMillis = clock(),
            ),
        )
    }

    suspend fun clear() {
        dao.delete(PRIMARY_SLOT)
    }

    companion object {
        const val PRIMARY_SLOT = "autosave"
    }
}
