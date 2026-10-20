package com.kidsee.app

import android.app.Application
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
        Timber.d("KidSee application initialized")

        stateManager.init()
    }
}
