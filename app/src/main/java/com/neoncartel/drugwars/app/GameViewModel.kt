package com.neoncartel.drugwars.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neoncartel.drugwars.data.local.SaveSlotRepository
import com.neoncartel.drugwars.domain.model.CharacterId
import com.neoncartel.drugwars.domain.model.Difficulty
import com.neoncartel.drugwars.domain.model.GameAction
import com.neoncartel.drugwars.domain.model.GameState
import com.neoncartel.drugwars.domain.system.GameEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameUiState(
    val game: GameState? = null,
    val loading: Boolean = true,
    val selectedCharacter: CharacterId = CharacterId.NOVA,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val message: String? = null,
)

class GameViewModel(
    private val engine: GameEngine,
    private val repository: SaveSlotRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = repository.load()
            _uiState.update { it.copy(game = saved, loading = false) }
        }
    }

    fun selectCharacter(characterId: CharacterId) {
        _uiState.update { it.copy(selectedCharacter = characterId) }
    }

    fun setDifficulty(difficulty: Difficulty) {
        _uiState.update { it.copy(difficulty = difficulty) }
    }

    fun startNewGame() {
        val current = _uiState.value
        val game = engine.newGame(current.selectedCharacter, current.difficulty)
        _uiState.update { it.copy(game = game, message = "New run started.") }
        autosave(game)
    }

    fun continueOrStart() {
        if (_uiState.value.game == null) startNewGame()
    }

    fun dispatch(action: GameAction) {
        val game = _uiState.value.game ?: return
        val result = engine.reduce(game, action)
        _uiState.update {
            it.copy(
                game = result.state,
                message = result.effects.lastOrNull { effect -> effect.message != null }?.message,
            )
        }
        autosave(result.state)
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun abandonRun() {
        viewModelScope.launch {
            repository.clear()
            _uiState.update { it.copy(game = null, message = "Save cleared.") }
        }
    }

    private fun autosave(game: GameState) {
        viewModelScope.launch {
            repository.save(game)
        }
    }

    class Factory(
        private val engine: GameEngine,
        private val repository: SaveSlotRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(GameViewModel::class.java))
            return GameViewModel(engine, repository) as T
        }
    }
}
