package com.sara.assistant

import android.app.Application

/**
 * Base Application class for SARA.
 * Step 1: Establishes the app lifecycle root.
 * Future steps will initialize Firebase, Gemini client, and voice services here.
 */
class SaraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
