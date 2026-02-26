package io.fgluten

import android.app.Application
import com.google.firebase.FirebaseApp

class FGlutenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Ensure Firebase is initialized before any ViewModel/Repository access.
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
    }
}
