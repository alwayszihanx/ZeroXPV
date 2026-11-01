package io.alwayszihan.zeroxpv.ui.iptv.player

import android.content.Context
import androidx.preference.PreferenceManager
import io.alwayszihan.zeroxpv.ui.iptv.parser.M3UResult
import io.alwayszihan.zeroxpv.ui.iptv.parser.M3U8Channel
import io.alwayszihan.zeroxpv.ui.iptv.parser.M3ULoader

class PlayerRepository(private val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    suspend fun parseM3U8FromUrl(url: String): M3UResult<List<M3U8Channel>> {
        return M3ULoader.parseM3U8FromUrl(url)
    }

    suspend fun parseM3U8FromContent(content: String): M3UResult<List<M3U8Channel>> {
        return M3ULoader.parseM3U8(content)
    }

    fun getAutoHideControls(): Boolean = prefs.getBoolean(PREF_AUTO_HIDE_CONTROLS, true)

    fun getEnablePip(): Boolean = prefs.getBoolean(PREF_ENABLE_PIP, true)

    fun getBackgroundPlay(): Boolean = prefs.getBoolean(PREF_BACKGROUND_PLAY, false)

    fun getAspectRatio(): Int = prefs.getInt(PREF_ASPECT_RATIO, 0)

    fun getMirrorFlip(): Boolean = prefs.getBoolean(PREF_MIRROR_FLIP, false)

    fun getQualityPreference(): Int = prefs.getInt(PREF_QUALITY_PREFERENCE, 0)

    fun getShowLogos(): Boolean = prefs.getBoolean(PREF_SHOW_LOGOS, true)
    fun getAutoRefreshPlaylists(): Boolean = prefs.getBoolean(PREF_AUTO_REFRESH, false)
    fun getChannelLayout(): Int = prefs.getInt(PREF_CHANNEL_LAYOUT, 0)
    fun getBufferSizeMb(): Int = prefs.getInt(PREF_BUFFER_SIZE, 0)
    fun getReadTimeoutSec(): Int = prefs.getInt(PREF_READ_TIMEOUT, 10)

    fun setEnablePip(value: Boolean) = prefs.edit().putBoolean(PREF_ENABLE_PIP, value).apply()

    fun setBackgroundPlay(value: Boolean) = prefs.edit().putBoolean(PREF_BACKGROUND_PLAY, value).apply()

    fun setAspectRatio(value: Int) = prefs.edit().putInt(PREF_ASPECT_RATIO, value).apply()

    fun setMirrorFlip(value: Boolean) = prefs.edit().putBoolean(PREF_MIRROR_FLIP, value).apply()

    fun setShowLogos(value: Boolean) = prefs.edit().putBoolean(PREF_SHOW_LOGOS, value).apply()
    fun setAutoRefreshPlaylists(value: Boolean) = prefs.edit().putBoolean(PREF_AUTO_REFRESH, value).apply()
    fun setChannelLayout(value: Int) = prefs.edit().putInt(PREF_CHANNEL_LAYOUT, value).apply()
    fun setBufferSizeMb(value: Int) = prefs.edit().putInt(PREF_BUFFER_SIZE, value).apply()
    fun setReadTimeoutSec(value: Int) = prefs.edit().putInt(PREF_READ_TIMEOUT, value).apply()

    companion object {
        private const val PREF_AUTO_HIDE_CONTROLS = "iptv_auto_hide_controls"
        private const val PREF_ENABLE_PIP = "iptv_enable_pip"
        private const val PREF_BACKGROUND_PLAY = "iptv_background_play"
        private const val PREF_ASPECT_RATIO = "iptv_aspect_ratio"
        private const val PREF_MIRROR_FLIP = "iptv_mirror_flip"
        private const val PREF_QUALITY_PREFERENCE = "iptv_quality_preference"
        private const val PREF_SHOW_LOGOS = "iptv_show_logos"
        private const val PREF_AUTO_REFRESH = "iptv_auto_refresh_playlists"
        private const val PREF_CHANNEL_LAYOUT = "iptv_channel_layout"
        private const val PREF_BUFFER_SIZE = "iptv_buffer_size_mb"
        private const val PREF_READ_TIMEOUT = "iptv_read_timeout_sec"
    }
}
