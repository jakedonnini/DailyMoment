package com.example.dailymoment.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dailymoment.data.Entry
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val detailDateFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy · h:mm a")

/** Full-screen entry viewer. Swipe left/right to move between entries in date order. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(entries: List<Entry>, startEntryId: Long, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        if (entries.isEmpty()) {
            TextButton(onClick = onBack) { Text("‹ Back") }
            return@Column
        }

        val initialPage = entries.indexOfFirst { it.id == startEntryId }.coerceAtLeast(0)
        val pagerState = rememberPagerState(initialPage = initialPage) { entries.size }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) { Text("‹ Calendar") }
            Text(
                "${pagerState.currentPage + 1} / ${entries.size}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(end = 12.dp)
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { entries.getOrNull(it)?.id ?: it }
        ) { page ->
            EntryPage(entries[page])
        }
    }
}

@Composable
private fun EntryPage(entry: Entry) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        DualPhoto(
            backPath = entry.photoPath,
            frontPath = entry.frontPhotoPath,
            modifier = Modifier.fillMaxWidth().height(380.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(50),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault()).format(detailDateFormat),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
        if (entry.happened.isNotBlank()) JournalCard("What happened", entry.happened)
        if (entry.learned.isNotBlank()) JournalCard("What I learned", entry.learned)
        if (entry.happened.isBlank() && entry.learned.isBlank()) {
            JournalCard(title = null, body = "No journal text for this day.", muted = true)
        }
    }
}

/** Rounded card holding one journal answer, with an accent-colored heading. */
@Composable
private fun JournalCard(title: String?, body: String, muted: Boolean = false) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            if (title != null) {
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                body,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
