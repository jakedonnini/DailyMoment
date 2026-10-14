package com.example.dailymoment.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun JournalScreen(
    photoPath: String,
    frontPhotoPath: String?,
    onSave: (happened: String, learned: String) -> Unit
) {
    var happened by rememberSaveable { mutableStateOf("") }
    var learned by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        DualPhoto(
            backPath = photoPath,
            frontPath = frontPhotoPath,
            modifier = Modifier.fillMaxWidth().height(320.dp)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = happened,
            onValueChange = { happened = it },
            label = { Text("What happened today?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = learned,
            onValueChange = { learned = it },
            label = { Text("What did you learn?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onSave(happened, learned) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }
    }
}
