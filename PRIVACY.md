# Конфиденциальность / Privacy

Применяется к Li Widget 1.0 (`dev.li.widget`). Обновлено 16 ноября 2026 года.

## Русский

Li Widget — локальный компаньон для виджета отдельно установленного Li Auto. У проекта нет сервера для сбора данных, рекламы, аналитики или отправки отчётов о сбоях. Приложение не запрашивает разрешение на интернет и не выполняет собственных сетевых запросов.

После системного подтверждения Android компаньон получает представление штатного виджета (`RemoteViews`). Оно может содержать имя автомобиля, заряд, запас хода, состояние кнопок, сообщения и время обновления. Эти данные используются на устройстве для отображения. Li Widget не просит пароль, токены аккаунта или VIN и не читает приватное хранилище Li Auto.

В закрытых настройках приложения сохраняются выбранный язык, оформление, идентификаторы привязанных виджетов, исходная текстовая отметка обновления и вычисленная временная привязка. Это помогает сохранять настройки и показывать возраст данных. Автоматическое резервное копирование приложения отключено. Сброс данных или удаление приложения удаляет его локальные настройки; виджеты удаляются средствами Android.

Разрешения используются для системной привязки виджета, службы обновления с уведомлением и восстановления работы после загрузки устройства. Уведомления можно отключить в настройках Android, но ограничения фоновой работы могут мешать обновлениям. Геолокация, контакты, Bluetooth и доступ к общему хранилищу не запрашиваются.

Нажатия на штатные действия передаются оригинальным обработчикам Li Auto. Данные и сетевое взаимодействие оригинального приложения регулируются его собственными условиями и политикой; эта политика их не заменяет.

Обращения через GitHub подчиняются правилам GitHub. Публичные Issues видны всем: удаляйте VIN, регистрационные номера, имена, местоположение и данные аккаунта из текста и изображений. Вопросы по конфиденциальности с чувствительными деталями можно отправить через [приватное сообщение о безопасности](https://github.com/alexbel123/li-widget/security/advisories/new).

## English

Li Widget is a local companion to the separately installed Li Auto app. It has no data-collection server, ads, analytics or crash-report upload. It does not request internet permission or make its own network requests.

After Android's binding approval, the companion receives the original widget's `RemoteViews`. These may contain the vehicle name, battery, range, button states, messages and update time. They are used on-device for display. Li Widget does not request credentials, account tokens or VIN and does not read Li Auto's private storage.

Private app preferences store the selected language, appearance settings, widget binding identifiers, the source update-time text and a derived timestamp anchor. App backup is disabled. Clearing app data or uninstalling removes these local preferences; Android manages widget removal.

Permissions support widget binding, a foreground update service and notification, and recovery after device boot. Android settings control notifications and background restrictions. The app requests no location, contacts, Bluetooth or shared-storage access.

Original controls invoke Li Auto's handlers. The original app's data processing and network activity are governed by its own terms and privacy policy.

GitHub handles repository interactions under its own policies. Public issues are visible to everyone: remove account information, VIN, registration plates, names and location data before posting. Sensitive privacy concerns can be sent through [private security reporting](https://github.com/alexbel123/li-widget/security/advisories/new).
