package io.alwayszihan.zeroxpv.ui.iptv.model

data class EpgProgram(
    val title: String,
    val startTime: Long,
    val stopTime: Long,
    val channelId: String
)
