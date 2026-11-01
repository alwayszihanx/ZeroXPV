package io.alwayszihan.zeroxpv.ui.iptv.manager

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HistoryItem(
    val url: String,
    val name: String,
    val lastChannelUrl: String? = null,
    val lastChannelId: String? = null,
    val logoUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

object HistoryManager {

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())

    val historyFlow: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    fun getHistory(): List<HistoryItem> = _history.value

    fun addOrUpdateHistory(
        url: String,
        name: String,
        lastChannelUrl: String? = null,
        lastChannelId: String? = null,
        logoUrl: String = ""
    ) {
        val current = _history.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.url == url }
        val entry = HistoryItem(
            url = url,
            name = name,
            lastChannelUrl = lastChannelUrl,
            lastChannelId = lastChannelId,
            logoUrl = logoUrl
        )
        if (existingIndex >= 0) {
            current[existingIndex] = entry
        } else {
            current.add(entry)
        }
        _history.value = current
    }

    fun deleteHistoryItem(item: HistoryItem) {
        _history.value = _history.value.filter { it.url != item.url }
    }

    suspend fun deleteHistoryItemWithUndo(
        item: HistoryItem,
        snackbarHostState: SnackbarHostState
    ) {
        val url = item.url
        val name = item.name
        val lastChannelUrl = item.lastChannelUrl
        val lastChannelId = item.lastChannelId
        val logoUrl = item.logoUrl

        deleteHistoryItem(item)
        val result = snackbarHostState.showSnackbar(
            message = "Item deleted",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            addOrUpdateHistory(url, name, lastChannelUrl, lastChannelId, logoUrl)
        }
    }
}
