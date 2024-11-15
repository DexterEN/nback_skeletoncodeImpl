package mobappdev.example.nback_cimpl.ui.viewmodels

import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.NBackHelper
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository

/**
 * This is the GameViewModel.
 *
 * It is good practice to first make an interface, which acts as the blueprint
 * for your implementation. With this interface we can create fake versions
 * of the viewmodel, which we can use to test other parts of our app that depend on the VM.
 *
 * Our viewmodel itself has functions to start a game, to specify a gametype,
 * and to check if we are having a match
 *
 * Date: 25-08-2023
 * Version: Version 1.0
 * Author: Yeetivity
 *
 */


interface GameViewModel {
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>

    fun setGameType(gameType: GameType)

    fun startGame()
    fun resetGame()

    fun checkMatch():Boolean
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository,
): GameViewModel, ViewModel() {
    private var textToSpeech: TextToSpeech? = null

    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState>
        get() = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int>
        get() = _score

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int>
        get() = _highscore


    private var job: Job? = null  // coroutine job for the game event
    private val eventInterval: Long = 2000L  // 2000 ms (2s)

    private val nBackHelper = NBackHelper()  // Helper that generate the event array
    private var events = emptyArray<Int>() // Array with all events

    // Call this from MainActivity to initialize TTS in GameVM
    fun initTextToSpeech(tts: TextToSpeech) {
        textToSpeech = tts
    }

    override fun setGameType(gameType: GameType) {
        // update the gametype in the gamestate
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun startGame() {
        job?.cancel()  // Cancel any existing game loop


        // Get the events from our C-model (returns IntArray, so we need to convert to Array<Int>)
        events = nBackHelper.generateNBackString(_gameState.value.eventSize, 9, 30, _gameState.value.nBackValue).toList().toTypedArray()  // Todo Higher Grade: currently the size etc. are hardcoded, make these based on user input
        Log.d("GameVM", "The following sequence was generated: ${events.contentToString()}")

       totallMatches(events)

        _score.value = 0
        _gameState.value = _gameState.value.copy(urMatches = 0,roundFinished = false, eventIndex = 0)
        job = viewModelScope.launch {
            when (gameState.value.gameType) {
                GameType.Audio -> runAudioGame(events)
                GameType.AudioVisual -> runAudioVisualGame()
                GameType.Visual -> runVisualGame(events)
            }
            if(_score.value > _highscore.value) {
                _highscore.value = _score.value
                userPreferencesRepository.saveHighScore(_highscore.value)
            }
            // Todo: update the highscore (done?)
        }
    }
    private fun totallMatches(events: Array<Int>){
        var matchCount = 0

        for (index in _gameState.value.nBackValue until events.size) {
            if (events[index] == events[index - _gameState.value.nBackValue]) {
                matchCount++
            }
        }

         _gameState.value = _gameState.value.copy(actualMatches = matchCount)

    }

    override fun checkMatch():Boolean {
        /**
         * Todo: This function should check if there is a match when the user presses a match button
         * Make sure the user can only register a match once for each event.
         */
        if (_gameState.value.eventIndex>=_gameState.value.nBackValue){
            val isMatch = events[_gameState.value.eventIndex] == events[_gameState.value.eventIndex - _gameState.value.nBackValue]
            if (isMatch) {
                _gameState.value = _gameState.value.copy(urMatches = _gameState.value.urMatches + 1)
                _score.value += _gameState.value.nBackValue
                events[_gameState.value.eventIndex - _gameState.value.nBackValue] = -1
                Log.d("GameVM", "Match found at index ${_gameState.value.eventIndex}")
                return true
            } else {
                _score.value -= 1
                _gameState.value = _gameState.value.copy(urMatches = _gameState.value.urMatches - 1)
                Log.d("GameVM", "No match at index ${_gameState.value.eventIndex}")
            }
        } else {
            _score.value = 0
            Log.d("GameVM", "Not enough events yet to check for a match")
        }
        return false
    }

    private suspend fun runAudioGame(events: Array<Int>) {
        val numberToLetter = arrayOf('A', 'Z', 'Q', 'T', 'R', 'K', 'W', 'X', 'I')
        // Todo: Make work for Basic grade
        for (value in events) {
            _gameState.value = _gameState.value.copy(eventValue = value)
            textToSpeech?.speak(numberToLetter.get(value-1).toString(), TextToSpeech.QUEUE_FLUSH, null, null)
            delay(eventInterval)
            _gameState.value = _gameState.value.copy(eventIndex = _gameState.value.eventIndex+1)
        }
        _gameState.value = _gameState.value.copy(roundFinished = true)
        Log.d("GameVM", "The following sequence finished the game: ${events.contentToString()}")
        Log.d("GameVM", "Round finished with score: ${_score.value} matches ${_gameState.value.urMatches} " +
                "out of ${_gameState.value.actualMatches} = " +
                "${_gameState.value.urMatches/_gameState.value.actualMatches}%")
    }

    private suspend fun runVisualGame(events: Array<Int>){
        // Todo: Replace this code for actual game code
        for (value in events) {
            _gameState.value = _gameState.value.copy(eventValue = value)
            delay(eventInterval)
            _gameState.value = _gameState.value.copy(eventIndex = _gameState.value.eventIndex+1)
        }


        _gameState.value = _gameState.value.copy(roundFinished = true)
        Log.d("GameVM", "The following sequence finished the game: ${events.contentToString()}")
        Log.d("GameVM", "Round finished with score: ${_score.value} matches ${_gameState.value.urMatches} " +
                "out of ${_gameState.value.actualMatches} = " +
                "${_gameState.value.urMatches/_gameState.value.actualMatches}%")
    }

   override fun resetGame() {
        _gameState.value = GameState() // Resets GameState to initial values
        _score.value = 0
        events = emptyArray()
    }

    private fun runAudioVisualGame(){
        // Todo: Make work for Higher grade
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GameApplication)
                GameVM(application.userPreferencesRespository)
            }
        }
    }

    init {
        // Code that runs during creation of the vm
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect {
                _highscore.value = it
            }
        }
    }

}

// Class with the different game types
enum class GameType{
    Audio,
    Visual,
    AudioVisual
}

data class GameState(
    // You can use this state to push values from the VM to your UI.
    val gameType: GameType = GameType.Visual,  // Type of the game
    val eventValue: Int = -1,  // The value of the array string
    val roundFinished: Boolean = false,
    val nBackValue: Int = 2,
    val eventSize: Int = 10,
    val eventIndex: Int = -1,
    val urMatches: Int = 0,
    val actualMatches: Int = 0

)

class FakeVM: GameViewModel{
    override val gameState: StateFlow<GameState>
        get() = MutableStateFlow(GameState()).asStateFlow()
    override val score: StateFlow<Int>
        get() = MutableStateFlow(2).asStateFlow()
    override val highscore: StateFlow<Int>
        get() = MutableStateFlow(42).asStateFlow()

    override fun setGameType(gameType: GameType) {
    }

    override fun startGame() {
    }

    override fun checkMatch():Boolean {
        return true
    }

    override fun resetGame() {
    }
}