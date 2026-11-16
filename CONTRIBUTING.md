# Contributing / Участие в проекте

Bug reports and pull requests in Russian or English are welcome.

1. Check existing issues, [compatibility](README.en.md#compatibility) and [verification limits](docs/VERIFICATION.md).
2. For a bug, include the companion and original app versions, Android version, launcher, reproduction steps and expected/actual behavior. Remove personal data from all attachments.
3. Keep changes focused. Do not include the original APK, extracted resources, account data, signing keys, generated build outputs or private paths.
4. Run `python tools/check_release.py`, build using [BUILD.md](docs/BUILD.md), and describe relevant device/emulator checks. Keep Russian and English strings in sync.
5. Preserve original control handlers and confirmations. Do not add account extraction, embedded credentials, guessed live telemetry or vehicle commands that bypass the original app.

By submitting your own contribution, you agree to its distribution under the project's MIT license. Contributions must be yours to license. Security issues belong in [private reporting](SECURITY.md), not public issues. Be respectful and keep discussion focused on the work; there is no guaranteed response time.

---

Принимаются описания ошибок и pull request на русском и английском. Укажите версии приложений, Android, лаунчер, шаги воспроизведения и ожидаемый результат. Удалите личные данные. Не добавляйте оригинальный APK, извлечённые материалы, ключи подписи и результаты сборки.

Перед отправкой выполните `python tools/check_release.py`, соберите APK по [инструкции](docs/BUILD.md) и опишите проверку. Сохраняйте обработчики и подтверждения Li Auto, синхронно меняйте оба языка. Собственный вклад публикуется под MIT; вы должны иметь право предоставлять его по этой лицензии.
