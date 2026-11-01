package io.alwayszihan.zeroxpv.ui.browser.recentlyplayed

import io.alwayszihan.zeroxpv.database.entities.PlaylistEntity
import io.alwayszihan.zeroxpv.domain.media.model.Video

sealed class RecentlyPlayedItem {
  abstract val timestamp: Long

  data class VideoItem(
    val video: Video,
    override val timestamp: Long,
  ) : RecentlyPlayedItem()

  data class PlaylistItem(
    val playlist: PlaylistEntity,
    val videoCount: Int,
    val mostRecentVideoPath: String,
    override val timestamp: Long,
  ) : RecentlyPlayedItem()
}

