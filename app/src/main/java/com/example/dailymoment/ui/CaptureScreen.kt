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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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

    // Start on the rear camera.
    LaunchedEffect(Unit) {
        bindCamera(context, lifecycleOwner, previewView, imageCapture, CameraSelector.LENS_FACING_BACK)
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

        Button(
            enabled = !busy,
            onClick = {
                busy = true
                scope.launch {
                    val dir = File(context.filesDir, "photos").apply { mkdirs() }
                    val stamp = System.currentTimeMillis()

                    // 1) Rear camera
                    val backFile = File(dir, "IMG_${stamp}_back.jpg")
                    if (!imageCapture.takePhoto(context, backFile)) {
                        Toast.makeText(context, "Capture failed, try again", Toast.LENGTH_SHORT).show()
                        busy = false
                        return@launch
                    }

                    // 2) Switch to front camera, short countdown, then capture
                    var frontPath: String? = null
                    val frontReady = bindCamera(
                        context, lifecycleOwner, previewView, imageCapture, CameraSelector.LENS_FACING_FRONT
                    )
                    if (frontReady) {
                        for (n in 2 downTo 1) {
                            overlay = "Selfie in $n…"
                            delay(1000)
                        }
                        overlay = null
                        val frontFile = File(dir, "IMG_${stamp}_front.jpg")
                        if (imageCapture.takePhoto(context, frontFile)) frontPath = frontFile.absolutePath
                    }

                    onPhotosTaken(backFile.absolutePath, frontPath)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp)
                .fillMaxWidth()
        ) { Text(if (busy) "Capturing…" else "Capture") }
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
