package io.alwayszihan.zeroxpv.ui.preferences

import io.alwayszihan.zeroxpv.ui.icons.Icon
import io.alwayszihan.zeroxpv.ui.icons.Icons

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import io.alwayszihan.zeroxpv.BuildConfig
import io.alwayszihan.zeroxpv.R
import io.alwayszihan.zeroxpv.presentation.Screen
import io.alwayszihan.zeroxpv.presentation.crash.CrashActivity.Companion.collectDeviceInfo
import io.alwayszihan.zeroxpv.ui.utils.LocalBackStack
import io.alwayszihan.zeroxpv.ui.utils.LocalShowSettingsBackArrow
import io.alwayszihan.zeroxpv.ui.utils.popSafely
import io.alwayszihan.zeroxpv.utils.clipboard.SafeClipboard
import io.alwayszihan.zeroxpv.utils.update.UpdateViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

@Serializable
object AboutScreen : Screen {
  @Suppress("DEPRECATION")
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val packageManager: PackageManager = context.packageManager
    val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName?.substringBefore('-') ?: packageInfo.versionName ?: BuildConfig.VERSION_NAME
    val buildType = BuildConfig.BUILD_TYPE
    val githubRepoUrl = stringResource(R.string.github_repo_url)

    val updateViewModel: UpdateViewModel? = if (BuildConfig.ENABLE_UPDATE_FEATURE) {
      viewModel(context as androidx.activity.ComponentActivity)
    } else {
      null
    }
    val updateState by (updateViewModel?.updateState ?: MutableStateFlow(UpdateViewModel.UpdateState.Idle)).collectAsState()

