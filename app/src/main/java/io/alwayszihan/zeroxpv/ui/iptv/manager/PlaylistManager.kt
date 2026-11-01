package io.alwayszihan.zeroxpv.ui.iptv.manager

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

enum class PlaylistSourceType {
    URL,
    FILE
}

@Serializable
data class PlaylistSourceEntry(
    val source: String,
    val type: PlaylistSourceType
)

@Serializable
data class PlaylistItem(
    val id: String,
    val name: String,
    val sources: List<PlaylistSourceEntry> = emptyList(),
    val epgUrl: String = "",
    val channelCount: Int = 0,
    val logoUrl: String = "",
    val isDefault: Boolean = false,
    val order: Int = 0,
    val favourites: List<String> = emptyList(),
    val lastChannelUrl: String = "",
    val lastChannelName: String = "",
    val lastChannelId: String? = null,
    val health: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val primarySource: PlaylistSourceEntry?
        get() = sources.firstOrNull()

    val displayType: String
        get() = if (sources.size > 1) "Mixed (${sources.size})" else sources.firstOrNull()?.type?.name
            ?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Empty"
}

object PlaylistManager {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val _playlists = MutableStateFlow<List<PlaylistItem>>(emptyList())

    val playlistsFlow: StateFlow<List<PlaylistItem>> = _playlists.asStateFlow()

    fun getPlaylists(): List<PlaylistItem> = _playlists.value

    fun getDefaultPlaylist(): PlaylistItem? = _playlists.value.firstOrNull { it.isDefault }

    fun init(context: Context) {
        loadFromDisk(context)
    }

    private fun storageFile(context: Context): File {
        return File(context.filesDir, "iptv_playlists.json")
    }

