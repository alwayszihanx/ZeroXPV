package io.alwayszihan.zeroxpv.preferences

import io.alwayszihan.zeroxpv.preferences.preference.Preference
import io.alwayszihan.zeroxpv.preferences.preference.PreferenceStore

/**
 * Tracks whether the first-run onboarding screen has been completed.
 */
class OnboardingPreferences(
  preferenceStore: PreferenceStore,
) {
  val completed: Preference<Boolean> =
    preferenceStore.getBoolean("onboarding_completed", false)
}