    LaunchedEffect(updateState) {
        if (BuildConfig.ENABLE_UPDATE_FEATURE && updateViewModel != null && updateState is UpdateViewModel.UpdateState.NoUpdate) {
            Toast.makeText(context, "Already using latest version", Toast.LENGTH_SHORT).show()
            updateViewModel.dismissNoUpdate()
        }
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = stringResource(id = R.string.pref_about_title),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
            )
          },
          navigationIcon = {
            if (LocalShowSettingsBackArrow.current) {
              IconButton(onClick = { backstack.popSafely() }) {
                Icon(
                  imageVector = Icons.Default.ArrowBack,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                )
              }
            }
          },
        )
      },
    ) { paddingValues ->
      val cs = MaterialTheme.colorScheme
      val colorPrimary = cs.primaryContainer
      val colorTertiary = cs.tertiaryContainer
      val transition = rememberInfiniteTransition()
      val fraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 5000),
            repeatMode = RepeatMode.Reverse,
          ),
      )
      val cornerRadius = 28.dp

      Column(
        modifier =
          Modifier
            .padding(paddingValues)
            .verticalScroll(rememberScrollState()),
      ) {
        // App Header Card
        PreferenceCard {
          Box(
            modifier =
              Modifier
                .drawWithCache {
                  val cx = size.width - size.width * fraction
                  val cy = size.height * fraction

                  val gradient =
                    Brush.radialGradient(
                      colors = listOf(colorPrimary, colorTertiary),
                      center = Offset(cx, cy),
                      radius = 800f,
                    )

                  onDrawBehind {
                    drawRoundRect(
                      brush = gradient,
                      cornerRadius =
                        CornerRadius(
                          cornerRadius.toPx(),
                          cornerRadius.toPx(),
                        ),
                    )
                  }
                }
                .padding(16.dp),
          ) {
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(64.dp)) {
                  AndroidView(
                    modifier = Modifier.matchParentSize(),
                    factory = { ctx ->
                      ImageView(ctx).apply {
                        setImageResource(R.mipmap.ic_launcher)
                      }
                    },
                  )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "ZeroXPV",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.onPrimaryContainer,
                  )
                  Spacer(Modifier.height(4.dp))
                  Text(
                    text = "v$versionName $buildType",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onPrimaryContainer.copy(alpha = 0.85f),
                  )
                  Spacer(Modifier.height(8.dp))
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cs.primary.copy(alpha = 0.16f),
                  ) {
                    Text(
                      text = "Apache-2.0",
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.SemiBold,
                      color = cs.onPrimaryContainer,
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              Text(
                text = "Feature-rich, efficient, powerful Android video player based on libmpv. No ads. No trackers. No noise. Just a serious video player with a calmer surface and a sharper edge.",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onPrimaryContainer.copy(alpha = 0.85f),
              )
            }
          }
        }

        Spacer(Modifier.height(8.dp))

        // About ZeroXPV
        PreferenceSectionHeader(title = "About ZeroXPV")
        PreferenceCard {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "ZeroXPV is a feature-rich, efficient, and powerful Android video player based on libmpv. It pushes the mpv-android experience further with deep customization, thermal-aware performance, and unique quality-of-life features.",
              style = MaterialTheme.typography.bodyMedium,
              color = cs.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
              text = "The project is open source under the Apache License 2.0. Contributions, bug reports, and feature requests are welcome on GitHub.",
              style = MaterialTheme.typography.bodyMedium,
              color = cs.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = cs.secondaryContainer,
                modifier = Modifier.weight(1f).clickable {
                  context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/alwayszihanx/ZeroXPV/issues".toUri()))
                },
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center,
                ) {
                  Icon(Icons.Outlined.BugReport, null, modifier = Modifier.size(16.dp), tint = cs.onSecondaryContainer)
                  Spacer(Modifier.width(6.dp))
                  Text("Issues", style = MaterialTheme.typography.labelLarge, color = cs.onSecondaryContainer)
                }
              }
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = cs.secondaryContainer,
                modifier = Modifier.weight(1f).clickable {
                  context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/alwayszihanx/ZeroXPV".toUri()))
                },
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center,
                ) {
                  Text("Star", style = MaterialTheme.typography.labelLarge, color = cs.onSecondaryContainer)
                }
              }

            }
          }
        }

        Spacer(Modifier.height(8.dp))

        // Developer Section
        PreferenceSectionHeader(title = "Developer")
        PreferenceCard {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(56.dp),
                contentAlignment = Alignment.Center,
              ) {
                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = cs.primaryContainer,
                  modifier = Modifier.size(56.dp),
                ) {
                  Image(
                    painter = painterResource(id = R.drawable.developer),
                    contentDescription = "Developer",
                    modifier = Modifier.size(56.dp),
                  )
                }
              }
              Spacer(Modifier.width(16.dp))
              Column {
                Text("Latiful Hassan Zihan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.onSurface)
                Spacer(Modifier.height(2.dp))
                Text("Flutter Developer - Automation Enthusiast - Open Source Contributor", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
              }
            }

            Spacer(Modifier.height(12.dp))
            Text(
              text = "Building privacy-focused applications, automation systems, and modern cross-platform software. Independent developer from Bangladesh.",
              style = MaterialTheme.typography.bodyMedium,
              color = cs.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            // Social links
            val links = listOf(
              "GitHub" to "https://github.com/alwayszihanx",
              "Telegram" to "https://t.me/latifulhassanzihan",
              "Email" to "mailto:alwayszihan@proton.me",
              "Facebook" to "https://facebook.com/alwayszihan",
              "Instagram" to "https://instagram.com/alwayszihan",
            )
            links.forEachIndexed { index, (name, url) ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
                  .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(name, style = MaterialTheme.typography.bodyMedium, color = cs.primary, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp), tint = cs.outline)
              }
              if (index < links.lastIndex) {
                HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.3f))
              }
            }

            Spacer(Modifier.height(12.dp))

            // Focus tags
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("Privacy First", "Open Source", "Automation").forEach { tag ->
                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = cs.tertiaryContainer,
                ) {
                  Text(
                    text = tag,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onTertiaryContainer,
                  )
                }
              }
            }
          }
        }

        Spacer(Modifier.height(8.dp))

        // Libraries & GitHub
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          val btnContainer = cs.primary
          val btnContent = cs.onPrimary
          Button(
            onClick = { backstack.add(LibrariesScreen) },
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = btnContainer, contentColor = btnContent),
          ) {
            Text(
              text = stringResource(id = R.string.pref_about_oss_libraries),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
            )
          }

          Button(
            onClick = {
              context.startActivity(Intent(Intent.ACTION_VIEW, githubRepoUrl.toUri()))
            },
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = btnContainer, contentColor = btnContent),
          ) {
            Text("GitHub", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          }
        }

        Spacer(Modifier.height(16.dp))

        // Device Info
        PreferenceCard {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                SafeClipboard.copyPlainText(context, "zeroxpv_device_info", collectDeviceInfo())
              }
              .padding(16.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
              Icon(imageVector = Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(20.dp), tint = cs.onSurfaceVariant)
              Spacer(Modifier.width(8.dp))
              Text("Device Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
            }
            Text(text = collectDeviceInfo(), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
          }
        }

        Spacer(Modifier.height(8.dp))

        // Updates Section
        if (BuildConfig.ENABLE_UPDATE_FEATURE && updateViewModel != null) {
          PreferenceSectionHeader(title = "Updates")
          PreferenceCard {
                val isAutoUpdateEnabled by updateViewModel.isAutoUpdateEnabled.collectAsState()
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { updateViewModel.toggleAutoUpdate(!isAutoUpdateEnabled) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto Check for Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                            Spacer(Modifier.height(2.dp))
                            Text("Check on startup", style = MaterialTheme.typography.bodyMedium, color = cs.outline)
                        }
                        androidx.compose.material3.Switch(checked = isAutoUpdateEnabled, onCheckedChange = { updateViewModel.toggleAutoUpdate(it) })
                    }

                    PreferenceDivider()

                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = { updateViewModel.checkForUpdate(manual = true) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = cs.secondaryContainer, contentColor = cs.onSecondaryContainer),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                             Icon(Icons.Default.Update, null, modifier = Modifier.size(18.dp))
                             Spacer(Modifier.width(8.dp))
                             Text("Check for Updates Now", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
          }

          Spacer(Modifier.height(8.dp))
        }

        // System Stats
        PreferenceSectionHeader(title = "System")

        val systemStats = remember { collectSystemStats(context) }
        PreferenceCard {
          systemStats.forEachIndexed { index, (label, value) ->
            SystemStatRow(label = label, value = value)
            if (index < systemStats.lastIndex) PreferenceDivider()
          }
        }

        Spacer(Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun SystemStatRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f).padding(end = 8.dp),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1.4f),
    )
  }
}

