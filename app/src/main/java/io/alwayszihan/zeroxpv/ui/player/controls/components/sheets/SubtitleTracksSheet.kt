package io.alwayszihan.zeroxpv.ui.player.controls.components.sheets

import io.alwayszihan.zeroxpv.ui.icons.Icon
import io.alwayszihan.zeroxpv.ui.icons.Icons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import io.alwayszihan.zeroxpv.R
import io.alwayszihan.zeroxpv.presentation.components.PlayerSheet
import io.alwayszihan.zeroxpv.ui.player.TrackNode
import io.alwayszihan.zeroxpv.ui.theme.spacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

sealed class SubtitleItem {
  data class Track(val node: TrackNode) : SubtitleItem()
  data class Header(val title: String) : SubtitleItem()
  object Divider : SubtitleItem()
  object Off : SubtitleItem()
}

@Composable
fun SubtitlesSheet(
  onDismissRequest: () -> Unit,
  tracks: ImmutableList<TrackNode>,
  onToggleSubtitle: (Int) -> Unit,
  isSubtitleSelected: (Int) -> Boolean,
  onAddSubtitle: () -> Unit,
  onOpenSubtitleSettings: () -> Unit,
  onOpenSubtitleDelay: () -> Unit,
  onRemoveSubtitle: (Int) -> Unit,
  onOpenOnlineSearch: () -> Unit,
  subtitlesOff: Boolean = false,
  onDisableSubtitles: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val items = remember(tracks, subtitlesOff) {
    val list = mutableListOf<SubtitleItem>()
    list.add(SubtitleItem.Off)
    val internal = tracks.filter { it.external != true }
    val external = tracks.filter { it.external == true }

    if (internal.isNotEmpty() || external.isNotEmpty()) {
      list.add(SubtitleItem.Header(if (internal.isNotEmpty()) "Embedded Subtitles" else "Local Subtitles"))
      list.addAll(internal.map { SubtitleItem.Track(it) })
      if (internal.isNotEmpty() && external.isNotEmpty()) {
        list.add(SubtitleItem.Header("External Subtitles"))
      }
      list.addAll(external.map { SubtitleItem.Track(it) })
    }

    list.toImmutableList()
  }

  PlayerSheet(onDismissRequest) {
    Column(modifier) {
      AddTrackRow(
        stringResource(R.string.player_sheets_add_ext_sub),
        onAddSubtitle,
        actions = {
          IconButton(onClick = onOpenOnlineSearch) {
            Icon(Icons.Default.Search, null)
          }
          IconButton(onClick = onOpenSubtitleSettings) {
            Icon(Icons.Default.Palette, null)
          }
          IconButton(onClick = onOpenSubtitleDelay) {
            Icon(Icons.Default.AvTimer, null)
          }
        },
      )

      LazyColumn {
        items(items) { item ->
          when (item) {
            is SubtitleItem.Track -> {
              val track = item.node
              SubtitleTrackRow(
                title = getTrackTitle(track),
                isSelected = isSubtitleSelected(track.id),
                isExternal = track.external == true,
                onToggle = { onToggleSubtitle(track.id) },
                onRemove = { onRemoveSubtitle(track.id) },
              )
            }
            is SubtitleItem.Header -> {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = item.title,
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
            is SubtitleItem.Off -> {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable(onClick = onDisableSubtitles)
                  .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
              ) {
                Checkbox(checked = subtitlesOff, onCheckedChange = { onDisableSubtitles() })
                Text(
                  stringResource(R.string.player_sheets_off),
                  fontWeight = if (subtitlesOff) FontWeight.Bold else FontWeight.Normal,
                  modifier = Modifier.weight(1f),
                )
              }
            }
            SubtitleItem.Divider -> {
              HorizontalDivider(
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
              )
            }
          }
        }
        item {
          Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        }
      }
    }
  }
}

@Composable
fun SubtitleTrackRow(
  title: String,
  isSelected: Boolean,
  isExternal: Boolean,
  onToggle: () -> Unit,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onToggle)
      .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.extraSmall),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
  ) {
    Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
    Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
    
    if (isExternal) {
      IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = null) }
    }
  }
}
