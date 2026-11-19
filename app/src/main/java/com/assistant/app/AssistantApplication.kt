package com.assistant.app

import android.app.Application
import com.assistant.app.utils.AppContainer
import com.assistant.app.utils.DefaultAppContainer

/**
 * Custom [Application] class initializing the DI [AppContainer].
 */
class AssistantApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
