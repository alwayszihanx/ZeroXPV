package app.gyrolet.mpvrx.ui.preferences

import app.gyrolet.mpvrx.ui.icons.Icon
import app.gyrolet.mpvrx.ui.icons.Icons

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.gyrolet.mpvrx.preferences.BrowserPreferences
import app.gyrolet.mpvrx.preferences.preference.collectAsState
import app.gyrolet.mpvrx.presentation.Screen
import app.gyrolet.mpvrx.ui.preferences.components.SwitchPreference
import app.gyrolet.mpvrx.ui.utils.LocalBackStack
import app.gyrolet.mpvrx.ui.utils.popSafely
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.PreferenceCard
import me.zhanghai.compose.preference.PreferenceSectionHeader
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.koin.compose.koinInject

@Serializable
object DeletionPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val browserPreferences = koinInject<BrowserPreferences>()

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = "Deletion",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
            )
          },
          navigationIcon = {
            IconButton(onClick = { backstack.popSafely() }) {
              Icon(
                Icons.Default.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
            }
          },
        )
      },
    ) { padding ->
      ProvidePreferenceLocals {
        LazyColumn(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(padding),
        ) {
          item {
            PreferenceSectionHeader(title = "Folder Deletion")
          }

          item {
            PreferenceCard {
              val deleteFolderAllContents by browserPreferences.deleteFolderAllContents.collectAsState()
              SwitchPreference(
                value = deleteFolderAllContents,
                onValueChange = { browserPreferences.deleteFolderAllContents.set(it) },
                title = { Text("Delete folder + all contents") },
                summary = {
                  Text(
                    if (deleteFolderAllContents) {
                      "When disabled, only media files (audio/video) are deleted from the folder"
                    } else {
                      "When enabled, deletes the entire folder with all files (images, logs, etc.)"
                    },
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )
            }
          }

          item {
            PreferenceSectionHeader(title = "How it works")
          }

          item {
            PreferenceCard {
              Text(
                text = "By default, deleting a folder only removes audio and video files while leaving other files (images, logs, documents) untouched. Enable \"Delete folder + all contents\" to remove the entire folder and everything inside it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        }
      }
    }
  }
}
