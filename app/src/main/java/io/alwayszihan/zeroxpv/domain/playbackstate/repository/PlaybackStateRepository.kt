package io.alwayszihan.zeroxpv.domain.playbackstate.repository

import io.alwayszihan.zeroxpv.database.entities.PlaybackStateEntity

interface PlaybackStateRepository {
  suspend fun upsert(playbackState: PlaybackStateEntity)

  suspend fun getVideoDataByTitle(mediaTitle: String): PlaybackStateEntity?

  suspend fun getAllPlaybackStates(): List<PlaybackStateEntity>

  suspend fun clearAllPlaybackStates()

  suspend fun deleteByTitle(mediaTitle: String)

  suspend fun updateMediaTitle(
    oldTitle: String,
    newTitle: String,
  )
}

