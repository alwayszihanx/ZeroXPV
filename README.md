<p align="center">
  <img src="fastlane\metadata\android\en-US\images\icon.png" width="250" height="250" />
</p>

<h1 align="center">MpvRx</h1>

<p align="center">
  <b>Feature-rich, Efficient Powerful Android video player based on libmpv.</b>
  <br>
  <i>No ads. No trackers. No noise. Just a serious video player with a calmer surface and a sharper edge.</i>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen.svg" />
  <img src="https://img.shields.io/badge/License-Apache--2.0-blue.svg" />
  <img src="https://img.shields.io/github/v/release/Riteshp2001/mpvRx.svg?logo=github&label=Release&cacheSeconds=3600" />
  <img src="https://img.shields.io/github/downloads/Riteshp2001/mpvRx/total?logo=github&cacheSeconds=3600" />
</p>

---

## Showcase

<div align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/player.png" width="92%">
</div>

<br>

<div align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/videoscreen.png" width="31%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/pip.png" width="31%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/about.jpg" width="31%">
</div>

<br>

<div align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/playlistwindow.png" width="48%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/chapters.png" width="48%">
</div>

---

## Features

MpvRx pushes the mpv-android experience further with deep customization, thermal-aware performance, and unique quality-of-life features. Here's what sets it apart:

<details open>
<summary><b>🎨 Theme & Visual System</b></summary>

| Feature | Description |
|---|---|
| **25+ Color Themes** | Default, Dynamic (Material You), Catppuccin, Nord, Tokyo Night, Rose Pine, Gruvbox, Dracula, and many more |
| **AMOLED Pure Black Mode** | Every theme has a dedicated variant with pure black backgrounds |
| **Player Controls Animation** | 5 animation styles: Default, Elastic Bounce, Cinematic Scale, Slide Up, Minimal Fade |
| **Always Dark Mode** | Option to keep player controls in dark theme regardless of app theme |
| **Themed Player Controls** | Adaptive controls that match your app theme or system accent |

</details>

<details open>
<summary><b>🖐️ Gesture System</b></summary>

| Feature | Description |
|---|---|
| **Refined Tap Logic** | Configurable double-tap seek zones (left/center/right) with independently assignable actions |
| **Multi-Tap Continuous Seeking** | Triple/quadruple tap to keep seeking further without lifting |
| **Horizontal Swipe to Seek** | Swipe across video to seek with live time/delta overlay |
| **Long-Press Dynamic Speed** | Long-press activates configurable speed boost; swipe left/right to adjust across 8 presets |
| **Subtitle Drag Gesture** | Long-press center screen to drag subtitles vertically when active |
| **Pinch-to-Zoom with Pan** | Pinch to zoom (-1x to 3x) with simultaneous pan and single-finger pan after zoom |
| **Volume Boost via Gesture** | Vertical swipe volume can exceed 100% into configurable boost range |
| **Swap Volume/Brightness Sides** | Option to swap which screen side controls volume vs brightness |

</details>

<details open>
<summary><b>📺 HDR & Video Pipeline</b></summary>

