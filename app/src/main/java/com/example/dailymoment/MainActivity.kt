package com.example.dailymoment

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dailymoment.ui.CaptureScreen
import com.example.dailymoment.ui.EntryViewModel
import com.example.dailymoment.ui.JournalScreen
import com.example.dailymoment.ui.TimelineScreen

class MainActivity : ComponentActivity() {
    private var captureRequest by mutableStateOf(0) // bumps each time a notification opens the app

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_CAPTURE, false)) captureRequest++
        setContent {
            val ctx = LocalContext.current
            val dark = isSystemInDarkTheme()
            MaterialTheme(
                colorScheme = if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            ) {
                AppRoot(captureRequest)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_CAPTURE, false)) captureRequest++
    }

    companion object {
        const val EXTRA_CAPTURE = "open_capture"
    }
}

private sealed interface Screen {
    data object Timeline : Screen
    data object Capture : Screen
    data class Journal(val photoPath: String) : Screen
}

@Composable
private fun AppRoot(captureRequest: Int, vm: EntryViewModel = viewModel()) {
    var screen by remember { mutableStateOf<Screen>(Screen.Timeline) }

    // Ask for notification permission once (Android 13+).
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) { notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }

    // Tapping the notification jumps straight to the camera.
    LaunchedEffect(captureRequest) { if (captureRequest > 0) screen = Screen.Capture }

    BackHandler(enabled = screen !is Screen.Timeline) { screen = Screen.Timeline }

    val entries by vm.entries.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()

    when (val s = screen) {
        Screen.Timeline -> TimelineScreen(
            entries = entries,
            streak = streak,
            onCapture = { screen = Screen.Capture }
        )
        Screen.Capture -> CaptureScreen(
            onPhotoTaken = { path -> screen = Screen.Journal(path) },
            onCancel = { screen = Screen.Timeline }
        )
        is Screen.Journal -> JournalScreen(
            photoPath = s.photoPath,
            onSave = { happened, learned ->
                vm.save(s.photoPath, happened, learned) { screen = Screen.Timeline }
            }
        )
    }
}
