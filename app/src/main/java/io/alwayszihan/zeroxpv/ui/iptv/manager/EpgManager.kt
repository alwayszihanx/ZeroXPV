package io.alwayszihan.zeroxpv.ui.iptv.manager

import io.alwayszihan.zeroxpv.ui.iptv.parser.EpgParser
import io.alwayszihan.zeroxpv.ui.iptv.parser.EpgProgram
import io.alwayszihan.zeroxpv.ui.iptv.parser.M3U8Channel
import io.alwayszihan.zeroxpv.ui.iptv.parser.M3UChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream

data class ChannelWithEpg(
    val channel: M3UChannel,
    val epgTitle: String = "",
    val epgPrograms: List<EpgProgram> = emptyList()
)

object EpgManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var epgDataBySource: Map<String, Map<String, List<EpgProgram>>> = emptyMap()
    private val channelIndex = ConcurrentHashMap<String, MutableSet<String>>()
    private var onEpgLoadedCallback: ((List<ChannelWithEpg>) -> Unit)? = null
    private var cachedChannels: List<M3UChannel> = emptyList()
    private var epgJob: kotlinx.coroutines.Job? = null

    private val _refreshingEpgUrls = MutableStateFlow<List<String>>(emptyList())

    fun loadEpg(
        channels: List<M3UChannel>,
        epgUrl: String?,
        client: OkHttpClient,
        callback: (List<ChannelWithEpg>) -> Unit
    ) {
        cachedChannels = channels
        onEpgLoadedCallback = callback

        epgJob?.cancel()
        epgJob = scope.launch {
            downloadAndParseEpg(epgUrl, client)
            val channelsWithEpg = attachEpgToChannels(channels)
            onEpgLoadedCallback?.invoke(channelsWithEpg)
        }
    }

    private suspend fun downloadAndParseEpg(epgUrl: String?, client: OkHttpClient) {
        if (epgUrl == null) return

        val urls = epgUrl.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val newEpgDataBySource = mutableMapOf<String, Map<String, List<EpgProgram>>>()

        withContext(Dispatchers.IO) {
            for (singleUrl in urls) {
                if (singleUrl in _refreshingEpgUrls.value) {
                    continue
                }

                try {
                    _refreshingEpgUrls.value += singleUrl

                    val request = Request.Builder().url(singleUrl).build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val stream = response.body.byteStream()
                            val inputStream = if (singleUrl.endsWith(".gz", ignoreCase = true) ||
                                response.header("Content-Encoding")?.contains("gzip", ignoreCase = true) == true) {
                                GZIPInputStream(stream)
                            } else {
                                stream
                            }

                            val channelMap = mutableMapOf<String, MutableList<EpgProgram>>()
                            EpgParser.parse(inputStream).collect { program ->
                                channelMap.getOrPut(program.channelId) { mutableListOf() }.add(program)
                            }

                            val sortedData = channelMap.mapValues { entry ->
                                entry.value.distinctBy { it.startTime }.sortedBy { it.startTime }
                            }

                            newEpgDataBySource[singleUrl] = sortedData
                            sortedData.keys.forEach { channelId ->
                                channelIndex.getOrPut(channelId) { mutableSetOf() }.add(singleUrl)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    _refreshingEpgUrls.value -= singleUrl
                }
            }
        }

        epgDataBySource = epgDataBySource + newEpgDataBySource
    }

    private fun attachEpgToChannels(channels: List<M3UChannel>): List<ChannelWithEpg> {
        if (epgDataBySource.isEmpty()) {
            return channels.map { ChannelWithEpg(it) }
        }

        val currentTime = System.currentTimeMillis()

        return channels.map { channel ->
            val relationId = channel.relationId

            if (relationId.isEmpty()) {
                return@map ChannelWithEpg(channel)
            }

            val programs = findProgramsForChannel(relationId, currentTime)

            if (programs.isEmpty()) {
                return@map ChannelWithEpg(channel)
            }

            val currentProgram = programs.find { currentTime in it.startTime..it.stopTime }
            val epgTitle = currentProgram?.title ?: ""

            ChannelWithEpg(
                channel = channel,
                epgTitle = epgTitle,
                epgPrograms = programs
            )
        }
    }

    private fun findProgramsForChannel(relationId: String, currentTime: Long): List<EpgProgram> {
        val epgUrls = channelIndex[relationId]

        if (epgUrls != null && epgUrls.isNotEmpty()) {
            for (epgUrl in epgUrls) {
                val epgData = epgDataBySource[epgUrl] ?: continue
                val programs = epgData[relationId] ?: continue
                if (programs.any { currentTime in it.startTime..it.stopTime || it.stopTime > currentTime }) {
                    return programs
                }
            }
        } else {
            for ((_, epgData) in epgDataBySource) {
                val programs = epgData[relationId] ?: continue
                if (programs.any { currentTime in it.startTime..it.stopTime || it.stopTime > currentTime }) {
                    return programs
                }
            }
        }

        return emptyList()
    }

    fun clear() {
        onEpgLoadedCallback = null
        epgJob?.cancel()
        epgJob = null
        epgDataBySource = emptyMap()
        channelIndex.clear()
        _refreshingEpgUrls.value = emptyList()
    }
}

fun ChannelWithEpg.toM3U8Channel() = M3U8Channel(
    id = channel.id,
    name = channel.name,
    url = channel.url,
    group = channel.group,
    logoUrl = channel.logoUrl,
    relationId = channel.relationId,
    epgTitle = epgTitle,
    epgPrograms = epgPrograms
)
