package com.example.objectrecognitionfortemi.segmentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

// Escucha de forma continua y llama a onKeyword cuando oye alguna de las palabras clave.
// Debe usarse desde el hilo principal.
class KeywordListener(
    private val context: Context,
    private val keywords: List<String>,
    private val onKeyword: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var active = false

    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)   // reacciona sin esperar a que acabe la frase
    }

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("KeywordListener", "Reconocimiento de voz no disponible en este dispositivo")
            return
        }
        active = true
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply { setRecognitionListener(listener) }
        recognizer?.startListening(intent)
    }

    fun stop() {
        active = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
    }

    private fun restart() {
        if (!active) return
        handler.postDelayed({ if (active) recognizer?.startListening(intent) }, 300)
    }

    private fun heard(bundle: Bundle?): Boolean {
        val texts = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return false
        return texts.any { text -> keywords.any { text.contains(it, ignoreCase = true) } }
    }

    private fun trigger() {
        if (!active) return
        active = false
        recognizer?.cancel()
        onKeyword()
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            if (heard(partialResults)) trigger()
        }

        override fun onResults(results: Bundle?) {
            if (heard(results)) trigger() else restart()
        }

        override fun onError(error: Int) {
            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                active = false
                Log.w("KeywordListener", "Sin permiso de micrófono")
                return
            }
            restart()
        }
    }
}