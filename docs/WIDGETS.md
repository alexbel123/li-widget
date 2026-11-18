# Widget formats / Форматы виджетов

Implemented in version 1.0, build17. Each row is a separate Android AppWidget provider, not a picture or a different setting inside one fixed-size provider. Existing overview widgets retain their provider and settings when upgrading.

Реализовано в версии 1.0, сборка 17. Каждый вариант зарегистрирован отдельно в каталоге виджетов Android. Старые универсальные виджеты и их настройки сохраняются при обновлении.

| Формат / Format | Ячейки / Target cells | Минимум / Minimum dp | Содержимое / Contents |
|---|---|---|---|
| Универсальный / Overview | 4×2 | 110×48 | Запас хода, полоса команд или полная панель по размеру рамки / Range, action strip or dashboard |
| Большой / Large | 4×3 | 110×48 | Расширенная панель с аватаром при доступном месте / Expanded dashboard with room for an avatar |
| Минимальный / Minimal | 2×1 | 110×48 | Электрический и бензиновый запас хода / Electric and fuel range |
| Запас хода / Range | 2×2 | 110×110 | Два показателя запаса хода, батарея и обновление / Two range readings, battery and freshness |
| Избранное / Favorites | 4×1 | 110×48 | Ряд, сетка или страницы по размеру рамки / Row, grid or pages to fit |
| Избранное / Favorites | 2×2 | 110×48 | Ряд, сетка или страницы по размеру рамки / Row, grid or pages to fit |
| Зарядка / Charging | 4×2 | 250×110 | Процент батареи, статус и исходное время зарядки / Battery percentage, state and original countdown |

При изменении рамки универсальный виджет и большой сохраняют оформление и выбор команд. Пример для Pixel Launcher: 2×1 показывает два запаса хода, 4×1 — команды, 4×3 — аватар, показатели и полосу команд, 3×4 и 4×4 — кнопки в два ряда. Фактическая компоновка зависит от свободной ширины и высоты в dp, а также от масштаба шрифта. В обоих «Избранных» тесная однострочная рамка получает переключатель страниц. Он листает выбранные в Li Auto команды по порядку.

Overview and Large retain their settings when resized. Short, narrow frames prioritize range. A short, wide frame fits the action strip. Taller frames show the vehicle header, readings and either one or two rows of controls. Both Favorites providers offer a page switcher when all four touch targets no longer fit.

## Добавление / Adding

Откройте Li Widget, выберите «Новый виджет» → «Вид» → «Тип виджета», затем «Добавить виджет». Также все варианты доступны через каталог виджетов рабочего стола. Для уже размещённого экземпляра тип закреплён за его Android-провайдером; для другого типа добавьте новый экземпляр. Оформление настраивается отдельно для каждого.

In Li Widget, choose New widget → Style → Widget type → Add widget, or use your launcher's widget picker. Existing widgets keep their provider type; add another widget to change format. Styling remains per widget.

