<p align="center">
  <img src="docs/logo.png" width="90" alt="Basic Home Logo">
</p>

# Basic Home

> **No bullshit, simply a launcher.**

[![Release](https://img.shields.io/badge/release-v2.0.0--Zwei-blue.svg)](https://github.com/akshraj2022/basichome/releases)
[![Platform](https://img.shields.io/badge/platform-Android%20TV-green.svg)](https://developer.android.com/tv)
[![License](https://img.shields.io/badge/license-MIT-purple.svg)](LICENSE)

---

## Visual Showcase

| Home View (Sleek Top Bar & Ambient Scrim) | Focused Row Navigation |
|---|---|
| ![Home](docs/screenshots/home_topbar.png) | ![Top Bar Focus](docs/screenshots/topbar_navigation.png) |

| Search Navigation | Rows Gliding Under Ambient Scrim |
|---|---|
| ![Search](docs/screenshots/search_focused.png) | ![Scrolled Rows](docs/screenshots/scrolled_rows.png) |

---

## Key Features

- **Simple & Elegant**: Something you actually use daily.
- **Elegant & Visible**: Let's be real, who actually uses Google recommendations? So we just gave you apps and big ones cuz we like 'em big & visible.
- **Simply Sorted**: Most recent apps at the top, and everything else cleanly divided into 6 categories (Entertainment, Media & Music, Games, Tools & VPN, System, and Inputs).
- **Simply Liquid**: Like liquid, it just adjusts. It switches automatically between light and dark mode based on actual local sunset and sunrise times.
- **Simply Optimized**: Our standards are low, like your parents to you (kidding). We tested this on a 7-year-old TV chip and 1.5 GB RAM, so even if you have your oldest Android TV, it can take it. And if you have a Sony flagship, that's great. Plus it's only 3.2 MB, so I mean if you don't have that much storage left, you need help. Like, serious help.
- **Simply Easy**: It's easy to use and install. Here are the directions:

---

## Directions to Install

### 1. Download
Grab the latest release APK from [Releases](https://github.com/akshraj2022/basichome/releases):
- **[BasicHome-v2.0.0.apk](https://github.com/akshraj2022/basichome/releases/download/v2.0.0/BasicHome-v2.0.0.apk)**

### 2. Connect via ADB
Enable **Developer Options** & **Network Debugging** on your Android TV:
```bash
adb connect <TV_IP_ADDRESS>:5555
```

### 3. Install
```bash
adb install -r BasicHome-v2.0.0.apk
```

### 4. Launch
```bash
adb shell am start -n com.minimal.tvlauncher/.MainActivity
```

### 5. (Optional) Set as Default Launcher
```bash
adb shell cmd package set-home-activity com.minimal.tvlauncher/.MainActivity
```

### 6. (Optional) Disable Stock Google TV Bloat
```bash
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
```

---

## Simply Licensed

Oh yeah, you can't just copy this, bro — my bad. Check the [LICENSE](LICENSE) file for more details.
