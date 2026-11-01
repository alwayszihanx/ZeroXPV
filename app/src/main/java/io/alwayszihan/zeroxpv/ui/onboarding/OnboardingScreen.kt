package io.alwayszihan.zeroxpv.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import io.alwayszihan.zeroxpv.R
import io.alwayszihan.zeroxpv.ui.icons.Icon
import io.alwayszihan.zeroxpv.ui.icons.Icons
import io.alwayszihan.zeroxpv.ui.theme.AppShapeScale
import io.alwayszihan.zeroxpv.utils.permission.PermissionUtils

/**
 * First-run onboarding screen. Only asks for Files & Media access (video/audio files) — no
 * microphone or camera permission is requested. Shows once until the user hits Next/Skip.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(
  onDone: () -> Unit,
) {
  val permissionState = rememberPermissionState(PermissionUtils.getStoragePermission())

  LaunchedEffect(Unit) {
    if (permissionState.status != PermissionStatus.Granted) {
      permissionState.launchPermissionRequest()
    }
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 28.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(modifier = Modifier.height(32.dp))

      Surface(
        modifier = Modifier.size(88.dp),
        shape = AppShapeScale.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Filled.VideoLibrary,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = stringResource(R.string.onboarding_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = stringResource(R.string.onboarding_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(36.dp))

      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.VideoFile,
              contentDescription = null,
              modifier = Modifier.size(28.dp),
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
              text = stringResource(R.string.onboarding_files_media_title),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          Text(
            text = stringResource(R.string.onboarding_files_media_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (permissionState.status is PermissionStatus.Denied) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = stringResource(R.string.onboarding_permission_denied),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          textAlign = TextAlign.Center,
        )
      }

      Spacer(modifier = Modifier.weight(1f, fill = false))

      Spacer(modifier = Modifier.height(32.dp))

      Button(
        onClick = onDone,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = AppShapeScale.large,
      ) {
        Text(
          text = stringResource(R.string.onboarding_next),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      TextButton(onClick = onDone) {
        Text(
          text = stringResource(R.string.onboarding_skip),
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
        )
      }
    }
  }
}