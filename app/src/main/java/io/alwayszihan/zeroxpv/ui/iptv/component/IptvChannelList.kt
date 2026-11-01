package io.alwayszihan.zeroxpv.ui.iptv.component

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import io.alwayszihan.zeroxpv.ui.icons.Icon
import io.alwayszihan.zeroxpv.ui.icons.Icons
import io.alwayszihan.zeroxpv.ui.iptv.component.IptvChannelLogo
import io.alwayszihan.zeroxpv.ui.iptv.component.IptvEpgProgramList
import io.alwayszihan.zeroxpv.ui.iptv.component.IptvPlayerStatsDialog
import io.alwayszihan.zeroxpv.ui.iptv.component.IptvSearchBar
import io.alwayszihan.zeroxpv.ui.iptv.parser.M3U8Channel
import io.alwayszihan.zeroxpv.ui.iptv.player.Media3Player
import kotlinx.coroutines.launch

private fun Context.getShowPlayerStats(): Boolean {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    return prefs.getBoolean("show_player_stats", false)
}

@Composable
fun IptvChannelListSection(
    channels: List<M3U8Channel>,
    currentChannelUrl: String,
    modifier: Modifier = Modifier,
    actualPlayingUrl: String? = null,
    isLoadingChannels: Boolean,
    listState: LazyListState,
    media3Player: Media3Player?,
    showLogos: Boolean = true,
    layout: Int = 0,
    favouriteUrls: Set<String> = emptySet(),
    onToggleFavourite: (M3U8Channel) -> Unit = {},
    onChannelClick: (M3U8Channel) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val showPlayerStatsButton = remember { context.getShowPlayerStats() }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf("") }
    var favouritesOnly by remember { mutableStateOf(false) }

    val groupSet = remember(channels) {
        channels.flatMap { it.group.split(";").map { tag -> tag.trim() } }
            .filter { it.isNotEmpty() }
            .toSet()
            .sorted()
    }

    val filteredChannels = remember(channels, searchQuery, selectedGroup, favouritesOnly, favouriteUrls) {
        var result = channels
        if (favouritesOnly) {
            result = result.filter { it.url in favouriteUrls }
        }
        if (selectedGroup.isNotEmpty()) {
            result = result.filter { channel ->
                channel.group.split(";").any { it.trim() == selectedGroup }
            }
        }
        if (searchQuery.isNotEmpty()) {
            result = result.filter { channel ->
                channel.name.contains(searchQuery, ignoreCase = true)
            }
        }
        result
    }
    
    LaunchedEffect(isSearching) {
        if (!isSearching && currentChannelUrl.isNotEmpty()) {
            val currentIndex = channels.indexOfFirst { it.url == currentChannelUrl }
            if (currentIndex >= 0) {
                listState.animateScrollToItem(currentIndex)
            }
        }
    }
    
    Column(modifier = modifier) {
        var showStatsDialog by remember { mutableStateOf(false) }
        
        if (channels.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp)
                    .height(40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Channels: ${filteredChannels.size}",
                    style = MaterialTheme.typography.titleMedium
                )
                if (channels.size > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { favouritesOnly = !favouritesOnly },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.PushPin,
                                "Favourites",
                                tint = if (favouritesOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(
                            onClick = {
                                isSearching = !isSearching
                                if (!isSearching) searchQuery = ""
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Search, "Search")
                        }
                        if (showPlayerStatsButton) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { showStatsDialog = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.Info, "Stats")
                            }
                        }
                    }
                } else if (showPlayerStatsButton) {
                    IconButton(
                        onClick = { showStatsDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.Info, "Stats")
                    }
                }
            }

            if (channels.size > 1) {
                AnimatedVisibility(visible = isSearching) {
                    IptvSearchBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 2.dp, bottom = 0.dp),
                        text = searchQuery,
                        placeholderText = "Search channels...",
                        onValueChange = { searchQuery = it },
                        onClear = {
                            scope.launch {
                                val currentIndex = channels.indexOfFirst { it.url == currentChannelUrl }
                                if (currentIndex >= 0) {
                                    listState.animateScrollToItem(currentIndex)
                                }
                            }
                        }
                    )
                }
            }

            if (groupSet.size > 1) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clipToBounds()
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedGroup.isEmpty(),
                                onClick = {
                                    selectedGroup = ""
                                    scope.launch {
                                        val currentIndex = channels.indexOfFirst { it.url == currentChannelUrl }
                                        if (currentIndex >= 0) {
                                            listState.animateScrollToItem(currentIndex)
                                        }
                                    }
                                },
                                label = { Text("All") }
                            )
                        }
                        items(groupSet.toList()) { group ->
                            FilterChip(
                                selected = selectedGroup == group,
                                onClick = {
                                    selectedGroup = group
                                },
                                label = { Text(group) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (layout == 1) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rows = filteredChannels.chunked(2)
                    items(
                        items = rows,
                        key = { row -> row.first().id }
                    ) { rowChannels ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowChannels.forEach { channel ->
                                Box(modifier = Modifier.weight(1f)) {
                                    IptvChannelItem(
                                        channel = channel,
                                        isPlaying = channel.url == currentChannelUrl,
                                        actualPlayingUrl = if (channel.url == currentChannelUrl) actualPlayingUrl else null,
                                        showLogo = showLogos,
                                        isFavourite = channel.url in favouriteUrls,
                                        onToggleFavourite = { onToggleFavourite(channel) },
                                        onClick = { onChannelClick(channel) }
                                    )
                                }
                            }
                            if (rowChannels.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredChannels,
                        key = { channel -> channel.id }
                    ) { channel ->
                        IptvChannelItem(
                            channel = channel,
                            isPlaying = channel.url == currentChannelUrl,
                            actualPlayingUrl = if (channel.url == currentChannelUrl) actualPlayingUrl else null,
                            showLogo = showLogos,
                            isFavourite = channel.url in favouriteUrls,
                            onToggleFavourite = { onToggleFavourite(channel) },
                            onClick = { onChannelClick(channel) }
                        )
                    }
                }
            }
        } else if (isLoadingChannels) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading channels...")
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Single stream",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (showStatsDialog) {
            IptvPlayerStatsDialog(
                media3Player = media3Player,
                onDismiss = { showStatsDialog = false }
            )
        }
    }
}

