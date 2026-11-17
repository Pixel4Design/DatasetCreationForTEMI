package com.example.objectrecognitionfortemi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.objectrecognitionfortemi.segmentation.SegmentationFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Flujo: captura con botón -> puntos -> máscara (SAM 2) -> guardado
        setContent { MaterialTheme { SegmentationFlow() } }
    }
}