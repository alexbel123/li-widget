package dev.li.widget;

import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import java.util.*;

final class Appearance {
  private final Context local, source;
  private final Resources res;
  private final RemoteViews rv;
  private final View snapshot;
  private final boolean dark;
  private final WidgetStyle style;
  private final WidgetColors colors;
  private final Freshness fresh;
  private final int widthDp, heightDp;
  private final Map<String, String> texts = new HashMap<>();
  private final int primary, secondary, icon, button, alarm;

  private Appearance(
      Context c, RemoteViews original, View view, WidgetStyle s, int w, int h, Freshness f)
      throws Exception {
    local = c;
    source = c.createPackageContext(State.SOURCE, 0);
    res = source.getResources();
    rv = original.clone();
    snapshot = view;
    style = s;
    fresh = f;
    dark = s.dark(c);
    widthDp = Math.max(1, w);
    heightDp = s.heightWithin(h);
    colors = new WidgetColors(c, s);
    primary = colors.primary;
    secondary = colors.secondary;
    icon = colors.icon;
    button = colors.primary;
    alarm = colors.warning;
  }

  static boolean visible(View v) {
    if (v == null) return false;
    while (v != null) {
      if (v.getVisibility() != View.VISIBLE) return false;
      android.view.ViewParent p = v.getParent();
      v = p instanceof View ? (View) p : null;
    }
    return true;
  }

  static String sourceText(Context c, View root, String name) {
    try {
      Resources r = c.getPackageManager().getResourcesForApplication(State.SOURCE);
      View v = root.findViewById(r.getIdentifier(name, "id", State.SOURCE));
      return v instanceof TextView ? ((TextView) v).getText().toString() : "";
    } catch (Exception e) {
      return "";
    }
  }

