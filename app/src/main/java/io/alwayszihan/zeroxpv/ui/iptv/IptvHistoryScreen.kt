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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import io.alwayszihan.zeroxpv.presentation.Screen
import io.alwayszihan.zeroxpv.ui.icons.Icon
import io.alwayszihan.zeroxpv.ui.icons.Icons
import io.alwayszihan.zeroxpv.ui.iptv.component.IptvSearchBar
import io.alwayszihan.zeroxpv.ui.iptv.component.IptvSwipeableCard
import io.alwayszihan.zeroxpv.ui.iptv.manager.HistoryItem
import io.alwayszihan.zeroxpv.ui.iptv.manager.HistoryManager
import io.alwayszihan.zeroxpv.ui.iptv.util.formatDateTime
import io.alwayszihan.zeroxpv.ui.utils.LocalBackStack
import io.alwayszihan.zeroxpv.ui.utils.popSafely
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
object IptvHistoryScreen : Screen {

    @Composable
    override fun Content() {
        val backstack = LocalBackStack.current
        val context = LocalContext.current
        val view = LocalView.current
        val scope = rememberCoroutineScope()
        val historyList by HistoryManager.historyFlow.collectAsState()
        var selectedItems by remember { mutableStateOf(setOf<String>()) }
        val isSelectionMode = selectedItems.isNotEmpty()
        var showMenu by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        var isSearching by remember { mutableStateOf(false) }

        val filteredHistoryList = remember(historyList, searchQuery) {
            if (searchQuery.isEmpty()) {
                historyList
            } else {
                historyList.filter { item ->
                    item.name.contains(searchQuery, ignoreCase = true) ||
                            item.url.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        val checkBoxState by remember(selectedItems, filteredHistoryList) {
            derivedStateOf {
                when {
                    selectedItems.isEmpty() -> ToggleableState.Off
                    selectedItems.size == filteredHistoryList.size && selectedItems.isNotEmpty() -> ToggleableState.On
                    else -> ToggleableState.Indeterminate
                }
            }
        }

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(historyList.isEmpty()) {
            if (historyList.isEmpty()) {
                isSearching = false
                searchQuery = ""
            }
        }

        val scrollBehavior = if (historyList.isNotEmpty()) {
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
                rememberTopAppBarState(),
                canScroll = { true }
            )
        } else {
            TopAppBarDefaults.pinnedScrollBehavior()
        }

        androidx.activity.compose.BackHandler(onBack = {
            if (isSelectionMode) {
                selectedItems = emptySet()
            } else {
                backstack.popSafely()
            }
        })

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text("History")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            if (isSelectionMode) {
                                selectedItems = emptySet()
                            } else {
                                backstack.popSafely()
                            }
                        }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        if (!isSelectionMode) {
                            if (historyList.isNotEmpty()) {
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
                                    if (historyList.isNotEmpty()) {
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(Icons.Default.Share, null)
                                            },
                                            text = { Text("Export") },
                                            onClick = {
                                                showMenu = false
                                                Toast.makeText(
                                                    context,
                                                    "No history to export",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(Icons.Default.Refresh, null)
                                        },
                                        text = { Text("Import") },
                                        onClick = {
                                            showMenu = false
                                            Toast.makeText(
                                                context,
                                                "Import not available",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                }
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
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
                                        filteredHistoryList.map { it.url }.toSet()
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
                                selectedItems.forEach { url ->
                                    historyList.find { it.url == url }?.let { item ->
                                        HistoryManager.deleteHistoryItem(item)
                                    }
                                }
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
                        placeholderText = "Search history",
                        onValueChange = { searchQuery = it }
                    )
                }

                if (filteredHistoryList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No play history",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredHistoryList, key = { it.url }) { item ->
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
                                    if (isSelectionMode) {
                                        selectedItems = if (selectedItems.contains(item.url)) {
                                            selectedItems - item.url
                                        } else {
                                            selectedItems + item.url
                                        }
                                    } else {
                                        backstack.add(
                                            IptvPlayerScreen(
                                                url = item.url,
                                                initialChannelUrl = item.lastChannelUrl,
                                                initialVideoTitle = item.name,
                                                initialChannelId = item.lastChannelId
                                            )
                                        )
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        selectedItems = setOf(item.url)
                                    }
                                },
                                onEdit = if (!isSelectionMode) {
                                    {
                                        backstack.add(
                                            IptvChannelEditScreen(
                                                historyItem = item,
                                                channels = emptyList()
                                            )
                                        )
                                    }
                                } else null,
                                name = item.name,
                                logoUrl = item.logoUrl,
                                url = item.url,
                                tags = listOf("Last played: ${item.timestamp.formatDateTime()}"),
                                leadingIcon = if (isSelectionMode) {
                                    {
                                        Icon(
                                            imageVector = if (selectedItems.contains(item.url)) Icons.Default.Check else Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (selectedItems.contains(item.url)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(end = 12.dp)
                                        )
                                    }
                                } else null,
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
