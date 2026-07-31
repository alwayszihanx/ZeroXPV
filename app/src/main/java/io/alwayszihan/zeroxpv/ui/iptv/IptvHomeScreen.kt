package io.alwayszihan.zeroxpv.ui.iptv

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.alwayszihan.zeroxpv.presentation.Screen
import io.alwayszihan.zeroxpv.ui.icons.Icon
import io.alwayszihan.zeroxpv.ui.icons.Icons
import io.alwayszihan.zeroxpv.ui.iptv.component.IptvSwipeableCard
import io.alwayszihan.zeroxpv.ui.iptv.manager.HistoryItem
import io.alwayszihan.zeroxpv.ui.iptv.manager.HistoryManager
import io.alwayszihan.zeroxpv.ui.iptv.manager.PlaylistManager
import io.alwayszihan.zeroxpv.ui.iptv.IptvSettingsScreen
import io.alwayszihan.zeroxpv.ui.iptv.util.ErrorHandler
import io.alwayszihan.zeroxpv.ui.iptv.util.formatDateTime
import io.alwayszihan.zeroxpv.ui.utils.LocalBackStack
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
object IptvHomeScreen : Screen {

    private var defaultPlaylistLaunched = false

    @Composable
    override fun Content() {
        val backstack = LocalBackStack.current
        var url by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        val clipboard = LocalClipboard.current
        val keyboardController = LocalSoftwareKeyboardController.current
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }

        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri?.let {
                try {
                    val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(it, flag)
                } catch (e: Exception) {
                    ErrorHandler.logError("IptvHomeScreen", "Failed to take persistable URI permission", e)
                }
                url = it.toString()
            }
        }

        val view = LocalView.current

        LaunchedEffect(Unit) {
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            val autoRefresh = prefs.getBoolean("iptv_auto_refresh_playlists", false)
            if (autoRefresh) {
                PlaylistManager.getPlaylists().forEach { item ->
                    scope.launch { PlaylistManager.refreshMeta(context, item) }
                }
            }
            if (!IptvHomeScreen.defaultPlaylistLaunched) {
                val default = PlaylistManager.getDefaultPlaylist()
                if (default != null) {
                    val uri = PlaylistManager.resolvePlayUri(context, default)
                    if (uri.isNotBlank()) {
                        IptvHomeScreen.defaultPlaylistLaunched = true
                        backstack.add(IptvPlayerScreen(url = uri))
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {},
                        modifier = Modifier.padding(horizontal = 8.dp),
                        navigationIcon = {
                            IconButton(onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                backstack.add(io.alwayszihan.zeroxpv.ui.preferences.PreferencesScreen)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Settings"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                backstack.add(IptvHistoryScreen)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "History"
                                )
                            }
                        }
                    )
                },
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(modifier = Modifier.padding(start = 12.dp, top = 24.dp)) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(top = 12.dp, bottom = 3.dp)
                        ) {
                            Text(
                                text = "ZeroXPV",
                                style = MaterialTheme.typography.displaySmall
                            )
                            AnimatedVisibility(visible = isLoading) {
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(top = 24.dp)
                    ) {
                        OutlinedCard(
                            onClick = {
                                backstack.add(IptvPlaylistManagerScreen)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewList,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Playlist Manager",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Saved playlists configured by URL or file",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        OutlinedCard(
                            onClick = {
                                backstack.add(IptvSettingsScreen)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "IPTV Settings",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Logos, layout, playlists & playback",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("Enter M3U URL or choose a local file") },
                            modifier = Modifier
                                .padding(0.dp, 16.dp)
                                .fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = {
                                        val mimeTypes = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                            arrayOf("*/*")
                                        } else {
                                            arrayOf("audio/x-mpegurl", "application/vnd.apple.mpegurl", "audio/mpegurl")
                                        }
                                        filePickerLauncher.launch(mimeTypes)
                                    }) {
                                        Icon(Icons.Default.FileUpload, "Import from device")
                                    }
                                    if (url.isNotEmpty()) {
                                        IconButton(onClick = { url = "" }) {
                                            Icon(Icons.Default.Clear, "Clear")
                                        }
                                    }
                                }
                            },
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (url.isNotBlank()) {
                                        keyboardController?.hide()
                                        backstack.add(IptvPlayerScreen(url = url))
                                    }
                                }
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val clipEntry = clipboard.getClipEntry()
                                        val clipData = clipEntry?.clipData
                                        if (clipData != null && clipData.itemCount > 0) {
                                            val text = clipData.getItemAt(0).text?.toString()
                                            if (!text.isNullOrBlank()) {
                                                url = text
                                            }
                                        }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Paste")
                            }

                            Button(
                                onClick = {
                                    if (url.isNotBlank()) {
                                        keyboardController?.hide()
                                        backstack.add(IptvPlayerScreen(url = url))
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Play")
                            }
                        }

                        val historyList by HistoryManager.historyFlow.collectAsState()

                        if (historyList.take(3).isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                historyList.take(3).forEach { item ->
                                    key(item.url) {
                                        IptvSwipeableCard(
                                            onDelete = {
                                                scope.launch {
                                                    HistoryManager.deleteHistoryItemWithUndo(
                                                        item,
                                                        snackbarHostState
                                                    )
                                                }
                                            },
                                            onClick = {
                                                backstack.add(
                                                    IptvPlayerScreen(
                                                        url = item.url,
                                                        initialChannelUrl = item.lastChannelUrl,
                                                        initialVideoTitle = item.name,
                                                        initialChannelId = item.lastChannelId
                                                    )
                                                )
                                            },
                                            name = item.name,
                                            logoUrl = item.logoUrl,
                                            url = item.url,
                                            tags = listOf("Last played: ${item.timestamp.formatDateTime()}"),
                                            trailingIcon = {
                                                Box(modifier = Modifier.padding(end = 12.dp)) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Play",
                                                        tint = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(160.dp))
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .imePadding()
            )
        }
    }
}
