package com.example.placy

import android.app.Application
import ru.sulgik.mapkit.MapKit

/** MapKit requires its key to be set once, before initialization in this process. */
class PlacyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapKit.setApiKey(BuildConfig.MAPKIT_API_KEY)
        MapKit.initialize(this)
    }
}
