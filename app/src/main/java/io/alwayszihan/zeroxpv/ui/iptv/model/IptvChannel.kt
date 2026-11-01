package io.alwayszihan.zeroxpv.ui.iptv.model

data class IptvChannel(
    val id: String,
    val name: String,
    val url: String,
    val logo: String = "",
    val groupTitle: String = "",
    val tvgId: String = "",
    val tvgName: String = "",
    val tvgLogo: String = "",
    val country: String = "",
    val language: String = "",
    val isPlaying: Boolean = false,
    val epgPrograms: List<EpgProgram> = emptyList(),
    val drmLicenseType: String? = null,
    val drmLicenseKey: String? = null,
    val channelName: String = "",
    val relationId: String = ""
) {
    companion object {
        const val LICENSE_TYPE_WIDEVINE = "com.widevine.alpha"
        const val LICENSE_TYPE_CLEAR_KEY = "clearkey"
        const val LICENSE_TYPE_CLEAR_KEY_2 = "org.w3.clearkey"
        const val LICENSE_TYPE_PLAY_READY = "com.microsoft.playready"
    }
}
