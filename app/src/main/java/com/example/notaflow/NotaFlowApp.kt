// NotaFlowApp.kt
package com.example.notaflow

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class NotaFlowApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        // Instead of using BuildConfig.DEBUG which may not be recognized,
        // we can use a safer approach
        if (isDebugBuild()) {
            Timber.plant(Timber.DebugTree())
        }
    }

    /**
     * Determines if the app is running in debug mode.
     * This is a more reliable alternative to BuildConfig.DEBUG
     */
    private fun isDebugBuild(): Boolean {
        return try {
            // The application ID will have .debug suffix in debug builds
            // if you've configured your build variants properly
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageName.contains(".debug") ||
                    packageName.endsWith(".test") ||
                    android.os.Build.TYPE.equals("eng", ignoreCase = true)
        } catch (e: Exception) {
            // If there's an error, assume it's a debug build to be safe
            true
        }
    }
}