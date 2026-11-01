package io.alwayszihan.zeroxpv.ui.iptv.viewmodel

import androidx.lifecycle.ViewModel
import io.alwayszihan.zeroxpv.ui.iptv.manager.HistoryItem
import io.alwayszihan.zeroxpv.ui.iptv.parser.M3U8Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackState(
    val playingUrl: String? = null,
    val initialChannelUrl: String? = null,
    val initialVideoTitle: String? = null,
    val initialChannelId: String? = null
)

data class ChannelEditState(
    val historyItem: HistoryItem? = null,
    val channels: List<M3U8Channel> = emptyList()
)

class IptvMainViewModel : ViewModel() {

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _channelEditState = MutableStateFlow(ChannelEditState())
    val channelEditState: StateFlow<ChannelEditState> = _channelEditState.asStateFlow()

    fun startPlayback(
        url: String,
        channelUrl: String? = null,
        videoTitle: String? = null,
        channelId: String? = null
    ) {
        _playbackState.value = PlaybackState(
            playingUrl = url,
            initialChannelUrl = channelUrl,
            initialVideoTitle = videoTitle,
            initialChannelId = channelId
        )
    }

    fun startPlaybackFromHistory(historyItem: HistoryItem) {
        _playbackState.value = PlaybackState(
            playingUrl = historyItem.url,
            initialChannelUrl = historyItem.lastChannelUrl,
            initialVideoTitle = historyItem.name,
            initialChannelId = historyItem.lastChannelId
        )
    }

    fun stopPlayback() {
        _playbackState.value = PlaybackState()
    }

    fun startChannelEdit(historyItem: HistoryItem, channels: List<M3U8Channel>) {
        _channelEditState.value = ChannelEditState(historyItem = historyItem, channels = channels)
    }

    fun updateChannels(channels: List<M3U8Channel>) {
        _channelEditState.value = _channelEditState.value.copy(channels = channels)
    }

    fun stopChannelEdit() {
        _channelEditState.value = ChannelEditState()
    }
}
