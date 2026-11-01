package io.alwayszihan.zeroxpv.ui.iptv.parser

import io.alwayszihan.zeroxpv.ui.iptv.manager.EpgManager
import io.alwayszihan.zeroxpv.ui.iptv.manager.toM3U8Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

sealed class M3UResult<out T> {
    data class Success<T>(val data: T) : M3UResult<T>()
    data class Error(val exception: Exception, val message: String? = null) : M3UResult<Nothing>()

    inline fun onSuccess(action: (T) -> Unit): M3UResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (Exception, String?) -> Unit): M3UResult<T> {
        if (this is Error) action(exception, message)
        return this
    }
}

private const val M3U8_MAX_SIZE_BYTES = 10 * 1024 * 1024
private const val M3U8_CONNECTION_TIMEOUT_MS = 10000L
private const val M3U8_READ_TIMEOUT_MS = 10000L

data class M3U8Channel(
    val id: String,
    val name: String,
    val url: String,
    val group: String = "",
    val country: String = "",
    val language: String = "",
    val logoUrl: String = "",
    val relationId: String = "",
    val epgTitle: String = "",
    val epgPrograms: List<EpgProgram> = emptyList(),
    val licenseType: String? = null,
    val licenseKey: String? = null,
    val catchupType: String = "",
    val catchupDays: Int = 0,
    val catchupSource: String = ""
) {
    companion object {
        const val LICENSE_TYPE_WIDEVINE = "com.widevine.alpha"
        const val LICENSE_TYPE_CLEAR_KEY = "clearkey"
        const val LICENSE_TYPE_CLEAR_KEY_2 = "org.w3.clearkey"
        const val LICENSE_TYPE_PLAY_READY = "com.microsoft.playready"
    }
}

object M3ULoader {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(M3U8_CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(M3U8_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build()
    }

    private var epgCallback: ((List<M3U8Channel>) -> Unit)? = null

    suspend fun parseM3U8(content: String): M3UResult<List<M3U8Channel>> = withContext(Dispatchers.IO) {
        try {
            val (channels, epgUrl) = M3UParser.parse(content)
            val m3u8Channels = channels.map { it.toM3U8Channel() }

            if (epgUrl != null && channels.isNotEmpty()) {
                EpgManager.loadEpg(channels, epgUrl, client) { channelsWithEpg ->
                    val updatedChannels = channelsWithEpg.map { it.toM3U8Channel() }
                    epgCallback?.invoke(updatedChannels)
                }
            }

            M3UResult.Success(m3u8Channels)
        } catch (e: Exception) {
            M3UResult.Error(e, "Failed to parse M3U8 content")
        }
    }

    suspend fun parseM3U8FromUrl(url: String): M3UResult<List<M3U8Channel>> = withContext(Dispatchers.IO) {
        val httpsUrl = convertToHttps(url)

        try {
            val content = fetchM3U8Content(httpsUrl)
            val result = parseM3U8(content)

            if (result is M3UResult.Success && result.data.isEmpty()) {
                val singleChannel = M3U8Channel(
                    id = "${httpsUrl.hashCode()}_0",
                    name = httpsUrl.substringAfterLast("/").substringBeforeLast(".").ifEmpty {
                        httpsUrl.substringAfterLast("/").substringBefore("?").ifEmpty { "Stream" }
                    },
                    url = httpsUrl
                )
                return@withContext M3UResult.Success(listOf(singleChannel))
            }

            return@withContext result
        } catch (e: Exception) {
            if (httpsUrl != url) {
                try {
                    val content = fetchM3U8Content(url)
                    val result = parseM3U8(content)

                    if (result is M3UResult.Success && result.data.isEmpty()) {
                        val singleChannel = M3U8Channel(
                            id = "${url.hashCode()}_0",
                            name = url.substringAfterLast("/").substringBeforeLast(".").ifEmpty {
                                url.substringAfterLast("/").substringBefore("?").ifEmpty { "Stream" }
                            },
                            url = url
                        )
                        return@withContext M3UResult.Success(listOf(singleChannel))
                    }

                    return@withContext result
                } catch (fallbackException: Exception) {
                    M3UResult.Error(fallbackException, "Failed to fetch M3U8 from URL: ${fallbackException.message}")
                }
            } else {
                M3UResult.Error(e, "Failed to fetch M3U8 from URL: ${e.message}")
            }
        }
    }

    private fun fetchM3U8Content(url: String): String {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}")
        }

        val contentLength = response.body.contentLength()
        if (contentLength > M3U8_MAX_SIZE_BYTES) {
            response.close()
            throw Exception("Content too large")
        }

        return response.body.string()
    }

    fun setEpgLoadedCallback(callback: (List<M3U8Channel>) -> Unit) {
        epgCallback = callback
    }

    fun clearEpgLoadedCallback() {
        epgCallback = null
        EpgManager.clear()
    }

    private fun M3UChannel.toM3U8Channel() = M3U8Channel(
        id = id,
        name = name,
        url = url,
        group = group,
        logoUrl = logoUrl,
        relationId = relationId,
        licenseType = licenseType,
        licenseKey = licenseKey,
        catchupType = catchupType,
        catchupDays = catchupDays,
        catchupSource = catchupSource
    )

    private fun convertToHttps(url: String): String {
        return if (url.startsWith("http://", ignoreCase = true)) {
            url.replaceFirst("http://", "https://", ignoreCase = true)
        } else url
    }
}