@Composable
fun IptvChannelItem(
    channel: M3U8Channel,
    isPlaying: Boolean,
    actualPlayingUrl: String? = null,
    showLogo: Boolean = true,
    isFavourite: Boolean = false,
    onToggleFavourite: () -> Unit = {},
    onClick: () -> Unit
) {
    val displayUrl = actualPlayingUrl ?: channel.url
    val tags = buildList {
        if (channel.group.isNotEmpty()) {
            channel.group.split(";").forEach { tag ->
                val trimmedTag = tag.trim()
                if (trimmedTag.isNotEmpty()) add(trimmedTag)
            }
        }
        if (channel.country.isNotEmpty()) add(channel.country)
        if (channel.language.isNotEmpty()) add(channel.language)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) 
                MaterialTheme.colorScheme.primaryContainer
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        var isEpgExpanded by remember { mutableStateOf(false) }
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 96.dp)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (showLogo && channel.logoUrl.isNotEmpty()) {
                            IptvChannelLogo(
                                logoUrl = channel.logoUrl,
                                contentDescription = channel.name
                            )
                        }
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Text(
                        text = displayUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            tags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Playing",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(
                    onClick = onToggleFavourite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Favourite",
                        tint = if (isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            if (channel.epgPrograms.isNotEmpty()) {
                val currentTime = System.currentTimeMillis()
                val upcomingPrograms = remember(channel.epgPrograms) {
                    channel.epgPrograms.filter { it.stopTime > currentTime }
                }
                
                if (upcomingPrograms.isNotEmpty()) {
                    val currentProgram = upcomingPrograms.first()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isEpgExpanded = !isEpgExpanded }
                            .padding(horizontal = 16.dp)
                            .padding(top = 0.dp, bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = currentProgram.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isEpgExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isEpgExpanded) "Collapse EPG" else "Expand EPG",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        AnimatedVisibility(visible = isEpgExpanded) {
                            IptvEpgProgramList(
                                programs = upcomingPrograms.take(10),
                                selectedDate = null,
                                scrollable = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
