package io.alwayszihan.zeroxpv.ui.iptv.viewmodel

import android.app.Application
import android.view.Surface
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.alwayszihan.zeroxpv.ui.iptv.manager.HistoryManager
import io.alwayszihan.zeroxpv.ui.iptv.parser.M3ULoader
import io.alwayszihan.zeroxpv.ui.iptv.parser.M3U8Channel
import io.alwayszihan.zeroxpv.ui.iptv.player.Media3Player
import io.alwayszihan.zeroxpv.ui.iptv.player.IptvPlayerHolder
import io.alwayszihan.zeroxpv.ui.iptv.player.QualityManager
import io.alwayszihan.zeroxpv.ui.iptv.player.VideoQuality
import io.alwayszihan.zeroxpv.ui.iptv.player.PlayerRepository
import io.alwayszihan.zeroxpv.ui.iptv.util.ErrorHandler
import io.alwayszihan.zeroxpv.ui.iptv.util.WatchTimeTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPosition: Long = 0L,
    val bufferedPosition: Long = 0L,
    val duration: Long = 0L,
    val videoTitle: String = "",
    val currentChannelUrl: String = "",
    val currentChannelId: String? = null,
    val actualPlayingUrl: String? = null,
    val channels: List<M3U8Channel> = emptyList(),
    val isLoadingChannels: Boolean = false,
    val showControls: Boolean = true,
    val enablePip: Boolean = true,
    val backgroundPlay: Boolean = false,
    val aspectRatio: Int = 3,
    val mirrorFlip: Boolean = false,
    val autoHideControls: Boolean = true,
    val playlistUrl: String = "",
    val shouldScrollToChannel: Boolean = false,
    val isFullscreen: Boolean = false,
    val availableQualities: List<VideoQuality> = emptyList(),
    val currentQuality: VideoQuality? = null,
    val isLocked: Boolean = false,
    val currentLicenseType: String? = null,
    val currentLicenseKey: String? = null,
    val showLogos: Boolean = true,
    val channelLayout: Int = 0
)

data class PlayerProgressState(
    val accumulatedWatchTime: Long = 0L,
    val estimatedProgress: Float = 0f,
    val currentCycleDuration: Float = 20f
)

class IptvPlayerViewModel(private val application: Application) : ViewModel() {

    private val repository = PlayerRepository(application)
    private val watchTimeTracker = WatchTimeTracker()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _progressState = MutableStateFlow(PlayerProgressState())
    val progressState: StateFlow<PlayerProgressState> = _progressState.asStateFlow()