| Feature | Description |
|---|---|
| **Shader-Based HDR Pipeline** | Powered by [hdr-toys](https://github.com/natural-harmonia-gropius/hdr-toys) — 77 bundled GLSL shaders |
| **Four HDR Modes** | BT.2100 PQ (HDR10), BT.2100 HLG, BT.2020 gamut mapping, Linear HDR |
| **SDR-to-HDR Boost** | Boost SDR content into HDR range when using Linear HDR pipeline |
| **GPU Deband** | CPU (gradfun) or GPU deband with configurable iterations, threshold, range, grain |
| **Smart Render Backend** | Auto-selects between OpenGL/Vulkan and gpu/gpu-next based on device support |

</details>

<details open>
<summary><b>🔥 Thermal & Battery Management</b></summary>

| Feature | Description |
|---|---|
| **ThermalMonitor** | Samples thermal headroom every 10s during playback |
| **Adaptive Shader Budget** | Ambient shader budget auto-capped based on thermal headroom |
| **Anime4K Proactive Throttling** | Auto-downgrades Anime4K quality when thermal headroom drops below 40% |
| **Background Poll Optimization** | Position poll interval doubles when controls are hidden, cutting JNI wake-ups 50% |
| **Stats Poll Backoff** | Stats page poll loop backs off from 1s to 2s when playback is paused |

</details>

<details open>
<summary><b>🧩 Anime4K & Upscaling</b></summary>

| Feature | Description |
|---|---|
| **7 Preset Quality Tiers** | Off, A, B, C, A+, B+, C+ with clean switching |
| **Quality Tiers in Decoder Settings** | Fast / Balanced / High quality choices |
| **4K/8K Safety Guard** | Auto-disables Anime4K for high-resolution content |
| **Thermal-Guarded Selection** | Auto-downgrades quality tier under thermal pressure before frame drops |

</details>

<details open>
<summary><b>💡 Ambient Mode</b></summary>

| Feature | Description |
|---|---|
| **Two Visual Modes** | GLOW and FRAME_EXTEND — both rendered via custom GLSL at runtime |
| **15+ Configurable Parameters** | Blur samples, glow intensity, saturation, warmth, vignette, dither noise, and more |
| **Shader Recompilation Caching** | Skips recompilation when parameters match last compiled version |

</details>

<details open>
<summary><b>📝 Subtitle System</b></summary>

| Feature | Description |
|---|---|
| **Dual Subtitle Support** | Primary + secondary with auto-offset to prevent overlap |
| **ASS Override Modes** | Smart force/scale handling for secondary subtitles |
| **Comprehensive Styling** | Font, size, bold, italic, border, shadow, colors, justification, scale by window |
| **Three Online Search Modes** | Wyzie, SubtitleHub (6 aggregated sources), and Hybrid (both merged) |
| **TMDB Integration** | Full media search with season/episode browsing for subtitles |

</details>

<details open>
<summary><b>🎮 Player Controls</b></summary>

| Feature | Description |
|---|---|
| **Fully Customizable Layout** | Four configurable zones (top-left/right, bottom-left/right) + portrait bottom row |
| **24+ Button Types** | Mirror, Vertical Flip, A-B Loop, Custom Skip, Background Playback, Ambient, and more |
| **Custom User Buttons** | Create arbitrary buttons executing Lua, JavaScript, or mpv commands |
| **Landscape/Portrait Adaptive Layouts** | Completely different control layouts per orientation |
| **"Slide to Unlock" Controls** | Slide mechanism when controls are locked |
| **Hide Button Backgrounds** | Transparent buttons with only icons visible |
| **Centralized "More Sheet"** | Quick access to all player buttons and custom controls |
| **In-Player Settings** | Toggle 10+ settings (gestures, PiP, UI behavior) without leaving playback |

</details>

<details open>
<summary><b>🧭 Smart Orientation</b></summary>

| Feature | Description |
|---|---|
| **8 Orientation Modes** | Free, Video (auto aspect ratio), Portrait, Reverse Portrait, Sensor Portrait, Landscape, Reverse Landscape, Sensor Landscape |
| **Persistent Per-Video** | Orientation remembered per-video across sessions |

</details>

<details open>
<summary><b>🔍 File Browser & Navigation</b></summary>

| Feature | Description |
|---|---|
| **Dual Browser Modes** | Album View (folder grid) and Tree View (file manager hierarchy) |
| **Folder Pinning** | Pin frequently accessed folders to top |
| **Single-Child Auto-Flatten** | Folders with one subfolder auto-flatten for faster browsing |
| **Auto-Scroll to Last Played** | Opens to the last played video position |
| **Recursive File/Folder Counts** | Shows total video count, duration, size computed recursively |
| **"NEW" Badges** | Configurable threshold for new video indicators |
| **Grid/List Layout** | Per-orientation column count settings |
| **Multi-Protocol Network** | Built-in SMB, FTP, and WebDAV clients |

</details>

<details open>
<summary><b>🤖 AI Integration</b></summary>

| Feature | Description |
|---|---|
| **Provider Support** | Gemini and Groq with configurable API keys and models |
| **AI Subtitle Translation** | Translate subtitles with custom prompts |
| **AI Subtitle Formatting** | Reformat subtitle styling with custom prompts |
| **AI File Renaming** | Bulk rename video files with custom rename prompts |

</details>

<details open>
<summary><b>📜 Scripting & Editor</b></summary>

| Feature | Description |
|---|---|
| **Dual Language** | Lua (.lua) and JavaScript (.js) script support |
| **Sora Code Editor** | Built-in editor with TextMate syntax highlighting |
| **Runtime Script Loading** | Enable/disable scripts without restarting |
| **Config Editor** | Built-in editor for mpv.conf and input.conf |

</details>

<details open>
<summary><b>⚙️ Utilities</b></summary>

| Feature | Description |
|---|---|
| **Stats Page 6** | Live system monitor: FPS, dropped frames, codecs, network sparkline, battery |
| **Video Compressor** | Built-in FFmpeg-based compression with presets |
| **12 Video Filter Presets** | Vivid, Cinematic, Dramatic, Ghibli Style, Neon Pop, Deep Black, and more |
| **Custom Skip Segments** | Intro/outro/recap/credits/preview detection from IntroDB, TIDB, AniSkip |
| **A-B Loop** | In-player looping with visual markers on seekbar |
| **Frame Navigation** | Frame-by-frame forward/backward with frame number display |
| **Sleep Timer** | Built-in with quick presets (15/30/45/60 min) |
| **Adaptive Background Playback** | Auto-PiP on Home, auto-resume after screen unlock |
| **Notification Styles** | None, Media, or Progress with Chapters (Android 16+) |
| **Safe Area / Window Offset** | Prevents camera notch overlap |
| **Display Cutout Mode** | Full-bleed on notch devices |
| **Remember Brightness** | Persists brightness level set during playback |
| **M3U Playlist Support** | Parse and play local M3U playlists |

</details>

---

## Download

<div align="center">
  <a href="https://github.com/Riteshp2001/mpvRx/releases">
    <img src="https://img.shields.io/badge/Download-Stable_Release-blue?style=for-the-badge&logo=github" alt="Stable Release">
  </a>
  <!-- <a href="https://riteshp2001.github.io/mpvRx/">
    <img src="https://img.shields.io/badge/Download-Preview_Build-orange?style=for-the-badge&logo=github" alt="Preview Build">
  </a> -->
</div>

<!-- <div align="center">
  <i>Note: Previews may be unstable and are intended for testing purposes only.</i>
</div> -->

If something breaks, feels off, or deserves another pass, _don't be Dumb and ask for Trash Features which only you require it wll be auto deleted_, 
report it in the [Issues](https://github.com/Riteshp2001/mpvRx/issues).

---

## Build

### Requirements

- JDK 17
- Android SDK with modern build tools installed
- Git

### Debug Build

```powershell
./gradlew.bat :app:assembleStandardDebug
```

### Release Variants

| Variant | Description |
|---|---|
| `standard` | Main release with in-app update support |

### APK Variants

| Variant | Description |
|---|---|
| `universal` | Works on all supported devices |
| `arm64-v8a` | Recommended for most current Android devices |
| `armeabi-v7a` | For older 32-bit ARM devices |
| `x86` | For 32-bit Intel and AMD Android devices |
| `x86_64` | For 64-bit Intel and AMD Android devices |

---

## Support

If you find MpvRx useful and would like to support its development, consider buying me a coffee! Your support keeps the project alive and helps push new features.

<div align="center">

### ☕ Buy Me a Coffee

<a href="https://www.buymeacoffee.com/riteshp2001">
  <img src="https://img.shields.io/badge/Buy_Me_A_Coffee-FFDD00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black" alt="Buy Me a Coffee">
</a>

### UPI

**`panditritesh2001@okhdfcbank`**

<img src="fastlane\metadata\android\en-US\images\upiqr-code.svg" width="250" height="250" alt="UPI QR Code">

Scan with any UPI app (Google Pay, PhonePe, Paytm, BHIM)

</div>

---

## Release Notes For Maintainers

To cut a signed GitHub release through Actions, configure these repository secrets:

| Secret Name | Description |
|---|---|
| `SIGNING_KEYSTORE` | Base64-encoded keystore file (`.jks` or `.keystore`) |
| `SIGNING_KEY_ALIAS` | Key alias inside the keystore |
| `SIGNING_STORE_PASSWORD` | Password for the keystore |
| `KEY_PASSWORD` | Password for the signing key |

Then bump `versionCode` and `versionName` in `app/build.gradle.kts`, create a tag, and push it:

```bash
git tag -a v1.3.1 -m "Release version 1.3.1"
git push origin v1.3.1
```

Preview releases use the same flow with preview tags such as:

```bash
git tag -a v1.3.1-preview.1 -m "Preview release"
git push origin v1.3.1-preview.1
```

---

## Acknowledgments

- [mpv-android](https://github.com/mpv-android)
- [mpvExtended](https://github.com/marlboro-advance/mpvEx)
- [mpvKt](https://github.com/abdallahmehiz/mpvKt)
- [Next Player](https://github.com/anilbeesetti/nextplayer)
- [Gramophone](https://github.com/FoedusProgramme/Gramophone)
- [hdr-toys](https://github.com/natural-harmonia-gropius/hdr-toys)

---

## License

Distributed under the **Apache License 2.0**. See `LICENSE` for more information.

---

## Star History

<a href="https://www.star-history.com/#Riteshp2001/mpvRx&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=Riteshp2001/mpvRx&type=date&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=Riteshp2001/mpvRx&type=date&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=Riteshp2001/mpvRx&type=date&legend=top-left" />
 </picture>
</a>
