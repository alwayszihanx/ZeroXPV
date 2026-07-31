package io.alwayszihan.zeroxpv.ui.iptv

import android.app.Application
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import io.alwayszihan.zeroxpv.presentation.Screen
import io.alwayszihan.zeroxpv.ui.iptv.component.*
import io.alwayszihan.zeroxpv.ui.browser.NavigationBarState
import io.alwayszihan.zeroxpv.ui.iptv.manager.PlaylistManager
import io.alwayszihan.zeroxpv.ui.iptv.player.Media3Player
import io.alwayszihan.zeroxpv.ui.iptv.player.QualityManager
import io.alwayszihan.zeroxpv.ui.iptv.viewmodel.IptvPlayerViewModel
import io.alwayszihan.zeroxpv.ui.utils.LocalBackStack
import io.alwayszihan.zeroxpv.ui.utils.popSafely
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
data class IptvPlayerScreen(
    val url: String,
    val initialChannelUrl: String? = null,
    val initialVideoTitle: String? = null,
    val initialChannelId: String? = null
) : Screen {

    @Composable
    override fun Content() {
        val backstack = LocalBackStack.current
        val context = LocalContext.current
        val activity = context as? ComponentActivity
        val scope = rememberCoroutineScope()
        val lifecycleOwner = LocalLifecycleOwner.current
        val app = context.applicationContext as Application

        val viewModel: IptvPlayerViewModel = viewModel(
            key = url,
            factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return IptvPlayerViewModel(app) as T
                }
            }
        )

        DisposableEffect(Unit) {
            activity?.window?.setSustainedPerformanceMode(true)
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose {
                activity?.window?.setSustainedPerformanceMode(false)
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        DisposableEffect(Unit) {
            NavigationBarState.updateBottomBarVisibility(false)
            onDispose {
                NavigationBarState.updateBottomBarVisibility(true)
            }
        }

        val uiState by viewModel.uiState.collectAsState()
        val progressState by viewModel.progressState.collectAsState()
        val watchTimeTracker = viewModel.getWatchTimeTracker()

        var settingsVersion by remember { mutableIntStateOf(0) }

        val allPlaylists by PlaylistManager.playlistsFlow.collectAsState()
        val currentPlaylist = remember(uiState.playlistUrl, allPlaylists) {
            allPlaylists.firstOrNull { pl ->
                pl.sources.any { s -> s.source == uiState.playlistUrl }
            }
        }

        var media3Player by remember {
            mutableStateOf(viewModel.getOrCreatePlayer())
        }

        LaunchedEffect(media3Player, settingsVersion) {
            val decoderType = 0
            media3Player.setHardwareAcceleration(decoderType == 0)

            if (settingsVersion > 0 && uiState.availableQualities.isNotEmpty()) {
                val qualityPref = viewModel.getQualityPreference()
                val autoSelected = QualityManager.selectQualityByPreference(uiState.availableQualities, qualityPref)
                if (autoSelected != null) {
                    viewModel.setVideoQuality(autoSelected)
                }
            }
        }

        val isFullscreen = uiState.isFullscreen
        val listState = rememberLazyListState()
        var showPlayerSettings by remember { mutableStateOf(false) }
        var showEpgSidebar by remember { mutableStateOf(false) }

        val gestureState = rememberIptvGestureControlState(context, activity)

        val originalBrightness = remember {
            activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: -1f
        }

        val configuration = LocalConfiguration.current
        var pendingAutoPlay by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            viewModel.refreshPreferences()
        }

        var isPlayerClean by remember { mutableStateOf(false) }

        LaunchedEffect(url) {
            media3Player.apply {
                stop()
                clearVideoSurface()
                exoPlayer?.clearMediaItems()
            }
            isPlayerClean = true
            viewModel.loadChannels(url, initialChannelUrl, initialChannelId, initialVideoTitle)
        }

        LaunchedEffect(url, uiState.currentChannelUrl) {
            if (uiState.currentChannelUrl.isNotEmpty()) {
                val currentMediaUrl = media3Player.exoPlayer?.currentMediaItem?.localConfiguration?.uri?.toString()
                val autoPlayEnabled = true

                if (currentMediaUrl != uiState.currentChannelUrl) {
                    media3Player.setDataSource(
                        uiState.currentChannelUrl,
                        uiState.currentLicenseType,
                        uiState.currentLicenseKey,
                        uiState.videoTitle
                    )
                    if (autoPlayEnabled || uiState.isPlaying) {
                        pendingAutoPlay = true
                        media3Player.start()
                    }
                } else if (autoPlayEnabled && !media3Player.isPlaying) {
                    media3Player.start()
                }
            }
        }

        LaunchedEffect(uiState.shouldScrollToChannel, uiState.channels, uiState.currentChannelUrl) {
            if (uiState.shouldScrollToChannel && uiState.channels.isNotEmpty() && uiState.currentChannelUrl.isNotEmpty()) {
                val currentIndex = uiState.channels.indexOfFirst { it.url == uiState.currentChannelUrl }
                if (currentIndex >= 0) {
                    listState.scrollToItem(currentIndex)
                }
                viewModel.clearScrollFlag()
            }
        }

        LaunchedEffect(uiState.videoTitle, uiState.currentChannelUrl, uiState.currentChannelId) {
            viewModel.saveHistory()
        }

        LaunchedEffect(uiState.isPlaying) {
            if (uiState.isPlaying) {
                viewModel.startWatchTimeTracking()
            } else {
                viewModel.stopWatchTimeTracking()
            }
        }

        LaunchedEffect(uiState.showControls, uiState.autoHideControls, uiState.isPlaying, uiState.isBuffering) {
            if (uiState.autoHideControls && uiState.showControls && uiState.isPlaying && !uiState.isBuffering) {
                delay(5000L)
                viewModel.hideControls()
            }
        }

        LaunchedEffect(uiState.autoHideControls) {
            viewModel.showControls()
        }

        LaunchedEffect(uiState.isBuffering) {
            if (uiState.isBuffering) {
                viewModel.showControls()
            }
        }

        var wasPlayingBeforePause by remember { mutableStateOf(false) }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        if (activity?.isInPictureInPictureMode == true) {
                            wasPlayingBeforePause = media3Player.isPlaying
                        } else if (uiState.backgroundPlay && !(activity?.isChangingConfigurations == true)) {
                            viewModel.saveHistory(force = true)
                            wasPlayingBeforePause = media3Player.isPlaying
                            media3Player.attachSurface(null)
                            IptvPlaybackService.start(context)
                        } else {
                            viewModel.saveHistory(force = true)
                            wasPlayingBeforePause = media3Player.isPlaying
                            media3Player.pause()
                        }
                    }

                    Lifecycle.Event.ON_RESUME -> {
                        val oldSettingsVersion = settingsVersion
                        viewModel.refreshPreferences()
                        settingsVersion = oldSettingsVersion + 1

                        val currentPos = media3Player.currentPosition
                        val buffered = media3Player.bufferedPosition
                        val dur = media3Player.duration
                        val isPlaying = media3Player.isPlaying

                        viewModel.updatePlaybackState(isPlaying, currentPos, buffered, dur)

                        if (uiState.backgroundPlay) {
                            IptvPlaybackService.stop(context)
                        }

                        if (wasPlayingBeforePause) {
                            pendingAutoPlay = true
                            wasPlayingBeforePause = false
                        }
                    }

                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                viewModel.saveHistory(force = true)
                media3Player.pause()
                media3Player.attachSurface(null)
                IptvPlaybackService.stop(context)
                activity?.window?.attributes = activity.window.attributes.apply {
                    screenBrightness = originalBrightness
                }
            }
        }

        LaunchedEffect(isFullscreen) {
            activity?.window?.let { window ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (isFullscreen) {
                        window.insetsController?.hide(android.view.WindowInsets.Type.systemBars())
                        window.insetsController?.systemBarsBehavior =
                            android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        window.insetsController?.show(android.view.WindowInsets.Type.systemBars())
                    }
                }
            }
            activity?.requestedOrientation =
                if (isFullscreen) {
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
        }

        BackHandler(enabled = true) {
            if (isFullscreen) {
                viewModel.updateFullscreen(false)
            } else {
                viewModel.saveHistory(force = true)
                media3Player.pause()
                backstack.popSafely()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize()
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isFullscreen) PaddingValues(0.dp) else paddingValues)
                ) {
                    IptvGestureControlEffects(
                        gestureState = gestureState,
                        onGestureStateChange = { gestureState.value = it }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isFullscreen) {
                                    Modifier.fillMaxHeight()
                                } else {
                                    Modifier.aspectRatio(16f / 9f)
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .iptvGestureControlModifier(
                                        context = context,
                                        activity = activity,
                                        gestureState = gestureState,
                                        onGestureStateChange = { gestureState.value = it }
                                    )
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        viewModel.toggleControls()
                                    }
                            ) {
                                if (uiState.currentChannelUrl.isNotEmpty() && isPlayerClean) {
                                    IptvPlayerSurface(
                                        player = media3Player,
                                        aspectRatio = uiState.aspectRatio,
                                        mirrorFlip = uiState.mirrorFlip,
                                        onSurfaceReady = {
                                            if (pendingAutoPlay && !media3Player.isPlaying) {
                                                pendingAutoPlay = false
                                                media3Player.start()
                                            } else if (pendingAutoPlay) {
                                                pendingAutoPlay = false
                                            }
                                        },
                                        currentChannelUrl = uiState.currentChannelUrl
                                    )
                                }

                                val onPlayPauseClick = remember(uiState.isPlaying) {
                                    {
                                        if (uiState.isPlaying) {
                                            media3Player.pause()
                                        } else {
                                            media3Player.start()
                                        }
                                    }
                                }

                                val onBackClick = remember(isFullscreen) {
                                    {
                                        if (isFullscreen) {
                                            viewModel.updateFullscreen(false)
                                        } else {
                                            viewModel.saveHistory(force = true)
                                            backstack.popSafely()
                                            Unit
                                        }
                                    }
                                }

                                val onFullscreenClick = remember {
                                    { viewModel.updateFullscreen(!uiState.isFullscreen) }
                                }

                                val onSettingsClick = remember {
                                    { showPlayerSettings = true }
                                }

                                val onEpgClick = remember {
                                    { showEpgSidebar = !showEpgSidebar }
                                }

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = uiState.showControls,
                                    enter = fadeIn(tween(100)),
                                    exit = fadeOut(tween(100))
                                ) {
                                    IptvPlayerControlsOverlay(
                                        modifier = Modifier.fillMaxSize(),
                                        isPlaying = uiState.isPlaying,
                                        isFullscreen = isFullscreen,
                                        enablePip = uiState.enablePip,
                                        isBuffering = uiState.isBuffering,
                                        activity = activity,
                                        media3Player = media3Player,
                                        watchTimeTracker = watchTimeTracker,
                                        currentCycleDuration = progressState.currentCycleDuration,
                                        onCycleDurationChange = { viewModel.updateCycleDuration(it) },
                                        onPlayPauseClick = onPlayPauseClick,
                                        onBackClick = onBackClick,
                                        onFullscreenClick = onFullscreenClick,
                                        onSettingsClick = onSettingsClick,
                                        isLocked = uiState.isLocked,
                                        onLockChange = { viewModel.setLocked(it) },
                                        onEpgClick = onEpgClick,
                                        hasEpgData = uiState.channels.find { it.url == uiState.currentChannelUrl }?.epgPrograms?.isNotEmpty() == true,
                                        epgPrograms = uiState.channels.find { it.url == uiState.currentChannelUrl }?.epgPrograms ?: emptyList()
                                    )
                                }

                                IptvVolumeIndicator(
                                    show = gestureState.value.showVolumeIndicator,
                                    value = gestureState.value.volumeIndicatorValue,
                                    modifier = Modifier.align(Alignment.Center)
                                )

                                IptvBrightnessIndicator(
                                    show = gestureState.value.showBrightnessIndicator,
                                    value = gestureState.value.brightnessIndicatorValue,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            if (isFullscreen) {
                                val currentChannel = uiState.channels.find { it.url == uiState.currentChannelUrl }
                                IptvEpgSidebar(
                                    visible = showEpgSidebar,
                                    programs = currentChannel?.epgPrograms ?: emptyList(),
                                    onDismiss = { showEpgSidebar = false }
                                )
                            }
                        }
                    }

                    if (!isFullscreen) {
                        if (uiState.videoTitle.isNotEmpty()) {
                            Text(
                                text = uiState.videoTitle,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        IptvChannelListSection(
                            channels = uiState.channels,
                            currentChannelUrl = uiState.currentChannelUrl,
                            actualPlayingUrl = uiState.actualPlayingUrl,
                            isLoadingChannels = uiState.isLoadingChannels,
                            listState = listState,
                            media3Player = media3Player,
                            showLogos = uiState.showLogos,
                            layout = uiState.channelLayout,
                            favouriteUrls = currentPlaylist?.favourites?.toSet() ?: emptySet(),
                            onToggleFavourite = { channel ->
                                currentPlaylist?.let {
                                    PlaylistManager.toggleFavourite(context, it.id, channel.url)
                                }
                            },
                            onChannelClick = { channel ->
                                viewModel.switchChannel(channel)
                            },
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }
            }

            if (showPlayerSettings) {
                IptvPlayerSettingsBottomSheet(
                    enablePip = uiState.enablePip,
                    backgroundPlay = uiState.backgroundPlay,
                    aspectRatio = uiState.aspectRatio,
                    mirrorFlip = uiState.mirrorFlip,
                    onEnablePipChange = { viewModel.updateEnablePip(it) },
                    onBackgroundPlayChange = { viewModel.updateBackgroundPlay(it) },
                    onAspectRatioChange = { viewModel.updateAspectRatio(it) },
                    onMirrorFlipChange = { viewModel.updateMirrorFlip(it) },
                    onDismiss = { showPlayerSettings = false },
                    availableQualities = uiState.availableQualities,
                    currentQuality = uiState.currentQuality,
                    onQualityChange = { viewModel.setVideoQuality(it) }
                )
            }
        }
    }
}
