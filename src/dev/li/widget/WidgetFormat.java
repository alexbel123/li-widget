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
