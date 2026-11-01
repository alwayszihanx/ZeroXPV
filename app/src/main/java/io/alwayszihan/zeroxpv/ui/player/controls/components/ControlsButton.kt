package io.alwayszihan.zeroxpv.ui.player.controls.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.alwayszihan.zeroxpv.ui.icons.AppIcon
import io.alwayszihan.zeroxpv.ui.icons.Icon
import io.alwayszihan.zeroxpv.ui.icons.Icons
import io.alwayszihan.zeroxpv.ui.player.controls.LocalPlayerButtonsClickEvent
import io.alwayszihan.zeroxpv.ui.theme.spacing
import org.koin.compose.koinInject

@Suppress("ModifierClickableOrder")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ControlsButton(
  icon: AppIcon,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: () -> Unit = {},
  title: String? = null,
  color: Color? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }

  val clickEvent = LocalPlayerButtonsClickEvent.current
  Surface(
    modifier =
      modifier
        .clip(CircleShape)
        .combinedClickable(
          onClick = {
            clickEvent()
            onClick()
          },
          onLongClick = onLongClick,
          interactionSource = interactionSource,
          indication = ripple(),
        ),
    shape = CircleShape,
    color = Color.Transparent,
    contentColor = color ?: MaterialTheme.colorScheme.onSurface,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border =
      null,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = color ?: MaterialTheme.colorScheme.onSurface,
      modifier =
        Modifier
          .padding(MaterialTheme.spacing.small)
          .size(20.dp),
    )
  }
}

@Composable
fun ControlsGroup(
  modifier: Modifier = Modifier,
  content: @Composable RowScope.() -> Unit,
) {
  val spacing = MaterialTheme.spacing

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement =
      androidx.compose.foundation.layout.Arrangement
        .spacedBy(spacing.extraSmall),
    content = content,
  )
}

@Preview
@Composable
private fun PreviewControlsButton() {
  ControlsButton(
    Icons.Default.CatchingPokemon,
    onClick = {},
  )
}