private fun collectSystemStats(context: Context): List<Pair<String, String>> {
  val pm = context.packageManager
  val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

  // RAM
  val memInfo = ActivityManager.MemoryInfo()
  am.getMemoryInfo(memInfo)
  val totalRamMb = memInfo.totalMem / (1024 * 1024)
  val ramStr = if (totalRamMb >= 1024) "${"%.1f".format(totalRamMb / 1024f)} GB" else "$totalRamMb MB"

  // GLES version
  val configInfo = am.deviceConfigurationInfo
  val glesVersion = if (configInfo.reqGlEsVersion != android.content.pm.ConfigurationInfo.GL_ES_VERSION_UNDEFINED) {
    configInfo.glEsVersion
  } else "Unknown"

  // Vulkan
  val vulkanStr = when {
    pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL, 1) -> {
      // Try to get Vulkan version from system features
      val features = pm.systemAvailableFeatures
      val vulkanVersionFeature = features.firstOrNull {
        it.name?.startsWith("android.hardware.vulkan.version") == true
      }
      if (vulkanVersionFeature != null && Build.VERSION.SDK_INT >= 26) {
        val ver = vulkanVersionFeature.version
        val major = (ver shr 22) and 0x3FF
        val minor = (ver shr 12) and 0x3FF
        "Vulkan $major.$minor (Level 1)"
      } else "Vulkan 1.1+ (Level 1)"
    }
    pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL, 0) -> "Vulkan 1.0 (Level 0)"
    pm.hasSystemFeature("android.hardware.vulkan.compute") -> "Vulkan (compute)"
    else -> "Not supported"
  }

  // CPU ABIs
  val abis = Build.SUPPORTED_ABIS.take(2).joinToString(", ")

  // CPU cores
  val cores = Runtime.getRuntime().availableProcessors()

  return listOf(
    "Manufacturer"  to Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
    "Device"        to "${Build.MODEL} (${Build.DEVICE})",
    "Android"       to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    "CPU ABI"       to abis,
    "CPU Cores"     to "$cores cores",
    "RAM"           to ramStr,
    "OpenGL ES"     to glesVersion,
    "Vulkan"        to vulkanStr,
    "GPU Renderer"  to (Build.HARDWARE.ifBlank { "Unknown" }),
    "Board"         to Build.BOARD,
    "Kernel"        to System.getProperty("os.version", "Unknown"),
  )
}

