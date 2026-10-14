package com.example.dailymoment.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

/** BeReal-style photo: big main photo with the other camera's photo as a small inset. Tap the inset to swap. */
@Composable
fun DualPhoto(backPath: String, frontPath: String?, modifier: Modifier = Modifier) {
    var swapped by remember(backPath) { mutableStateOf(false) }
    val main = if (swapped && frontPath != null) frontPath else backPath
    val inset = if (swapped) backPath else frontPath

    Box(modifier.clip(RoundedCornerShape(20.dp))) {
        AsyncImage(
            model = File(main),
            contentDescription = "Photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (inset != null) {
            val insetShape = RoundedCornerShape(12.dp)
            AsyncImage(
                model = File(inset),
                contentDescription = "Tap to swap photos",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .width(96.dp)
                    .aspectRatio(3f / 4f)
                    .clip(insetShape)
                    .border(2.dp, Color.White, insetShape)
                    .clickable { swapped = !swapped }
            )
        }
    }
}