    private fun loadFromDisk(context: Context) {
        try {
            val file = storageFile(context)
            if (file.exists()) {
                val content = file.readText()
                if (content.isNotBlank()) {
                    val list = json.decodeFromString<List<PlaylistItem>>(content)
                    _playlists.value = list.sortedBy { it.order }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveToDisk(context: Context) {
        try {
            val file = storageFile(context)
            val temp = File(context.filesDir, "iptv_playlists.json.tmp")
            temp.writeText(json.encodeToString(_playlists.value))
            temp.renameTo(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addOrUpdatePlaylist(
        context: Context,
        id: String? = null,
        name: String,
        sources: List<PlaylistSourceEntry>,
        epgUrl: String = "",
        channelCount: Int = 0,
        logoUrl: String = "",
        isDefault: Boolean = false,
        order: Int? = null,
        favourites: List<String>? = null,
        lastChannelUrl: String? = null,
        lastChannelName: String? = null,
        lastChannelId: String? = null,
        health: String? = null
    ) {
        val current = _playlists.value.toMutableList()
        val resolvedId = id ?: "${System.currentTimeMillis()}_${(0..9999).random()}"
        val existingIndex = if (id != null) current.indexOfFirst { it.id == id } else -1
        val resolvedOrder = order ?: (current.maxOfOrNull { it.order }?.plus(1) ?: 0)
        val existing = if (existingIndex >= 0) current[existingIndex] else null

        if (isDefault) {
            current.forEachIndexed { index, item ->
                if (item.isDefault && item.id != resolvedId) {
                    current[index] = item.copy(isDefault = false)
                }
            }
        }

        val entry = PlaylistItem(
            id = resolvedId,
            name = name,
            sources = sources,
            epgUrl = epgUrl,
            channelCount = channelCount,
            logoUrl = logoUrl,
            isDefault = isDefault,
            order = resolvedOrder,
            favourites = favourites ?: existing?.favourites ?: emptyList(),
            lastChannelUrl = lastChannelUrl ?: existing?.lastChannelUrl ?: "",
            lastChannelName = lastChannelName ?: existing?.lastChannelName ?: "",
            lastChannelId = lastChannelId ?: existing?.lastChannelId,
            health = health ?: existing?.health ?: "",
            timestamp = System.currentTimeMillis()
        )

        if (existingIndex >= 0) {
            current[existingIndex] = entry
        } else {
            current.add(entry)
        }
        current.sortBy { it.order }
        _playlists.value = current
        saveToDisk(context)
    }

    fun setChannelCount(context: Context, id: String, count: Int) {
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(channelCount = count)
            _playlists.value = current
            saveToDisk(context)
        }
    }

    fun updateMeta(context: Context, id: String, count: Int, logoUrl: String, health: String = "") {
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(channelCount = count, logoUrl = logoUrl, health = health)
            _playlists.value = current
            saveToDisk(context)
        }
    }

    fun reorder(context: Context, fromIndex: Int, toIndex: Int) {
        val current = _playlists.value.toMutableList()
        if (fromIndex < 0 || fromIndex >= current.size || toIndex < 0 || toIndex >= current.size) return
        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)
        current.forEachIndexed { index, item ->
            current[index] = item.copy(order = index)
        }
        _playlists.value = current
        saveToDisk(context)
    }

    fun deletePlaylist(context: Context, item: PlaylistItem) {
        _playlists.value = _playlists.value.filter { it.id != item.id }
        saveToDisk(context)
    }

    suspend fun deletePlaylistWithUndo(
        context: Context,
        item: PlaylistItem,
        snackbarHostState: SnackbarHostState
    ) {
        val copy = item
        deletePlaylist(context, item)
        val result = snackbarHostState.showSnackbar(
            message = "Playlist deleted",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            addOrUpdatePlaylist(
                context,
                copy.id,
                copy.name,
                copy.sources,
                copy.epgUrl,
                copy.channelCount,
                copy.logoUrl,
                copy.isDefault,
                copy.order
            )
        }
    }

    fun validate(item: PlaylistItem): List<String> {
        val issues = mutableListOf<String>()
        if (item.name.isBlank()) issues.add("Name is empty")
        if (item.sources.isEmpty()) {
            issues.add("No source configured")
        } else {
            item.sources.forEach { entry ->
                if (entry.source.isBlank()) {
                    issues.add("A source is empty")
                } else if (entry.type == PlaylistSourceType.URL &&
                    !entry.source.startsWith("http", ignoreCase = true)
                ) {
                    issues.add("URL source is not a valid http(s) link")
                }
            }
        }
        return issues
    }

    fun mergedContent(context: Context, item: PlaylistItem): String {
        val sb = StringBuilder()
        sb.append("#EXTM3U")
        if (item.epgUrl.isNotBlank()) {
            sb.append(" url-tvg=\"${item.epgUrl}\"")
        }
        sb.append("\n")
        for (entry in item.sources) {
            val content = try {
                when (entry.type) {
                    PlaylistSourceType.URL -> fetchUrlContent(entry.source)
                    PlaylistSourceType.FILE -> readFileContent(context, entry.source)
                }
            } catch (e: Exception) {
                null
            }
            if (content != null) {
                content.lineSequence()
                    .filter { it.isNotBlank() }
                    .forEach { line -> sb.append(line.trimEnd()).append("\n") }
            }
        }
        return sb.toString()
    }

    private fun fetchUrlContent(url: String): String? {
        return try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
            val request = okhttp3.Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) response.body.string() else null
        } catch (e: Exception) {
            null
        }
    }

    private fun readFileContent(context: Context, uriString: String): String? {
        return try {
            val uri = android.net.Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
        } catch (e: Exception) {
            null
        }
    }

    fun resolvePlayUri(context: Context, item: PlaylistItem): String {
        if (item.sources.size <= 1) {
            return item.primarySource?.source ?: ""
        }
        val merged = mergedContent(context, item)
        return try {
            val file = File(context.cacheDir, "merged_playlist_${item.id}.m3u")
            file.writeText(merged)
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            ).toString()
        } catch (e: Exception) {
            item.primarySource?.source ?: ""
        }
    }

    suspend fun fetchChannels(context: Context, item: PlaylistItem): List<io.alwayszihan.zeroxpv.ui.iptv.parser.M3U8Channel> {
        val loader = io.alwayszihan.zeroxpv.ui.iptv.parser.M3ULoader
        val uri = resolvePlayUri(context, item)
        if (uri.isBlank()) return emptyList()
        return if (uri.startsWith("http", ignoreCase = true)) {
            loader.parseM3U8FromUrl(uri).let { result ->
                if (result is io.alwayszihan.zeroxpv.ui.iptv.parser.M3UResult.Success) result.data else emptyList()
            }
        } else {
            val content = try {
                if (uri.startsWith("content://") || uri.startsWith("file://")) {
                    context.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use { it.bufferedReader().readText() }
                } else {
                    mergedContent(context, item)
                }
            } catch (e: Exception) { null }
            content?.let { loader.parseM3U8(it).let { r -> if (r is io.alwayszihan.zeroxpv.ui.iptv.parser.M3UResult.Success) r.data else emptyList() } }
                ?: emptyList()
        }
    }

    suspend fun refreshMeta(context: Context, item: PlaylistItem) {
        val channels = fetchChannels(context, item)
        val logo = channels.firstOrNull { it.logoUrl.isNotBlank() }?.logoUrl ?: ""
        val health = when {
            item.sources.isEmpty() -> "No source"
            channels.isEmpty() -> "Unreachable"
            else -> ""
        }
        updateMeta(context, item.id, channels.size, logo, health)
    }

    fun toggleFavourite(context: Context, id: String, channelUrl: String) {
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return
        val item = current[index]
        val favs = item.favourites.toMutableList()
        if (favs.contains(channelUrl)) favs.remove(channelUrl) else favs.add(channelUrl)
        current[index] = item.copy(favourites = favs)
        _playlists.value = current
        saveToDisk(context)
    }

    fun setLastChannel(
        context: Context,
        id: String,
        channelUrl: String,
        channelName: String,
        channelId: String?
    ) {
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return
        val item = current[index]
        if (item.lastChannelUrl == channelUrl) return
        current[index] = item.copy(
            lastChannelUrl = channelUrl,
            lastChannelName = channelName,
            lastChannelId = channelId
        )
        _playlists.value = current
        saveToDisk(context)
    }

    fun setHealth(context: Context, id: String, health: String) {
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return
        current[index] = current[index].copy(health = health)
        _playlists.value = current
        saveToDisk(context)
    }

    fun exportToString(): String = json.encodeToString(_playlists.value)

    fun importFromString(context: Context, jsonString: String): Boolean {
        return try {
            val imported = json.decodeFromString<List<PlaylistItem>>(jsonString)
            val current = _playlists.value.toMutableList()
            imported.forEach { incoming ->
                val idx = current.indexOfFirst { it.id == incoming.id }
                if (idx >= 0) current[idx] = incoming else current.add(incoming)
            }
            current.sortBy { it.order }
            _playlists.value = current
            saveToDisk(context)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
