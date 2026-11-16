# Security / Безопасность

## Report privately

Use **[Report a vulnerability](https://github.com/alexbel123/li-widget/security/advisories/new)** for security issues in Li Widget. Do not post sensitive reports in public issues. Include the affected version, Android version, reproduction steps and impact. Use synthetic data; omit credentials, tokens, VIN, private signing keys and personal information.

Reports are reviewed on a best-effort basis. There is no guaranteed response time or bug-bounty program. The current public 1.0 release is the review baseline; verify against the latest release when possible.

## Scope and boundaries

The project is an Android companion (`dev.li.widget`) that hosts a user-authorized widget from the separately installed Li Auto app. It has no server or internet permission. Covered project components include Java code, resources, the exported configuration activity, private receivers/service, build scripts and CI.

Inputs include activity intents, widget identifiers and dimensions supplied by Android, and `RemoteViews`/text from the selected provider. Android's app sandbox and binding approval form the platform boundary. Provider identification checks the expected package and provider class; it is not cryptographic publisher verification. Li Auto's authentication and command implementation remain in that application.

Security properties to preserve:

- Binding requires Android approval, and an externally supplied widget ID must belong to this project's provider before it can be configured.
- Companion receivers and service stay unexported; configuration requests must not expose private preferences or trigger unauthorized actions.
- Original action handlers and their confirmations remain intact. The companion must not introduce direct vehicle commands, collect credentials or claim guessed telemetry as measured data.
- Source content must not introduce code execution, data disclosure or access outside intended widget hosting.
- Release files and Git history must not contain private keys, credentials, personal data or private machine paths. CI must not have the release signing key.

## Assessing reports and limitations

Explain a realistic input path and impact. Authentication or confirmation bypass, unintended vehicle actions, sensitive-data exposure, malicious input handling and release-integrity issues are relevant. No finding class is automatically excluded, and no security risk is declared accepted by this document.

The original Li Auto application and Android are separate dependencies; this repository cannot patch their implementation. Reports showing a flaw in this companion's interaction with them remain relevant. Real-vehicle behavior was not tested with a connected car or owner account. Compatibility checks cover one original widget structure and do not constitute a security audit of the original app. A passing build or fixture does not prove that all security properties hold.

## Русский

Об уязвимостях сообщайте через **[приватную форму](https://github.com/alexbel123/li-widget/security/advisories/new)**. Укажите версию, шаги воспроизведения и последствия. Не включайте VIN, пароли, токены и личные данные. Срок ответа не гарантирован, программа вознаграждений не заявлена.

Li Widget работает в пределах системного разрешения на размещение виджета. Оригинальный Li Auto отвечает за авторизацию и выполнение команд. Обход подтверждений, неожиданные действия, раскрытие данных и нарушения целостности релиза относятся к значимым проблемам. Проверка пакета провайдера не заменяет проверку его сертификата. Документ не объявляет классы уязвимостей исключёнными или риски принятыми.
