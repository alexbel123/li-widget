package dev.li.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;

/** Native launcher layouts, populated only from the current authorized source snapshot. */
final class VariantAppearance {
  private static final String MISSING = "—";

  private static final class Reading {
    String name = "", electric = MISSING, fuel = MISSING, time = "", message = "";
    int percent = -1;
    boolean charging, known, electricAlarm, fuelAlarm;
  }

  private static String field(Context source, View root, String name) {
    int id = source.getResources().getIdentifier(name, "id", State.SOURCE);
    View view = id == 0 ? null : root.findViewById(id);
    return view instanceof TextView && Appearance.visible(view) ? ((TextView) view).getText().toString().trim() : "";
  }

  private static Reading read(Context local, Context source, View root) {
    Reading result = new Reading();
    result.name = field(source, root, "widget_vehicle_name");
    result.message = Translations.translate(local, source, "widget_text", field(source, root, "widget_text"));
    ChargeReading charge = ChargeReading.read(local, root);
    result.known = charge != null;
    result.charging = charge != null && charge.charging;
    result.percent = charge == null ? -1 : charge.percentValue();
    result.time = Translations.translate(local, source, "tv_widget_charge_desc", field(source, root, "tv_widget_charge_desc"));
    if (result.message.isEmpty()) {
      result.electric = range(local, source, root, "widget_current_mileage");
      result.fuel = range(local, source, root, "widget_total_mileage");
      result.electricAlarm = alarm(source, root, "widget_current_mileage");
      result.fuelAlarm = alarm(source, root, "widget_total_mileage");
    }
    return result;
  }

