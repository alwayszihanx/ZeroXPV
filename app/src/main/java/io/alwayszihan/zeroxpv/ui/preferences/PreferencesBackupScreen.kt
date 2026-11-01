package io.alwayszihan.zeroxpv.ui.preferences

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import io.alwayszihan.zeroxpv.presentation.Screen
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
object PreferencesBackupScreen : Screen {
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

        val exportLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    val map = LinkedHashMap<String, JsonElement>()
                    for ((k, raw) in prefs.all) {
                        val element: JsonElement = when (raw) {
                            is String -> JsonPrimitive(raw)
                            is Boolean -> JsonPrimitive(raw)
                            is Int -> JsonPrimitive(raw)
                            is Long -> JsonPrimitive(raw)
                            is Float -> JsonPrimitive(raw)
                            is Set<*> -> JsonPrimitive(raw.joinToString("\u0000") { it?.toString() ?: "" })
                            else -> JsonPrimitive(raw?.toString() ?: "")
                        }
                        map[k] = element
                    }
                    val json = JsonObject(map)
                    out.bufferedWriter().use { it.write(Json.encodeToString(json)) }
                }
            }.onSuccess {
                Toast.makeText(context, "Preferences exported", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Export failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        val importLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.use { inp ->
                    inp.bufferedReader().readText()
                } ?: return@runCatching
                val obj = Json.parseToJsonElement(text).jsonObject
                val editor = prefs.edit()
                obj.forEach { (k, v) ->
                    val p = v.jsonPrimitive
                    when {
                        p.isString -> {
                            val s = p.content
                            if (s.contains("\u0000")) {
                                editor.putStringSet(k, s.split("\u0000").toSet())
                            } else {
                                editor.putString(k, s)
                            }
                        }
                        else -> {
                            val s = p.content
                            when {
                                s == "true" || s == "false" -> editor.putBoolean(k, s.toBoolean())
                                s.toLongOrNull() != null -> editor.putLong(k, s.toLong())
                                s.toIntOrNull() != null -> editor.putInt(k, s.toInt())
                                s.toFloatOrNull() != null -> editor.putFloat(k, s.toFloat())
                                else -> editor.putString(k, s)
                            }
                        }
                    }
                }
                editor.commit()
            }.onSuccess {
                Toast.makeText(context, "Preferences imported - restart app to apply", Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(context, "Import failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Backup & Restore",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Export all app preferences to a JSON file, or import a previously exported backup. Note: a restart may be required for some changes to take full effect.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Export preferences", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Save current settings to a file",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(onClick = {
                        exportLauncher.launch("zeroxpv_preferences_${System.currentTimeMillis()}.json")
                    }) {
                        Text("Export")
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Import preferences", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Restore settings from a backup file",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(onClick = {
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    }) {
                        Text("Import")
                    }
                }
            }
        }
    }
}
