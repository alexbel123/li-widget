# Установка Li Widget 1.0

1. Установите [Li-Widget-1.0.apk](https://github.com/alexbel123/li-widget/releases/latest/download/Li-Widget-1.0.apk). Если установлена предыдущая версия нашего компаньона 1.0 или 2.2 с той же подписью, выберите обновление; удалять её не требуется.
2. Откройте оригинальное приложение Li Auto и войдите в аккаунт автомобиля.
3. В Li Widget нажмите «Подключить Li Auto» и подтвердите системное разрешение Android на использование штатного виджета.
4. При запросе разрешите уведомления: постоянное уведомление сопровождает службу обновления.
5. В каталоге виджетов лаунчера → Li Widget выберите нужный формат. В приложении: «Новый виджет» → «Вид» → «Тип виджета» → «Добавить на главный экран». [Все семь вариантов и размеры](WIDGETS.md).

После обновления уже добавленные виджеты сохраняют свой тип и настройки. Чтобы получить другой тип, добавьте новый экземпляр: уменьшение универсального виджета не превращает его в «Минимальный» или «Избранное».

В разделе «Вид» выберите **Русский / English**, тему, палитру и формат. Язык общий для всех виджетов, остальные параметры оформления относятся к выбранному экземпляру. Штатный редактор кнопок и другие экраны Li Auto сохраняют свой язык.

«Системная» тема следует светлому/тёмному режиму телефона. Палитра «Android» использует системные нейтральные цвета, которые могут иметь оттенок обоев. «Чёрная» тема использует фон #000000 независимо от палитры. Для нейтрального серого выберите палитру «Нейтральные».

Чтобы освободить место на рабочем столе, удерживайте виджет и уменьшите его рамку средствами лаунчера. Изменение внутреннего формата само по себе не меняет количество занятых ячеек. Минимум — 250 × 110 dp; точное число ячеек определяет лаунчер.

В разделе «Кнопки» откройте штатный редактор. Выберите четыре действия, удерживайте их для перестановки и сохраните. Набор связан с выбранным автомобилем в Li Auto и может быть общим для нескольких виджетов.

Если обновления остановились, откройте Li Widget и Li Auto. Ограничения фоновой работы зависят от телефона. Отметка времени не означает, что автомобиль находится в сети: это время, переданное источником. Если источник не даёт дату, сначала показывается его время; относительный возраст появляется после наблюдаемого нового обновления.

## English

Install [Li-Widget-1.0.apk](https://github.com/alexbel123/li-widget/releases/latest/download/Li-Widget-1.0.apk) over the previous release with the same signature. Open and sign in to the original Li Auto app, then connect it from Li Widget and approve Android's widget binding request. Allow the ongoing update notification and add Li Widget to your home screen.

Select **Style → Language → English**. Appearance is configured separately for each widget. Resize the widget frame in your launcher to free home-screen cells. Select its four actions in the original Li Auto editor.

Android 12+ is required. Temperature readings and direct vehicle access are not supplied by this companion.

## Если что-то не работает / Troubleshooting

| Ситуация / Issue | Что проверить / Check |
|---|---|
| Не найден Li Auto / App not found | Нужен пакет `com.chehejia.oc.m01` со штатным `SimpleWidgetProvider`. Проверена структура 8.28.3 (7020). / The expected package and original widget provider must be installed. |
| Показан вход в аккаунт / Sign-in shown | Откройте Li Auto и войдите в аккаунт там. / Open and sign in through the original app. |
| Данные не обновляются / Updates stopped | Откройте оба приложения; проверьте ограничения фоновой работы и сохранность привязки. / Open both apps; check background restrictions and widget binding. |
| Компактный вид занимает большую рамку / Compact view still occupies large cells | Уменьшите рамку удержанием виджета на рабочем столе. / Resize its frame in the launcher. |
| Фон имеет оттенок обоев / Wallpaper tint | Выберите «Нейтральные», либо «Чёрная» для фона #000000. / Choose Neutral colors or Black theme. |
| Обновление APK отклонено / Update rejected | Сравните подпись: локальная сборка имеет другой ключ. Не удаляйте приложение, если хотите сохранить настройки. / Check signing identity before uninstalling and losing settings. |

Проверка файла в PowerShell: `Get-FileHash ./Li-Widget-1.0.apk -Algorithm SHA256`. В Linux: `sha256sum Li-Widget-1.0.apk`; в macOS: `shasum -a 256 Li-Widget-1.0.apk`. Сравните с `SHA256SUMS.txt` из того же релиза. / Compare the result with the same release's checksum file.

Не снимайте штатные подтверждения Li Auto и следуйте ограничениям оригинального приложения при использовании действий автомобиля. / Keep the original confirmations and follow the original app's vehicle-operation restrictions.
