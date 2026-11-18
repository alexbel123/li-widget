package dev.li.widget;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.widget.RemoteViews;

public class LiWidgetProvider extends AppWidgetProvider {
  private static final String NEXT_ACTIONS = "dev.li.widget.NEXT_ACTIONS";

  static PendingIntent nextActions(Context c, WidgetStyle style, int offset) {
    Intent intent = new Intent(c, style.format.provider).setAction(NEXT_ACTIONS)
        .setIdentifier(style.widgetId + "/" + offset)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, style.widgetId).putExtra("offset", offset);
    return PendingIntent.getBroadcast(c, style.widgetId, intent, PendingIntent.FLAG_IMMUTABLE);
  }

  @Override
  public void onReceive(Context c, Intent intent) {
    if (!NEXT_ACTIONS.equals(intent.getAction())) {
      super.onReceive(c, intent);
      return;
    }
    int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
    int offset = intent.getIntExtra("offset", -1);
    if (offset < 0 || offset > 3 || !State.ownsWidget(c, id)) return;
    State.prefs(c).edit().putInt(WidgetStyle.key(id) + "action_offset", offset).apply();
    AppWidgetManager.getInstance(c).updateAppWidget(id, BridgeService.forWidget(c, id));
  }

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
    r.setViewVisibility(R.id.filled, android.view.View.GONE);
    r.setViewVisibility(R.id.empty, android.view.View.VISIBLE);
    r.setInt(R.id.shell, "setBackgroundResource", colors.surfaceRes);
    r.setTextViewText(R.id.empty_title, title);
    r.setTextViewText(R.id.empty_message, message);
    r.setTextColor(R.id.empty_title, colors.primary);
    r.setTextColor(R.id.empty_message, colors.secondary);
    r.setInt(R.id.brand, "setColorFilter", colors.icon);
    r.setViewLayoutHeight(R.id.empty, height, android.util.TypedValue.COMPLEX_UNIT_DIP);
    r.setViewLayoutHeight(R.id.shell, height, android.util.TypedValue.COMPLEX_UNIT_DIP);
    r.setViewVisibility(R.id.brand, shortView ? android.view.View.GONE : android.view.View.VISIBLE);
    r.setViewVisibility(R.id.empty_message, height < 70 ? android.view.View.GONE : android.view.View.VISIBLE);
    r.setViewLayoutMargin(R.id.empty_title, RemoteViews.MARGIN_TOP, shortView ? 0 : 12,
        android.util.TypedValue.COMPLEX_UNIT_DIP);
    r.setTextViewTextSize(R.id.empty_title, android.util.TypedValue.COMPLEX_UNIT_SP, height < 70 ? 11 : shortView ? 14 : 17);
    r.setTextViewTextSize(R.id.empty_message, android.util.TypedValue.COMPLEX_UNIT_SP, shortView ? 11 : 12);
    r.setInt(R.id.empty_title, "setMaxLines", height < 70 ? 2 : Integer.MAX_VALUE);
    int pad = Math.round((shortView ? 8 : 20) * c.getResources().getDisplayMetrics().density);
    r.setViewPadding(R.id.empty, pad, pad, pad, pad);
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
    for (int id : ids) {
      BridgeService.forgetClock(id);
      WidgetStyle.remove(c, id);
    }
  }

  @Override
  public void onAppWidgetOptionsChanged(
      Context c, AppWidgetManager m, int id, android.os.Bundle b) {
    m.updateAppWidget(id, BridgeService.forWidget(c, id));
    if (State.connected(c) && !BridgeService.hasSnapshot(c)) State.start(c);
  }
}
