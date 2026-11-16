package dev.li.widget;

import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.SizeF;
import java.util.*;

final class WidgetStyle {
  static final int MIN_WIDTH = 250, MIN_HEIGHT = 110, COMPACT_HEIGHT = 160;
  String theme = "system", palette = "system", layout = "auto", font = "normal";
  boolean showTime = true, showBattery = true;
  int staleMinutes = 30;

  static String key(int id) {
    return id < 0 ? "" : "w." + id + ".";
  }

  static WidgetStyle read(Context c, int id) {
    SharedPreferences p = State.prefs(c);
    String k = key(id);
    if (id >= 0 && !p.getBoolean(k + "initialized", false)) {
      WidgetStyle inherited = read(c, -1);
      inherited.save(c, id);
    }
    WidgetStyle s = new WidgetStyle();
    s.theme = p.getString(k + "theme", "system");
    s.palette = p.getString(k + "palette", "system");
    s.layout = p.getString(k + "layout", "auto");
    s.font = p.getString(k + "font", "normal");
    s.showTime = p.getBoolean(k + "show_update_time", true);
    s.showBattery = p.getBoolean(k + "show_battery_percent", true);
    s.staleMinutes = p.getInt(k + "stale_minutes", 30);
    return s;
  }

  void save(Context c, int id) {
    String k = key(id);
    State.prefs(c)
        .edit()
        .putString(k + "theme", theme)
        .putString(k + "palette", palette)
        .putString(k + "layout", layout)
        .putString(k + "font", font)
        .putBoolean(k + "show_update_time", showTime)
        .putBoolean(k + "show_battery_percent", showBattery)
        .putInt(k + "stale_minutes", staleMinutes)
        .putBoolean(k + "initialized", true)
        .apply();
  }

  static void remove(Context c, int id) {
    String k = key(id);
    SharedPreferences.Editor e = State.prefs(c).edit();
    for (String name : State.prefs(c).getAll().keySet()) if (name.startsWith(k)) e.remove(name);
    e.apply();
  }

  boolean dark(Context c) {
    return theme.equals("dark")
        || theme.equals("dim")
        || (theme.equals("system")
            && (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES);
  }

  boolean compact(int height) {
    return layout.equals("compact") || height < 232;
  }

  int previewHeight() {
    return layout.equals("detailed") ? 244 : COMPACT_HEIGHT;
  }

  int heightWithin(int available) {
    return Math.max(1, Math.min(available, layout.equals("compact") ? COMPACT_HEIGHT : 280));
  }

  float fontSize() {
    return font.equals("large") ? 12 : 11;
  }

  static int[] size(Context c, int id) {
    if (id < 0) return new int[] {352, read(c, id).previewHeight()};
    Bundle b = AppWidgetManager.getInstance(c).getAppWidgetOptions(id);
    boolean landscape =
        c.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    int w =
        b.getInt(
            landscape
                ? AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
                : AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
            352);
    int h =
        b.getInt(
            landscape
                ? AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
                : AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
            COMPACT_HEIGHT);
    return new int[] {Math.max(1, w), Math.max(1, h)};
  }

  @SuppressWarnings("deprecation")
  static List<SizeF> sizes(Bundle options) {
    ArrayList<SizeF> supplied =
        options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
    LinkedHashSet<SizeF> valid = new LinkedHashSet<>();
    if (supplied != null)
      for (SizeF size : supplied)
        if (size != null
            && size.getWidth() > 0
            && size.getHeight() > 0
            && Float.isFinite(size.getWidth())
            && Float.isFinite(size.getHeight())) {
          valid.add(size);
          if (valid.size() == 16) break;
        }
    if (valid.isEmpty()) {
      int minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 352),
          maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minW);
      int minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, COMPACT_HEIGHT),
          maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minH);
      valid.add(new SizeF(Math.max(1, minW), Math.max(1, maxH)));
      valid.add(new SizeF(Math.max(1, maxW), Math.max(1, minH)));
    }
    return new ArrayList<>(valid);
  }
}
