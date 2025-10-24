package ai.fd.droid.aichat

import ai.fd.shared.aichat.platform.initLogger
import android.app.Application

class AiChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initLogger()
    }
}
