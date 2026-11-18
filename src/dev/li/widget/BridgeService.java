package dev.li.widget;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.content.res.Configuration;
import android.os.*;
import android.util.SizeF;
import android.view.*;
import android.widget.RemoteViews;
import java.util.*;

public class BridgeService extends Service {
  private static RemoteViews sourceViews;
  private static View sourceSnapshot;
  private static Freshness freshness = new Freshness("", 0);
  private static volatile BridgeService instance;
  private AppWidgetHost host;
  private ThemeWatcher themeWatcher;
  private String notificationLanguage = "";
  private final Map<Integer, String> clockLabels = new HashMap<>();
  private boolean clockHasReadings;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private int attachedId = -1;
  private final Runnable refresh = () -> refreshSnapshot();
  // The launcher ends the animation itself. Clear its cached trigger afterwards as well.
  private final Runnable settleCharge = () -> {
    if (sourceViews != null && sourceSnapshot != null && State.connected(this))
      publishWidgets(false, false);
  };
  private final Runnable minute =
      new Runnable() {
        public void run() {
          if (getSystemService(PowerManager.class).isInteractive()) updateClock();
          handler.postDelayed(this, 60000);
        }
      };
  private final BroadcastReceiver clock =
      new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
          updateClock();
        }
      };

  @Override
  public void onCreate() {
    super.onCreate();
    themeWatcher = new ThemeWatcher(this, () -> {
      handler.removeCallbacks(refresh);
      handler.post(refresh);
    });
    updateNotification();
    host =
        new AppWidgetHost(this, State.HOST) {
          @Override
          protected AppWidgetHostView onCreateView(Context c, int id, AppWidgetProviderInfo info) {
            return new AppWidgetHostView(c) {
              @Override
              public void updateAppWidget(RemoteViews rv) {
                super.updateAppWidget(rv);
                handler.removeCallbacks(refresh);
                handler.postDelayed(refresh, 100);
              }
            };
          }
        };
    instance = this;
    IntentFilter clockFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
    clockFilter.addAction(Intent.ACTION_TIME_CHANGED);
    clockFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
    if (Build.VERSION.SDK_INT >= 33)
      registerReceiver(clock, clockFilter, Context.RECEIVER_NOT_EXPORTED);
    else registerReceiver(clock, clockFilter);
    handler.postDelayed(minute, 60000);
  }

  private void updateNotification() {
    String language = Language.code(this);
    if (language.equals(notificationLanguage)) return;
    NotificationManager n = getSystemService(NotificationManager.class);
    NotificationChannel channel =
        new NotificationChannel(
            "sync",
            Language.text(this, R.string.channel_updates),
            NotificationManager.IMPORTANCE_LOW);
    channel.setShowBadge(false);
    n.createNotificationChannel(channel);
    PendingIntent open =
        PendingIntent.getActivity(
            this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
    startForeground(
        71,
        new Notification.Builder(this, "sync")
            .setSmallIcon(R.drawable.widget_mark)
            .setContentTitle(Language.text(this, R.string.notification_active))
            .setContentText(Language.text(this, R.string.notification_connected))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build());
    notificationLanguage = language;
  }

  static boolean refreshIfRunning() {
    BridgeService s = instance;
    if (s == null || s.attachedId != State.sourceId(s) || s.attachedId == -1) return false;
    s.handler.removeCallbacks(s.refresh);
    s.handler.post(s.refresh);
    return true;
  }

  @Override
  public int onStartCommand(Intent i, int flags, int startId) {
    int id = State.sourceId(this);
    AppWidgetProviderInfo info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id);
    if (!State.validSource(info)) {
      sourceViews = null;
      sourceSnapshot = null;
      LiWidgetProvider.showEmpty(
          this,
          Language.text(this, R.string.connect_title),
          Language.text(this, R.string.restore_connection));
      stopSelf();
      return START_NOT_STICKY;
    }
    if (attachedId != id) {
      host.stopListening();
      sourceViews = null;
      sourceSnapshot = null;
      host.createView(this, id, info);
      attachedId = id;
      host.startListening();
    }
    handler.removeCallbacks(refresh);
    handler.post(refresh);
    return START_STICKY;
  }

  private void refreshSnapshot() {
    updateNotification();
    if (!State.connected(this)) return;
    handler.removeCallbacks(settleCharge);
    try {
      // A non-listening host asks the system for its complete cached RemoteViews.
      // This keeps original PendingIntents intact after partial source updates.
      final CaptureView[] captured = new CaptureView[1];
      AppWidgetHost snapshot =
          new AppWidgetHost(this, State.HOST) {
            @Override
            protected AppWidgetHostView onCreateView(
                Context c, int id, AppWidgetProviderInfo info) {
              captured[0] = new CaptureView(c);
              return captured[0];
            }
          };
      snapshot.createView(
          this,
          State.sourceId(this),
          AppWidgetManager.getInstance(this).getAppWidgetInfo(State.sourceId(this)));
      View previous = sourceSnapshot;
      Freshness last = freshness;
      sourceViews = captured[0] == null ? null : captured[0].remote;
      sourceSnapshot = captured[0];
      boolean changed = previous != null && sourceViews != null && readingsChanged(this, previous, sourceSnapshot);
      clockHasReadings = sourceViews != null && visibleSource("ll_widget_car") && !visibleSource("widget_text");
      if (sourceViews != null && visibleSource("ll_widget_car"))
        freshness = Freshness.observe(this, Appearance.sourceText(this, sourceSnapshot, "widget_update_time"),
            System.currentTimeMillis(), changed && clockHasReadings);
      boolean newReading = sourceViews != null && previous != null
          && (!freshness.original.equals(last.original) || changed);
      ChargeReading charge = ChargeReading.read(this, sourceSnapshot);
      boolean chargeChanged = sourceViews != null && charge != null
          && charge.changedFrom(ChargeReading.read(this, previous));
      publishWidgets(newReading, chargeChanged);
      if (chargeChanged)
        handler.postDelayed(settleCharge, WidgetMotion.CHARGE_PULSE_MILLIS + 150);
      sendBroadcast(new Intent(State.UPDATE).setPackage(getPackageName()));
    } catch (Exception e) {
      clockHasReadings = false;
      clockLabels.clear();
      sourceViews = null;
      sourceSnapshot = null;
      LiWidgetProvider.showEmpty(
          this,
          Language.text(this, R.string.widget_unavailable),
          Language.text(this, R.string.reconnect_source));
      sendBroadcast(new Intent(State.UPDATE).setPackage(getPackageName()));
    }
  }

  private boolean visibleSource(String field) {
    try {
      int id = getPackageManager().getResourcesForApplication(State.SOURCE).getIdentifier(field, "id", State.SOURCE);
      View v = sourceSnapshot == null ? null : sourceSnapshot.findViewById(id);
      return Appearance.visible(v) && (!(v instanceof android.widget.TextView) || ((android.widget.TextView) v).length() != 0);
    } catch (android.content.pm.PackageManager.NameNotFoundException gone) {
      return false;
    }
  }

  static String clockLabel(Context c, Freshness freshness, WidgetStyle style, Collection<SizeF> sizes, long now) {
    if (style.format.favorites()) return null;
    boolean stale = freshness.stale(now, style.staleMinutes);
    boolean hasMetrics = false, timed = false;
    float scale = c.getResources().getConfiguration().fontScale;
    for (SizeF size : sizes) {
      int h = (int) size.getHeight();
      WidgetFormat display = style.format.presentation(style, (int) size.getWidth(), h, scale);
      if (display.favorites()) continue;
      hasMetrics = true;
      if (display != WidgetFormat.MINIMAL && (stale || style.showTime
          && (display != WidgetFormat.RANGE || h >= 144 * scale))) timed = true;
    }
    if (!hasMetrics) return null;
    return timed ? freshness.text(c, now, style.staleMinutes) : stale ? "stale" : "";
  }

  private void updateClock() {
    if (!clockHasReadings || clockLabels.isEmpty()) return;
    long now = System.currentTimeMillis();
    if (now < freshness.observedSince || now < freshness.anchor)
      freshness = Freshness.observe(this, freshness.original, now);
    AppWidgetManager manager = AppWidgetManager.getInstance(this);
    for (int widget : new ArrayList<>(clockLabels.keySet())) {
      WidgetStyle style = WidgetStyle.read(this, widget);
      String next = clockLabel(this, freshness, style, WidgetStyle.sizes(manager.getAppWidgetOptions(widget), style.format), now);
      if (!Objects.equals(next, clockLabels.get(widget)))
        manager.updateAppWidget(widget, forWidget(this, widget));
    }
  }

  static void forgetClock(int id) {
    if (instance != null) instance.clockLabels.remove(id);
  }

  static View sourceForEditor() { return sourceSnapshot; }

  static boolean hasSnapshot(Context c) {
    return instance != null && instance.attachedId == State.sourceId(c) && sourceViews != null;
  }

  private void publishWidgets(boolean newReading, boolean chargeChanged) {
    AppWidgetManager manager = AppWidgetManager.getInstance(this);
    for (int widget : State.ownIds(this))
      manager.updateAppWidget(widget, forWidget(this, widget, newReading, chargeChanged));
  }

  private static boolean readingsChanged(Context c, View previous, View current) {
    for (String field : new String[] {"widget_current_mileage", "widget_total_mileage",
        "widget_battery_percent", "tv_widget_charge_desc"}) {
      if (!Appearance.sourceText(c, previous, field).equals(Appearance.sourceText(c, current, field))) return true;
    }
    return false;
  }

  private static RemoteViews preview(Context c, int id, int width, int height,
      boolean newReading, boolean chargeChanged) {
    if (sourceViews == null || sourceSnapshot == null)
      return LiWidgetProvider.empty(
          c,
          id,
          State.connected(c)
              ? Language.text(c, R.string.loading_source)
              : Language.text(c, R.string.connect_title),
          State.connected(c)
              ? Language.text(c, R.string.open_and_sign_in)
              : Language.text(c, R.string.connect_for_data),
          height);
    try {
      return Appearance.render(
          c, sourceViews, sourceSnapshot, WidgetStyle.read(c, id), width, height, freshness,
          newReading, chargeChanged);
    } catch (Exception e) {
      return LiWidgetProvider.empty(
          c,
          id,
          Language.text(c, R.string.widget_unavailable),
          Language.text(c, R.string.reconnect_source),
          height);
    }
  }

  static RemoteViews editorPreview(Context c, int id, int width, int height) {
    if (sourceViews == null || sourceSnapshot == null
        || Appearance.sourceText(c, sourceSnapshot, "widget_vehicle_name").trim().isEmpty()) return null;
    try {
      return Appearance.render(c, sourceViews, sourceSnapshot, WidgetStyle.read(c, id), width, height, freshness);
    } catch (Exception unavailable) {
      return null;
    }
  }

  static RemoteViews forWidget(Context c, int id) {
    return forWidget(c, id, false, false);
  }

  private static RemoteViews forWidget(Context c, int id, boolean newReading, boolean chargeChanged) {
    Map<SizeF, RemoteViews> views = new LinkedHashMap<>();
    WidgetStyle style = WidgetStyle.read(c, id);
    List<SizeF> sizes = WidgetStyle.sizes(AppWidgetManager.getInstance(c).getAppWidgetOptions(id), style.format);
    if (instance != null) {
      String label = instance.clockHasReadings ? clockLabel(c, freshness, style, sizes, System.currentTimeMillis()) : null;
      if (label == null) instance.clockLabels.remove(id);
      else instance.clockLabels.put(id, label);
    }
    for (SizeF size : sizes)
      views.put(
          size,
          preview(c, id, (int) Math.floor(size.getWidth()), (int) Math.floor(size.getHeight()),
              newReading, chargeChanged));
    return views.size() == 1 ? views.values().iterator().next() : new RemoteViews(views);
  }

  static final class CaptureView extends AppWidgetHostView {
    RemoteViews remote;

    CaptureView(Context c) {
      super(c);
    }

    @Override
    public void updateAppWidget(RemoteViews r) {
      super.updateAppWidget(r);
      remote = r == null ? null : r.clone();
    }
  }

  @Override
  public void onConfigurationChanged(Configuration c) {
    super.onConfigurationChanged(c);
    handler.removeCallbacks(refresh);
    handler.post(refresh);
  }

  @Override
  public void onDestroy() {
    if (themeWatcher != null) themeWatcher.close();
    instance = null;
    handler.removeCallbacksAndMessages(null);
    unregisterReceiver(clock);
    if (host != null) host.stopListening();
    sourceViews = null;
    sourceSnapshot = null;
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent i) {
    return null;
  }
}
