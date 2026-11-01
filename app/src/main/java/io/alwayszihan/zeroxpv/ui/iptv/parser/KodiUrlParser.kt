package io.alwayszihan.zeroxpv.ui.iptv.parser

internal object KodiUrlParser {
    private const val HTTP_OPTION_UA = "user-agent"
    
    fun parseKodiUrlOptions(url: String): Map<String, String?> {
        val index = url.indexOf('|')
        if (index == -1) return emptyMap()
        val options = url.drop(index + 1).split("&")
        return options
            .filter { it.isNotBlank() }
            .associate {
                val pair = it.split("=")
                val key = pair.getOrNull(0).orEmpty()
                val value = pair.getOrNull(1)
                key to value
            }
    }
    
    fun extractUserAgent(url: String): String? {
        val kodiOptions = parseKodiUrlOptions(url)
        return kodiOptions[HTTP_OPTION_UA]
    }
    
    fun cleanUrl(url: String): String {
        val index = url.indexOf('|')
        return if (index == -1) url else url.take(index)
    }
}
