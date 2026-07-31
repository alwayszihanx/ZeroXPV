package io.alwayszihan.zeroxpv.ui.iptv.player

import androidx.media3.common.Player

/**
 * Process-level holder for the single ExoPlayer instance used by the IPTV player.
 * Lets [IptvPlaybackService] attach a MediaSession to the same player that the
 * activity UI is rendering, so audio keeps playing and notification/lock-screen
 * controls work when the app is backgrounded.
 */
object IptvPlayerHolder {
    @Volatile
    var player: Player? = null
        private set

    fun setPlayer(player: Player?) {
        this.player = player
    }
}