Размер в ячейках — ориентир для лаунчера: физическая сетка и системные отступы зависят от устройства. У маленьких форматов собственные ограничения размера; они не наследуют минимум универсального виджета. [Android sizing documentation](https://developer.android.com/develop/ui/views/appwidgets/layouts).

## Нажатия / Taps

В сборке 18 нажатие на карточку «Минимальный», «Запас хода» или «Зарядка» вызывает исходный обработчик обновления Li Auto. В строке времени рядом с отметкой обновления показывается небольшая круговая стрелка. Она не означает, что автомобиль уже ответил. Отключённое или недоступное действие источника не заменяется собственной командой.

Значок настроек у «Запаса хода» и «Зарядки» имеет область нажатия 48×48 dp и открывает оформление отдельно. В минимальном формате и тесной компоновке «Запаса хода» настройки доступны через приложение или системное редактирование виджета. Образец в редакторе не выполняет действия.

In build 18, tap Minimal, Range or Charging to invoke the original Li Auto refresh handler. A small refresh arrow accompanies the update-age line; it does not imply that the vehicle has already responded. Disabled or missing source actions remain unavailable. The Range and Charging settings icon has a separate 48×48 dp target. For Minimal and tightly resized Range, use the app or the launcher widget editor to configure appearance. In-app previews do not dispatch actions.

В сборке 19 тонкие разделители яснее обозначают соседние кнопки; в «Избранном 2×2» разделены и ряды. Минимальные области нажатия сохраняются. Штатный щелчок зависит от настроек звуков касания и лаунчера Android. Дополнительная вибрация на нажатие не реализована: исходные обработчики Li Auto вызываются напрямую.

Build 19 makes the thin control dividers clearer and also separates Favorites 2×2 rows, preserving minimum touch targets. Native click sound follows Android touch-sound settings and launcher behavior. Additional tap vibration is not implemented; original Li Auto handlers are invoked directly.

## Данные и кнопки / Data and controls

Все значения берутся из текущего штатного виджета Li Auto. Если при зарядке исходное поле запаса хода заменено процентом, вместо недоступного запаса хода отображается «—». Скрытые значения не подставляются из старого снимка. Получение и перевод времени зарядки используют прежний механизм; отдельного расчёта времени нет.

«Избранное» показывает четыре действия, выбранные в штатном редакторе Li Auto. Обе компоновки сохраняют исходные PendingIntent, подтверждения, отключённые состояния и активные значки. Выбор кнопок остаётся общим с исходной панелью Li Auto. Приложение не создаёт собственные команды автомобилю.

All readings come from the current original widget. Unavailable or hidden range values show a dash, including when charging replaces range with a percentage. Countdown handling is unchanged. Favorites preserves the original four actions, their PendingIntents and state; use the Li Auto editor to select and reorder them. The selection is shared with that original panel.

Тестовые значения показываются только в образце редактора без подключения. На рабочем столе без источника отображается предложение подключить Li Auto.

Sample values are confined to the unconnected editor preview. An unconnected home-screen widget shows a connection prompt.

## Снимки сборки 19 / Build 19 screenshots

Настоящие RemoteViews установленного APK, отрисованные в эмуляторе Android с синтетическими показаниями. Это не изображения-концепции. Исходные значки и доступность действий при подключении определяет Li Auto.

These are native RemoteViews rendered from the installed APK on an Android emulator, using synthetic readings. They are not concept images. The connected Li Auto app supplies the original action icons and availability.

<table>
<tr><th>Формат / Format</th><th>Светлая / Light</th><th>Тёмная / Dark</th></tr>
<tr><td>Универсальный / Overview · 4×2</td><td><img src="images/full-light-ru.png" width="320" alt="Универсальный / Overview · 4×2, light"></td><td><img src="images/full-dim-ru.png" width="320" alt="Универсальный / Overview · 4×2, dark"></td></tr>
<tr><td>Большой / Large · 4×3</td><td><img src="images/large-light-ru.png" width="320" alt="Большой / Large · 4×3, light"></td><td><img src="images/large-dim-ru.png" width="320" alt="Большой / Large · 4×3, dark"></td></tr>
<tr><td>Минимальный / Minimal · 2×1</td><td><img src="images/minimal-light-ru.png" width="320" alt="Минимальный / Minimal · 2×1, light"></td><td><img src="images/minimal-dim-ru.png" width="320" alt="Минимальный / Minimal · 2×1, dark"></td></tr>
<tr><td>Запас хода / Range · 2×2</td><td><img src="images/range-light-ru.png" width="320" alt="Запас хода / Range · 2×2, light"></td><td><img src="images/range-dim-ru.png" width="320" alt="Запас хода / Range · 2×2, dark"></td></tr>
<tr><td>Избранное / Favorites · 4×1</td><td><img src="images/favorites-light-ru.png" width="320" alt="Избранное / Favorites · 4×1, light"></td><td><img src="images/favorites-dim-ru.png" width="320" alt="Избранное / Favorites · 4×1, dark"></td></tr>
<tr><td>Избранное / Favorites · 2×2</td><td><img src="images/favorites_grid-light-ru.png" width="320" alt="Избранное / Favorites · 2×2, light"></td><td><img src="images/favorites_grid-dim-ru.png" width="320" alt="Избранное / Favorites · 2×2, dark"></td></tr>
<tr><td>Зарядка / Charging · 4×2</td><td><img src="images/charging-light-ru.png" width="320" alt="Зарядка / Charging · 4×2, light"></td><td><img src="images/charging-dim-ru.png" width="320" alt="Зарядка / Charging · 4×2, dark"></td></tr>
</table>

Меню типов и системный диалог добавления 2×2 (снимки сборки 17; состав форматов не менялся) / Format picker and Android 2×2 pin dialog (build 17 captures; the format catalog is unchanged):

<p><img src="images/formats-menu.png" width="240" alt="Семь типов виджета / Seven widget formats"> <img src="images/android-add-favorites-grid.png" width="240" alt="Android: избранное 2×2 / Favorites 2×2"></p>
