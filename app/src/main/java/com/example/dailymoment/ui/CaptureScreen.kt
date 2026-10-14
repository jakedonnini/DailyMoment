package com.example.dailymoment.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

@Composable
fun CaptureScreen(onPhotosTaken: (backPath: String, frontPath: String?) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }

    if (!hasPermission) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Camera permission is needed to capture your moment.")
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Grant permission") }
            TextButton(onClick = onCancel) { Text("Back") }
        }
        return
    }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }
    var busy by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf<String?>(null) }
    // Which camera takes the first photo. Toggled by the swap button.
    var frontFirst by remember { mutableStateOf(false) }
    val firstLens = if (frontFirst) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
    val secondLens = if (frontFirst) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT

    // Preview whichever camera goes first; rebinds when the order is swapped.
    LaunchedEffect(frontFirst) {
        val ok = bindCamera(context, lifecycleOwner, previewView, imageCapture, firstLens)
        if (!ok && frontFirst) {
            Toast.makeText(context, "Front camera not available", Toast.LENGTH_SHORT).show()
            frontFirst = false
        }
    }
    // Release the camera when leaving this screen.
    DisposableEffect(Unit) {
        onDispose {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({ runCatching { future.get().unbindAll() } }, ContextCompat.getMainExecutor(context))
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        TextButton(
            onClick = onCancel,
            enabled = !busy,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp)
        ) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }

        overlay?.let {
            Text(
                it,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            )
        }

        val capture: () -> Unit = {
            busy = true
            scope.launch {
                val dir = File(context.filesDir, "photos").apply { mkdirs() }
                val stamp = System.currentTimeMillis()

                val backFile = File(dir, "IMG_${stamp}_back.jpg")
                val frontFile = File(dir, "IMG_${stamp}_front.jpg")
                val firstFile = if (frontFirst) frontFile else backFile
                val secondFile = if (frontFirst) backFile else frontFile

                // 1) First camera (whatever is currently previewing)
                if (!imageCapture.takePhoto(context, firstFile)) {
                    Toast.makeText(context, "Capture failed, try again", Toast.LENGTH_SHORT).show()
                    busy = false
                    return@launch
                }

                // 2) Switch to the other camera, short countdown, then capture
                if (bindCamera(context, lifecycleOwner, previewView, imageCapture, secondLens)) {
                    for (n in 2 downTo 1) {
                        overlay = if (frontFirst) "Back camera in $n…" else "Selfie in $n…"
                        delay(1000)
                    }
                    overlay = null
                    imageCapture.takePhoto(context, secondFile)
                }

                // A rear photo is required; the selfie is optional.
                if (!backFile.exists()) {
                    frontFile.delete()
                    Toast.makeText(context, "Back camera capture failed, try again", Toast.LENGTH_SHORT).show()
                    busy = false
                    bindCamera(context, lifecycleOwner, previewView, imageCapture, firstLens)
                    return@launch
                }
                val frontPath = frontFile.takeIf { it.exists() }?.absolutePath
                onPhotosTaken(backFile.absolutePath, frontPath)
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            // Round shutter button in the middle.
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(76.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(if (busy) Color.White.copy(alpha = 0.4f) else Color.White)
                    .clickable(enabled = !busy, onClickLabel = "Capture", onClick = capture)
            )

            // Swap which camera shoots first. Pressing again reverses it.
            Column(
                Modifier.align(Alignment.CenterEnd),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(enabled = !busy, onClickLabel = "Swap camera order") { frontFirst = !frontFirst },
                    contentAlignment = Alignment.Center
                ) { Text("⇄", color = Color.White, style = MaterialTheme.typography.titleLarge) }
                Text(
                    if (frontFirst) "Selfie first" else "Back first",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/** Binds the preview + image capture to the given lens. Returns false if that camera isn't available. */
private suspend fun bindCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    lens: Int
): Boolean = suspendCancellableCoroutine { cont ->
    val future = ProcessCameraProvider.getInstance(context)
    future.addListener({
        try {
            val provider = future.get()
            val selector = CameraSelector.Builder().requireLensFacing(lens).build()
            if (!provider.hasCamera(selector)) {
                if (cont.isActive) cont.resume(false)
                return@addListener
            }
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
            if (cont.isActive) cont.resume(true)
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(false)
        }
    }, ContextCompat.getMainExecutor(context))
}

private suspend fun ImageCapture.takePhoto(context: Context, file: File): Boolean =
    suspendCancellableCoroutine { cont ->
        takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    if (cont.isActive) cont.resume(true)
                }
                override fun onError(e: ImageCaptureException) {
                    if (cont.isActive) cont.resume(false)
                }
            }
        )
    }
