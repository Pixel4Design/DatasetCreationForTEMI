package com.example.objectrecognitionfortemi.segmentation

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Step { CAMERA, EDITING, REVIEW }

// Orquesta: cámara -> edición de puntos -> revisión de la máscara -> guardado
@Composable
fun SegmentationFlow() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(Step.CAMERA) }
    var captured by remember { mutableStateOf<Bitmap?>(null) }
    var maskImage by remember { mutableStateOf<Bitmap?>(null) }
    val added = remember { mutableStateListOf<ImagePoint>() }
    val removed = remember { mutableStateListOf<ImagePoint>() }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Vuelve a la cámara y descarta todo
    fun reset() {
        captured = null; maskImage = null
        added.clear(); removed.clear()
        loading = false; error = null
        step = Step.CAMERA
    }

    BackHandler(enabled = step != Step.CAMERA && !loading) { reset() }

    val image = captured

    if (step == Step.CAMERA || image == null) {
        LiveCameraScreen { bitmap ->
            captured = bitmap
            step = Step.EDITING
        }
    } else if (step == Step.EDITING) {
        PointsEditorScreen(
            image = image,
            added = added,
            removed = removed,
            isLoading = loading,
            errorMessage = error,
            onClose = { reset() },
            onGenerateMask = {
                if (added.isNotEmpty() && !loading) {
                    loading = true; error = null
                    scope.launch {
                        try {
                            maskImage = SegmentationApi.segment(image, added.toList(), removed.toList())
                            step = Step.REVIEW
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = "Could not generate the mask: ${e.message}"
                        } finally {
                            loading = false
                        }
                    }
                }
            }
        )
    } else {
        val mask = maskImage
        if (mask != null) {
            MaskReviewScreen(
                maskImage = mask,
                isLoading = loading,
                errorMessage = error,
                onClose = { reset() },
                onRedo = {
                    // Vuelve a la captura con los puntos ya pintados
                    maskImage = null; error = null
                    step = Step.EDITING
                },
                onConfirm = { label ->
                    if (!loading) {
                        loading = true; error = null
                        scope.launch {
                            try {
                                val annotation = SegmentationApi.confirm(label)
                                withContext(Dispatchers.IO) { saveSegmentation(context, image, label, annotation) }
                                Toast.makeText(context, "Saved: $label", Toast.LENGTH_SHORT).show()
                                reset()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                error = "Could not confirm: ${e.message}"
                            } finally {
                                loading = false
                            }
                        }
                    }
                }
            )
        }
    }
}