@Suppress("DEPRECATION")
@Serializable
object LibrariesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    Scaffold(
      topBar = {
        TopAppBar(
          title = { 
            Text(
              text = stringResource(id = R.string.pref_about_oss_libraries),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
            ) 
          },
          navigationIcon = {
            IconButton(onClick = { backstack.popSafely() }) {
              Icon(
                imageVector = Icons.Default.ArrowBack, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          },
        )
      },
    ) { paddingValues ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "Core open source dependencies used by ZeroXPV.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OPEN_SOURCE_LIBRARIES.forEach { library ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                context.startActivity(
                  Intent(Intent.ACTION_VIEW, library.url.toUri()),
                )
              },
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ),
            shape = RoundedCornerShape(18.dp),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = library.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = library.artifact,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = library.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = library.license,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
              )
            }
          }
        }
      }
    }
  }
}

private data class OpenSourceLibrary(
  val name: String,
  val artifact: String,
  val description: String,
  val license: String,
  val url: String,
)

private val OPEN_SOURCE_LIBRARIES = listOf(
  OpenSourceLibrary(
    name = "Jetpack Compose",
    artifact = "androidx.compose",
    description = "Declarative UI toolkit used across the app interface.",
    license = "Apache-2.0",
    url = "https://developer.android.com/jetpack/compose",
  ),
  OpenSourceLibrary(
    name = "Material 3",
    artifact = "androidx.compose.material3:material3",
    description = "Compose Material components and theming.",
    license = "Apache-2.0",
    url = "https://developer.android.com/jetpack/androidx/releases/compose-material3",
  ),
  OpenSourceLibrary(
    name = "Navigation 3",
    artifact = "androidx.navigation3:navigation3-runtime",
    description = "Navigation model used for app screens and flows.",
    license = "Apache-2.0",
    url = "https://developer.android.com/jetpack/androidx/releases/navigation3",
  ),
  OpenSourceLibrary(
    name = "Koin",
    artifact = "io.insert-koin",
    description = "Dependency injection for repositories, preferences, and view models.",
    license = "Apache-2.0",
    url = "https://insert-koin.io/",
  ),
  OpenSourceLibrary(
    name = "Room",
    artifact = "androidx.room",
    description = "Local database layer for playback state and cached metadata.",
    license = "Apache-2.0",
    url = "https://developer.android.com/jetpack/androidx/releases/room",
  ),
  OpenSourceLibrary(
    name = "OkHttp",
    artifact = "com.squareup.okhttp3:okhttp",
    description = "HTTP client for subtitle search and other network requests.",
    license = "Apache-2.0",
    url = "https://square.github.io/okhttp/",
  ),
  OpenSourceLibrary(
    name = "Coil 3",
    artifact = "io.coil-kt.coil3",
    description = "Async image loading for posters, thumbnails, and artwork.",
    license = "Apache-2.0",
    url = "https://coil-kt.github.io/coil/",
  ),
  OpenSourceLibrary(
    name = "kotlinx.serialization",
    artifact = "org.jetbrains.kotlinx:kotlinx-serialization-json",
    description = "JSON serialization for API payloads and internal models.",
    license = "Apache-2.0",
    url = "https://github.com/Kotlin/kotlinx.serialization",
  ),
  OpenSourceLibrary(
    name = "Accompanist Permissions",
    artifact = "com.google.accompanist:accompanist-permissions",
    description = "Compose helpers for Android runtime permission flows.",
    license = "Apache-2.0",
    url = "https://github.com/google/accompanist",
  ),
  OpenSourceLibrary(
    name = "MediaInfo Android",
    artifact = "com.github.marlboro-advance:mediainfoAndroid",
    description = "Media inspection for codecs, subtitles, and technical metadata.",
    license = "Open source",
    url = "https://github.com/marlboro-advance/mediainfoAndroid",
  ),
  OpenSourceLibrary(
    name = "SMBJ",
    artifact = "com.hierynomus:smbj",
    description = "SMB/CIFS client support for network shares.",
    license = "Apache-2.0",
    url = "https://github.com/hierynomus/smbj",
  ),
  OpenSourceLibrary(
    name = "Commons Net",
    artifact = "commons-net:commons-net",
    description = "FTP and related network protocol support.",
    license = "Apache-2.0",
    url = "https://commons.apache.org/proper/commons-net/",
  ),
  OpenSourceLibrary(
    name = "Sardine Android",
    artifact = "com.github.thegrizzlylabs:sardine-android",
    description = "WebDAV client support for remote file access.",
    license = "Apache-2.0",
    url = "https://github.com/thegrizzlylabs/sardine-android",
  ),
  OpenSourceLibrary(
    name = "NanoHTTPD",
    artifact = "org.nanohttpd:nanohttpd",
    description = "Embedded HTTP server components used by local streaming features.",
    license = "BSD-3-Clause",
    url = "https://github.com/NanoHttpd/nanohttpd",
  ),
  OpenSourceLibrary(
    name = "FSAF",
    artifact = "com.github.K1rakishou:Fuck-Storage-Access-Framework",
    description = "Storage Access Framework helpers for file and tree operations.",
    license = "Apache-2.0",
    url = "https://github.com/K1rakishou/Fuck-Storage-Access-Framework",
  ),
  OpenSourceLibrary(
    name = "TrueType Parser",
    artifact = "io.github.yubyf:truetypeparser-light",
    description = "Reads font metadata for subtitle font handling.",
    license = "Apache-2.0",
    url = "https://github.com/yubyf/truetypeparser",
  ),
  OpenSourceLibrary(
    name = "Compose Preference",
    artifact = "me.zhanghai.compose.preference:preference",
    description = "Preference UI components used across settings screens.",
    license = "Apache-2.0",
    url = "https://github.com/zhanghai/ComposePreference",
  ),
  OpenSourceLibrary(
    name = "LazyColumnScrollbar",
    artifact = "com.github.nanihadesuka:LazyColumnScrollbar",
    description = "Scrollbar component for long Compose lists.",
    license = "Apache-2.0",
    url = "https://github.com/Nanihadesuka/LazyColumnScrollbar",
  ),
  OpenSourceLibrary(
    name = "Reorderable",
    artifact = "sh.calvin.reorderable:reorderable",
    description = "Drag-and-drop list reordering for Compose surfaces.",
    license = "Apache-2.0",
    url = "https://github.com/Calvin-LL/Reorderable",
  ),
  OpenSourceLibrary(
    name = "Seeker",
    artifact = "io.github.2307vivek:seeker",
    description = "A customizable seekbar component for Jetpack Compose.",
    license = "Apache-2.0",
    url = "https://github.com/2307vivek/seeker",
  ),
  OpenSourceLibrary(
    name = "Sora Editor",
    artifact = "io.github.Rosemoe.sora-editor",
    description = "A powerful, efficient code editor for Android with TextMate highlighting.",
    license = "LGPL-2.1",
    url = "https://github.com/Rosemoe/sora-editor",
  ),
  OpenSourceLibrary(
    name = "Media3",
    artifact = "androidx.media3",
    description = "Android media libraries for advanced playback and effects.",
    license = "Apache-2.0",
    url = "https://developer.android.com/jetpack/androidx/releases/media3",
  ),
  OpenSourceLibrary(
    name = "Jsoup",
    artifact = "org.jsoup:jsoup",
    description = "Java HTML Parser for web scraping and document extraction.",
    license = "MIT",
    url = "https://jsoup.org/",
  ),
  OpenSourceLibrary(
    name = "Llamatik",
    artifact = "com.llamatik:library",
    description = "Local LLM inference bridge for running AI models on Android.",
    license = "Apache-2.0",
    url = "https://github.com/llamatik/library",
  ),
)