    private var localPlayer: Media3Player? = null
    private var lastSurface: Surface? = null
    private var isBackgroundRetained = false
    private var lastAppliedQualityId: String? = null

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        _uiState.value = _uiState.value.copy(
            enablePip = repository.getEnablePip(),
            backgroundPlay = repository.getBackgroundPlay(),
            aspectRatio = repository.getAspectRatio(),
            mirrorFlip = repository.getMirrorFlip(),
            autoHideControls = repository.getAutoHideControls(),
            showLogos = repository.getShowLogos(),
            channelLayout = repository.getChannelLayout()
        )
    }

    fun refreshPreferences() {
        loadPreferences()
        lastAppliedQualityId = null
    }

    fun loadChannels(url: String, initialChannelUrl: String?, initialChannelId: String?, initialVideoTitle: String?) {
        val currentUrl = _uiState.value.playlistUrl
        if (currentUrl == url) {
            val startChannel = when {
                initialChannelUrl != null -> _uiState.value.channels.find { it.url == initialChannelUrl }
                initialChannelId != null -> _uiState.value.channels.find { it.id == initialChannelId }
                else -> null
            }
            if (startChannel != null) {
                _uiState.value = _uiState.value.copy(
                    currentChannelUrl = startChannel.url,
                    currentChannelId = startChannel.id,
                    videoTitle = startChannel.name,
                    shouldScrollToChannel = true,
                    currentLicenseType = startChannel.licenseType,
                    currentLicenseKey = startChannel.licenseKey
                )
                return
            }
        }

        localPlayer?.stop()

        M3ULoader.setEpgLoadedCallback { channelsWithEpg ->
            viewModelScope.launch(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(channels = channelsWithEpg)
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                channels = emptyList<M3U8Channel>(),
                isLoadingChannels = true,
                currentChannelUrl = "",
                currentChannelId = null,
                videoTitle = "",
                currentPosition = 0L,
                duration = 0L,
                bufferedPosition = 0L,
                isPlaying = false,
                isBuffering = false,
                playlistUrl = ""
            )
            _progressState.value = PlayerProgressState()
            watchTimeTracker.reset()

            try {
                var parsedChannels: List<M3U8Channel> = emptyList<M3U8Channel>()
                var isM3U8Playlist = false

                when {
                    url.startsWith("http") || url.startsWith("https") ||
                    url.startsWith("rtmp") || url.startsWith("rtsp") -> {
                        repository.parseM3U8FromUrl(url).onSuccess { channels ->
                            parsedChannels = channels
                            isM3U8Playlist = channels.isNotEmpty()
                        }.onError { _, _ ->
                        }
                    }
                    url.startsWith("content://") || url.startsWith("file://") -> {
                        try {
                            val uri = url.toUri()
                            application.contentResolver.openInputStream(uri)?.use { inputStream ->
                                val content = inputStream.bufferedReader().readText()
                                if (content.contains("#EXTM3U")) {
                                    repository.parseM3U8FromContent(content).onSuccess { channels ->
                                        parsedChannels = channels
                                        isM3U8Playlist = channels.isNotEmpty()
                                    }
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                    url.contains("#EXTM3U") -> {
                        repository.parseM3U8FromContent(url).onSuccess { channels ->
                            parsedChannels = channels
                            isM3U8Playlist = channels.isNotEmpty()
                        }
                    }
                }

                val channels = parsedChannels

                val startChannel = when {
                    initialChannelUrl != null -> channels.find { it.url == initialChannelUrl }
                        ?: channels.find { it.url.trim() == initialChannelUrl.trim() }
                    initialChannelId != null -> channels.find { it.id == initialChannelId }
                    else -> null
                } ?: channels.firstOrNull()

                val title = when {
                    startChannel != null -> startChannel.name
                    url.startsWith("content") || url.startsWith("file") -> "Local Video"
                    else -> "Unknown Video"
                }

                if (isM3U8Playlist) {
                    initialVideoTitle?.takeIf { it.isNotEmpty() } ?: url.substringAfterLast("/").substringBeforeLast(".").ifEmpty {
                        url.substringAfterLast("/").ifEmpty { "Playlist" }
                    }
                } else {
                    ""
                }

                _uiState.value = _uiState.value.copy(
                    channels = channels,
                    videoTitle = title,
                    currentChannelUrl = startChannel?.url ?: url,
                    currentChannelId = startChannel?.id,
                    playlistUrl = if (isM3U8Playlist) url else "",
                    isLoadingChannels = false,
                    shouldScrollToChannel = true,
                    currentLicenseType = startChannel?.licenseType,
                    currentLicenseKey = startChannel?.licenseKey
                )
            } catch (e: Exception) {
                ErrorHandler.logError("PlayerViewModel", "Failed to load channels", e)
                _uiState.value = _uiState.value.copy(isLoadingChannels = false)
            }
        }
    }

    fun updatePlaybackState(isPlaying: Boolean, position: Long, buffered: Long, duration: Long) {
        _uiState.value = _uiState.value.copy(
            isPlaying = isPlaying,
            currentPosition = position,
            bufferedPosition = buffered,
            duration = duration
        )

        if (isPlaying) {
            watchTimeTracker.start()
        } else {
            watchTimeTracker.pause()
        }
    }

    fun updateBufferingState(isBuffering: Boolean) {
        _uiState.value = _uiState.value.copy(isBuffering = isBuffering)
    }

    fun updateCycleDuration(newDuration: Float) {
        _progressState.value = _progressState.value.copy(currentCycleDuration = newDuration)
    }

    private var watchTimeJob: Job? = null

    fun startWatchTimeTracking() {
        watchTimeJob?.cancel()
        watchTimeJob = viewModelScope.launch(Dispatchers.Default) {
            delay(300L)
            while (true) {
                val watchTime = watchTimeTracker.getAccumulatedTime()
                val cycleDuration = _progressState.value.currentCycleDuration
                val estimatedProgress = if (cycleDuration > 0) {
                    (watchTime / (cycleDuration * 1000f)).coerceIn(0f, 1f)
                } else 0f
                _progressState.value = _progressState.value.copy(
                    accumulatedWatchTime = watchTime,
                    estimatedProgress = estimatedProgress
                )
                delay(1000L)
            }
        }
    }

    fun stopWatchTimeTracking() {
        watchTimeJob?.cancel()
        watchTimeJob = null
    }

    fun toggleControls() {
        _uiState.value = _uiState.value.copy(showControls = !_uiState.value.showControls)
    }

    fun showControls() {
        _uiState.value = _uiState.value.copy(showControls = true)
    }

    fun hideControls() {
        _uiState.value = _uiState.value.copy(showControls = false)
    }

    fun setLocked(locked: Boolean) {
        _uiState.value = _uiState.value.copy(isLocked = locked)
    }

    fun updateFullscreen(isFullscreen: Boolean) {
        _uiState.value = _uiState.value.copy(isFullscreen = isFullscreen)
    }

    fun switchChannel(channel: M3U8Channel) {
        if (_uiState.value.currentChannelUrl == channel.url) {
            return
        }
        val wasPlaying = _uiState.value.isPlaying
        lastAppliedQualityId = null
        _uiState.value = _uiState.value.copy(
            currentChannelUrl = channel.url,
            currentChannelId = channel.id,
            videoTitle = channel.name,
            currentPosition = 0L,
            duration = 0L,
            bufferedPosition = 0L,
            isPlaying = wasPlaying,
            isBuffering = false,
            shouldScrollToChannel = false,
            actualPlayingUrl = null,
            availableQualities = emptyList<VideoQuality>(),
            currentQuality = null,
            currentLicenseType = channel.licenseType,
            currentLicenseKey = channel.licenseKey
        )
        _progressState.value = PlayerProgressState(currentCycleDuration = 20f)
        watchTimeTracker.reset()
        recordLastChannel(channel)
    }

    private fun recordLastChannel(channel: M3U8Channel) {
        val playlistUrl = _uiState.value.playlistUrl
        if (playlistUrl.isBlank()) return
        val playlist = io.alwayszihan.zeroxpv.ui.iptv.manager.PlaylistManager.getPlaylists()
            .firstOrNull { it.primarySource?.source == playlistUrl || it.sources.any { s -> s.source == playlistUrl } }
            ?: return
        io.alwayszihan.zeroxpv.ui.iptv.manager.PlaylistManager.setLastChannel(
            application,
            playlist.id,
            channel.url,
            channel.name,
            channel.id
        )
    }

    fun clearScrollFlag() {
        _uiState.value = _uiState.value.copy(shouldScrollToChannel = false)
    }

    fun getOrCreatePlayer(): Media3Player {
        if (localPlayer == null) {
            localPlayer = Media3Player(application)
        }
        IptvPlayerHolder.setPlayer(localPlayer!!.exoPlayer)

        val player = localPlayer!!
        player.apply {
            setHardwareAcceleration(true)
            onPreparedListener = {
                _uiState.value = _uiState.value.copy(actualPlayingUrl = getActualPlayingUrl())
            }
            onBufferingListener = { buffering ->
                updateBufferingState(buffering)
            }
            onPlaybackStateChanged = { playing, position, buffered, dur ->
                updatePlaybackState(playing, position, buffered, dur)
            }
            onErrorListener = { errorMsg, _ ->
                viewModelScope.launch(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        application,
                        errorMsg,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            onTracksChangedListener = { tracks ->
                exoPlayer?.let { exo ->
                    val (qualities, selected) = QualityManager.parseQualities(tracks, exo.trackSelectionParameters)

                    val finalQuality = if (selected == null && qualities.isNotEmpty()) {
                        val qualityPref = repository.getQualityPreference()
                        val autoSelected = QualityManager.selectQualityByPreference(qualities, qualityPref)
                        if (autoSelected != null) {
                            QualityManager.setQuality(exo, autoSelected)
                            lastAppliedQualityId = autoSelected.id
                            autoSelected
                        } else {
                            null
                        }
                    } else {
                        selected
                    }

                    _uiState.value = _uiState.value.copy(
                        availableQualities = qualities,
                        currentQuality = finalQuality
                    )
                }
            }
            onPlayerRecreatedListener = { exo ->
                IptvPlayerHolder.setPlayer(exo)
            }
        }

        return player
    }

    fun releasePlayer() {
        IptvPlayerHolder.setPlayer(null)
        localPlayer?.let { player ->
            player.pause()
            player.onErrorListener = null
            player.release()
        }
        localPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        stopWatchTimeTracking()
        M3ULoader.clearEpgLoadedCallback()
        releasePlayer()
    }

    fun updateEnablePip(value: Boolean) {
        repository.setEnablePip(value)
        _uiState.value = _uiState.value.copy(enablePip = value)
    }

    fun updateBackgroundPlay(value: Boolean) {
        repository.setBackgroundPlay(value)
        _uiState.value = _uiState.value.copy(backgroundPlay = value)
    }

    fun updateAspectRatio(value: Int) {
        repository.setAspectRatio(value)
        _uiState.value = _uiState.value.copy(aspectRatio = value)
    }

    fun updateMirrorFlip(value: Boolean) {
        repository.setMirrorFlip(value)
        _uiState.value = _uiState.value.copy(mirrorFlip = value)
    }

    fun setVideoQuality(quality: VideoQuality?) {
        val player = localPlayer
        player?.let { p ->
            QualityManager.setQuality(p.exoPlayer, quality)
        }
    }

    fun getQualityPreference(): Int = repository.getQualityPreference()

    fun saveHistory(force: Boolean = false) {
        val state = _uiState.value
        val watchTime = watchTimeTracker.getAccumulatedTime()
        if (!force && watchTime == 0L) {
            return
        }
        val urlToSave = state.playlistUrl.ifEmpty { state.currentChannelUrl }
        if (urlToSave.isNotEmpty() && state.videoTitle.isNotEmpty()) {
            val logoUrl = state.channels.find { it.url == state.currentChannelUrl }?.logoUrl ?: ""
            HistoryManager.addOrUpdateHistory(
                urlToSave,
                state.videoTitle,
                state.currentChannelUrl,
                state.currentChannelId,
                logoUrl
            )
        }
    }

    fun getWatchTimeTracker() = watchTimeTracker

    fun onAppBackground() {
        if (_uiState.value.backgroundPlay) {
            isBackgroundRetained = true
        }
    }

    fun onAppForeground() {
        if (isBackgroundRetained) {
            isBackgroundRetained = false
            localPlayer?.exoPlayer?.let { player ->
                if (lastSurface != null && lastSurface!!.isValid) {
                    player.setVideoSurface(lastSurface)
                }
            }
        }
    }
}
