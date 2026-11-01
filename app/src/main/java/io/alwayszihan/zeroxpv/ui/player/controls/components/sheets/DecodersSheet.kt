package io.alwayszihan.zeroxpv.ui.player.controls.components.sheets

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.alwayszihan.zeroxpv.R
import io.alwayszihan.zeroxpv.presentation.components.PlayerSheet
import io.alwayszihan.zeroxpv.ui.player.Decoder

@Composable
fun DecodersSheet(
  selectedDecoder: Decoder,
  onSelect: (Decoder) -> Unit,
  onDismissRequest: () -> Unit,
) {
  PlayerSheet(onDismissRequest) {
    LazyColumn {
      items(Decoder.entries.minusElement(Decoder.Auto)) { decoder ->
        AudioTrackRow(
          title = stringResource(R.string.player_sheets_decoder_formatted, decoder.title, decoder.value),
          isSelected = selectedDecoder == decoder,
          onClick = { onSelect(decoder) },
        )
      }
    }
  }
}

