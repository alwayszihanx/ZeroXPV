package io.alwayszihan.zeroxpv.ui.iptv

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.alwayszihan.zeroxpv.presentation.Screen
import io.alwayszihan.zeroxpv.ui.icons.Icon
import io.alwayszihan.zeroxpv.ui.icons.Icons
import io.alwayszihan.zeroxpv.ui.iptv.component.IptvChannelLogo
import io.alwayszihan.zeroxpv.ui.iptv.component.IptvPreferenceSwitch
import io.alwayszihan.zeroxpv.ui.iptv.manager.PlaylistItem
import io.alwayszihan.zeroxpv.ui.iptv.manager.PlaylistManager
import io.alwayszihan.zeroxpv.ui.iptv.manager.PlaylistSourceEntry
import io.alwayszihan.zeroxpv.ui.iptv.manager.PlaylistSourceType
import io.alwayszihan.zeroxpv.ui.utils.LocalBackStack
import io.alwayszihan.zeroxpv.ui.utils.popSafely
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
object IptvPlaylistManagerScreen : Screen {

    @Composable
    override fun Content() {
        val backstack = LocalBackStack.current
        val context = LocalContext.current
        val view = LocalView.current
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        val playlists by PlaylistManager.playlistsFlow.collectAsState()

        var showEditor by remember { mutableStateOf(false) }
        var editingItem by remember { mutableStateOf<PlaylistItem?>(null) }
        var showOverflow by remember { mutableStateOf(false) }

        val lazyListState = rememberLazyListState()
        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
            PlaylistManager.reorder(context, from.index, to.index)
        }

