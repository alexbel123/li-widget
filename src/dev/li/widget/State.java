package dev.li.widget;

import android.appwidget.*;
import android.content.*;
import android.os.Bundle;

final class State {
  static final String SOURCE = "com.chehejia.oc.m01", UPDATE = "dev.li.widget.UPDATE";
  static final int HOST = 741903;

  static SharedPreferences prefs(Context c) {
    return c.getSharedPreferences("widget", 0);
  }

  static int sourceId(Context c) {
    return prefs(c).getInt("source_id", -1);
  }

  static boolean validSource(AppWidgetProviderInfo info) {
    return info != null
        && info.provider != null
        && SOURCE.equals(info.provider.getPackageName())
        && info.provider.getClassName().endsWith(".SimpleWidgetProvider");
  }

  static boolean connected(Context c) {
    int id = sourceId(c);
    return id != -1 && validSource(AppWidgetManager.getInstance(c).getAppWidgetInfo(id));
  }

  static int[] ownIds(Context c) {
    return AppWidgetManager.getInstance(c)
        .getAppWidgetIds(new ComponentName(c, LiWidgetProvider.class));
  }

  static boolean ownsWidget(Context c, int id) {
    AppWidgetProviderInfo info = AppWidgetManager.getInstance(c).getAppWidgetInfo(id);
    return info != null && new ComponentName(c, LiWidgetProvider.class).equals(info.provider);
  }

  static ComponentName source(Context c) {
    for (AppWidgetProviderInfo p : AppWidgetManager.getInstance(c).getInstalledProviders())
      if (validSource(p)) return p.provider;
    return null;
  }

  static Bundle options() {
    Bundle b = new Bundle();
    b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 360);
    b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 480);
    b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 192);
    b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 250);
    b.putInt(
        AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
        AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
    return b;
  }

  static void start(Context c) {
    if (BridgeService.refreshIfRunning()) return;
    try {
      c.startForegroundService(new Intent(c, BridgeService.class));
    } catch (RuntimeException e) {
      LiWidgetProvider.showEmpty(
          c, Language.text(c, R.string.open_widget), Language.text(c, R.string.restore_updates));
    }
  }
}
