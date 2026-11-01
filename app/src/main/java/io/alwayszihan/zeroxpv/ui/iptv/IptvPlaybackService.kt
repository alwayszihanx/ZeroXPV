package io.alwayszihan.zeroxpv.ui.iptv

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.media3.common.Player
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import io.alwayszihan.zeroxpv.MainActivity
import io.alwayszihan.zeroxpv.R
import io.alwayszihan.zeroxpv.ui.iptv.player.IptvPlayerHolder

/**
 * Foreground MediaSessionService for IPTV background playback.
 *
 * It does not create its own player — it attaches the MediaSession to the
 * single ExoPlayer instance held by [IptvPlayerHolder] (the same one the
 * activity renders). While the service is foregrounded, audio keeps playing
 * after the activity is dismissed and a media notification with play/pause and
 * lock-screen controls is shown.
 */
class IptvPlaybackService : MediaSessionService() {

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "zeroxpv_iptv_background"

        fun start(context: Context) {
            val intent = Intent(context, IptvPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, IptvPlaybackService::class.java))
        }
    }

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val provider = DefaultMediaNotificationProvider(
            this,
            { NOTIFICATION_ID },
            CHANNEL_ID,
            R.drawable.ic_launcher_monochrome,
        )
        setMediaNotificationProvider(provider)

        val player = IptvPlayerHolder.player ?: createFallbackPlayer()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    private fun createFallbackPlayer(): Player {
        val fallback = androidx.media3.exoplayer.ExoPlayer.Builder(this).build()
        IptvPlayerHolder.setPlayer(fallback)
        return fallback
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "IPTV Background Playback",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Notification for IPTV background playback"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            (getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.pause()
        stopSelf()
    }

    override fun onDestroy() {
        // The player is owned by the activity's ViewModel (IptvPlayerHolder),
        // so only release the session here, never the shared player.
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
