package io.alwayszihan.zeroxpv.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.alwayszihan.zeroxpv.database.converters.NetworkProtocolConverter
import io.alwayszihan.zeroxpv.database.dao.NetworkConnectionDao
import io.alwayszihan.zeroxpv.database.dao.PlaybackStateDao
import io.alwayszihan.zeroxpv.database.dao.PlaylistDao
import io.alwayszihan.zeroxpv.database.dao.RecentlyPlayedDao
import io.alwayszihan.zeroxpv.database.dao.SecureMediaDao
import io.alwayszihan.zeroxpv.database.dao.VideoMetadataDao
import io.alwayszihan.zeroxpv.database.entities.PlaybackStateEntity
import io.alwayszihan.zeroxpv.database.entities.PlaylistEntity
import io.alwayszihan.zeroxpv.database.entities.PlaylistItemEntity
import io.alwayszihan.zeroxpv.database.entities.RecentlyPlayedEntity
import io.alwayszihan.zeroxpv.database.entities.SecureMediaEntity
import io.alwayszihan.zeroxpv.database.entities.VideoMetadataEntity
import io.alwayszihan.zeroxpv.domain.network.NetworkConnection

@Database(
  entities = [
    PlaybackStateEntity::class,
    RecentlyPlayedEntity::class,
    VideoMetadataEntity::class,
    NetworkConnection::class,
    PlaylistEntity::class,
    PlaylistItemEntity::class,
    SecureMediaEntity::class,
  ],
  version = 13,
  exportSchema = true,
)
@TypeConverters(NetworkProtocolConverter::class)
abstract class ZeroXPVDatabase : RoomDatabase() {
  abstract fun videoDataDao(): PlaybackStateDao

  abstract fun recentlyPlayedDao(): RecentlyPlayedDao

  abstract fun videoMetadataDao(): VideoMetadataDao

  abstract fun networkConnectionDao(): NetworkConnectionDao

  abstract fun playlistDao(): PlaylistDao

  abstract fun secureMediaDao(): SecureMediaDao
}

