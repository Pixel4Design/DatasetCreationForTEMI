package com.example.objectrecognitionfortemi.segmentation

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

private enum class PointMode { NONE, ADD, REMOVE }

// Dónde se dibuja la imagen dentro de su contenedor (ContentScale.Fit)
private class FitRect(val left: Float, val top: Float, val scale: Float)

private fun fitRect(box: IntSize, imgW: Int, imgH: Int): FitRect {
    val scale = minOf(box.width.toFloat() / imgW, box.height.toFloat() / imgH)
    return FitRect((box.width - imgW * scale) / 2f, (box.height - imgH * scale) / 2f, scale)
}

@Composable
fun PointsEditorScreen(
    image: Bitmap,
    added: SnapshotStateList<ImagePoint>,
    removed: SnapshotStateList<ImagePoint>,
    isLoading: Boolean,
    errorMessage: String?,
    onGenerateMask: () -> Unit,
    onClose: () -> Unit
) {
    var mode by remember { mutableStateOf(PointMode.NONE) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    val hint = when (mode) {
        PointMode.ADD -> "Touch the object to add a Point"
        PointMode.REMOVE -> "Touch a part of the object that you want to remove"
        PointMode.NONE -> ""
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(hint, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.heightIn(min = 28.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onSizeChanged { boxSize = it }
                    .pointerInput(mode, boxSize, image, isLoading) {
                        detectTapGestures { tap ->
                            if (mode == PointMode.NONE || isLoading || boxSize == IntSize.Zero) return@detectTapGestures
                            val fit = fitRect(boxSize, image.width, image.height)
                            val x = (tap.x - fit.left) / fit.scale
                            val y = (tap.y - fit.top) / fit.scale
                            // Ignorar toques fuera de la imagen (en los márgenes)
                            if (x < 0f || y < 0f || x >= image.width || y >= image.height) return@detectTapGestures
                            val point = ImagePoint(x.toInt(), y.toInt())
                            if (mode == PointMode.ADD) added.add(point) else removed.add(point)
                        }
                    }
            ) {
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = "Captured image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                Canvas(Modifier.fillMaxSize()) {
                    if (boxSize == IntSize.Zero) return@Canvas
                    val fit = fitRect(boxSize, image.width, image.height)
                    val radius = 8.dp.toPx()

                    fun dot(p: ImagePoint, color: Color) {
                        val c = Offset(fit.left + p.x * fit.scale, fit.top + p.y * fit.scale)
                        drawCircle(color, radius, c)
                        drawCircle(Color.White, radius, c, style = Stroke(width = 2.dp.toPx()))
                    }
                    added.forEach { dot(it, Color(0xFF00C853)) }     // verde: parte del objeto
                    removed.forEach { dot(it, Color(0xFFE53935)) }   // rojo: parte a excluir
                }
            }

            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ModeButton("Add point", mode == PointMode.ADD) { mode = PointMode.ADD }
                // Quitar puntos solo tiene sentido cuando ya hay algún punto añadido
                if (added.isNotEmpty()) {
                    ModeButton("Remove point", mode == PointMode.REMOVE) { mode = PointMode.REMOVE }
                }
                // Borrar todos los puntos (verdes y rojos) para empezar de nuevo
                if (added.isNotEmpty() || removed.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            added.clear()
                            removed.clear()
                            mode = PointMode.NONE
                        },
                        enabled = !isLoading
                    ) {
                        Text("Clear points")
                    }
                }
                Button(onClick = onGenerateMask, enabled = added.isNotEmpty() && !isLoading) {
                    Text("Generate Mask")
                }
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

@Composable
private fun ModeButton(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick) { Text(text) }
    else OutlinedButton(onClick = onClick) { Text(text) }
}