package com.example.placy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.sulgik.mapkit.MapKit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TempApp()
        }
    }

    override fun onStart() {
        super.onStart()
        MapKit.getInstance().onStart()
    }

    override fun onStop() {
        super.onStop()
        MapKit.getInstance().onStop()
    }

}