package com.Lia.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class LiaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_VOICE,
                getString(R.string.notification_channel_voice),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active voice session with Lia"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_VOICE = "lia_voice_session"
        const val NOTIF_ID_VOICE = 1001
        const val NOTIF_ID_FORGE_READY = 1002
    }
}
