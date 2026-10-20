package com.kidsee.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.kidsee.app.state.KidSeeStateManager
import timber.log.Timber

class KidSeeApp : Application() {

    companion object {
        lateinit var instance: KidSeeApp
            private set
    }

    val stateManager by lazy {
        KidSeeStateManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize Firebase safely before initializing state manager
        initFirebase()

        Timber.d("KidSee application initialized")
        stateManager.init()
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:123456789012:android:abcdef1234567890")
                    .setApiKey("AIzaSyMockKeyForKidSeeApplicationDevBuild")
                    .setProjectId("kidsee-app")
                    .setDatabaseUrl("https://kidsee-app-default-rtdb.firebaseio.com")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Timber.d("FirebaseApp initialized successfully")
            }
        } catch (e: Throwable) {
            Timber.w(e, "Could not initialize FirebaseApp, will continue in offline mode")
        }
    }
}
