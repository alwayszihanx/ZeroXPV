package io.alwayszihan.zeroxpv.ui.iptv

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.alwayszihan.zeroxpv.presentation.Screen
import io.alwayszihan.zeroxpv.ui.icons.Icon
import io.alwayszihan.zeroxpv.ui.icons.Icons
import io.alwayszihan.zeroxpv.ui.iptv.component.IptvSearchBar
import io.alwayszihan.zeroxpv.ui.iptv.component.IptvSwipeableCard
import io.alwayszihan.zeroxpv.ui.iptv.manager.HistoryItem
import io.alwayszihan.zeroxpv.ui.iptv.parser.M3U8Channel
import io.alwayszihan.zeroxpv.ui.utils.LocalBackStack
import io.alwayszihan.zeroxpv.ui.utils.popSafely
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
data class IptvChannelEditScreen(
    val historyItem: HistoryItem,
    val channels: List<M3U8Channel>
) : Screen {

    @Composable
    override fun Content() {
        val backstack = LocalBackStack.current
        val context = LocalContext.current
        val view = LocalView.current
        val itemDeletedMsg = "Channel deleted"
        val undoMsg = "Undo"
        var editableChannels by remember { mutableStateOf(channels.toList()) }
        var displayedCount by remember { mutableIntStateOf(100) }
        var selectedItems by remember { mutableStateOf(setOf<String>()) }
        val isSelectionMode = selectedItems.isNotEmpty()
        var showMenu by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        var isSearching by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(true) }
        var hasChanges by remember { mutableStateOf(false) }
        var showExitDialog by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
        val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
            editableChannels = editableChannels.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
            hasChanges = true
        }

        LaunchedEffect(channels) {
            if (channels.isNotEmpty()) {
                editableChannels = channels.toList()
                displayedCount = minOf(100, channels.size)
                isLoading = false
            }
        }

        val filteredChannels = remember(editableChannels, searchQuery) {
            if (searchQuery.isEmpty()) {
                editableChannels
            } else {
                editableChannels.filter { channel ->
                    channel.name.contains(searchQuery, ignoreCase = true) ||
                            channel.url.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        val displayedChannels = remember(filteredChannels, displayedCount) {
            filteredChannels.take(displayedCount)
        }

        val checkBoxState by remember(selectedItems, filteredChannels) {
            derivedStateOf {
                when {
                    selectedItems.isEmpty() -> ToggleableState.Off
                    selectedItems.size == filteredChannels.size && selectedItems.isNotEmpty() -> ToggleableState.On
                    else -> ToggleableState.Indeterminate
                }
            }
        }

        androidx.activity.compose.BackHandler(onBack = {
            if (isSelectionMode) {
                selectedItems = emptySet()
            } else if (hasChanges) {
                showExitDialog = true
            } else {
                backstack.popSafely()
            }
        })

        val isLocalFile = historyItem.url.startsWith("file://") ||
                historyItem.url.startsWith("content://")

        val m3u8Content = remember(editableChannels) {
            buildString {
                append("#EXTM3U\n")
                editableChannels.forEach { channel ->
                    append("#EXTINF:-1")
                    if (channel.logoUrl.isNotBlank()) {
                        append(" tvg-logo=\"${channel.logoUrl}\"")
                    }
                    append(",$${channel.name}\n")
                    append("${channel.url}\n")
                }
            }
        }

        val saveSuccess = "Saved"
        val failed = "Save failed"

        val saveToOriginalFile: () -> Unit = {
            if (historyItem.url.startsWith("file://") || historyItem.url.startsWith("content://")) {
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val content = m3u8Content
                    try {
                        if (historyItem.url.startsWith("file://")) {
                            val filePath = historyItem.url.removePrefix("file://")
                            val file = java.io.File(filePath)
                            file.writeText(content)
                        } else {
                            val uri = historyItem.url.toUri()
                            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                outputStream.write(content.toByteArray())
                            }
                        }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            Toast.makeText(context, saveSuccess, Toast.LENGTH_SHORT).show()
                            hasChanges = false
                        }
                    } catch (_: Exception) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Edit Channels")
                            if (isLoading) {
                                Spacer(modifier = Modifier.width(12.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            if (isSelectionMode) {
                                selectedItems = emptySet()
                            } else if (hasChanges) {
                                showExitDialog = true
                            } else {
                                backstack.popSafely()
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        if (!isSelectionMode) {
                            if (editableChannels.size > 1) {
                                IconButton(onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    isSearching = !isSearching
                                    if (!isSearching) searchQuery = ""
                                }) {
                                    Icon(Icons.Default.Search, "Search")
                                }
                            }
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, null)
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    if (isLocalFile) {
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(Icons.Default.Check, null)
                                            },
                                            text = { Text("Save") },
                                            onClick = {
                                                showMenu = false
                                                if (editableChannels.isEmpty()) {
                                                    Toast.makeText(
                                                        context,
                                                        "Nothing to save",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    saveToOriginalFile()
                                                }
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(Icons.Default.Share, null)
                                        },
                                        text = { Text("Export") },
                                        onClick = {
                                            showMenu = false
                                            if (editableChannels.isEmpty()) {
                                                Toast.makeText(
                                                    context,
                                                    "Nothing to export",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Export not supported",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = isSelectionMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    BottomAppBar {
                        val selectAllText = "Select all"
                        TriStateCheckbox(
                            modifier = Modifier.semantics {
                                this.contentDescription = selectAllText
                            },
                            state = checkBoxState,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                selectedItems = when (checkBoxState) {
                                    ToggleableState.On -> emptySet()
                                    else -> {
                                        filteredChannels.map { it.id }.toSet()
                                    }
                                }
                            }
                        )
                        Text(
                            modifier = Modifier.weight(1f),
                            text = "${selectedItems.size} selected",
                            style = MaterialTheme.typography.labelLarge
                        )
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                selectedItems.forEach { id ->
                                    editableChannels.find { it.id == id }?.let { channel ->
                                        editableChannels = editableChannels.toMutableList().apply {
                                            remove(channel)
                                        }
                                    }
                                }
                                hasChanges = true
                                selectedItems = emptySet()
                            },
                            enabled = selectedItems.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete"
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AnimatedVisibility(visible = isSearching) {
                    IptvSearchBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        text = searchQuery,
                        placeholderText = "Search channels",
                        onValueChange = { searchQuery = it }
                    )
                }

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = displayedChannels.size,
                        key = { index -> displayedChannels[index].id }
                    ) { index ->
                        val channel = displayedChannels[index]
                        ReorderableItem(reorderableLazyListState, key = channel.id) {
                            IptvSwipeableCard(
                                onDelete = {
                                    val deleteIndex = editableChannels.indexOfFirst { it.id == channel.id }
                                    if (deleteIndex >= 0) {
                                        editableChannels = editableChannels.toMutableList().apply {
                                            removeAt(deleteIndex)
                                        }
                                        hasChanges = true
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = itemDeletedMsg,
                                                actionLabel = undoMsg,
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                editableChannels = editableChannels.toMutableList().apply {
                                                    add(deleteIndex.coerceAtMost(size), channel)
                                                }
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedItems = if (selectedItems.contains(channel.id)) {
                                            selectedItems - channel.id
                                        } else {
                                            selectedItems + channel.id
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        selectedItems = setOf(channel.id)
                                    }
                                },
                                name = channel.name,
                                logoUrl = channel.logoUrl,
                                url = channel.url,
                                leadingIcon = {
                                    if (isSelectionMode) {
                                        Icon(
                                            imageVector = if (selectedItems.contains(channel.id)) Icons.Filled.CheckCircle else Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = if (selectedItems.contains(channel.id)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(end = 12.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(end = 12.dp),
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            )
                        }
                    }

                    if (displayedCount < filteredChannels.size) {
                        item {
                            LaunchedEffect(Unit) {
                                displayedCount = minOf(displayedCount + 100, filteredChannels.size)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }

        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Unsaved changes") },
                text = { Text("You have unsaved changes. Save before leaving?") },
                confirmButton = {
                    TextButton(onClick = {
                        if (editableChannels.isEmpty()) {
                            Toast.makeText(
                                context,
                                if (isLocalFile) "Nothing to save" else "Nothing to export",
                                Toast.LENGTH_SHORT
                            ).show()
                            showExitDialog = false
                        } else {
                            if (isLocalFile) {
                                saveToOriginalFile()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Export not supported",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            showExitDialog = false
                        }
                    }) {
                        Text(if (isLocalFile) "Save" else "Export")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        backstack.popSafely()
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
