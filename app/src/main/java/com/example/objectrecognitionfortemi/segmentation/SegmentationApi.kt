package com.example.objectrecognitionfortemi.segmentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// Emulador: "http://10.0.2.2:8000" (es tu ordenador). TEMI o dispositivo real: IP local de tu ordenador
private const val SERVER_URL = "http://10.233.10.170:8000"

// Punto en coordenadas de píxel de la imagen capturada
data class ImagePoint(val x: Int, val y: Int)

// Respuesta de /confirm: polígonos en píxeles + tamaño de la imagen
data class MaskAnnotation(val width: Int, val height: Int, val polygons: List<List<Pair<Int, Int>>>)

object SegmentationApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)   // SAM 2 en CPU puede tardar
        .build()

    // POST /segment -> imagen con la máscara dibujada
    suspend fun segment(
        image: Bitmap,
        added: List<ImagePoint>,
        removed: List<ImagePoint>
    ): Bitmap = withContext(Dispatchers.IO) {
        val jpeg = ByteArrayOutputStream().use { out ->
            image.compress(Bitmap.CompressFormat.JPEG, 95, out)
            out.toByteArray()
        }

        // [{"x": 300, "y": 250, "label": 1}, ...]  (1 = añadir, 0 = quitar)
        val points = JSONArray()
        added.forEach { points.put(JSONObject().put("x", it.x).put("y", it.y).put("label", 1)) }
        removed.forEach { points.put(JSONObject().put("x", it.x).put("y", it.y).put("label", 0)) }

        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", "capture.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
            .addFormDataPart("points", points.toString())
            .build()

        client.newCall(Request.Builder().url("$SERVER_URL/segment").post(body).build()).execute().use { resp ->
            val bytes = resp.body?.bytes() ?: ByteArray(0)
            if (!resp.isSuccessful) error("Server error ${resp.code}: ${String(bytes)}")
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("The server returned an invalid image")
        }
    }

    // POST /confirm -> anotaciones de la última máscara generada
    suspend fun confirm(label: String): MaskAnnotation = withContext(Dispatchers.IO) {
        val body = FormBody.Builder().add("label", label).build()
        client.newCall(Request.Builder().url("$SERVER_URL/confirm").post(body).build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Server error ${resp.code}: $text")
            parse(text)
        }
    }

    private fun parse(text: String): MaskAnnotation {
        val json = JSONObject(text)
        val boxes = json.getJSONArray("boxes")
        val polygons = (0 until boxes.length()).map { i ->
            val pts = boxes.getJSONObject(i).getJSONArray("points")
            (0 until pts.length()).map { j ->
                val p = pts.getJSONArray(j)
                Pair(p.getInt(0), p.getInt(1))
            }
        }
        return MaskAnnotation(json.getInt("width"), json.getInt("height"), polygons)
    }
}