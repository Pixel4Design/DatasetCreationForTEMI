package com.example.objectrecognitionfortemi.segmentation

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

private const val CLASSES_PER_ROW = 4

@Composable
fun MaskReviewScreen(
    maskImage: Bitmap,
    isLoading: Boolean,
    errorMessage: String?,
    onConfirm: (label: String) -> Unit,
    onRedo: () -> Unit,
    onClose: () -> Unit
) {
    var selected by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                bitmap = maskImage.asImageBitmap(),
                contentDescription = "Generated mask",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            Text("Select the class of the object")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LABELS.chunked(CLASSES_PER_ROW).forEach { rowLabels ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowLabels.forEach { label ->
                            if (label == selected) {
                                Button(onClick = { selected = label }, modifier = Modifier.weight(1f)) { Text(label) }
                            } else {
                                OutlinedButton(onClick = { selected = label }, modifier = Modifier.weight(1f)) { Text(label) }
                            }
                        }
                        repeat(CLASSES_PER_ROW - rowLabels.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { selected?.let(onConfirm) },
                    enabled = selected != null && !isLoading
                ) { Text("Confirm mask") }
                OutlinedButton(onClick = onRedo, enabled = !isLoading) { Text("Redo mask") }
            }

            if (isLoading) CircularProgressIndicator()
        }

        IconButton(
            onClick = onClose,
            enabled = !isLoading,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Text("✕", style = MaterialTheme.typography.headlineSmall)
        }
    }
}