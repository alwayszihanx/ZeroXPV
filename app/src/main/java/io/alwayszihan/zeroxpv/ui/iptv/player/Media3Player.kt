package io.alwayszihan.zeroxpv.ui.iptv.player

import android.content.Context
import android.view.Surface
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import io.alwayszihan.zeroxpv.ui.iptv.util.NetworkUtils
import androidx.media3.common.Tracks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal object KodiUrlParser {
    private const val HTTP_OPTION_UA = "user-agent"

    fun parseKodiUrlOptions(url: String): Map<String, String?> {
        val index = url.indexOf('|')
        if (index == -1) return emptyMap()
        val options = url.drop(index + 1).split("&")
        return options
            .filter { it.isNotBlank() }
            .associate {
                val pair = it.split("=")
                val key = pair.getOrNull(0).orEmpty()
                val value = pair.getOrNull(1)
                key to value
            }
    }

    fun extractUserAgent(url: String): String? {
        val kodiOptions = parseKodiUrlOptions(url)
        return kodiOptions[HTTP_OPTION_UA]
    }

    fun cleanUrl(url: String): String {
        val index = url.indexOf('|')
        return if (index == -1) url else url.take(index)
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class Media3Player(private val context: Context) {
    var exoPlayer: ExoPlayer? = null
        private set
    internal var surface: Surface? = null
    private var currentHardwareAcceleration: Boolean = true
    private var initialM3uUrl: String? = null
    private var actualPlayingUrl: String? = null
    private var currentLicenseType: String? = null
    private var currentLicenseKey: String? = null
    private var currentUserAgent: String? = null
    private val state = PlayerState(context = context)

    var onPreparedListener: (() -> Unit)? = null
    var onBufferingListener: ((Boolean) -> Unit)? = null
    var onPlaybackStateChanged: ((isPlaying: Boolean, position: Long, bufferedPosition: Long, duration: Long) -> Unit)? = null
    var onErrorListener: ((error: String, isRetriable: Boolean) -> Unit)? = null
    var onTracksChangedListener: ((Tracks) -> Unit)? = null
    var onPlayerRecreatedListener: ((ExoPlayer?) -> Unit)? = null

    init {
        NetworkUtils.setupAndroid7SSL()
        state.context = context
        createPlayer(true)
    }

    private fun createPlayer(hardwareAcceleration: Boolean) {
        onPlayerRecreatedListener?.invoke(null)
        val callbacks = PlayerCallbacks(
            onPrepared = { onPreparedListener?.invoke() },
            onBuffering = { onBufferingListener?.invoke(it) },
            onStateChanged = { playing, pos, buffered, dur -> onPlaybackStateChanged?.invoke(playing, pos, buffered, dur) },
            onError = { msg, retriable -> onErrorListener?.invoke(msg, retriable) },
            onRetryWithFix = { retryWithFixedM3u8() },
            onReloadOriginal = { reloadOriginalUrl() },
            onRetryWithSoftwareDecoder = { retryWithSoftwareDecoder() }
        )

        exoPlayer = PlayerFactory.createExoPlayer(
            context = context,
            hardwareAcceleration = hardwareAcceleration,
            urlUpgradeListener = PlayerListeners.createUrlUpgradeListener(
                isInitialLoad = { state.isInitialLoad },
                initialM3uUrl = { initialM3uUrl },
                onUrlResolved = { actualPlayingUrl = it }
            ),
            latencyMonitor = PlayerFactory.createLatencyMonitor { exoPlayer },
            combinedListener = PlayerListeners.createCombinedListener({ exoPlayer }, state, callbacks),
            licenseType = currentLicenseType,
            licenseKey = currentLicenseKey,
            userAgent = currentUserAgent
        )
        onPlayerRecreatedListener?.invoke(exoPlayer)

        exoPlayer?.addListener(object : androidx.media3.common.Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                onTracksChangedListener?.invoke(tracks)
            }
            override fun onTrackSelectionParametersChanged(parameters: androidx.media3.common.TrackSelectionParameters) {
                exoPlayer?.currentTracks?.let { onTracksChangedListener?.invoke(it) }
            }
        })
    }

    fun attachSurface(surface: Surface?) {
        if (this.surface === surface) {
            return
        }
        this.surface = surface
        try {
            if (surface == null || surface.isValid) {
                exoPlayer?.setVideoSurface(surface)
            }
        } catch (_: Exception) {
        }
    }

    fun setDataSource(url: String, licenseType: String? = null, licenseKey: String? = null, title: String? = null) {
        val cleanedUrl = KodiUrlParser.cleanUrl(url)
        val userAgent = KodiUrlParser.extractUserAgent(url)

        initialM3uUrl = cleanedUrl
        currentLicenseType = licenseType
        currentLicenseKey = licenseKey
        currentUserAgent = userAgent

        state.currentMediaUrl = cleanedUrl
        state.isInitialLoad = true
        state.hasTriedM3u8Fix = false
        state.isFixingM3u8 = false
        state.hasTriedSoftwareDecoder = false
        actualPlayingUrl = null

        val currentSurface = surface
        val isRtmp = cleanedUrl.startsWith("rtmp", ignoreCase = true)
        val itemTitle = title ?: ""

        val needsRecreate = (licenseType != null && licenseType != currentLicenseType) ||
                            (licenseKey != null && licenseKey != currentLicenseKey) ||
                            (userAgent != null && userAgent != currentUserAgent)

        if (needsRecreate && exoPlayer != null) {
            val currentPos = exoPlayer?.currentPosition ?: 0L
            val wasPlaying = exoPlayer?.isPlaying ?: false

            release()
            createPlayer(currentHardwareAcceleration)

            if (currentPos > 0) {
                exoPlayer?.seekTo(currentPos)
            }
            if (wasPlaying) {
                state.shouldPlayWhenReady = true
            }
        }

        exoPlayer?.apply {
            stop()
            clearMediaItems()
            clearVideoSurface()

            if (isRtmp) {
                val rtmpSource = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(
                    androidx.media3.datasource.rtmp.RtmpDataSource.Factory()
                ).createMediaSource(MediaItem.fromUri(cleanedUrl))
                setMediaSource(rtmpSource)
            } else {
                setMediaItem(createMediaItem(cleanedUrl, itemTitle))
            }

            prepare()

            if (currentSurface != null && currentSurface.isValid) {
                setVideoSurface(currentSurface)
            }
        }
    }

    private fun createMediaItem(url: String, title: String = ""): MediaItem {
        val builder = MediaItem.Builder().setUri(url)
        if (title.isNotEmpty()) {
            builder.setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_TV_SHOW)
                    .build()
            )
        }
        return builder.build()
    }

    internal fun retryWithFixedM3u8() {
        val url = initialM3uUrl ?: return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val fixedUrl = UrlInterceptor.fixMalformedM3u8(context, url)
                state.currentMediaUrl = fixedUrl

                exoPlayer?.apply {
                    setMediaItem(createMediaItem(fixedUrl))
                    prepare()
                    if (state.shouldPlayWhenReady) play()
                }
                state.isFixingM3u8 = false
            } catch (e: UrlInterceptor.NetworkBlockedException) {
                onBufferingListener?.invoke(false)
                state.isFixingM3u8 = false
                onErrorListener?.invoke(e.message ?: "Network connection failed", false)
            } catch (_: Exception) {
                onBufferingListener?.invoke(false)
                state.isFixingM3u8 = false
            }
        }
    }

    internal fun reloadOriginalUrl() {
        val url = initialM3uUrl ?: return
        state.hasTriedM3u8Fix = false

        exoPlayer?.apply {
            stop()
            clearMediaItems()
            setMediaItem(createMediaItem(url))
            prepare()
            if (state.shouldPlayWhenReady) play()
        }
    }

    internal fun retryWithSoftwareDecoder() {
        if (currentHardwareAcceleration) {
            val currentUrl = exoPlayer?.currentMediaItem?.localConfiguration?.uri?.toString()
            val currentPos = exoPlayer?.currentPosition ?: 0L
            val wasPlaying = exoPlayer?.isPlaying ?: false

            currentHardwareAcceleration = false
            release()
            createPlayer(false)

            currentUrl?.let {
                setDataSource(it, currentLicenseType, currentLicenseKey)
                exoPlayer?.seekTo(currentPos)
                if (wasPlaying) start()
            }
        }
    }

    fun start() {
        state.shouldPlayWhenReady = true
        exoPlayer?.let {
            val s = surface
            if (s != null && s.isValid) {
                it.setVideoSurface(s)
            }
            if (it.playbackState == androidx.media3.common.Player.STATE_IDLE) {
                it.prepare()
            }
            it.play()
        }
    }

    fun pause() {
        state.shouldPlayWhenReady = false
        exoPlayer?.pause()
    }

    fun stop() {
        exoPlayer?.stop()
        exoPlayer?.clearVideoSurface()
    }

    fun clearVideoSurface() {
        exoPlayer?.clearVideoSurface()
    }

    fun setHardwareAcceleration(enabled: Boolean) {
        if (currentHardwareAcceleration != enabled) {
            currentHardwareAcceleration = enabled
            val currentUrl = exoPlayer?.currentMediaItem?.localConfiguration?.uri?.toString()
            val currentPos = exoPlayer?.currentPosition ?: 0L
            val wasPlaying = exoPlayer?.isPlaying ?: false

            release()
            createPlayer(enabled)

            currentUrl?.let {
                setDataSource(it, currentLicenseType, currentLicenseKey)
                exoPlayer?.seekTo(currentPos)
                if (wasPlaying) start()
            }
        }
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }

    val isPlaying: Boolean get() = exoPlayer?.isPlaying ?: false
    val currentPosition: Long get() = exoPlayer?.currentPosition ?: 0L
    val bufferedPosition: Long get() = exoPlayer?.bufferedPosition ?: 0L
    val duration: Long get() = exoPlayer?.duration?.takeIf { it != C.TIME_UNSET } ?: 0L

    fun getActualPlayingUrl(): String? = actualPlayingUrl
}
