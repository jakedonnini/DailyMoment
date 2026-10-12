package com.example.dailymoment.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.dailymoment.data.Entry
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("EEEE, MMM d · h:mm a")

@Composable
fun TimelineScreen(entries: List<Entry>, streak: Int, onCapture: () -> Unit) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onCapture) { Text("Capture now") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Text("Daily Moment", style = MaterialTheme.typography.headlineMedium)
            Text(
                if (streak > 0) "🔥 $streak day streak" else "Start your streak today",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No entries yet. You'll get a notification at a random time today.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)
                ) {
                    items(entries, key = { it.id }) { EntryCard(it) }
                }
            }
        }
    }
}

@Composable
private fun EntryCard(entry: Entry) {
    Card(Modifier.fillMaxWidth()) {
        Column {
            AsyncImage(
                model = File(entry.photoPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(220.dp)
            )
            Column(Modifier.padding(16.dp)) {
                Text(
                    Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault()).format(dateFormat),
                    style = MaterialTheme.typography.labelLarge
                )
                if (entry.happened.isNotBlank()) {
                    Text(entry.happened, modifier = Modifier.padding(top = 8.dp))
                }
                if (entry.learned.isNotBlank()) {
                    Text(
                        "Learned: ${entry.learned}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
