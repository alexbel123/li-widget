package dev.li.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;

/** Each launcher format has its own provider and real minimum size. */
enum WidgetFormat {
  FULL("full", R.string.format_full, LiWidgetProvider.class, 352, 160),
  LARGE("large", R.string.format_large, FormatProviders.Large.class, 352, 244),
  MINIMAL("minimal", R.string.format_minimal, FormatProviders.Minimal.class, 176, 64),
  RANGE("range", R.string.format_range, FormatProviders.Range.class, 176, 176),
  FAVORITES("favorites", R.string.format_favorites, FormatProviders.Favorites.class, 352, 72),
  FAVORITES_GRID("favorites_grid", R.string.format_favorites_grid, FormatProviders.FavoritesGrid.class, 176, 176),
  CHARGING("charging", R.string.format_charging, FormatProviders.Charging.class, 352, 160);

  final String key;
  final int title, width, height;
  final Class<? extends LiWidgetProvider> provider;

  WidgetFormat(String key, int title, Class<? extends LiWidgetProvider> provider, int width, int height) {
    this.key = key;
    this.title = title;
    this.provider = provider;
    this.width = width;
    this.height = height;
  }

  boolean standard() { return this == FULL || this == LARGE; }
  boolean favorites() { return this == FAVORITES || this == FAVORITES_GRID; }
  boolean hasActions() { return standard() || favorites(); }

  WidgetFormat presentation(WidgetStyle style, int width, int height, float fontScale) {
    if (!standard()) return this;
    int boardWidth = Math.round(238 * Math.min(1.25f, Math.max(1f, fontScale)));
    if (height < 104) return width >= boardWidth ? FAVORITES : MINIMAL;
    int gridMinimum = 48 + 97 + 8 + 32 + Math.round(42 * fontScale);
    if (width < boardWidth) return height >= gridMinimum ? LARGE : RANGE;
    boolean tallGrid = actionRows(width, height) == 2;
    if (tallGrid && height < gridMinimum) return FULL;
    int expanded = (style.layout.equals("detailed") ? 180 : 230) + Math.round(24 * Math.max(0, fontScale - 1));
    if (height >= expanded && (tallGrid || !style.layout.equals("compact"))) return LARGE;
    return FULL;
  }

  static int actionRows(int width, int height) {
    if (height < 97) return 1;
    return width < 230 || height > width * (width < 344 ? .82f : 1f) ? 2 : 1;
  }

  static final class OverviewSize {
    final int inset, rows, header, footer, actions, hero;
    OverviewSize(int width, int height, int statusHeight) {
      rows = actionRows(width, height);
      inset = width < 114 ? 0 : width < 145 ? 4 : Appearance.actionInset(width);
      header = height >= 244 ? Appearance.DETAILED_HEADER_DP : 48;
      footer = statusHeight;
      int available = height - header - footer - 8;
      int rowHeight = Math.min(96, Math.max(48, (available - 46 - (rows - 1)) / rows));
      actions = rows * rowHeight + rows - 1;
      hero = Math.max(1, available - actions);
    }
  }

  static int firstRowCount(int width, int rows) {
    if (rows == 2) return 2;
    if (width >= 203) return 4;
    // One 48 dp target advances to the remaining commands in the shortest frames.
    return Math.max(1, (width - 8 - 48) / 49);
  }
  ComponentName component(Context context) { return new ComponentName(context, provider); }

  static WidgetFormat parse(String value) {
    for (WidgetFormat format : values()) if (format.key.equals(value)) return format;
    return FULL;
  }

  static WidgetFormat of(Context context, ComponentName component) {
    for (WidgetFormat format : values()) if (format.component(context).equals(component)) return format;
    return null;
  }

  static WidgetFormat of(Context context, int widgetId) {
    AppWidgetProviderInfo info = AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId);
    return info == null ? null : of(context, info.provider);
  }
}
