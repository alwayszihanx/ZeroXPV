package io.alwayszihan.zeroxpv.ui.utils

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import io.alwayszihan.zeroxpv.presentation.Screen

val LocalBackStack: ProvidableCompositionLocal<NavBackStack<Screen>> =
  compositionLocalOf { error("LocalBackStack not initialized!") }

val LocalShowSettingsBackArrow: ProvidableCompositionLocal<Boolean> =
  compositionLocalOf { true }

