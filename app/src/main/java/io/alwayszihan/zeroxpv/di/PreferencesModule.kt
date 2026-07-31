package io.alwayszihan.zeroxpv.di

import io.alwayszihan.zeroxpv.database.ZeroXPVDatabase
import io.alwayszihan.zeroxpv.preferences.AdvancedPreferences
import io.alwayszihan.zeroxpv.preferences.AppearancePreferences
import io.alwayszihan.zeroxpv.preferences.AudioPreferences
import io.alwayszihan.zeroxpv.preferences.BrowserPreferences
import io.alwayszihan.zeroxpv.preferences.DecoderPreferences
import io.alwayszihan.zeroxpv.preferences.FoldersPreferences
import io.alwayszihan.zeroxpv.preferences.GesturePreferences
import io.alwayszihan.zeroxpv.preferences.PlayerPreferences
import io.alwayszihan.zeroxpv.preferences.SettingsManager
import io.alwayszihan.zeroxpv.preferences.SubtitlesPreferences
import io.alwayszihan.zeroxpv.preferences.YtdlPreferences
import io.alwayszihan.zeroxpv.preferences.preference.AndroidPreferenceStore
import io.alwayszihan.zeroxpv.preferences.preference.PreferenceStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val PreferencesModule =
  module {
    single { AndroidPreferenceStore(androidContext()) }.bind(PreferenceStore::class)

    single { AppearancePreferences(get()) }
    singleOf(::PlayerPreferences)
    singleOf(::GesturePreferences)
    singleOf(::DecoderPreferences)
    singleOf(::SubtitlesPreferences)
    singleOf(::AudioPreferences)
    singleOf(::AdvancedPreferences)
    single { BrowserPreferences(get(), androidContext()) }
    singleOf(::FoldersPreferences)
    singleOf(::YtdlPreferences)
    singleOf(::SettingsManager)
  }

