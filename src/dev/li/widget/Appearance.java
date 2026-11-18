package dev.li.widget;

import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import java.util.*;

final class Appearance {
  static final int DETAILED_HEADER_DP = 64;

  static float detailedNameSize(WidgetStyle style) {
    return style.font.equals("large") ? 24 : 22;
  }

  static int actionInset(int width) { return width < 300 ? 8 : 12; }
  static int actionDividerHeight(int rowHeight) { return Math.max(1, Math.min(36, rowHeight - 16)); }
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
  private final Map<String, Integer> resourceIds = new HashMap<>();
  private final Map<String, int[]> sourceColorVariants = new HashMap<>();
  private final int primary, secondary, icon, button, alarm;
  private Bitmap tailgateGlyph;

  private Appearance(
      Context c, RemoteViews original, View view, WidgetStyle s, int w, int h, Freshness f)
      throws Exception {
    local = Language.context(c);
    source = c.createPackageContext(State.SOURCE, 0);
    res = source.getResources();
    rv = original.clone();
    snapshot = view;
    style = s;
    fresh = f;
    dark = s.dark(c);
    widthDp = Math.max(1, w);
    heightDp = s.heightWithin(h);
    colors = WidgetColors.forWidget(c, s);
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

  private int labelLines(float available) {
    Paint paint = new Paint();
    paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
    paint.setTextSize(sp(style.fontSize()));
    for (int i = 1; i <= 4; i++) {
      String value = texts.getOrDefault("control_button_text" + i, "");
      if (value.contains("\n") || paint.measureText(value) > px(available - 1)) return 2;
    }
    return 1;
  }

  private int id(String name) {
    return resource(name, "id");
  }

  private int drawable(String name) {
    return resource(name, "drawable");
  }

  private int resource(String name, String type) {
    String key = type + ':' + name;
    Integer cached = resourceIds.get(key);
    if (cached == null) {
      cached = res.getIdentifier(name, type, State.SOURCE);
      resourceIds.put(key, cached);
    }
    return cached;
  }

  private int color(String name) {
    int n = resource(name, "color");
    return n == 0 ? 0 : source.getColor(n);
  }

  private boolean sourceColor(String name, int value) {
    int n = resource(name, "color");
    if (n == 0) return false;
    int[] variants = sourceColorVariants.get(name);
    if (variants == null) {
      int[] modes = {Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES};
      variants = new int[modes.length];
      for (int i = 0; i < modes.length; i++) {
        Configuration config = new Configuration(res.getConfiguration());
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | modes[i];
        variants[i] = source.createConfigurationContext(config).getColor(n);
      }
      sourceColorVariants.put(name, variants);
    }
    return variants[0] == value || variants[1] == value;
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
        : sourceColor("charge_10_text", original) ? colors.chargeText : primary;
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
        if (n.equals("ic_widget_tail") || n.equals("ic_widget_mxi_tail_off"))
          rv.setImageViewBitmap(id(name), tailgateGlyph());
        tint(name);
        return;
      }
    }
    // Active, warning and animated icons retain the original provider's state colors.
  }

  private Bitmap tailgateGlyph() {
    if (tailgateGlyph == null) {
      // RemoteViews belongs to Li Auto: a local resource ID would resolve in that package.
      Drawable glyph = local.getDrawable(R.drawable.preview_tailgate).mutate();
      int edge = px(32);
      tailgateGlyph = Bitmap.createBitmap(edge, edge, Bitmap.Config.ARGB_8888);
      glyph.setBounds(0, 0, edge, edge);
      glyph.draw(new Canvas(tailgateGlyph));
    }
    return tailgateGlyph;
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
    return render(c, original, view, s, width, height, fresh, false);
  }

  static RemoteViews render(Context c, RemoteViews original, View view, WidgetStyle style,
      int width, int height, Freshness fresh, boolean newReading) throws Exception {
    return render(c, original, view, style, width, height, fresh, newReading, false);
  }

  static RemoteViews render(Context c, RemoteViews original, View view, WidgetStyle style,
      int width, int height, Freshness fresh, boolean newReading, boolean chargeChanged) throws Exception {
    WidgetFormat display = style.format.presentation(style, width, height, c.getResources().getConfiguration().fontScale);
    if (!display.standard())
      return VariantAppearance.render(c, original, view, style, display, width, height, fresh, chargeChanged);
    Appearance a = new Appearance(c, original, view, style, width, height, fresh);
    if (display == WidgetFormat.LARGE && visible(view.findViewById(a.id("ll_widget_car"))))
      return a.overview(original, newReading, chargeChanged);
    return a.build(newReading, chargeChanged);
  }

  /** Retain the source PendingIntents and states; only restyle a selected row of its controls. */
  static RemoteViews actionRow(Context c, RemoteViews original, View snapshot, WidgetStyle style,
      int width, int height, int first, int last) throws Exception {
    Appearance a = new Appearance(c, original, snapshot, style, width, height, new Freshness("", 0));
    a.translate();
    if (a.id("widget_buttons") == 0 || a.id("control_button4") == 0)
      throw new IllegalStateException("Unsupported source controls");
    for (String hidden : new String[] {"widget_header", "widget_content", "card_theme_background_2", "re_widget_login"})
      a.hidden(hidden);
    a.integer("ll_widget_car", "setBackgroundResource", 0);
    a.padding("ll_widget_car", 0, 0, 0, 0);
    a.height("ll_widget_car", height);
    a.height("re_widget_content", height);
    a.height("widget_buttons", height);
    a.margin("widget_buttons", RemoteViews.MARGIN_LEFT, 0);
    a.margin("widget_buttons", RemoteViews.MARGIN_RIGHT, 0);
    a.margin("widget_buttons", RemoteViews.MARGIN_TOP, 0);
    a.margin("widget_buttons", RemoteViews.MARGIN_BOTTOM, 0);
    a.padding("widget_buttons", 0, 0, 0, 0);
    a.integer("widget_buttons", "setBackgroundResource", 0);
    a.actionDividers(first, last, height);
    int glyph = height >= 70 ? 34 : height >= 56 ? 30 : 24;
    int lines = height >= 74 ? 2 : 1;
    float labelWidth = (width - (last - first)) / (float) (last - first + 1) - 8;
    int shape = a.drawable("shape_widget_bg");
    for (int index = 1; index <= 4; index++) {
      String cell = "control_button" + index;
      if (index < first || index > last) { a.hidden(cell); continue; }
      a.height(cell, height);
      a.margin(cell, RemoteViews.MARGIN_LEFT, 0);
      a.margin(cell, RemoteViews.MARGIN_RIGHT, 0);
      a.padding(cell, 4, 2, 4, 2);
      String value = a.texts.getOrDefault("control_button_text" + index, "");
      boolean confirm = value.equals(Language.text(a.local, R.string.action_confirm));
      if (shape != 0) {
        a.integer(cell, "setBackgroundResource", shape);
        a.rv.setColorStateList(a.id(cell), "setBackgroundTintList", new ColorStateList(
            new int[][] {new int[] {android.R.attr.state_pressed}, new int[] {android.R.attr.state_focused}, new int[] {}},
            new int[] {a.colors.outline, a.colors.outline, confirm ? a.colors.confirmation : Color.TRANSPARENT}));
      }
      a.width("control_button_icon" + index, glyph);
      a.height("control_button_icon" + index, glyph);
      a.normalIcon("control_button_icon" + index);
      float labelSize = Math.min(style.fontSize(), (height - glyph - 7)
          / (1.25f * lines * c.getResources().getConfiguration().fontScale));
      a.size("control_button_text" + index, a.fitLabel(value, labelSize, labelWidth));
      a.integer("control_button_text" + index, "setMaxLines", lines);
      a.integer("control_button_text" + index, "setMinLines", lines);
      a.integer("control_button_text" + index, "setGravity", Gravity.CENTER);
      a.margin("control_button_text" + index, RemoteViews.MARGIN_TOP, 3);
      a.textColor("control_button_text" + index, confirm ? a.alarm : a.primary);
      View button = snapshot.findViewById(a.id(cell));
      if (button != null && !button.isEnabled()) a.rv.setFloat(a.id(cell), "setAlpha", .5f);
    }
    return a.rv;
  }

  private void actionDividers(int first, int last, int rowHeight) {
    for (int preceding = 1; preceding <= 3; preceding++) {
      String name = "card_theme_background_" + (preceding + 2);
      int dividerId = id(name);
      if (dividerId == 0) continue;
      rv.setViewVisibility(dividerId, preceding >= first && preceding < last ? View.VISIBLE : View.GONE);
      integer(name, "setBackgroundColor", colors.actionDivider);
      width(name, 1);
      height(name, actionDividerHeight(rowHeight));
    }
  }

  /** Preserve the source refresh PendingIntent, including its disabled state, without extracting it. */
  static RemoteViews refreshOverlay(Context c, RemoteViews original, View snapshot, WidgetStyle style,
      int width, int height) throws Exception {
    Appearance a = new Appearance(c, original, snapshot, style, width, height, new Freshness("", 0));
    View refresh = snapshot.findViewById(a.id("iv_widget_refresh"));
    if (!visible(refresh) || !refresh.hasOnClickListeners()) return null;
    for (String hidden : new String[] {"widget_content", "widget_buttons", "re_widget_login",
        "widget_logo", "widget_ll_name", "widget_update_time", "widget_iv_setting",
        "card_theme_background_1", "card_theme_background_2"}) a.hidden(hidden);
    for (String container : new String[] {"re_widget_content", "ll_widget_car", "widget_header"}) {
      a.width(container, width);
      a.height(container, height);
      a.padding(container, 0, 0, 0, 0);
      a.integer(container, "setBackgroundResource", 0);
      a.rv.setOnClickPendingIntent(a.id(container), null);
    }
    for (int edge : new int[] {RemoteViews.MARGIN_LEFT, RemoteViews.MARGIN_RIGHT,
        RemoteViews.MARGIN_TOP, RemoteViews.MARGIN_BOTTOM}) a.margin("widget_header", edge, 0);
    a.width("iv_widget_refresh", width);
    a.height("iv_widget_refresh", height);
    a.padding("iv_widget_refresh", 0, 0, 0, 0);
    a.rv.setImageViewResource(a.id("iv_widget_refresh"), 0);
    a.rv.setBoolean(a.id("iv_widget_refresh"), "setEnabled", refresh.isEnabled());
    a.rv.setContentDescription(a.id("iv_widget_refresh"), Language.text(a.local, R.string.desc_refresh));
    return a.rv;
  }

  private RemoteViews build(boolean newReading, boolean chargeChanged) {
    if (id("re_widget_content") == 0 || id("control_button1") == 0)
      throw new IllegalStateException("Unsupported source widget layout");
    translate();
    boolean compact = style.compact(heightDp),
        hasCar = visible(snapshot.findViewById(id("ll_widget_car")));
    String message =
        visible(snapshot.findViewById(id("widget_text")))
            ? texts.getOrDefault("widget_text", "")
            : "";
    long now = System.currentTimeMillis();
    boolean hasMessage = !message.trim().isEmpty();
    boolean stale = fresh.stale(now, style.staleMinutes);
    ChargeReading charge = ChargeReading.read(local, snapshot);
    boolean charging = charge != null && charge.charging;
    boolean showStatus = hasCar && (style.showTime || hasMessage || charging || stale);
    int side = actionInset(widthDp);
    float labelWidth = (widthDp - 2 * side - 3) / 4f - 6;
    int lines = labelLines(labelWidth);
    int footer = hasMessage ? 36 : showStatus ? (compact ? 24 : 28) : 12;
    int controls =
        Math.max(
            compact ? 64 : 68,
            4
                * (int)
                    Math.ceil(
                        (44
                                + 1.25 * lines
                                    * sp(style.fontSize())
                                    / local.getResources().getDisplayMetrics().density)
                            / 4));
    int top = compact ? 4 : 6, above = compact ? 0 : 2, below = compact ? 4 : 8;
    int header = compact ? 48 : DETAILED_HEADER_DP;
    int body = Math.max(24, heightDp - 8 - footer - top - header - above - below - controls);
    int actualHeight = 8 + footer + top + header + above + below + controls + body;
    boolean dense = heightDp < 192 || actualHeight > heightDp;
    if (dense) {
      header = 48;
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
    height("widget_header", header);
    margin("widget_header", RemoteViews.MARGIN_LEFT, 16);
    margin("widget_header", RemoteViews.MARGIN_RIGHT, 8);
    margin("widget_header", RemoteViews.MARGIN_TOP, top);
    width("widget_logo", 25);
    height("widget_logo", 11);
    size("widget_vehicle_name", compact || dense ? 16 : detailedNameSize(style));
    if (!compact && !dense) {
      int nameId = id("widget_vehicle_name");
      String name = texts.getOrDefault("widget_vehicle_name", "");
      SpannableString styledName = new SpannableString(name);
      styledName.setSpan(new TypefaceSpan("sans-serif-medium"), 0, name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      rv.setTextViewText(nameId, styledName);
      integer("widget_vehicle_name", "setMaxLines", 2);
    }
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
    for (String field : new String[] {"widget_current_mileage", "widget_total_mileage"})
      if (id(field) != 0) rv.setTextViewText(id(field), VariantAppearance.withUnit(texts.getOrDefault(field, "")));
    size("widget_current_mileage", metric);
    size("widget_total_mileage", metric);
    size("widget_battery_percent", 11);
    size("tv_widget_charge_desc", 11);
    integer("tv_widget_charge_desc", "setMaxLines", 2);
    height("widget_buttons", controls);
    padding("widget_buttons", 0, 0, 0, 0);
    margin("widget_buttons", RemoteViews.MARGIN_LEFT, side);
    margin("widget_buttons", RemoteViews.MARGIN_RIGHT, side);
    int originalShape = drawable("shape_widget_bg");
    if (originalShape != 0) {
      integer("widget_buttons", "setBackgroundResource", originalShape);
      rv.setColorStateList(id("widget_buttons"), "setBackgroundTintList", ColorStateList.valueOf(colors.panel));
    }
    hidden("card_theme_background_2");
    actionDividers(1, 4, controls);
    int glyph = dense && controls < 68 ? (controls < 56 ? 24 : 28) : 32;
    float labelSize = dense
        ? Math.min(style.fontSize(), (controls - glyph - 8)
            / (1.25f * lines * local.getResources().getConfiguration().fontScale))
        : style.fontSize();
    for (int i = 1; i <= 4; i++) {
      String n = "control_button" + i, value = texts.getOrDefault("control_button_text" + i, "");
      boolean confirm = value.equals(Language.text(local, R.string.action_confirm));
      height(n, controls);
      padding(n, 3, 2, 3, 2);
      if (originalShape != 0) {
        integer(n, "setBackgroundResource", originalShape);
        rv.setColorStateList(
            id(n),
            "setBackgroundTintList",
            new ColorStateList(
                new int[][] {new int[] {android.R.attr.state_pressed},
                    new int[] {android.R.attr.state_focused}, new int[] {}},
                new int[] {colors.outline, colors.outline, confirm ? colors.confirmation : Color.TRANSPARENT}));
      }
      width("control_button_icon" + i, glyph);
      height("control_button_icon" + i, glyph);
      normalIcon("control_button_icon" + i);
      size("control_button_text" + i, fitLabel(value, labelSize, labelWidth));
      integer("control_button_text" + i, "setMaxLines", lines);
      integer("control_button_text" + i, "setMinLines", lines);
      integer("control_button_text" + i, "setGravity", Gravity.CENTER);
      margin("control_button_text" + i, RemoteViews.MARGIN_TOP, 3);
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
    RemoteViews compactTitle = null;
    if (dense) {
      hidden("widget_content");
      hidden("widget_logo");
      hidden("card_theme_background_1");
      hidden("widget_iv_back");
      margin("widget_header", RemoteViews.MARGIN_LEFT, 12);
      margin("widget_header", RemoteViews.MARGIN_RIGHT, 8);
      if (hasCar) compactTitle = compactHeader(message, footer == 0 && showStatus);
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
    shell.setViewVisibility(R.id.overview_metrics, View.GONE);
    shell.setViewVisibility(R.id.overview_actions, View.GONE);
    boolean motion = WidgetMotion.enabled(style);
    boolean movingAvatar = hasCar && !compact && !dense && addAvatar(shell, top, header,
        motion && style.motion.equals("avatar"));
    shell.setViewVisibility(R.id.avatar_motion, movingAvatar ? View.VISIBLE : View.GONE);
    if (!movingAvatar) shell.setImageViewBitmap(R.id.animated_avatar, null);
    shell.setInt(R.id.shell, "setBackgroundResource", colors.surfaceRes);
    shell.setViewLayoutHeight(R.id.shell, actualHeight, TypedValue.COMPLEX_UNIT_DIP);
    shell.setViewVisibility(R.id.empty, View.GONE);
    shell.setViewVisibility(R.id.filled, View.VISIBLE);
    shell.setViewLayoutHeight(
        R.id.source_slot, actualHeight - 8 - (hasCar ? footer : 0), TypedValue.COMPLEX_UNIT_DIP);
    shell.removeAllViews(R.id.source_slot);
    shell.addView(R.id.source_slot, rv);
    shell.removeAllViews(R.id.compact_header_slot);
    shell.setViewVisibility(R.id.compact_header_slot, compactTitle == null ? View.GONE : View.VISIBLE);
    if (compactTitle != null) {
      shell.setViewLayoutWidth(R.id.compact_header_slot, Math.max(1, widthDp - 125), TypedValue.COMPLEX_UNIT_DIP);
      shell.setViewLayoutMargin(R.id.compact_header_slot, RemoteViews.MARGIN_LEFT, 12, TypedValue.COMPLEX_UNIT_DIP);
      shell.setViewLayoutMargin(R.id.compact_header_slot, RemoteViews.MARGIN_TOP, top, TypedValue.COMPLEX_UNIT_DIP);
      shell.addView(R.id.compact_header_slot, compactTitle);
    }
    setStatus(shell, showStatus && footer > 0, footer, dense, hasCar, message, charging,
        newReading, chargeChanged, movingAvatar);
    return shell;
  }

  private void setStatus(RemoteViews shell, boolean footerVisible, int footer, boolean dense, boolean hasCar,
      String message, boolean charging, boolean newReading, boolean chargeChanged, boolean movingAvatar) {
    long now = System.currentTimeMillis();
    boolean stale = fresh.stale(now, style.staleMinutes);
    boolean hasMessage = !message.trim().isEmpty();
    boolean motion = WidgetMotion.enabled(style);
    charging = charging && !hasMessage;
    shell.setViewVisibility(R.id.status_row, footerVisible ? View.VISIBLE : View.GONE);
    shell.setViewLayoutHeight(R.id.status_row, footer, TypedValue.COMPLEX_UNIT_DIP);
    shell.setTextViewTextSize(R.id.status_text, TypedValue.COMPLEX_UNIT_SP, dense ? 10 : 11);
    shell.setTextViewTextSize(R.id.status_dot, TypedValue.COMPLEX_UNIT_SP, dense ? 14 : 17);
    int stateColor = hasMessage || stale ? alarm : secondary;
    String status = hasMessage ? message : fresh.text(local, now, style.staleMinutes);
    if (footerVisible && charging) {
      String chargingLabel = Language.text(local, R.string.widget_charging);
      status = style.showTime || stale ? chargingLabel + " · " + status : chargingLabel;
      int boltColor = colors.chargeAccent;
      shell.setInt(R.id.charge_bolt, "setColorFilter", stale ? secondary : boltColor);
    }
    shell.setViewVisibility(R.id.status_dot, footerVisible && charging ? View.GONE : View.VISIBLE);
    shell.setViewVisibility(R.id.charge_motion, footerVisible && charging ? View.VISIBLE : View.GONE);
    if (footerVisible && charging && motion && chargeChanged)
      shell.setDisplayedChild(R.id.charge_motion, 0);
    shell.setTextViewText(R.id.status_text, status);
    shell.setTextColor(R.id.status_text, stateColor);
    shell.setTextColor(R.id.status_dot, stateColor);
    if (newReading && motion && hasCar && !hasMessage && !stale && !charging) {
      if (movingAvatar) shell.setDisplayedChild(R.id.avatar_motion, 0);
      else if (footerVisible) shell.setDisplayedChild(R.id.status_motion, 0);
    }
  }

  private RemoteViews overview(RemoteViews original, boolean newReading, boolean chargeChanged) throws Exception {
    translate();
    String message = visible(snapshot.findViewById(id("widget_text"))) ? texts.getOrDefault("widget_text", "") : "";
    ChargeReading charge = ChargeReading.read(local, snapshot);
    boolean charging = charge != null && charge.charging;
    boolean status = style.showTime || !message.isEmpty() || charging || fresh.stale(System.currentTimeMillis(), style.staleMinutes);
    int footer = status ? (message.isEmpty() ? 23 : 32) : 0;
    WidgetFormat.OverviewSize size = new WidgetFormat.OverviewSize(widthDp, heightDp, footer);
    int header = size.header, inset = size.inset, hero = size.hero, actions = size.actions;

    RemoteViews shell = new RemoteViews(local.getPackageName(), R.layout.widget_shell);
    shell.setInt(R.id.shell, "setBackgroundResource", colors.surfaceRes);
    shell.setViewLayoutHeight(R.id.shell, heightDp, TypedValue.COMPLEX_UNIT_DIP);
    shell.setViewVisibility(R.id.empty, View.GONE);
    shell.setViewVisibility(R.id.filled, View.VISIBLE);
    shell.setViewVisibility(R.id.compact_header_slot, View.GONE);
    // Keep header handlers in this clone. Each visible control row retains its own source handlers.
    for (String field : new String[] {"widget_content", "widget_buttons", "card_theme_background_2",
        "widget_update_time", "widget_iv_back", "re_widget_login"}) hidden(field);
    integer("ll_widget_car", "setBackgroundResource", 0);
    padding("ll_widget_car", 0, 0, 0, 0);
    for (String field : new String[] {"re_widget_content", "ll_widget_car", "widget_header"}) height(field, header);
    margin("widget_header", RemoteViews.MARGIN_TOP, 0);
    margin("widget_header", RemoteViews.MARGIN_LEFT, widthDp < 230 ? 8 : 16);
    margin("widget_header", RemoteViews.MARGIN_RIGHT, 4);
    for (String field : new String[] {"iv_widget_refresh", "widget_iv_setting"}) {
      width(field, 48); height(field, 48);
      padding(field, 13, 13, 13, 13); tint(field);
    }
    rv.setContentDescription(id("iv_widget_refresh"), Language.text(local, R.string.desc_refresh));
    rv.setContentDescription(id("widget_iv_setting"), Language.text(local, R.string.desc_settings));
    if (widthDp < 225) hidden("widget_iv_setting");
    hidden("card_theme_background_1");
    size("widget_vehicle_name", widthDp < 230 ? 16 : detailedNameSize(style));
    integer("widget_vehicle_name", "setMaxLines", 1);
    textColor("widget_vehicle_name", primary);
    width("widget_logo", 24); height("widget_logo", 12); tint("widget_logo");
    boolean moving = widthDp >= 210 && addAvatar(shell, 0, header,
        WidgetMotion.enabled(style) && style.motion.equals("avatar"));
    if (widthDp < 210) hidden("widget_logo");
    shell.setViewVisibility(R.id.avatar_motion, moving ? View.VISIBLE : View.GONE);
    shell.setViewLayoutHeight(R.id.source_slot, header, TypedValue.COMPLEX_UNIT_DIP);
    shell.removeAllViews(R.id.source_slot);
    shell.addView(R.id.source_slot, rv);

    shell.setViewVisibility(R.id.overview_metrics, View.VISIBLE);
    shell.setViewLayoutHeight(R.id.overview_metrics, hero, TypedValue.COMPLEX_UNIT_DIP);
    shell.removeAllViews(R.id.overview_metrics);
    shell.addView(R.id.overview_metrics, VariantAppearance.overviewMetrics(local, source, snapshot, style, widthDp, hero));
    int panelWidth = widthDp - 2 * inset;
    RemoteViews panel = VariantAppearance.actionPanel(local, original, snapshot, style, panelWidth, actions, "", size.rows, true);
    shell.setViewVisibility(R.id.overview_actions, View.VISIBLE);
    shell.setViewLayoutHeight(R.id.overview_actions, actions, TypedValue.COMPLEX_UNIT_DIP);
    shell.setViewPadding(R.id.overview_actions, px(inset), 0, px(inset), 0);
    shell.removeAllViews(R.id.overview_actions);
    shell.addView(R.id.overview_actions, panel);
    setStatus(shell, status, footer, widthDp < 230, true, message, charging, newReading, chargeChanged, moving);
    return shell;
  }

  private boolean addAvatar(RemoteViews shell, int top, int header, boolean moving) {
    int logo = id("widget_logo");
    if (logo == 0) return false;
    Bitmap avatar = WidgetAvatar.icon(local, style.avatar);
    if (avatar == null) return false;
    int side = widthDp < 280 ? 38 : 44;
    width("widget_logo", side);
    height("widget_logo", side);
    margin("widget_logo", RemoteViews.MARGIN_RIGHT, 9);
    hidden("card_theme_background_1");
    if (moving) {
      rv.setViewVisibility(logo, View.INVISIBLE);
      shell.setViewLayoutWidth(R.id.avatar_motion, side, TypedValue.COMPLEX_UNIT_DIP);
      shell.setViewLayoutHeight(R.id.avatar_motion, side, TypedValue.COMPLEX_UNIT_DIP);
      shell.setViewLayoutMargin(R.id.avatar_motion, RemoteViews.MARGIN_LEFT, 16, TypedValue.COMPLEX_UNIT_DIP);
      shell.setViewLayoutMargin(R.id.avatar_motion, RemoteViews.MARGIN_TOP, top + (header - side) / 2f,
          TypedValue.COMPLEX_UNIT_DIP);
      shell.setImageViewBitmap(R.id.animated_avatar, avatar);
    } else {
      rv.setImageViewBitmap(logo, avatar);
      rv.setColorStateList(logo, "setImageTintList", (ColorStateList) null);
      // The source uses SRC_ATOP on this ImageView. Transparent clears our icon color filter.
      rv.setInt(logo, "setColorFilter", Color.TRANSPARENT);
    }
    return moving;
  }

  private RemoteViews compactHeader(String message, boolean inlineStatus) {
    // The visual overlay is non-interactive. The original name/refresh/settings handlers stay underneath.
    String name = texts.getOrDefault("widget_vehicle_name", "Li Auto");
    String battery = style.showBattery && visible(snapshot.findViewById(id("widget_battery_percent")))
        ? texts.getOrDefault("widget_battery_percent", "") : "";
    String charging = visible(snapshot.findViewById(id("tv_widget_charge_desc")))
        ? texts.getOrDefault("tv_widget_charge_desc", "") : "";
    String headline = (charging.isEmpty() ? name : charging) + (battery.isEmpty() ? "" : " · " + battery);
    boolean warning = !message.isEmpty() || fresh.stale(System.currentTimeMillis(), style.staleMinutes);
    if (inlineStatus) {
      String stamp = fresh.compactText(local, System.currentTimeMillis(), style.staleMinutes);
      headline = !message.isEmpty() ? message : warning ? stamp : headline + " · " + stamp;
    }
    RemoteViews header = new RemoteViews(local.getPackageName(), R.layout.widget_compact_header);
    SpannableString title = new SpannableString(headline);
    int batteryStart = battery.isEmpty() ? -1 : headline.indexOf(battery);
    if (batteryStart >= 0) title.setSpan(new ForegroundColorSpan(metricColor("widget_battery_percent")),
        batteryStart, batteryStart + battery.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    header.setTextViewText(R.id.compact_name, title);
    header.setTextColor(R.id.compact_name, inlineStatus && warning ? alarm : secondary);
    String[] fields = {"widget_current_mileage", "widget_total_mileage"};
    int[] cells = {R.id.compact_electric_cell, R.id.compact_fuel_cell};
    int[] icons = {R.id.compact_electric_icon, R.id.compact_fuel_icon};
    int[] numbers = {R.id.compact_electric, R.id.compact_fuel};
    int count = 0;
    for (String key : fields) if (visible(snapshot.findViewById(id(key)))) count++;
    float available = (Math.max(1, widthDp - 125) - (count > 1 ? 6 : 0)) / (float)Math.max(1, count) - 14;
    StringBuilder description = new StringBuilder(name).append(". ").append(headline);
    for (int i = 0; i < fields.length; i++) {
      boolean shown = visible(snapshot.findViewById(id(fields[i])));
      header.setViewVisibility(cells[i], shown ? View.VISIBLE : View.GONE);
      String value = texts.getOrDefault(fields[i], "");
      header.setTextViewText(numbers[i], VariantAppearance.withUnit(value));
      header.setTextViewTextSize(numbers[i], TypedValue.COMPLEX_UNIT_SP, fitLabel(value, 18, available));
      header.setTextColor(numbers[i], metricColor(fields[i]));
      header.setInt(icons[i], "setColorFilter", icon);
      if (shown) description.append(". ").append(Language.text(local,
          i == 0 ? (value.contains("%") ? R.string.battery_level : R.string.electric_range) : R.string.fuel_range))
          .append(": ").append(value);
    }
    header.setViewVisibility(R.id.compact_message, count == 0 ? View.VISIBLE : View.GONE);
    header.setTextViewText(R.id.compact_message, charging);
    header.setTextColor(R.id.compact_message, secondary);
    if (inlineStatus) description.append(". ").append(message.isEmpty()
        ? fresh.text(local, System.currentTimeMillis(), style.staleMinutes) : message);
    // Keep the original TextView's hit area and accessibility label; draw the same information above it.
    rv.setTextViewText(id("widget_vehicle_name"), headline + "\n" + texts.getOrDefault(fields[0], "")
        + " " + texts.getOrDefault(fields[1], ""));
    textColor("widget_vehicle_name", Color.TRANSPARENT);
    size("widget_vehicle_name", 10);
    integer("widget_vehicle_name", "setMinLines", 2);
    integer("widget_vehicle_name", "setMaxLines", 2);
    rv.setContentDescription(id("widget_vehicle_name"), description.toString());
    return header;
  }
}
