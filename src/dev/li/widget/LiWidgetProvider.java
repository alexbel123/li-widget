package dev.li.widget;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.widget.RemoteViews;

public class LiWidgetProvider extends AppWidgetProvider {
  static RemoteViews empty(Context c, String title, String message) {
    return empty(c, -1, title, message);
  }

  static RemoteViews empty(Context c, int id, String title, String message) {
    return empty(c, id, title, message, WidgetStyle.size(c, id)[1]);
  }

  static RemoteViews empty(Context c, int id, String title, String message, int availableHeight) {
    WidgetStyle style = WidgetStyle.read(c, id);
    WidgetColors colors = WidgetColors.forWidget(c, style);
    RemoteViews r = new RemoteViews(c.getPackageName(), R.layout.widget_shell);
    int height = style.heightWithin(availableHeight);
    boolean shortView =
        height < 150
            || (height < 200 && c.getResources().getConfiguration().fontScale > 1.15f);
    r.setInt(R.id.shell, "setBackgroundResource", colors.surfaceRes);
    r.setTextViewText(R.id.empty_title, title);
    r.setTextViewText(R.id.empty_message, message);
    r.setTextColor(R.id.empty_title, colors.primary);
    r.setTextColor(R.id.empty_message, colors.secondary);
    r.setInt(R.id.brand, "setColorFilter", colors.icon);
    r.setViewLayoutHeight(R.id.empty, height, android.util.TypedValue.COMPLEX_UNIT_DIP);
    r.setViewLayoutHeight(R.id.shell, height, android.util.TypedValue.COMPLEX_UNIT_DIP);
    if (shortView) {
      r.setViewVisibility(R.id.brand, android.view.View.GONE);
      r.setViewLayoutMargin(
          R.id.empty_title, RemoteViews.MARGIN_TOP, 0, android.util.TypedValue.COMPLEX_UNIT_DIP);
      r.setTextViewTextSize(R.id.empty_title, android.util.TypedValue.COMPLEX_UNIT_SP, 14);
      r.setTextViewTextSize(R.id.empty_message, android.util.TypedValue.COMPLEX_UNIT_SP, 11);
      int pad = Math.round(8 * c.getResources().getDisplayMetrics().density);
      r.setViewPadding(R.id.empty, pad, pad, pad, pad);
    }
    if (height < 70) {
      r.setViewVisibility(R.id.empty_message, android.view.View.GONE);
      r.setInt(R.id.empty_title, "setMaxLines", 2);
      r.setTextViewTextSize(R.id.empty_title, android.util.TypedValue.COMPLEX_UNIT_SP, 11);
    }
    r.setOnClickPendingIntent(
        R.id.empty,
        PendingIntent.getActivity(
            c,
            id,
            new Intent(c, MainActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    return r;
  }

  static void showEmpty(Context c, String title, String message) {
    for (int id : State.ownIds(c))
      AppWidgetManager.getInstance(c).updateAppWidget(id, empty(c, id, title, message));
  }

  @Override
  public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
    for (int id : ids) {
      WidgetStyle.read(c, id);
      m.updateAppWidget(id, BridgeService.forWidget(c, id));
    }
    if (State.connected(c)) State.start(c);
  }

  @Override
  public void onDisabled(Context c) {
    if (State.ownIds(c).length == 0) c.stopService(new Intent(c, BridgeService.class));
  }

  @Override
  public void onDeleted(Context c, int[] ids) {
    for (int id : ids) WidgetStyle.remove(c, id);
  }

  @Override
  public void onAppWidgetOptionsChanged(
      Context c, AppWidgetManager m, int id, android.os.Bundle b) {
    m.updateAppWidget(id, BridgeService.forWidget(c, id));
    if (State.connected(c)) State.start(c);
  }
}