  private static boolean alarm(Context source, View root, String name) {
    int field = source.getResources().getIdentifier(name, "id", State.SOURCE);
    int alarm = source.getResources().getIdentifier("mileage_alarm", "color", State.SOURCE);
    View view = field == 0 ? null : root.findViewById(field);
    if (!(view instanceof TextView) || alarm == 0) return false;
    int shown = ((TextView) view).getCurrentTextColor();
    for (int mode : new int[] {Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
      Configuration config = new Configuration(source.getResources().getConfiguration());
      config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | mode;
      if (shown == source.createConfigurationContext(config).getColor(alarm)) return true;
    }
    return false;
  }

  private static String range(Context local, Context source, View root, String field) {
    String value = VariantAppearance.field(source, root, field);
    // During charging the source puts a percentage in the EV-range field.
    if (value.isEmpty() || value.contains("%") || value.contains("％")) return MISSING;
    return Translations.translate(local, source, field, value);
  }

  static RemoteViews render(Context context, RemoteViews original, View snapshot, WidgetStyle style,
      int width, int height, Freshness freshness, boolean chargeChanged) throws Exception {
    Context local = Language.context(context);
    Context source = context.createPackageContext(State.SOURCE, 0);
    int car = source.getResources().getIdentifier("ll_widget_car", "id", State.SOURCE);
    if (car == 0 || !Appearance.visible(snapshot.findViewById(car)))
      return LiWidgetProvider.empty(context, style.widgetId, Language.text(local, R.string.connect_title),
          Language.text(local, R.string.open_and_sign_in), height);
    Reading reading = read(local, source, snapshot);
    if (style.format.favorites())
      return favorites(local, original, snapshot, style, width, height, reading.message);
    RemoteViews refresh = Appearance.refreshOverlay(local, original, snapshot, style, width, height);
    return metrics(local, style, width, height, freshness, chargeChanged, reading, true, refresh);
  }

  static RemoteViews sample(Context context, WidgetStyle style, int width, int height) {
    Context local = Language.context(context);
    Reading sample = new Reading();
    sample.name = Language.text(local, R.string.sample_widget_badge);
    sample.electric = "186 " + Language.text(local, R.string.sample_km);
    sample.fuel = "742 " + Language.text(local, R.string.sample_km);
    sample.percent = 76;
    sample.known = true;
    return metrics(local, style, width, height, new Freshness("", 0), false, sample, false, null);
  }

  private static RemoteViews base(Context c, WidgetStyle style, int layout, int height, WidgetColors colors) {
    RemoteViews result = new RemoteViews(c.getPackageName(), layout);
    result.setInt(R.id.variant_root, "setBackgroundResource", colors.surfaceRes);
    result.setViewLayoutHeight(R.id.variant_root, Math.max(1, height), TypedValue.COMPLEX_UNIT_DIP);
    return result;
  }

  private static RemoteViews favorites(Context c, RemoteViews original, View snapshot, WidgetStyle style,
      int width, int height, String message) throws Exception {
    WidgetColors colors = new WidgetColors(c, style);
    RemoteViews views = base(c, style, R.layout.widget_actions, height, colors);
    int rows = style.format == WidgetFormat.FAVORITES_GRID ? 2 : 1;
    int separator = rows - 1;
    int footer = message.isEmpty() ? 0 : Math.min(16, Math.max(0, height - 48 * rows - separator));
    int pad = Math.min(6, Math.max(0, (height - footer - rows * 48 - separator) / 2));
    int rowHeight = Math.max(1, (height - 2 * pad - footer - separator) / rows);
    views.setViewPadding(R.id.variant_root, dp(c, 4), dp(c, pad), dp(c, 4), dp(c, pad));
    views.setViewLayoutHeight(R.id.actions_first, rowHeight, TypedValue.COMPLEX_UNIT_DIP);
    views.removeAllViews(R.id.actions_first);
    views.addView(R.id.actions_first, Appearance.actionRow(c, original, snapshot, style,
        Math.max(1, width - 8), rowHeight, 1, rows == 2 ? 2 : 4));
    views.setViewVisibility(R.id.actions_second, rows == 2 ? View.VISIBLE : View.GONE);
    views.setViewVisibility(R.id.actions_divider, rows == 2 ? View.VISIBLE : View.GONE);
    views.setInt(R.id.actions_divider, "setBackgroundColor", colors.actionDivider);
    views.removeAllViews(R.id.actions_second);
    if (rows == 2) {
      views.setViewLayoutHeight(R.id.actions_second, rowHeight, TypedValue.COMPLEX_UNIT_DIP);
      views.addView(R.id.actions_second, Appearance.actionRow(c, original, snapshot, style,
          Math.max(1, width - 8), rowHeight, 3, 4));
    }
    views.setViewVisibility(R.id.variant_status, footer > 0 ? View.VISIBLE : View.GONE);
    views.setViewLayoutHeight(R.id.variant_status, footer, TypedValue.COMPLEX_UNIT_DIP);
    views.setTextViewText(R.id.variant_status, message);
    views.setTextColor(R.id.variant_status, colors.warning);
    views.setContentDescription(R.id.variant_root, message.isEmpty()
        ? Language.text(c, R.string.favorite_source_hint) : message);
    return views;
  }

  private static RemoteViews metrics(Context c, WidgetStyle style, int width, int height,
      Freshness freshness, boolean changed, Reading reading, boolean interactive, RemoteViews refresh) {
    boolean minimal = style.format == WidgetFormat.MINIMAL;
    boolean charging = style.format == WidgetFormat.CHARGING;
    float fontScale = c.getResources().getConfiguration().fontScale;
    boolean tightRange = !minimal && !charging && height < 144 * fontScale;
    WidgetColors colors = new WidgetColors(c, style);
    RemoteViews views = base(c, style, minimal ? R.layout.widget_minimal
        : charging ? R.layout.widget_charging : R.layout.widget_range, height, colors);
    long now = System.currentTimeMillis();
    boolean stale = freshness.stale(now, style.staleMinutes);
    String status = !reading.message.isEmpty() ? reading.message : freshness.text(c, now, style.staleMinutes);
    int warning = !reading.message.isEmpty() || stale ? colors.warning : colors.secondary;
    int pad = minimal ? (width < 140 ? 6 : 10) : width < 150 || height < 140 ? 8 : 12;
    int verticalPad = Math.min(pad, height / 8);
    views.setViewPadding(R.id.metrics_body, dp(c, pad), dp(c, verticalPad), dp(c, pad), dp(c, verticalPad));
    views.removeAllViews(R.id.metric_refresh_slot);
    if (refresh != null) views.addView(R.id.metric_refresh_slot, refresh);
    if (interactive) {
      PendingIntent configure = PendingIntent.getActivity(c, style.widgetId,
          new Intent(c, MainActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, style.widgetId),
          PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
      if (!minimal && !tightRange) views.setOnClickPendingIntent(R.id.variant_settings, configure);
      else if (refresh == null) views.setOnClickPendingIntent(R.id.variant_root, configure);
    }
    if (!minimal) {
      views.setViewVisibility(R.id.variant_header, tightRange ? View.GONE : View.VISIBLE);
      views.setViewVisibility(R.id.variant_settings, tightRange ? View.GONE : View.VISIBLE);
      views.setTextViewText(R.id.variant_name, reading.name.isEmpty() ? Language.text(c, R.string.app_name) : reading.name);
      views.setTextColor(R.id.variant_name, colors.primary);
      views.setInt(R.id.variant_settings, "setColorFilter", colors.icon);
      int headerHeight = height < 140 ? 24 : 32;
      views.setViewLayoutHeight(R.id.variant_header, headerHeight, TypedValue.COMPLEX_UNIT_DIP);
      // The independent overlay expands the hit target without stealing height from the readings.
      int buttonTop = Math.max(0, verticalPad + (headerHeight - 48) / 2);
      int iconTop = verticalPad + headerHeight / 2 - buttonTop - 9;
      views.setViewLayoutMargin(R.id.variant_settings, RemoteViews.MARGIN_TOP, buttonTop, TypedValue.COMPLEX_UNIT_DIP);
      views.setViewLayoutMargin(R.id.variant_settings, RemoteViews.MARGIN_RIGHT, Math.max(0, pad - 8), TypedValue.COMPLEX_UNIT_DIP);
      views.setViewPadding(R.id.variant_settings, dp(c, 15), dp(c, iconTop), dp(c, 15), dp(c, 30 - iconTop));
      views.setTextViewTextSize(R.id.variant_name, TypedValue.COMPLEX_UNIT_SP, height < 140 ? 12 : 15);
      views.setTextViewText(R.id.variant_status, status);
      views.setTextColor(R.id.variant_status, warning);
      views.setViewVisibility(R.id.variant_footer, !tightRange && (style.showTime || !reading.message.isEmpty()) ? View.VISIBLE : View.GONE);
      views.setViewVisibility(R.id.variant_refresh_mark, refresh == null ? View.GONE : View.VISIBLE);
      views.setInt(R.id.variant_refresh_mark, "setColorFilter", warning);
      views.setTextViewTextSize(R.id.variant_status, TypedValue.COMPLEX_UNIT_SP, height < 140 ? 9 : 10);
    }
    if (charging) {
      String percent = reading.percent >= 0 ? reading.percent + "%" : MISSING;
      number(c, views, R.id.charge_percent, percent, height < 140 ? 32 : 44, width * .35f, colors.primary);
      views.setTextViewText(R.id.charge_state, Language.text(c, !reading.known ? R.string.charge_unknown
          : reading.charging ? R.string.widget_charging : R.string.charge_idle));
      views.setTextColor(R.id.charge_state, colors.secondary);
      views.setTextViewText(R.id.charge_time, reading.charging
          ? (reading.time.isEmpty() ? Language.text(c, R.string.charge_no_time) : reading.time)
          : Language.text(c, R.string.battery_level));
      views.setTextColor(R.id.charge_time, colors.primary);
      views.setTextViewTextSize(R.id.charge_time, TypedValue.COMPLEX_UNIT_SP, height < 140 ? 12 : 15);
      views.setProgressBar(R.id.charge_progress, 100, Math.max(0, reading.percent), false);
      views.setFloat(R.id.charge_progress, "setAlpha", reading.percent < 0 ? .35f : 1f);
      views.setColorStateList(R.id.charge_progress, "setProgressTintList", ColorStateList.valueOf(stale ? colors.secondary : colors.chargeAccent));
      views.setColorStateList(R.id.charge_progress, "setProgressBackgroundTintList", ColorStateList.valueOf(colors.outline));
      views.setViewVisibility(R.id.charge_motion, reading.charging ? View.VISIBLE : View.GONE);
      views.setInt(R.id.charge_bolt, "setColorFilter", stale ? colors.secondary : colors.chargeAccent);
      if (reading.charging && !stale && WidgetMotion.enabled(style) && changed)
        views.setDisplayedChild(R.id.charge_motion, 0);
    } else {
      String electric = Language.text(c, minimal
          ? (width < 140 ? R.string.electric_tiny : R.string.electric_short)
          : tightRange ? R.string.electric_short : R.string.electric_range);
      if (!minimal && style.showBattery && reading.percent >= 0) electric += " · " + reading.percent + "%";
      views.setTextViewText(R.id.electric_caption, electric);
      views.setTextViewText(R.id.fuel_caption, Language.text(c, minimal ? R.string.fuel_short : R.string.fuel_range));
      views.setTextColor(R.id.electric_caption, (minimal || tightRange) && stale ? colors.warning : colors.secondary);
      views.setTextColor(R.id.fuel_caption, (minimal || tightRange) && stale ? colors.warning : colors.secondary);
      if (minimal && width < 140) {
        views.setTextViewTextSize(R.id.electric_caption, TypedValue.COMPLEX_UNIT_SP, 9);
        views.setTextViewTextSize(R.id.fuel_caption, TypedValue.COMPLEX_UNIT_SP, 9);
      }
      views.setInt(R.id.variant_divider, "setBackgroundColor", colors.outline);
      float column = minimal ? (width - 2 * pad - 17) / 2f : width - 2 * pad;
      float footer = !tightRange && (style.showTime || !reading.message.isEmpty()) ? 14 * fontScale + 4 : 0;
      float row = (height - 2 * Math.min(pad, height / 8) - (tightRange ? 0 : 32) - footer - 1) / 2f;
      float caption = (height < 140 ? 9 : 10) * fontScale * 1.4f;
      float size = minimal ? Math.min(24, height * .35f) : Math.min(32, (row - caption - 2) / (1.25f * fontScale));
      number(c, views, R.id.range_electric, reading.electric, size, column, reading.electricAlarm ? colors.warning : colors.primary);
      number(c, views, R.id.range_fuel, reading.fuel, size, column, reading.fuelAlarm ? colors.warning : colors.primary);
      if (!minimal && height < 140) {
        views.setTextViewTextSize(R.id.electric_caption, TypedValue.COMPLEX_UNIT_SP, 9);
        views.setTextViewTextSize(R.id.fuel_caption, TypedValue.COMPLEX_UNIT_SP, 9);
      }
    }
    views.setContentDescription(R.id.variant_root, Language.text(c, style.format.title) + ". " + status);
    return views;
  }

  private static void number(Context c, RemoteViews views, int id, String value, float size, float available, int color) {
    Paint paint = new Paint();
    paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    float scale = c.getResources().getDisplayMetrics().scaledDensity;
    float density = c.getResources().getDisplayMetrics().density;
    while (size > 11) {
      paint.setTextSize(size * scale);
      if (paint.measureText(value) <= available * density) break;
      size -= .5f;
    }
    SpannableString text = new SpannableString(value);
    int unit = value.indexOf(' ');
    if (unit < 0) unit = value.indexOf('\u00a0');
    if (unit >= 0) text.setSpan(new RelativeSizeSpan(.65f), unit, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    views.setTextViewText(id, text);
    views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, Math.max(11, size));
    views.setTextColor(id, color);
  }

  private static int dp(Context c, float value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
}
