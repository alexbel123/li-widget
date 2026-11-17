# Build and signing / Сборка и подпись

## Requirements

- Python 3.10 or newer (standard library only).
- JDK 21, with `java`, `javac` and `keytool` on `PATH` (or `JAVA_HOME` set).
- Android SDK Platform 36 and Build Tools 36.1.0.

Install the SDK packages with Android Studio or:

```sh
sdkmanager "platforms;android-36" "build-tools;36.1.0"
```

Review and accept the Android SDK licenses when prompted. The tools are not bundled in this repository. The build does not need the Li Auto APK or a vehicle account. After installing prerequisites, the build itself does not download dependencies.

## Build

From the repository root:

```sh
python tools/check_release.py
python build.py --sdk /path/to/android-sdk
```

On Windows, replace the SDK path with its actual location and quote paths containing spaces. `ANDROID_SDK_ROOT` or `ANDROID_HOME` can be used instead of `--sdk`.

Output: `dist/Li-Widget-1.0.apk`. Temporary compilation files are under `build/`. The pipeline is `aapt2 → javac → D8 → zipalign → apksigner`; Java debug information is disabled and D8 uses release mode. Run `python build.py --help` for options.

## Signing keys

Without `--keystore`, the builder generates a random password and a local signing key in `.local/`, then reuses them for future builds. This directory is ignored by Git. Keep it private and back it up securely if you distribute your own builds. **Never commit or upload it.**

On POSIX systems the builder restricts `.local/` to mode `0700` and managed signing files to `0600`, including existing files. The generated password is created exclusively with owner-only permissions. Managed paths cannot be symbolic links; use `--keystore` for a separately managed key. On Windows, keep the checkout in a private user directory and protect it with the appropriate Windows ACLs. Explicit external keystore permissions remain the operator's responsibility.

The official downloadable APK is signed with a separate release key that is not in the repository or CI. A locally built APK has a different signing identity and cannot update that APK. Uninstalling an app to switch signing identities clears its settings and widget bindings.

For an existing key, provide the keystore path and alias, and set the password in the environment variable `LI_WIDGET_STOREPASS` through your local secret mechanism:

```sh
python build.py --sdk /path/to/android-sdk --keystore /private/path/release.jks --alias liwidget
```

The password is passed only to key generation and signing subprocesses by environment-variable reference, not as a command-line value. Compilation, alignment and verification subprocesses do not receive the selected password variable. `--password-env` selects a different variable name. Keystore and key passwords must match for this build script.

Published APK signing-certificate SHA-256:

```text
8059d47d6463e65219722718ad63e64f1d985641d08b0500e5bdd0278ce8629c
```

This fingerprint identifies the signer; it is not a private key. Verify using Android SDK `apksigner verify --verbose --print-certs path/to/Li-Widget-1.0.apk`. Check the file hash against the release's `SHA256SUMS.txt` as well.

## Continuous integration

GitHub Actions checks the release boundary and compiles a disposable locally signed APK on Ubuntu 24.04. CI has read-only repository permissions and no release signing secrets. It does not publish that test APK. The checkout action is pinned to a commit. Passing CI is a build check, not a real-vehicle test.

The same source and signing identity do not imply byte-for-byte reproducible APKs: ZIP timestamps and toolchain differences may change file hashes.

## Русский

Установите Python 3.10+, JDK 21, Android SDK Platform 36 и Build Tools 36.1.0. Выполните команды выше из корня репозитория, указав свой путь к SDK. Результат — `dist/Li-Widget-1.0.apk`. Оригинальный APK Li Auto для сборки не нужен.

Без собственного ключа сборщик создаёт ключ и случайный пароль в `.local/`. Эта папка не должна попадать в Git или архивы. Локальная подпись отличается от подписи опубликованного APK: обновление поверх него не установится. Для сохранения своих обновлений используйте один и тот же приватный ключ и берегите его копию.

CI проверяет исходники и сборку, но не публикует APK и не имеет ключа релиза. Проверки реального автомобиля в CI нет.
