package io.alwayszihan.zeroxpv.ui.iptv.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import io.alwayszihan.zeroxpv.ui.iptv.player.Media3Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private fun Context.getDecoderType(): Int {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    return prefs.getInt("decoder_type", 0)
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun IptvPlayerStatsDialog(
    media3Player: Media3Player?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var stats by remember { mutableStateOf<IptvPlayerStats?>(null) }
    var lastCpuTime by remember { mutableLongStateOf(0L) }
    var lastCheckTime by remember { mutableLongStateOf(0L) }
    
    DisposableEffect(media3Player) {
        val job = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                media3Player?.exoPlayer?.let { player ->
                    val videoFormat = player.videoFormat
                    val audioFormat = player.audioFormat
                    val bufferedMs = player.bufferedPosition - player.currentPosition
                    val bufferPercentage = if (player.duration > 0) {
                        (bufferedMs.toFloat() / player.duration * 100).coerceIn(0f, 100f)
                    } else {
                        0f
                    }
                    
                    val playbackState = when (player.playbackState) {
                        androidx.media3.common.Player.STATE_IDLE -> "Idle"
                        androidx.media3.common.Player.STATE_BUFFERING -> "Buffering"
                        androidx.media3.common.Player.STATE_READY -> if (player.isPlaying) "Playing" else "Paused"
                        androidx.media3.common.Player.STATE_ENDED -> "Ended"
                        else -> "Unknown"
                    }
                    
                    val decoderType = context.getDecoderType()
                    val isHardwareDecoder = decoderType == 0
                    
                    val cpuUsage = try {
                        val pid = android.os.Process.myPid()
                        val stat = java.io.File("/proc/$pid/stat").readText().split(" ")
                        val utime = stat[13].toLong()
                        val stime = stat[14].toLong()
                        val currentCpuTime = utime + stime
                        val currentTime = System.currentTimeMillis()
                        
                        val usage = if (lastCpuTime > 0 && lastCheckTime > 0) {
                            val cpuDelta = currentCpuTime - lastCpuTime
                            val timeDelta = currentTime - lastCheckTime
                            if (timeDelta > 0) {
                                val cpuCount = Runtime.getRuntime().availableProcessors()
                                ((cpuDelta * 10f / timeDelta / cpuCount) * 100).toInt().coerceIn(0, 100)
                            } else 0
                        } else 0
                        
                        lastCpuTime = currentCpuTime
                        lastCheckTime = currentTime
                        usage
                    } catch (e: Exception) {
                        0
                    }
                    
                    stats = IptvPlayerStats(
                        droppedFrames = player.videoDecoderCounters?.droppedBufferCount ?: 0,
                        totalFrames = player.videoDecoderCounters?.renderedOutputBufferCount ?: 0,
                        currentRes = "${videoFormat?.width ?: 0}x${videoFormat?.height ?: 0}@${videoFormat?.frameRate?.toInt() ?: 0}",
                        videoCodec = videoFormat?.codecs ?: "N/A",
                        audioCodec = audioFormat?.codecs ?: "N/A",
                        bufferHealth = bufferedMs / 1000f,
                        bufferPercentage = bufferPercentage,
                        playbackState = playbackState,
                        isHardwareDecoder = isHardwareDecoder,
                        cpuUsage = cpuUsage
                    )
                }
                delay(1000)
            }
        }
        
        onDispose {
            job.cancel()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Player Stats") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                stats?.let { s ->
                    IptvStatsRow("State", s.playbackState)
                    IptvStatsRow("Resolution", s.currentRes)
                    IptvStatsRow("Frames", "${s.droppedFrames} / ${s.totalFrames}")
                    IptvStatsRow("Video Codec", s.videoCodec)
                    IptvStatsRow("Audio Codec", s.audioCodec)
                    IptvStatsRow("Decoder", if (s.isHardwareDecoder) "Hardware" else "Software")
                    IptvStatsRow("Buffer Health", "%.2f s (%.1f%%)".format(s.bufferHealth, s.bufferPercentage))
                    IptvStatsRow("CPU Usage", "${s.cpuUsage}%")
                } ?: Text("Loading...")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                stats?.let { s ->
                    val statsText = buildString {
                        appendLine("State: ${s.playbackState}")
                        appendLine("Resolution: ${s.currentRes}")
                        appendLine("Frames: ${s.droppedFrames} / ${s.totalFrames}")
                        appendLine("Video Codec: ${s.videoCodec}")
                        appendLine("Audio Codec: ${s.audioCodec}")
                        appendLine("Decoder: ${if (s.isHardwareDecoder) "Hardware" else "Software"}")
                        appendLine("Buffer Health: ${"%.2f s (%.1f%%)".format(s.bufferHealth, s.bufferPercentage)}")
                        appendLine("CPU Usage: ${s.cpuUsage}%")
                    }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Copy to clipboard", statsText))
                }
            }) {
                Text("Copy")
            }
        }
    )
}

@Composable
private fun IptvStatsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

private data class IptvPlayerStats(
    val droppedFrames: Int,
    val totalFrames: Int,
    val currentRes: String,
    val videoCodec: String,
    val audioCodec: String,
    val bufferHealth: Float,
    val bufferPercentage: Float,
    val playbackState: String,
    val isHardwareDecoder: Boolean,
    val cpuUsage: Int
)
