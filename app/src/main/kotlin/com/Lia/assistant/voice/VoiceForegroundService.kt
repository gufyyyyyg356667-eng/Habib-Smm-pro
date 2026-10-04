package com.Lia.assistant.voice

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.Lia.assistant.LiaApplication
import com.Lia.assistant.MainActivity
import com.Lia.assistant.R

class VoiceForegroundService : Service() {
    companion object {
        const val ACTION_STOP = "com.Lia.assistant.STOP_VOICE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            VoiceSessionManager.stop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification()
        startForeground(LiaApplication.NOTIF_ID_VOICE, notification)
        VoiceSessionManager.start(this)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        VoiceSessionManager.stop()
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, VoiceForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, LiaApplication.CHANNEL_VOICE)
            .setContentTitle(getString(R.string.notification_title_listening))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }
}
