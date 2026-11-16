<p align="center"><img src="docs/images/app-icon.png" alt="Li Widget" width="80"></p>
<h1 align="center">Li Widget</h1>
<p align="center">Your Li Auto, on your Android home screen.</p>
<p align="center"><a href="README.md">Русский</a> · <b>English</b></p>

**An unofficial companion for the original Li Auto widget.** Customize its appearance and translate known labels while keeping the original controls and their handlers. The original Li Auto app must be installed separately.

## Download 1.0

**[Download APK](https://github.com/alexbel123/li-widget/releases/latest/download/Li-Widget-1.0.apk)** · **[Download ZIP](https://github.com/alexbel123/li-widget/releases/latest/download/Li-Widget-1.0.zip)** · [All release files](https://github.com/alexbel123/li-widget/releases/latest)

The ZIP contains the same APK, installation notes and its checksum. Browse the source in this repository or download **Source code** from the release page. Use `SHA256SUMS.txt` to verify downloads.

<table>
  <tr><th>Light theme</th><th>Dark theme</th></tr>
  <tr><td><img src="docs/images/preview-light-en.png" width="360" alt="Li Widget light preview"></td><td><img src="docs/images/preview-dark-en.png" width="360" alt="Li Widget dark preview"></td></tr>
</table>

*These are Android widget-picker previews using the project's own artwork. Dashes indicate no data. A connected widget displays data and the original icons from the installed Li Auto app; their states and availability depend on the vehicle.*

## Features

- Russian and English for the companion app, widgets and update notification.
- Light, dark, pure black and system themes. Black stays `#000000`.
- Android system neutral colors or a neutral gray palette.
- Per-widget styling, compact and detailed layouts, text size, optional battery percentage and update time.
- Home-screen resizing down to **250 × 110 dp**; the launcher determines cell occupancy.
- Four actions selected and reordered in the original Li Auto editor.
- **Move** opens the original forward/backward remote-movement interface.
- Stale-data indication without inventing a date for cached timestamps.

## Installation

1. Install the APK from [Releases](https://github.com/alexbel123/li-widget/releases/latest) on Android 12 or newer. If requested, allow this browser or file manager to install apps.
2. Open the original Li Auto app and sign in to your vehicle account.
3. Open Li Widget, choose **Connect Li Auto**, and approve Android's widget-binding request.
4. Allow the ongoing update notification and add Li Widget to your home screen.
5. Choose **Style → Language → English**, then customize the appearance. Use **Buttons** to open the original action editor.

Updates signed with the same key install over the previous version. [Detailed setup and troubleshooting](docs/INSTALL.md).

## Compatibility

Android 12+ is required. Instrumented checks ran on Android 16 / API 36 with Pixel Launcher. The companion is designed around L7 and the L series, using the widget structure from Li Auto **8.28.3 (7020)**, package `com.chehejia.oc.m01`. Actual vehicle commands were not tested with a connected car or owner account.

The original app determines available functions, confirmations and restrictions. New Li Auto versions may break compatibility. Its screens and button editor are not translated. Unknown messages are retained as received. An update timestamp does not prove that the car is online. Cabin temperature is not displayed because the inspected original widget does not supply a measured reading. [Verification and limitations](docs/VERIFICATION.md).

## Privacy and architecture

With your approval, Android lets Li Widget host the original widget through `AppWidgetHost`. It styles the received `RemoteViews` and translates known text while preserving the original `PendingIntent` handlers. A foreground service with an ongoing notification keeps the presentation updated.

Li Widget does not ask for credentials, account tokens or VIN. It has no network client, ads or analytics, and requests no internet, Bluetooth, location or contacts permissions. Preferences and an update timestamp are stored locally. The original app's data practices apply separately. [Privacy](PRIVACY.md).

## Build and contribute

Java and standard Android APIs, without Gradle or third-party runtime libraries. Requirements: Python 3.10+, JDK 21, Android SDK Platform 36, Build Tools 36.1.0.

```sh
python tools/check_release.py
python build.py --sdk /path/to/android-sdk
```

The local build generates its own key in ignored `.local/`. This key differs from the published APK's key. [Build and signing](docs/BUILD.md) · [Contributing](CONTRIBUTING.md) · [Changelog](CHANGELOG.md).

## License and support

Project code, documentation and original artwork are [MIT licensed](LICENSE). Li Auto / 理想汽车 and related marks belong to their respective owners. This independent project is not affiliated with or endorsed by Li Auto. It does not distribute the original APK, its code or extracted artwork. MIT does not apply to the original app. [Third-party notices](THIRD_PARTY_NOTICES.md).

Report bugs and suggestions in [Issues](https://github.com/alexbel123/li-widget/issues). Remove account information, VIN and personal data from reports and screenshots. Report vulnerabilities [privately](https://github.com/alexbel123/li-widget/security/advisories/new); see [SECURITY.md](SECURITY.md).