  private float sp(float v) {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, v, local.getResources().getDisplayMetrics());
  }

  private float fitLabel(String value, float requested, float available) {
    Paint p = new Paint();
    p.setTypeface(Typeface.create("sans-serif", 0));
    float s = requested;
    while (s > 8f) {
      p.setTextSize(sp(s));
      float widest = 0;
      for (String line : value.split("\\n")) widest = Math.max(widest, p.measureText(line));
      if (widest <= px(available - 1)) break;
      s -= .25f;
    }
    return s;
  }

  private int id(String name) {
    return res.getIdentifier(name, "id", State.SOURCE);
  }

  private int drawable(String name) {
    return res.getIdentifier(name, "drawable", State.SOURCE);
  }

  private int color(String name) {
    int n = res.getIdentifier(name, "color", State.SOURCE);
    return n == 0 ? 0 : source.getColor(n);
  }

  private boolean sourceColor(String name, int value) {
    int n = res.getIdentifier(name, "color", State.SOURCE);
    if (n == 0) return false;
    for (int mode : new int[] {Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
      Configuration config = new Configuration(res.getConfiguration());
      config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | mode;
      if (source.createConfigurationContext(config).getColor(n) == value) return true;
    }
    return false;
  }

  private int px(float v) {
    return Math.round(v * local.getResources().getDisplayMetrics().density);
  }

  private void integer(String n, String method, int value) {
    int i = id(n);
    if (i != 0) rv.setInt(i, method, value);
  }

  private void size(String n, float value) {
    int i = id(n);
    if (i != 0) rv.setTextViewTextSize(i, TypedValue.COMPLEX_UNIT_SP, value);
  }

  private void height(String n, float value) {
    int i = id(n);
    if (i != 0) rv.setViewLayoutHeight(i, value, TypedValue.COMPLEX_UNIT_DIP);
  }

  private void width(String n, float value) {
    int i = id(n);
    if (i != 0) rv.setViewLayoutWidth(i, value, TypedValue.COMPLEX_UNIT_DIP);
  }

  private void margin(String n, int edge, float value) {
    int i = id(n);
    if (i != 0) rv.setViewLayoutMargin(i, edge, value, TypedValue.COMPLEX_UNIT_DIP);
  }

  private void padding(String n, int l, int t, int r, int b) {
    int i = id(n);
    if (i != 0) rv.setViewPadding(i, px(l), px(t), px(r), px(b));
  }

  private void tint(String n) {
    integer(n, "setColorFilter", icon);
  }

  private void textColor(String n, int value) {
    int i = id(n);
    if (i != 0) rv.setTextColor(i, value);
  }

  private void hidden(String n) {
    int i = id(n);
    if (i != 0) rv.setViewVisibility(i, View.GONE);
  }

  private void preserveMetricColor(String n) {
    textColor(n, metricColor(n));
  }

  private int metricColor(String n) {
    View v = snapshot.findViewById(id(n));
    int original = v instanceof TextView ? ((TextView) v).getCurrentTextColor() : 0;
    return sourceColor("mileage_alarm", original)
        ? alarm
        : sourceColor("charge_10_text", original) ? (dark ? 0xff85d9a2 : 0xff25814d) : primary;
  }

  private void normalIcon(String name) {
    View v = snapshot.findViewById(id(name));
    if (!(v instanceof ImageView)) return;
    Drawable d = ((ImageView) v).getDrawable();
    if (!(d instanceof BitmapDrawable)) return;
    Bitmap shown = ((BitmapDrawable) d).getBitmap();
    String[] names = {
      "ic_widget_lock",
      "ic_widget_unlock",
      "ic_widget_ac",
      "ic_widget_tail",
      // The source swaps to this normal tailgate asset for another vehicle variant.
      "ic_widget_mxi_tail_off",
      "ic_widget_parking",
      "ic_widget_control_deicing_off",
      "ic_widget_control_remote_off",
      "ic_widget_control_search",
      "ic_widget_control_wheel_off",
      "ic_widget_control_window_off"
    };
    for (String n : names) {
      int r = drawable(n);
      if (r == 0) continue;
      Drawable candidate = source.getDrawable(r);
      if (candidate instanceof BitmapDrawable
          && shown.sameAs(((BitmapDrawable) candidate).getBitmap())) {
        tint(name);
        return;
      }
    }
    // Active, warning and animated icons retain the original provider's state colors.
  }

  private void translate() {
    String[] textIds = {
      "widget_vehicle_name",
      "widget_update_time",
      "widget_current_mileage",
      "widget_total_mileage",
      "widget_battery_percent",
      "tv_widget_charge_desc",
      "widget_text",
      "control_button_text1",
      "control_button_text2",
      "control_button_text3",
      "control_button_text4",
      "tv_widget_login"
    };
    for (String name : textIds) {
      int i = id(name);
      View view = snapshot.findViewById(i);
      if (!(view instanceof TextView)) continue;
      String value = ((TextView) view).getText().toString();
      value = Translations.translate(local, source, name, value);
      texts.put(name, value);
      SpannableString styled = new SpannableString(value);
      styled.setSpan(
          new TypefaceSpan("sans-serif"), 0, styled.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      rv.setTextViewText(i, styled);
      if (name.startsWith("control_button_text")) {
        int index = Integer.parseInt(name.substring(name.length() - 1));
        int b = id("control_button" + index);
        if (b != 0) rv.setContentDescription(b, Translations.description(local, value));
      }
    }
  }

  static RemoteViews render(Context c, RemoteViews original, View view) throws Exception {
    WidgetStyle s = WidgetStyle.read(c, -1);
    return render(
        c,
        original,
        view,
        s,
        352,
        s.previewHeight(),
        Freshness.observe(
            c, sourceText(c, view, "widget_update_time"), System.currentTimeMillis()));
  }

  static RemoteViews render(
      Context c,
      RemoteViews original,
      View view,
      WidgetStyle s,
      int width,
      int height,
      Freshness fresh)
      throws Exception {
    return new Appearance(c, original, view, s, width, height, fresh).build();
  }

  private RemoteViews build() {
    if (id("re_widget_content") == 0 || id("control_button1") == 0)
      throw new IllegalStateException("Unsupported source widget layout");
    translate();
    boolean compact = style.compact(heightDp),
        hasCar = visible(snapshot.findViewById(id("ll_widget_car")));
    String message =
        visible(snapshot.findViewById(id("widget_text")))
            ? texts.getOrDefault("widget_text", "")
            : "";
    boolean hasMessage = !message.trim().isEmpty(),
        showStatus = hasCar && (style.showTime || hasMessage);
    int footer = hasMessage ? 36 : showStatus ? (compact ? 24 : 28) : 12;
    int controls =
        Math.max(
            compact ? 72 : 76,
            4
                * (int)
                    Math.ceil(
                        (44
                                + 2.4
                                    * sp(style.fontSize())
                                    / local.getResources().getDisplayMetrics().density)
                            / 4));
    int top = compact ? 4 : 8, above = compact ? 0 : 6, below = compact ? 4 : 12;
    int body = Math.max(24, heightDp - 8 - footer - top - 48 - above - below - controls);
    int actualHeight = 8 + footer + top + 48 + above + below + controls + body;
    boolean dense = heightDp < 192 || actualHeight > heightDp;
    if (dense) {
      footer = showStatus && heightDp >= 148 ? (hasMessage ? 32 : 22) : 0;
      controls = Math.min(76, Math.max(1, heightDp - 48 - 8 - footer));
      top = Math.max(0, heightDp - 48 - 8 - footer - controls);
      above = below = body = 0;
      actualHeight = heightDp;
    }
    integer("ll_widget_car", "setBackgroundResource", 0);
    integer("re_widget_login", "setBackgroundResource", 0);
    rv.setViewLayoutWidth(
        id("re_widget_content"), ViewGroup.LayoutParams.MATCH_PARENT, TypedValue.COMPLEX_UNIT_PX);
    height("re_widget_content", actualHeight - 8 - (hasCar ? footer : 0));
    padding("ll_widget_car", 0, 0, 0, 0);
    height("widget_header", 48);
    margin("widget_header", RemoteViews.MARGIN_LEFT, 16);
    margin("widget_header", RemoteViews.MARGIN_RIGHT, 8);
    margin("widget_header", RemoteViews.MARGIN_TOP, top);
    width("widget_logo", 25);
    height("widget_logo", 11);
    size("widget_vehicle_name", compact ? 16 : 18);
    hidden("widget_update_time");
    width("iv_widget_refresh", 48);
    height("iv_widget_refresh", 48);
    padding("iv_widget_refresh", 12, 12, 12, 12);
    width("widget_iv_setting", 48);
    height("widget_iv_setting", 48);
    padding("widget_iv_setting", 12, 12, 12, 12);
    rv.setContentDescription(id("iv_widget_refresh"), Language.text(local, R.string.desc_refresh));
    rv.setContentDescription(id("widget_iv_setting"), Language.text(local, R.string.desc_settings));
    height("widget_content", body);
    margin("widget_content", RemoteViews.MARGIN_TOP, above);
    margin("widget_content", RemoteViews.MARGIN_BOTTOM, below);
    margin("widget_content", RemoteViews.MARGIN_LEFT, 18);
    margin("widget_content", RemoteViews.MARGIN_RIGHT, 18);
    hidden("widget_text");
    margin("ll_widget_oil", RemoteViews.MARGIN_LEFT, 10);
    float metric =
        Math.min(
            compact ? 25 : 31, body / (1.25f * local.getResources().getConfiguration().fontScale));
    Paint p = new Paint();
    p.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
    for (; ; ) {
      p.setTextSize(sp(metric));
      float total = px(10);
      for (String key : new String[] {"widget_current_mileage", "widget_total_mileage"})
        if (visible(snapshot.findViewById(id(key))))
          total += p.measureText(texts.getOrDefault(key, "")) + px(18);
      if (style.showBattery && visible(snapshot.findViewById(id("widget_battery_percent")))) {
        p.setTextSize(sp(11));
        total += p.measureText(texts.getOrDefault("widget_battery_percent", "")) + px(6);
      }
      if (total <= px(widthDp - 36) || metric <= 16) break;
      metric -= .5f;
    }
    size("widget_current_mileage", metric);
    size("widget_total_mileage", metric);
    size("widget_battery_percent", 11);
    size("tv_widget_charge_desc", 11);
    integer("tv_widget_charge_desc", "setMaxLines", 2);
    int side = widthDp < 300 ? 8 : 12, gap = widthDp < 300 ? 4 : 6;
    height("widget_buttons", controls);
    padding("widget_buttons", 0, 0, 0, 0);
    margin("widget_buttons", RemoteViews.MARGIN_LEFT, side);
    margin("widget_buttons", RemoteViews.MARGIN_RIGHT, side);
    for (int i = 2; i <= 5; i++) hidden("card_theme_background_" + i);
    int originalShape = drawable("shape_widget_bg");
    for (int i = 1; i <= 4; i++) {
      String n = "control_button" + i, value = texts.getOrDefault("control_button_text" + i, "");
      boolean confirm = value.equals(Language.text(local, R.string.action_confirm));
      height(n, controls);
      padding(n, 2, dense ? 2 : 4, 2, dense ? 2 : 4);
      if (i < 4) margin(n, RemoteViews.MARGIN_RIGHT, gap);
      if (originalShape != 0) {
        integer(n, "setBackgroundResource", originalShape);
        rv.setColorStateList(
            id(n),
            "setBackgroundTintList",
            ColorStateList.valueOf(confirm ? colors.confirmation : colors.panel));
      }
      int glyph = dense && controls < 68 ? 24 : 32;
      float labelSize =
          dense
              ? Math.min(
                  style.fontSize(),
                  (controls - glyph - 6)
                      / (2.4f * local.getResources().getConfiguration().fontScale))
              : style.fontSize();
      width("control_button_icon" + i, glyph);
      height("control_button_icon" + i, glyph);
      normalIcon("control_button_icon" + i);
      size(
          "control_button_text" + i,
          fitLabel(value, labelSize, (widthDp - 2 * side - 3 * gap) / 4f - 4));
      integer("control_button_text" + i, "setMaxLines", 2);
      integer("control_button_text" + i, "setMinLines", 2);
      integer("control_button_text" + i, "setGravity", Gravity.CENTER);
      margin("control_button_text" + i, RemoteViews.MARGIN_TOP, dense ? 2 : 4);
      textColor("control_button_text" + i, confirm ? alarm : button);
      View original = snapshot.findViewById(id(n));
      if (original != null && !original.isEnabled()) rv.setFloat(id(n), "setAlpha", .5f);
    }
    for (String n :
        new String[] {
          "widget_logo",
          "widget_iv_back",
          "iv_widget_refresh",
          "widget_iv_setting",
          "card_theme_tint_1",
          "iv_widget_login"
        }) tint(n);
    textColor("widget_vehicle_name", primary);
    textColor("widget_update_time", secondary);
    textColor("tv_widget_charge_desc", secondary);
    textColor("widget_text", alarm);
    textColor("tv_widget_login", secondary);
    for (String n :
        new String[] {"widget_current_mileage", "widget_total_mileage", "widget_battery_percent"})
      preserveMetricColor(n);
    integer("card_theme_background_1", "setBackgroundColor", colors.outline);
    if (!style.showBattery) hidden("widget_battery_percent");
    if (dense) {
      hidden("widget_content");
      hidden("widget_logo");
      hidden("card_theme_background_1");
      hidden("widget_iv_back");
      margin("widget_header", RemoteViews.MARGIN_LEFT, 12);
      margin("widget_header", RemoteViews.MARGIN_RIGHT, 8);
      compactHeader(message, footer == 0 && showStatus);
    }
    height("re_widget_login", actualHeight - 8);
    padding("re_widget_login", 18, 18, 18, 18);
    width("iv_widget_login", 36);
    height("iv_widget_login", 36);
    size("tv_widget_login", 14);
    width("tv_widget_login", 240);
    integer("tv_widget_login", "setGravity", Gravity.CENTER);
    integer("tv_widget_login", "setMaxLines", 3);
    margin("tv_widget_login", RemoteViews.MARGIN_TOP, 12);
    if (actualHeight < 150) {
      hidden("iv_widget_login");
      hidden("card_theme_tint_1");
      padding("re_widget_login", 8, 8, 8, 8);
      margin("tv_widget_login", RemoteViews.MARGIN_TOP, 0);
      size("tv_widget_login", 12);
      width("tv_widget_login", Math.max(1, widthDp - 32));
    }
    RemoteViews shell = new RemoteViews(local.getPackageName(), R.layout.widget_shell);
    shell.setInt(R.id.shell, "setBackgroundResource", colors.surfaceRes);
    shell.setViewLayoutHeight(R.id.shell, actualHeight, TypedValue.COMPLEX_UNIT_DIP);
    shell.setViewVisibility(R.id.empty, View.GONE);
    shell.setViewVisibility(R.id.filled, View.VISIBLE);
    shell.setViewLayoutHeight(
        R.id.source_slot, actualHeight - 8 - (hasCar ? footer : 0), TypedValue.COMPLEX_UNIT_DIP);
    shell.removeAllViews(R.id.source_slot);
    shell.addView(R.id.source_slot, rv);
    shell.setViewVisibility(R.id.status_row, showStatus && footer > 0 ? View.VISIBLE : View.GONE);
    shell.setViewLayoutHeight(R.id.status_row, footer, TypedValue.COMPLEX_UNIT_DIP);
    if (dense) {
      shell.setTextViewTextSize(R.id.status_text, TypedValue.COMPLEX_UNIT_SP, 10);
      shell.setTextViewTextSize(R.id.status_dot, TypedValue.COMPLEX_UNIT_SP, 14);
    }
    boolean stale = fresh.stale(System.currentTimeMillis(), style.staleMinutes);
    int stateColor = hasMessage || stale ? alarm : secondary;
    shell.setTextViewText(
        R.id.status_text,
        hasMessage ? message : fresh.text(local, System.currentTimeMillis(), style.staleMinutes));
    shell.setTextColor(R.id.status_text, stateColor);
    shell.setTextColor(R.id.status_dot, stateColor);
    return shell;
  }

  private void compactHeader(String message, boolean inlineStatus) {
    // Keep the source header's click handlers. Only its text becomes two compact lines.
    String name = texts.getOrDefault("widget_vehicle_name", "Li Auto"),
        battery =
            style.showBattery && visible(snapshot.findViewById(id("widget_battery_percent")))
                ? texts.getOrDefault("widget_battery_percent", "")
                : "";
    String stamp = fresh.compactText(local, System.currentTimeMillis(), style.staleMinutes);
    String charging =
        visible(snapshot.findViewById(id("tv_widget_charge_desc")))
            ? texts.getOrDefault("tv_widget_charge_desc", "")
            : "";
    String headline =
        (charging.isEmpty() ? name : charging) + (battery.isEmpty() ? "" : " · " + battery);
    boolean warning =
        !message.isEmpty() || fresh.stale(System.currentTimeMillis(), style.staleMinutes);
    if (inlineStatus)
      headline = !message.isEmpty() ? message : warning ? stamp : headline + " · " + stamp;
    float available = px(Math.max(1, widthDp - 125));
    TextPaint paint = new TextPaint();
    paint.setTypeface(Typeface.create("sans-serif", 0));
    paint.setTextSize(sp(10));
    SpannableStringBuilder title =
        new SpannableStringBuilder(
            TextUtils.ellipsize(headline, paint, available, TextUtils.TruncateAt.END));
    title.setSpan(
        new ForegroundColorSpan(inlineStatus && warning ? alarm : secondary),
        0,
        title.length(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    int batteryStart = battery.isEmpty() ? -1 : title.toString().indexOf(battery);
    if (batteryStart >= 0)
      title.setSpan(
          new ForegroundColorSpan(metricColor("widget_battery_percent")),
          batteryStart,
          batteryStart + battery.length(),
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    title.append('\n');
    List<String> metrics = new ArrayList<>();
    for (String key : new String[] {"widget_current_mileage", "widget_total_mileage"})
      if (visible(snapshot.findViewById(id(key)))) metrics.add(key);
    float textSize = 18;
    for (; ; ) {
      paint.setTextSize(sp(textSize));
      paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
      float needed = 0;
      for (String key : metrics) needed += paint.measureText(texts.getOrDefault(key, "")) + px(18);
      if (needed <= available || textSize <= 9) break;
      textSize -= .25f;
    }
    for (String key : metrics) {
      int start = title.length();
      View group =
          snapshot.findViewById(
              id(key.equals("widget_current_mileage") ? "ll_widget_battery" : "ll_widget_oil"));
      Drawable drawable =
          group instanceof ViewGroup && ((ViewGroup) group).getChildAt(0) instanceof ImageView
              ? ((ImageView) ((ViewGroup) group).getChildAt(0)).getDrawable()
              : null;
      if (drawable != null) {
        Drawable glyph =
            drawable.getConstantState() != null
                ? drawable.getConstantState().newDrawable().mutate()
                : drawable;
        glyph.setBounds(0, 0, px(12), px(12));
        title.append(' ');
        title.setSpan(
            new ImageSpan(glyph, ImageSpan.ALIGN_BASELINE),
            start,
            start + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
      title.append(texts.getOrDefault(key, "")).append("  ");
      title.setSpan(
          new AbsoluteSizeSpan(Math.round(sp(textSize))),
          start,
          title.length(),
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      title.setSpan(
          new ForegroundColorSpan(metricColor(key)),
          start,
          title.length(),
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      title.setSpan(
          new StyleSpan(Typeface.BOLD), start, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    if (metrics.isEmpty()) title.append(texts.getOrDefault("tv_widget_charge_desc", ""));
    rv.setTextViewText(id("widget_vehicle_name"), title);
    size("widget_vehicle_name", 10);
    integer("widget_vehicle_name", "setMaxLines", 2);
    integer("widget_vehicle_name", "setMinLines", 2);
    rv.setContentDescription(
        id("widget_vehicle_name"),
        name
            + ". "
            + charging
            + ". "
            + (battery.isEmpty() ? "" : Language.text(local, R.string.desc_battery, battery) + ". ")
            + texts.getOrDefault("widget_current_mileage", "")
            + "; "
            + texts.getOrDefault("widget_total_mileage", "")
            + ". "
            + (inlineStatus
                ? message.isEmpty()
                    ? fresh.text(local, System.currentTimeMillis(), style.staleMinutes)
                    : message
                : ""));
  }
}
