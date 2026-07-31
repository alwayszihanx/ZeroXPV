package io.alwayszihan.zeroxpv.database.repository

import io.alwayszihan.zeroxpv.database.entities.PlaybackStateEntity
import io.alwayszihan.zeroxpv.database.ZeroXPVDatabase
import io.alwayszihan.zeroxpv.domain.playbackstate.repository.PlaybackStateRepository

class PlaybackStateRepositoryImpl(
  private val database: ZeroXPVDatabase,
) : PlaybackStateRepository {
  override suspend fun upsert(playbackState: PlaybackStateEntity) {
    database.videoDataDao().upsert(playbackState)
  }

  override suspend fun getVideoDataByTitle(mediaTitle: String): PlaybackStateEntity? =
    database.videoDataDao().getVideoDataByTitle(mediaTitle)

  override suspend fun getAllPlaybackStates(): List<PlaybackStateEntity> =
    database.videoDataDao().getAllPlaybackStates()

  override suspend fun clearAllPlaybackStates() {
    database.videoDataDao().clearAllPlaybackStates()
  }

  override suspend fun deleteByTitle(mediaTitle: String) {
    database.videoDataDao().deleteByTitle(mediaTitle)
  }

  override suspend fun updateMediaTitle(
    oldTitle: String,
    newTitle: String,
  ) {
    database.videoDataDao().updateMediaTitle(oldTitle, newTitle)
  }
}

