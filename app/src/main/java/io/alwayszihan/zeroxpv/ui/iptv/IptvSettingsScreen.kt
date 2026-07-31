package io.alwayszihan.zeroxpv.ui.iptv

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import io.alwayszihan.zeroxpv.presentation.Screen
import io.alwayszihan.zeroxpv.ui.icons.Icon
import io.alwayszihan.zeroxpv.ui.icons.Icons
import io.alwayszihan.zeroxpv.ui.iptv.component.*
import io.alwayszihan.zeroxpv.ui.iptv.manager.PlaylistManager
import io.alwayszihan.zeroxpv.ui.iptv.player.PlayerRepository
import io.alwayszihan.zeroxpv.ui.utils.LocalBackStack
import io.alwayszihan.zeroxpv.ui.utils.popSafely

@OptIn(ExperimentalMaterial3Api::class)
object IptvSettingsScreen : Screen {

    @Composable
    override fun Content() {
        val backstack = LocalBackStack.current
        val view = LocalView.current
        val context = androidx.compose.ui.platform.LocalContext.current
        val repository = remember { PlayerRepository(context) }

        val playlists by PlaylistManager.playlistsFlow.collectAsState()

        var showLogos by remember { mutableStateOf(repository.getShowLogos()) }
        var autoRefresh by remember { mutableStateOf(repository.getAutoRefreshPlaylists()) }
        var channelLayout by remember { mutableStateOf(repository.getChannelLayout()) }
        var bufferSize by remember { mutableStateOf(repository.getBufferSizeMb()) }
        var readTimeout by remember { mutableStateOf(repository.getReadTimeoutSec()) }

        var showLayoutChooser by remember { mutableStateOf(false) }
        var showBufferChooser by remember { mutableStateOf(false) }
        var showTimeoutChooser by remember { mutableStateOf(false) }
        var showDefaultChooser by remember { mutableStateOf(false) }

        val defaultPlaylist = playlists.firstOrNull { it.isDefault }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { Text("IPTV Settings") },
                        navigationIcon = {
                            IconButton(onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                backstack.popSafely()
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IptvPreferenceSubtitle(text = "Appearance")

                    IptvPreferenceSwitch(
                        title = "Show channel logos",
                        description = "Display tvg-logo images next to channels",
                        icon = Icons.Default.Palette,
                        isChecked = showLogos,
                        onCheckedChange = {
                            showLogos = it
                            repository.setShowLogos(it)
                        }
                    )

                    IptvPreferenceItem(
                        title = "Channel layout",
                        description = if (channelLayout == 1) "Grid (2 columns)" else "List",
                        icon = Icons.Default.GridView,
                        onClick = { showLayoutChooser = true }
                    )

                    IptvPreferenceSubtitle(text = "Playlists")

                    IptvPreferenceSwitch(
                        title = "Auto-refresh on launch",
                        description = "Reload saved playlists when the app starts",
                        icon = Icons.Default.Refresh,
                        isChecked = autoRefresh,
                        onCheckedChange = {
                            autoRefresh = it
                            repository.setAutoRefreshPlaylists(it)
                        }
                    )

                    IptvPreferenceItem(
                        title = "Default playlist",
                        description = defaultPlaylist?.name ?: "None",
                        icon = Icons.Default.PushPin,
                        onClick = { showDefaultChooser = true }
                    )

                    IptvPreferenceSubtitle(text = "Playback")

                    IptvPreferenceItem(
                        title = "Buffer size",
                        description = if (bufferSize == 0) "Default" else "$bufferSize MB",
                        icon = Icons.Default.Speed,
                        onClick = { showBufferChooser = true }
                    )

                    IptvPreferenceItem(
                        title = "Read timeout",
                        description = "$readTimeout seconds",
                        icon = Icons.Default.Speed,
                        onClick = { showTimeoutChooser = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (showLayoutChooser) {
                AlertDialog(
                    onDismissRequest = { showLayoutChooser = false },
                    title = { Text("Channel layout") },
                    text = {
                        Column {
                            IptvPreferenceSingleChoiceItem(
                                text = "List",
                                selected = channelLayout == 0,
                                onClick = { channelLayout = 0 }
                            )
                            IptvPreferenceSingleChoiceItem(
                                text = "Grid",
                                selected = channelLayout == 1,
                                onClick = { channelLayout = 1 }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            repository.setChannelLayout(channelLayout)
                            showLayoutChooser = false
                        }) { Text("Done") }
                    }
                )
            }

            if (showBufferChooser) {
                AlertDialog(
                    onDismissRequest = { showBufferChooser = false },
                    title = { Text("Buffer size (MB)") },
                    text = {
                        Column {
                            (0..32 step 4).forEach { value ->
                                IptvPreferenceSingleChoiceItem(
                                    text = if (value == 0) "Default" else "$value MB",
                                    selected = bufferSize == value,
                                    onClick = { bufferSize = value }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            repository.setBufferSizeMb(bufferSize)
                            showBufferChooser = false
                        }) { Text("Done") }
                    }
                )
            }

            if (showTimeoutChooser) {
                AlertDialog(
                    onDismissRequest = { showTimeoutChooser = false },
                    title = { Text("Read timeout (seconds)") },
                    text = {
                        Column {
                            (5..60 step 5).forEach { value ->
                                IptvPreferenceSingleChoiceItem(
                                    text = "$value s",
                                    selected = readTimeout == value,
                                    onClick = { readTimeout = value }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            repository.setReadTimeoutSec(readTimeout)
                            showTimeoutChooser = false
                        }) { Text("Done") }
                    }
                )
            }

            if (showDefaultChooser) {
                AlertDialog(
                    onDismissRequest = { showDefaultChooser = false },
                    title = { Text("Default playlist") },
                    text = {
                        Column {
                            IptvPreferenceSingleChoiceItem(
                                text = "None",
                                selected = defaultPlaylist == null,
                                onClick = { setDefaultPlaylist(context, null) }
                            )
                            playlists.forEach { item ->
                                IptvPreferenceSingleChoiceItem(
                                    text = item.name,
                                    selected = defaultPlaylist?.id == item.id,
                                    onClick = { setDefaultPlaylist(context, item.id) }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDefaultChooser = false }) { Text("Done") }
                    }
                )
            }
        }
    }

    private fun setDefaultPlaylist(context: android.content.Context, id: String?) {
        val playlists = PlaylistManager.getPlaylists()
        playlists.forEach { item ->
            val shouldBeDefault = item.id == id
            if (item.isDefault != shouldBeDefault) {
                PlaylistManager.addOrUpdatePlaylist(
                    context = context,
                    id = item.id,
                    name = item.name,
                    sources = item.sources,
                    epgUrl = item.epgUrl,
                    channelCount = item.channelCount,
                    logoUrl = item.logoUrl,
                    isDefault = shouldBeDefault,
                    order = item.order
                )
            }
        }
    }
}
