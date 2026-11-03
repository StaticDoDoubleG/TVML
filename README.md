# TVML — Moonlight for Android TV (auto-connect build)

A fork of [Moonlight Android](https://github.com/moonlight-stream/moonlight-android) v12.2 tuned for a dedicated Android TV streaming box. Main target: **Xiaomi Mi TV Stick 4K** (Android TV 11). Google TV devices should also work.

## Changes from upstream

- **Auto-connect on start.** The app can be set as the TV's launcher (HOME). When it starts (boot, the Home button, or the TV waking from standby), it tries to connect to the chosen host and sends Wake-on-LAN while the host is offline.
  - If a game is already running on the host, it resumes that game.
  - Otherwise, if an auto-start app is set, it starts that app.
  - Otherwise, it opens the host's app list.
- **Falls back to the PC list.** On timeout (30 s), an unpaired host, or the Back button, the PC list opens.
- **Manually added PCs only.** mDNS auto-discovery is disabled. Only PCs added by IP address appear.
- **Windows key goes to the PC.** An optional accessibility service sends the keyboard's Windows (Meta) key, and any key pressed while Meta is held (e.g. Win+D), to the PC while streaming, instead of Android handling it as Home. A Home key coming from a full (alphabetic) keyboard is treated as the Windows key. The TV remote's Home button is not affected.
- Bitrate and all other stream settings are unchanged from upstream.

## Setup

1. Add your PC by IP address (**+** button) and pair it.
2. Long-press the PC and choose **Auto-connect to this PC on startup**.
3. Optional: in the app list, long-press an app and choose **Start this app on auto-connect**.

### Make TVML the launcher (adb required)

Neither Android TV nor Google TV lets you change the default launcher in Settings, so disable the stock launcher once with adb.

Mi TV Stick 4K / Android TV:
```bash
adb shell pm disable-user --user 0 com.google.android.tvlauncher
```
Google TV:
```bash
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
```
To restore the stock launcher, run `adb shell pm enable <package>`.

### Turn on Windows key capture

Turn it on in **Settings → Device Preferences → Accessibility**, or with adb:
```bash
adb shell settings put secure enabled_accessibility_services com.limelight/com.limelight.binding.input.MetaKeyInterceptService
adb shell settings put secure accessibility_enabled 1
```
You can switch it off in Moonlight under **Settings → Input → Capture Windows/Meta key**.

---

# TVML — 안드로이드 TV용 Moonlight (자동 연결 빌드)

[Moonlight Android](https://github.com/moonlight-stream/moonlight-android) v12.2를 스트리밍 전용 TV 기기에 맞게 수정한 포크입니다. 주 대상 기기는 **샤오미 Mi TV Stick 4K**(Android TV 11)이고, Google TV 기기도 동작할 것으로 봅니다.

## 원본과 달라진 점

- **시작하면 자동 연결.** 이 앱을 TV의 런처(홈)로 지정할 수 있습니다. 부팅할 때, 홈 버튼을 누를 때, 대기모드에서 깨어날 때 지정한 PC에 연결을 시도하고, PC가 꺼져 있으면 WoL로 깨웁니다.
  - PC에서 이미 실행 중인 게임이 있으면 그 게임을 이어서 엽니다.
  - 자동 실행 앱을 지정해 뒀으면 그 앱을 시작합니다.
  - 둘 다 아니면 앱 목록을 엽니다.
- **실패하면 PC 목록으로.** 30초 안에 연결되지 않거나, 페어링이 안 돼 있거나, 뒤로 버튼을 누르면 PC 목록으로 갑니다.
- **직접 추가한 PC만 표시.** mDNS 자동 검색을 껐습니다. IP로 직접 추가한 PC만 목록에 나옵니다.
- **윈도우 키를 PC로 전달.** 선택해서 켜는 접근성 서비스가 스트리밍 중 키보드의 윈도우(Meta) 키와 윈도우 키 조합(Win+D 등)을 PC로 보냅니다. 안드로이드가 홈 키로 처리하지 않습니다. 글자 키가 있는 키보드에서 오는 홈 키도 윈도우 키로 처리합니다. 리모컨 홈 버튼은 영향을 받지 않습니다.
- 비트레이트를 비롯한 스트리밍 설정은 원본과 같습니다.

## 설정

1. **+** 버튼으로 PC를 IP로 추가하고 페어링합니다.
2. PC를 길게 눌러 **시작 시 이 PC에 자동 연결**을 선택합니다.
3. (선택) 앱 목록에서 앱을 길게 눌러 **자동 연결 시 이 앱 실행**을 선택합니다.

### 런처로 지정하기 (adb 필요)

Android TV와 Google TV 모두 설정 화면에서 기본 런처를 바꿀 수 없으므로, adb로 기본 런처를 한 번 꺼야 합니다. 명령어는 위 영문 섹션과 같습니다. 원래 런처로 되돌리려면 `adb shell pm enable <패키지명>`을 실행합니다.

### 윈도우 키 가로채기 켜기

**설정 → 기기 환경설정 → 접근성**에서 켜거나, 위 영문 섹션의 adb 명령을 실행합니다. Moonlight의 **설정 → 입력 → Windows/Meta 키 가로채기**에서 끌 수 있습니다.

---

# Moonlight Android

[![AppVeyor Build Status](https://ci.appveyor.com/api/projects/status/232a8tadrrn8jv0k/branch/master?svg=true)](https://ci.appveyor.com/project/cgutman/moonlight-android/branch/master)
[![Translation Status](https://hosted.weblate.org/widgets/moonlight/-/moonlight-android/svg-badge.svg)](https://hosted.weblate.org/projects/moonlight/moonlight-android/)

[Moonlight for Android](https://moonlight-stream.org) is an open source client for NVIDIA GameStream and [Sunshine](https://github.com/LizardByte/Sunshine).

Moonlight for Android will allow you to stream your full collection of games from your Windows PC to your Android device,
whether in your own home or over the internet.

Moonlight also has a [PC client](https://github.com/moonlight-stream/moonlight-qt) and [iOS/tvOS client](https://github.com/moonlight-stream/moonlight-ios).

You can follow development on our [Discord server](https://moonlight-stream.org/discord) and help translate Moonlight into your language on [Weblate](https://hosted.weblate.org/projects/moonlight/moonlight-android/).

## Downloads
* [Google Play Store](https://play.google.com/store/apps/details?id=com.limelight)
* [Amazon App Store](https://www.amazon.com/gp/product/B00JK4MFN2)
* [F-Droid](https://f-droid.org/packages/com.limelight)
* [APK](https://github.com/moonlight-stream/moonlight-android/releases)

## Building
* Install Android Studio and the Android NDK
* Run ‘git submodule update --init --recursive’ from within moonlight-android/
* In moonlight-android/, create a file called ‘local.properties’. Add an ‘ndk.dir=’ property to the local.properties file and set it equal to your NDK directory.
* Build the APK using Android Studio or gradle

## Authors

* [Cameron Gutman](https://github.com/cgutman)  
* [Diego Waxemberg](https://github.com/dwaxemberg)  
* [Aaron Neyer](https://github.com/Aaronneyer)  
* [Andrew Hennessy](https://github.com/yetanothername)

Moonlight is the work of students at [Case Western](http://case.edu) and was
started as a project at [MHacks](http://mhacks.org).