        val exportLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json")
        ) { uri: Uri? ->
            uri?.let {
                try {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        out.write(PlaylistManager.exportToString().toByteArray())
                    }
                    android.widget.Toast.makeText(context, "Playlists exported", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val importLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri?.let {
                try {
                    val text = context.contentResolver.openInputStream(it)?.use { inp -> inp.bufferedReader().readText() } ?: ""
                    val ok = PlaylistManager.importFromString(context, text)
                    android.widget.Toast.makeText(
                        context,
                        if (ok) "Playlists imported" else "Import failed",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { Text("Playlist Manager") },
                        navigationIcon = {
                            IconButton(onClick = {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                backstack.popSafely()
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                scope.launch {
                                    playlists.forEach { item ->
                                        PlaylistManager.refreshMeta(context, item)
                                    }
                                    android.widget.Toast.makeText(context, "Playlists refreshed", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh all")
                            }
                            Box {
                                IconButton(onClick = { showOverflow = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                                }
                                DropdownMenu(
                                    expanded = showOverflow,
                                    onDismissRequest = { showOverflow = false }
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.FileUpload, null) },
                                        text = { Text("Import backup") },
                                        onClick = {
                                            showOverflow = false
                                            importLauncher.launch(arrayOf("application/json"))
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.Share, null) },
                                        text = { Text("Export backup") },
                                        onClick = {
                                            showOverflow = false
                                            exportLauncher.launch("zeroxpv_playlists.json")
                                        }
                                    )
                                }
                            }
                        }
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = {
                            editingItem = null
                            showEditor = true
                        },
                        icon = { Icon(Icons.Outlined.PlaylistAdd, contentDescription = null) },
                        text = { Text("Add Playlist") }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    if (playlists.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.ViewList,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "No playlists yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "Tap + to add a playlist by URL or local file",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(playlists, key = { it.id }) { item ->
                                ReorderableItem(reorderableState, key = item.id) { isDragging ->
                                    PlaylistCard(
                                        item = item,
                                        isDragging = isDragging,
                                        modifier = Modifier.draggableHandle(),
                                        onPlay = {
                                            val uri = PlaylistManager.resolvePlayUri(context, item)
                                            if (uri.isNotBlank()) {
                                                backstack.add(
                                                    IptvPlayerScreen(
                                                        url = uri,
                                                        initialChannelUrl = item.lastChannelUrl.ifBlank { null },
                                                        initialChannelId = item.lastChannelId,
                                                        initialVideoTitle = item.lastChannelName.ifBlank { null }
                                                    )
                                                )
                                            }
                                        },
                                        onEdit = {
                                            editingItem = item
                                            showEditor = true
                                        },
                                        onDelete = {
                                            scope.launch {
                                                PlaylistManager.deletePlaylistWithUndo(context, item, snackbarHostState)
                                            }
                                        },
                                        onToggleDefault = {
                                            PlaylistManager.addOrUpdatePlaylist(
                                                context = context,
                                                id = item.id,
                                                name = item.name,
                                                sources = item.sources,
                                                epgUrl = item.epgUrl,
                                                channelCount = item.channelCount,
                                                logoUrl = item.logoUrl,
                                                isDefault = !item.isDefault,
                                                order = item.order
                                            )
                                        }
                                    )
                                }
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
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

        if (showEditor) {
            PlaylistEditorDialog(
                initial = editingItem,
                onDismiss = { showEditor = false },
                onSave = { name, sources, epgUrl, isDefault ->
                    PlaylistManager.addOrUpdatePlaylist(
                        context = context,
                        id = editingItem?.id,
                        name = name,
                        sources = sources,
                        epgUrl = epgUrl,
                        isDefault = isDefault,
                        order = editingItem?.order
                    )
                    showEditor = false
                    scope.launch {
                        val saved = PlaylistManager.getPlaylists().firstOrNull {
                            it.name == name && it.sources == sources
                        } ?: editingItem
                        saved?.let { PlaylistManager.refreshMeta(context, it) }
                    }
                }
            )
        }
    }
}

@Composable
private fun PlaylistCard(
    item: PlaylistItem,
    isDragging: Boolean,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 6.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reorder",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )

            if (item.logoUrl.isNotBlank()) {
                IptvChannelLogo(logoUrl = item.logoUrl, contentDescription = item.name)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (item.isDefault) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "DEFAULT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append("${item.channelCount} channels · ${item.displayType}")
                        if (item.health.isNotBlank()) append(" · ${item.health}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.health.isNotBlank()) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.lastChannelName.isNotBlank()) {
                    Text(
                        text = "Resume: ${item.lastChannelName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                if (item.sources.isNotEmpty()) {
                    Text(
                        text = item.sources.first().source,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onToggleDefault) {
                Icon(
                    imageVector = if (item.isDefault) Icons.Default.PushPin else Icons.Default.PushPin,
                    contentDescription = "Set default",
                    tint = if (item.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onPlay) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistEditorDialog(
    initial: PlaylistItem?,
    onDismiss: () -> Unit,
    onSave: (name: String, sources: List<PlaylistSourceEntry>, epgUrl: String, isDefault: Boolean) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var epgUrl by remember { mutableStateOf(initial?.epgUrl ?: "") }
    var isDefault by remember { mutableStateOf(initial?.isDefault ?: false) }
    var sources by remember {
        mutableStateOf(
            initial?.sources ?: listOf(PlaylistSourceEntry("", PlaylistSourceType.URL))
        )
    }
    var expandedSource by remember { mutableStateOf<Int?>(null) }
    var showError by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            sources = sources + PlaylistSourceEntry(it.toString(), PlaylistSourceType.FILE)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Playlist" else "Edit Playlist") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError && name.isBlank(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Next
                    )
                )

                Text(
                    text = "Sources",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                sources.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = entry.source,
                            onValueChange = { value ->
                                sources = sources.toMutableList().apply { this[index] = entry.copy(source = value) }
                            },
                            label = { Text(if (entry.type == PlaylistSourceType.URL) "M3U URL" else "File URI") },
                            singleLine = true,
                            isError = showError && entry.source.isBlank(),
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = {
                                        filePickerLauncher.launch(
                                            arrayOf(
                                                "audio/x-mpegurl",
                                                "application/vnd.apple.mpegurl",
                                                "audio/mpegurl",
                                                "*/*"
                                            )
                                        )
                                    }) {
                                        Icon(Icons.Default.FileUpload, "Pick file")
                                    }
                                    if (sources.size > 1) {
                                        IconButton(onClick = {
                                            sources = sources.toMutableList().apply { removeAt(index) }
                                        }) {
                                            Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = entry.type == PlaylistSourceType.URL,
                            onClick = {
                                sources = sources.toMutableList().apply {
                                    this[index] = entry.copy(type = PlaylistSourceType.URL)
                                }
                            },
                            label = { Text("URL") },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        FilterChip(
                            selected = entry.type == PlaylistSourceType.FILE,
                            onClick = {
                                sources = sources.toMutableList().apply {
                                    this[index] = entry.copy(type = PlaylistSourceType.FILE)
                                }
                            },
                            label = { Text("File") }
                        )
                    }
                }

                TextButton(onClick = {
                    sources = sources + PlaylistSourceEntry("", PlaylistSourceType.URL)
                }) {
                    Icon(Icons.Outlined.PlaylistAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add another source")
                }

                OutlinedTextField(
                    value = epgUrl,
                    onValueChange = { epgUrl = it },
                    label = { Text("EPG URL (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Done
                    )
                )

                IptvPreferenceSwitch(
                    title = "Set as default",
                    description = "Open this playlist automatically when IPTV opens",
                    icon = Icons.Default.PushPin,
                    isChecked = isDefault,
                    onCheckedChange = { isDefault = it }
                )

                if (showError) {
                    Text(
                        text = "Please provide a name and at least one valid source",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val draft = PlaylistItem(
                        id = initial?.id ?: "",
                        name = name.trim(),
                        sources = sources,
                        epgUrl = epgUrl.trim(),
                        isDefault = isDefault
                    )
                    val issues = PlaylistManager.validate(draft)
                    if (issues.isNotEmpty()) {
                        showError = true
                        return@TextButton
                    }
                    onSave(name.trim(), sources, epgUrl.trim(), isDefault)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
