package io.alwayszihan.zeroxpv.ui.iptv.component

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.net.toUri
import android.os.PowerManager
import android.provider.Settings
import android.util.Rational
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import io.alwayszihan.zeroxpv.ui.icons.Icon
import io.alwayszihan.zeroxpv.ui.icons.Icons
import io.alwayszihan.zeroxpv.ui.iptv.parser.EpgProgram
import io.alwayszihan.zeroxpv.ui.iptv.player.Media3Player
import io.alwayszihan.zeroxpv.ui.iptv.player.VideoQuality
import io.alwayszihan.zeroxpv.ui.iptv.util.WatchTimeTracker
import kotlinx.coroutines.delay

private fun Context.getEpgMarkersCount(): Int {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    return prefs.getInt("epg_markers_count", 3)
}

@Composable
fun IptvPlayerControlsOverlay(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    isFullscreen: Boolean,
    enablePip: Boolean,
    isBuffering: Boolean,
    activity: Activity?,
    media3Player: Media3Player?,
    watchTimeTracker: WatchTimeTracker,
    currentCycleDuration: Float,
    onCycleDurationChange: (Float) -> Unit,
    onPlayPauseClick: () -> Unit,
    onBackClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isLocked: Boolean,
    onLockChange: (Boolean) -> Unit,
    onEpgClick: () -> Unit = {},
    hasEpgData: Boolean = false,
    epgPrograms: List<EpgProgram> = emptyList()
) {
    var currentPosition by remember { mutableLongStateOf(media3Player?.currentPosition ?: 0L) }
    var bufferedPosition by remember { mutableLongStateOf(media3Player?.bufferedPosition ?: 0L) }
    
    var estimatedProgress by rememberSaveable { mutableFloatStateOf(0f) }
    var localCycleDuration by remember { mutableFloatStateOf(currentCycleDuration) }
    
    LaunchedEffect(currentCycleDuration) {
        localCycleDuration = currentCycleDuration
    }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentPosition = media3Player?.currentPosition ?: 0L
            bufferedPosition = media3Player?.bufferedPosition ?: 0L
            
            watchTimeTracker.update()
            val watchSeconds = watchTimeTracker.getAccumulatedTime() / 1000f
            val baseProgress = (watchSeconds / localCycleDuration).coerceIn(0f, 0.95f)
            
            if (baseProgress >= 0.95f) {
                val bufferedSeconds = ((bufferedPosition - currentPosition) / 1000f).coerceAtLeast(0.5f)
                val newCycleDuration = localCycleDuration + bufferedSeconds
                val backtrackAmount = (bufferedSeconds / newCycleDuration * 0.2f).coerceIn(0.05f, 0.2f)
                estimatedProgress = (0.95f - backtrackAmount).coerceIn(0.75f, 0.9f)
                localCycleDuration = newCycleDuration
                onCycleDurationChange(newCycleDuration)
            } else {
                estimatedProgress = baseProgress
            }
            
            delay(100)
        }
    }
    
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        if (isFullscreen) {
            IconButton(
                onClick = { onLockChange(!isLocked) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Filled.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        
        if (!isLocked) {
            if (isFullscreen) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Player settings",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Player settings",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                    ){
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                        IconButton(onClick = onPlayPauseClick) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IptvProgressBar(
                            modifier = Modifier.weight(1f),
                            isBuffering = isBuffering,
                            estimatedProgress = estimatedProgress,
                            epgPrograms = epgPrograms,
                            cycleDurationMs = (localCycleDuration * 1000).toLong()
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        if (enablePip && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(
                                onClick = {
                                    activity?.let { act ->
                                        if (act.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                                            val aspectRatio = Rational(16, 9)
                                            val builder = PictureInPictureParams.Builder()
                                                .setAspectRatio(aspectRatio)
                                            act.enterPictureInPictureMode(builder.build())
                                        }
                                    }
                                },
                                modifier = Modifier.offset(x = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = "Picture in Picture",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        if (isFullscreen && hasEpgData) {
                            IconButton(
                                onClick = onEpgClick,
                                modifier = Modifier.offset(x = (-2).dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewList,
                                    contentDescription = "EPG",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onFullscreenClick,
                            modifier = Modifier.offset(x = if (enablePip && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                if (isFullscreen && hasEpgData) (-6).dp else (-2).dp
                            } else 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitScreen,
                                contentDescription = if (isFullscreen) "Exit fullscreen" else "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IptvProgressBar(
    modifier: Modifier = Modifier,
    isBuffering: Boolean,
    estimatedProgress: Float,
    epgPrograms: List<EpgProgram> = emptyList(),
    cycleDurationMs: Long = 0L
) {
    val count = LocalContext.current.getEpgMarkersCount()
    val currentTime = System.currentTimeMillis()
    val currentProgram = epgPrograms.find { currentTime in it.startTime..it.stopTime }
    
    Column(modifier = modifier) {
        when {
            isBuffering -> IptvUnifiedProgressBar(mode = ProgressBarMode.Indeterminate)
            epgPrograms.isNotEmpty() && currentProgram != null && count > 0 -> {
                val upcomingPrograms = epgPrograms.filter { it.startTime > currentTime }.take(count)
                val totalDuration = (currentProgram.stopTime - currentProgram.startTime) + upcomingPrograms.dropLast(1).sumOf { it.stopTime - it.startTime }
                val currentPassed = currentTime - currentProgram.startTime
                val progress = if (totalDuration > 0) currentPassed.toFloat() / totalDuration else 0f
                
                IptvUnifiedProgressBar(
                    mode = ProgressBarMode.EpgBased(progress),
                    epgPrograms = epgPrograms,
                    totalDurationMs = totalDuration,
                    count = count
                )
            }
            else -> IptvUnifiedProgressBar(
                mode = ProgressBarMode.Estimated(estimatedProgress)
            )
        }
    }
}

private sealed class ProgressBarMode {
    object Indeterminate : ProgressBarMode()
    data class Determinate(val currentPosition: Long, val duration: Long) : ProgressBarMode()
    data class Estimated(val progress: Float) : ProgressBarMode()
    data class EpgBased(val progress: Float) : ProgressBarMode()
}

@Composable
private fun IptvUnifiedProgressBar(
    mode: ProgressBarMode,
    modifier: Modifier = Modifier,
    onSeek: ((Long) -> Unit)? = null,
    epgPrograms: List<EpgProgram> = emptyList(),
    totalDurationMs: Long = 0L,
    count: Int = 0
) {
    when (mode) {
        is ProgressBarMode.Indeterminate -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(6.dp)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
        is ProgressBarMode.Determinate -> {
            val progress = if (mode.duration > 0) {
                (mode.currentPosition.toFloat() / mode.duration.toFloat()).coerceIn(0f, 1f)
            } else 0f
            
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .pointerInput(mode.duration) {
                        detectTapGestures { offset ->
                            if (mode.duration > 0 && onSeek != null) {
                                val clickProgress = (offset.x / size.width).coerceIn(0f, 1f)
                                val newPosition = (mode.duration * clickProgress).toLong()
                                onSeek(newPosition)
                            }
                        }
                    }
                    .pointerInput(mode.duration) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            if (mode.duration > 0 && onSeek != null) {
                                val dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                                val newPosition = (mode.duration * dragProgress).toLong()
                                onSeek(newPosition)
                            }
                        }
                    }
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
        is ProgressBarMode.Estimated -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(6.dp)
            ) {
                LinearProgressIndicator(
                    progress = { mode.progress.coerceIn(0f, 0.95f) },
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
        is ProgressBarMode.EpgBased -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(6.dp)
            ) {
                LinearProgressIndicator(
                    progress = { mode.progress.coerceIn(0f, 0.95f) },
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                
                if (epgPrograms.isNotEmpty() && totalDurationMs > 0) {
                    IptvEpgMarkers(
                        programs = epgPrograms,
                        totalDurationMs = totalDurationMs,
                        count = count,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IptvPlayerSettingsBottomSheet(
    enablePip: Boolean,
    backgroundPlay: Boolean,
    aspectRatio: Int,
    mirrorFlip: Boolean,
    onEnablePipChange: (Boolean) -> Unit,
    onBackgroundPlayChange: (Boolean) -> Unit,
    onAspectRatioChange: (Int) -> Unit,
    onMirrorFlipChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    availableQualities: List<VideoQuality> = emptyList(),
    currentQuality: VideoQuality? = null,
    onQualityChange: (VideoQuality?) -> Unit = {}
) {
    val context = LocalContext.current
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isPipSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    
    var isBatteryOptimized by remember {
        mutableStateOf(!pm.isIgnoringBatteryOptimizations(context.packageName))
    }
    
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = "package:${context.packageName}".toUri()
    }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isBatteryOptimized = !pm.isIgnoringBatteryOptimizations(context.packageName)
    }
    
    val backgroundPlayInteractionSource = remember { MutableInteractionSource() }
    
    var currentScreen by remember { mutableStateOf(SettingsScreen.MAIN) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetMaxWidth = 640.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        androidx.compose.animation.AnimatedContent(
            targetState = currentScreen,
            label = "settings_navigation"
        ) { screen ->
            when (screen) {
                SettingsScreen.MAIN -> {
                    IptvMainSettingsContent(
                        isPipSupported = isPipSupported,
                        enablePip = enablePip,
                        onEnablePipChange = onEnablePipChange,
                        backgroundPlay = backgroundPlay,
                        isBatteryOptimized = isBatteryOptimized,
                        backgroundPlayInteractionSource = backgroundPlayInteractionSource,
                        onBackgroundPlayChange = onBackgroundPlayChange,
                        launcher = launcher,
                        intent = intent,
                        aspectRatio = aspectRatio,
                        onAspectRatioChange = onAspectRatioChange,
                        mirrorFlip = mirrorFlip,
                        onMirrorFlipChange = onMirrorFlipChange,
                        availableQualities = availableQualities,
                        currentQuality = currentQuality,
                        onNavigateToQuality = { currentScreen = SettingsScreen.QUALITY }
                    )
                }
                SettingsScreen.QUALITY -> {
                    IptvQualitySelectionContent(
                        availableQualities = availableQualities,
                        currentQuality = currentQuality,
                        onQualityChange = { 
                            onQualityChange(it)
                            currentScreen = SettingsScreen.MAIN
                        },
                        onBack = { currentScreen = SettingsScreen.MAIN }
                    )
                }
            }
        }
    }
}

private enum class SettingsScreen {
    MAIN, QUALITY
}

@Composable
private fun IptvMainSettingsContent(
    isPipSupported: Boolean,
    enablePip: Boolean,
    onEnablePipChange: (Boolean) -> Unit,
    backgroundPlay: Boolean,
    isBatteryOptimized: Boolean,
    backgroundPlayInteractionSource: MutableInteractionSource,
    onBackgroundPlayChange: (Boolean) -> Unit,
    launcher: androidx.activity.compose.ManagedActivityResultLauncher<Intent, androidx.activity.result.ActivityResult>,
    intent: Intent,
    aspectRatio: Int,
    onAspectRatioChange: (Int) -> Unit,
    mirrorFlip: Boolean,
    onMirrorFlipChange: (Boolean) -> Unit,
    availableQualities: List<VideoQuality>,
    currentQuality: VideoQuality?,
    onNavigateToQuality: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                0 to "16:9",
                1 to "4:3",
                2 to "Fill",
                3 to "Original"
            ).forEach { (value, label) ->
                FilterChip(
                    selected = aspectRatio == value,
                    onClick = { onAspectRatioChange(value) },
                    label = { Text(label) }
                )
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        if (availableQualities.size > 1) {
            val isBitrateMode = availableQualities.firstOrNull()?.isBitrateOnly == true
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { onNavigateToQuality() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isBitrateMode) Icons.Default.Speed else Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 12.dp).size(24.dp)
                    )
                    Text(
                        text = if (isBitrateMode) "Bitrate" else "Quality",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val displayText = if (currentQuality != null) {
                        "Auto (${currentQuality.label})"
                    } else {
                        "Auto"
                    }
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        if (isPipSupported) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureInPictureAlt,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 12.dp).size(24.dp)
                    )
                    Text(
                        text = "Picture in Picture",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                val thumbContent: (@Composable () -> Unit)? = if (enablePip) {
                    { Icon(Icons.Default.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                } else null
                Switch(
                    checked = enablePip,
                    onCheckedChange = onEnablePipChange,
                    thumbContent = thumbContent
                )
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .then(
                    if (isBatteryOptimized) {
                        Modifier.clickable(
                            indication = null,
                            interactionSource = backgroundPlayInteractionSource
                        ) {
                            launcher.launch(intent)
                        }
                    } else {
                        Modifier
                    }
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 12.dp).size(24.dp),
                    tint = if (isBatteryOptimized) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = "Background Play",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isBatteryOptimized) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            val thumbContent: (@Composable () -> Unit)? = if (backgroundPlay) {
                { Icon(Icons.Default.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
            } else null
            Switch(
                checked = backgroundPlay,
                onCheckedChange = if (isBatteryOptimized) null else onBackgroundPlayChange,
                enabled = !isBatteryOptimized,
                thumbContent = thumbContent
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Flip,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 12.dp).size(24.dp)
                )
                Text(
                    text = "Mirror/Flip",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            val thumbContent: (@Composable () -> Unit)? = if (mirrorFlip) {
                { Icon(Icons.Default.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
            } else null
            Switch(
                checked = mirrorFlip,
                onCheckedChange = onMirrorFlipChange,
                thumbContent = thumbContent
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun IptvQualitySelectionContent(
    availableQualities: List<VideoQuality>,
    currentQuality: VideoQuality?,
    onQualityChange: (VideoQuality?) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Quality",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        HorizontalDivider()
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onQualityChange(null) 
                    }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val autoText = if (currentQuality != null) {
                    "Auto (${currentQuality.label})"
                } else {
                    "Auto"
                }
                Text(
                    text = autoText,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (currentQuality != null) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            availableQualities.filter { it != currentQuality }.forEach { quality ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onQualityChange(quality) 
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = quality.label,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun IptvEpgMarkers(
    programs: List<EpgProgram>,
    totalDurationMs: Long,
    count: Int,
    modifier: Modifier = Modifier
) {
    if (programs.isEmpty() || totalDurationMs <= 0 || count <= 0) return
    
    val currentTime = System.currentTimeMillis()
    val currentProgram = programs.find { currentTime in it.startTime..it.stopTime } ?: return
    
    val upcomingPrograms = programs.filter { it.startTime > currentTime }.take(count)
    if (upcomingPrograms.isEmpty()) return
    
    BoxWithConstraints(modifier = modifier) {
        val progressBarWidth = maxWidth
        var accumulatedDuration = currentProgram.stopTime - currentProgram.startTime
        
        upcomingPrograms.forEach { program ->
            val position = (accumulatedDuration.toFloat() / totalDurationMs).coerceIn(0f, 0.95f)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = progressBarWidth * position)
                    .size(4.dp)
                    .background(Color.White, CircleShape)
            )
            accumulatedDuration += (program.stopTime - program.startTime)
        }
    }
}
