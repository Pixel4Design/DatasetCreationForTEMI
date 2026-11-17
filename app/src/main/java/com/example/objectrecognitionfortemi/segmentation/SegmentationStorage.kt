package com.example.objectrecognitionfortemi.segmentation

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.util.Locale

private const val FOLDER = "imagenesSegmentadas"

// Guarda en formato YOLO de segmentación:
//   imagenesSegmentadas/images/<nombre>.jpg
//   imagenesSegmentadas/labels/<nombre>.txt  ->  una línea por polígono: "clase x1 y1 x2 y2 ... xn yn" (normalizado 0..1)
fun saveSegmentation(context: Context, image: Bitmap, label: String, annotation: MaskAnnotation) {
    val root = File(context.getExternalFilesDir(null) ?: context.filesDir, FOLDER)
    val imagesDir = File(root, "images").apply { mkdirs() }
    val labelsDir = File(root, "labels").apply { mkdirs() }

    val name = "${System.currentTimeMillis()}_${label.replace(Regex("[^A-Za-z0-9]+"), "_")}"

    File(imagesDir, "$name.jpg").outputStream().use {
        image.compress(Bitmap.CompressFormat.JPEG, 95, it)
    }

    fun norm(v: Int, size: Int) = String.format(Locale.US, "%.6f", (v.toFloat() / size).coerceIn(0f, 1f))

    val classIndex = LABELS.indexOf(label)
    val lines = annotation.polygons.map { polygon ->
        classIndex.toString() + polygon.joinToString("") { (x, y) ->
            " ${norm(x, annotation.width)} ${norm(y, annotation.height)}"
        }
    }
    File(labelsDir, "$name.txt").writeText(lines.joinToString("\n") + "\n")
}