package com.autodroid.guardiansdk

import android.app.Application
import android.content.Context

class GuardianApplication : Application() {

    companion object {
        private var instance: GuardianApplication? = null

        fun getInstance(): GuardianApplication {
            return instance ?: throw IllegalStateException("GuardianApplication not initialized")
        }

        fun getContext(): Context {
            return instance?.applicationContext 
                ?: throw IllegalStateException("GuardianApplication not initialized")
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
