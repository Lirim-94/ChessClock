package com.example.chessclock

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val _player1Time = mutableStateOf(0L)
    private val _player2Time = mutableStateOf(0L)
    private val _activePlayer = mutableStateOf(0)
    private val _gameOver = mutableStateOf(false)
    private val _areControlsLocked = mutableStateOf(false)
    private var initialTimeInMillis: Long = 0
    private var timerJob: Job? = null
    private var lastUpdateTime: Long = 0

    val player1Time: State<Long> = _player1Time
    val player2Time: State<Long> = _player2Time
    val activePlayer: State<Int> = _activePlayer
    val gameOver: State<Boolean> = _gameOver
    val areControlsLocked: State<Boolean> = _areControlsLocked

    fun startGame(initialTimeInMinutes: Int) {
        initialTimeInMillis = initialTimeInMinutes * 60 * 1000L
        _player1Time.value = initialTimeInMillis
        _player2Time.value = initialTimeInMillis
        _activePlayer.value = 0
        _gameOver.value = false
        _areControlsLocked.value = false
        lastUpdateTime = System.currentTimeMillis()
    }

    fun onPlayerTap(player: Int) {
        if (_gameOver.value) return

        when (_activePlayer.value) {
            0 -> {
                // Game hasn't started, start with the tapped player
                _activePlayer.value = player
                startTimer()
            }
            player -> {
                // Active player tapped, switch to the other player
                _activePlayer.value = if (player == 1) 2 else 1
                updateTimeAndResetTimer()
            }
            // If the inactive player taps, do nothing
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        lastUpdateTime = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (isActive && !_gameOver.value) {
                delay(100) // Update every 100ms
                updateTime()
            }
        }
    }

    private fun updateTimeAndResetTimer() {
        updateTime()
        lastUpdateTime = System.currentTimeMillis()
    }

    private fun updateTime() {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastUpdateTime
        lastUpdateTime = currentTime

        when (_activePlayer.value) {
            1 -> {
                _player1Time.value = maxOf(0, _player1Time.value - elapsedTime)
                if (_player1Time.value == 0L) endGame()
            }
            2 -> {
                _player2Time.value = maxOf(0, _player2Time.value - elapsedTime)
                if (_player2Time.value == 0L) endGame()
            }
        }
    }

    private fun endGame() {
        _gameOver.value = true
        _activePlayer.value = 0
        timerJob?.cancel()
    }

    fun restartGame() {
        timerJob?.cancel()
        startGame((initialTimeInMillis / (60 * 1000)).toInt())
    }

    fun toggleControlLock() {
        _areControlsLocked.value = !_areControlsLocked.value
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}