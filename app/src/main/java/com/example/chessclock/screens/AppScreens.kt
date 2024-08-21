package com.example.chessclock.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chessclock.GameViewModel

@Composable
fun ChessClockApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("game/{time}") { backStackEntry ->
            val time = backStackEntry.arguments?.getString("time")?.toIntOrNull() ?: 5
            GameScreen(navController, time)
        }
    }
}

@Composable
fun HomeScreen(navController: NavController) {
    var customTime by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        listOf(1, 3, 5, 10, 30).forEach { time ->
            Button(
                onClick = { navController.navigate("game/$time") },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("$time min")
            }
        }

        OutlinedTextField(
            value = customTime,
            onValueChange = { customTime = it },
            label = { Text("Custom time (minutes)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Button(
            onClick = {
                val time = customTime.toIntOrNull() ?: return@Button
                navController.navigate("game/$time")
            },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Start Custom Game")
        }
    }
}

@Composable
fun GameScreen(navController: NavController, initialTimeInMinutes: Int) {
    val viewModel: GameViewModel = viewModel()
    val player1Time by viewModel.player1Time
    val player2Time by viewModel.player2Time
    val activePlayer by viewModel.activePlayer
    val gameOver by viewModel.gameOver
    val areControlsLocked by viewModel.areControlsLocked

    var showExitDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.startGame(initialTimeInMinutes)
    }

    if (showExitDialog) {
        ExitConfirmationDialog(
            onConfirm = { navController.popBackStack() },
            onDismiss = { showExitDialog = false }
        )
    }

    if (showRestartDialog) {
        RestartConfirmationDialog(
            onConfirm = {
                viewModel.restartGame()
                showRestartDialog = false
            },
            onDismiss = { showRestartDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlayerClock(
                timeInMillis = player2Time,
                isActive = activePlayer == 2,
                gameOver = gameOver,
                onClick = { viewModel.onPlayerTap(2) },
                modifier = Modifier.weight(1f),
                mirrored = true
            )

            PlayerClock(
                timeInMillis = player1Time,
                isActive = activePlayer == 1,
                gameOver = gameOver,
                onClick = { viewModel.onPlayerTap(1) },
                modifier = Modifier.weight(1f),
                mirrored = false
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LockButton(viewModel)
            IconButton(
                onClick = { if (!areControlsLocked) showExitDialog = true },
                enabled = !areControlsLocked
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Exit")
            }
            IconButton(
                onClick = { if (!areControlsLocked) showRestartDialog = true },
                enabled = !areControlsLocked
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Restart")
            }
        }
    }
}

@Composable
fun PlayerClock(
    timeInMillis: Long,
    isActive: Boolean,
    gameOver: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mirrored: Boolean
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(enabled = !gameOver, onClick = onClick)
    ) {
        Text(
            text = formatTime(timeInMillis),
            modifier = Modifier
                .align(Alignment.Center)
                .rotate(if (mirrored) 180f else 0f),
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                gameOver && timeInMillis <= 0 -> Color.Red
                isActive -> Color.Green
                else -> Color.Black
            },
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LockButton(viewModel: GameViewModel) {
    val isLocked = viewModel.areControlsLocked.value
    if (isLocked) {
        Button(
            onClick = { viewModel.toggleControlLock() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Unlock", color = MaterialTheme.colorScheme.onPrimary)
        }
    } else {
        IconButton(onClick = { viewModel.toggleControlLock() }) {
            Icon(Icons.Filled.Lock, contentDescription = "Lock controls")
        }
    }
}

@Composable
fun ExitConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exit Game") },
        text = { Text("Are you sure you want to exit the game? Your progress will be lost.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Exit")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RestartConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restart Game") },
        text = { Text("Are you sure you want to restart the game? Current progress will be lost.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Restart")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = milliseconds % 1000
    return "%02d:%02d.%03d".format(minutes, seconds, millis)
}