package dev.li.widget;

import android.Manifest;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
  private static final int BIND = 41;

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(Language.context(base));
  }

  private String t(int id, Object... args) {
    return Language.text(this, id, args);
  }

  private void chooseLanguage(String code) {
    if (code.equals(Language.code(this))) return;
    Language.select(this, code);
    recreate();
  }

  private int target = -1, configured = -1, tab = 0;
  private WidgetStyle style;
  private LinearLayout root, settings;
  private FrameLayout live;
  private boolean dark;
  private WidgetColors colors;
  private int canvas, surface, text, muted, line;
  private final BroadcastReceiver update =
      new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
          if (colorsChanged()) {
            buildUi();
            return;
          }
          updatePreview();
          if (tab == 2) buildSettings();
        }
      };

  private int dp(float n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  protected boolean connected() {
    return State.connected(this);
  }

  private TextView label(String value, float size, int color) {
    TextView v = new TextView(this);
    v.setText(value);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setTypeface(Typeface.create("sans-serif", 0));
    v.setIncludeFontPadding(false);
    v.setLineSpacing(dp(3), 1);
    return v;
  }

  private GradientDrawable shape(int color, int radius, int stroke) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    if (stroke != 0) d.setStroke(dp(1), stroke);
    return d;
  }

  private void space(LinearLayout parent, int h) {
    parent.addView(new Space(this), new LinearLayout.LayoutParams(1, dp(h)));
  }

  private void heading(String value) {
    TextView v = label(value, 11, muted);
    v.setLetterSpacing(.1f);
    settings.addView(v);
    space(settings, 12);
  }

  private void explanation(String value) {
    settings.addView(label(value, 12, muted));
    space(settings, 20);
  }

  private Button action(String value, Runnable click, boolean primary) {
    Button b = new Button(this);
    b.setText(value);
    b.setTextSize(14);
    b.setAllCaps(false);
    b.setTextColor(primary ? canvas : text);
    b.setBackground(
        new RippleDrawable(
            ColorStateList.valueOf(dark ? 0x33ffffff : 0x22000000),
            shape(primary ? text : surface, 16, primary ? 0 : line),
            null));
    b.setPadding(dp(14), 0, dp(14), 0);
    b.setMinHeight(dp(48));
    b.setOnClickListener(v -> click.run());
    return b;
  }

  private void fullAction(String title, Runnable click) {
    settings.addView(action(title, click, false), new LinearLayout.LayoutParams(-1, dp(50)));
    space(settings, 12);
  }

  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    configured = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
    setResult(RESULT_CANCELED);
    if (!State.ownsWidget(this, configured)) configured = -1;
    int[] ids = State.ownIds(this);
    for (int id : ids) WidgetStyle.read(this, id);
    target =
        saved != null
            ? saved.getInt("target", -1)
            : configured >= 0 ? configured : ids.length > 0 ? ids[0] : -1;
    if (target >= 0 && !State.ownsWidget(this, target)) target = -1;
    tab = saved == null ? 0 : Math.max(0, Math.min(2, saved.getInt("tab", 0)));
    buildUi();
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    out.putInt("target", target);
    out.putInt("tab", tab);
    super.onSaveInstanceState(out);
  }

  private boolean colorsChanged() {
    return colors == null
        || colors.signature() != new WidgetColors(this, WidgetStyle.read(this, target)).signature();
  }

  private void buildUi() {
    style = WidgetStyle.read(this, target);
    dark = style.dark(this);
    colors = new WidgetColors(this, style);
    canvas = colors.background;
    surface = colors.panel;
    text = colors.primary;
    muted = colors.secondary;
    line = colors.outline;
    getWindow().setDecorFitsSystemWindows(false);
    root = new LinearLayout(this);
    root.setOrientation(1);
    root.setBackgroundColor(canvas);
    root.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
          v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
          return insets;
        });
    LinearLayout top = new LinearLayout(this);
    top.setGravity(Gravity.CENTER_VERTICAL);
    top.setPadding(dp(20), dp(12), dp(20), dp(8));
    LinearLayout titles = new LinearLayout(this);
    titles.setOrientation(1);
    TextView title = label(t(R.string.app_name), 24, text);
    title.setTypeface(Typeface.create("sans-serif-medium", 0));
    titles.addView(title);
    space(titles, 7);
    titles.addView(
        label(
            connected() ? t(R.string.connected) : t(R.string.connect_title),
            12,
            connected() ? (dark ? 0xff9ebca7 : 0xff456f52) : muted));
    top.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
    top.addView(
        action(t(R.string.li_auto), () -> openSource(), false),
        new LinearLayout.LayoutParams(dp(90), dp(48)));
    root.addView(top);
    LinearLayout select = new LinearLayout(this);
    select.setPadding(dp(12), 0, dp(12), 0);
    final ArrayList<Integer> ids = new ArrayList<>();
    ids.add(-1);
    for (int id : State.ownIds(this)) ids.add(id);
    if (target >= 0 && !ids.contains(target)) ids.add(target);
    String[] names = new String[ids.size()];
    names[0] = t(R.string.new_widget);
    for (int i = 1; i < ids.size(); i++) {
      String theme = WidgetStyle.read(this, ids.get(i)).theme;
      int label =
          theme.equals("dark")
              ? R.string.theme_black
              : theme.equals("light")
                  ? R.string.theme_light
                  : theme.equals("dim") ? R.string.theme_dim : R.string.theme_system;
      names[i] = t(R.string.widget_number, i, t(label));
    }

    Spinner spinner = new Spinner(this);
    spinner.setBackgroundTintList(ColorStateList.valueOf(muted));
    ArrayAdapter<String> adapter =
        new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, names) {
          @Override
          public View getView(int p, View v, android.view.ViewGroup group) {
            TextView t = (TextView) super.getView(p, v, group);
            t.setTextColor(muted);
            t.setTextSize(12);
            return t;
          }
        };
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);
    spinner.setSelection(Math.max(0, ids.indexOf(target)));
    select.addView(spinner, new LinearLayout.LayoutParams(-1, dp(44)));
    root.addView(select);
    spinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(AdapterView<?> p) {}

          public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
            int next = ids.get(pos);
            if (next != target) {
              target = next;
              buildUi();
            }
          }
        });
    LinearLayout preview = new LinearLayout(this);
    preview.setOrientation(1);
    preview.setPadding(dp(16), dp(4), dp(16), dp(10));
    live = new FrameLayout(this);
    preview.addView(live, new LinearLayout.LayoutParams(-1, -2));
    space(preview, 8);
    TextView format =
        label(
            previewHeight() < 232 ? t(R.string.compact_format) : t(R.string.detailed_format),
            10,
            muted);
    format.setLetterSpacing(.1f);
    format.setGravity(Gravity.CENTER);
    preview.addView(format);
    root.addView(preview);
    LinearLayout tabs = new LinearLayout(this);
    tabs.setPadding(dp(16), 0, dp(16), dp(8));
    String[] labels = {t(R.string.tab_appearance), t(R.string.tab_data), t(R.string.tab_buttons)};
    for (int i = 0; i < 3; i++) {
      final int index = i;
      Button b =
          action(
              labels[i],
              () -> {
                tab = index;
                buildUi();
              },
              i == tab);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1);
      if (i < 2) lp.rightMargin = dp(6);
      tabs.addView(b, lp);
    }
    root.addView(tabs);
    ScrollView scroll = new ScrollView(this);
    scroll.setClipToPadding(false);
    settings = new LinearLayout(this);
    settings.setOrientation(1);
    settings.setPadding(dp(20), dp(16), dp(20), dp(12));
    scroll.addView(settings);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    LinearLayout bottom = new LinearLayout(this);
    bottom.setPadding(dp(16), dp(8), dp(16), dp(12));
    bottom.addView(
        action(
            !connected()
                ? t(R.string.connect)
                : target < 0 ? t(R.string.add_widget) : t(R.string.done),
            () -> finishOrPin(),
            true),
        new LinearLayout.LayoutParams(-1, dp(52)));
    root.addView(bottom);
    setContentView(root);
    WindowInsetsController controller = getWindow().getInsetsController();
    if (controller != null)
      controller.setSystemBarsAppearance(
          dark
              ? 0
              : WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                  | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
          WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
              | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
    buildSettings();
    live.post(
        () -> {
          updatePreview();
          if (tab == 2) buildSettings();
        });
  }

  private int previewHeight() {
    return target >= 0
        ? style.heightWithin(WidgetStyle.size(this, target)[1])
        : style.previewHeight();
  }

  protected RemoteViews previewFor(int width, int height) {
    return BridgeService.preview(this, target, width, height);
  }

  private void updatePreview() {
    if (live == null) return;
    int width =
        Math.max(
            1,
            Math.round(
                (live.getWidth() > 0
                        ? live.getWidth()
                        : getResources().getDisplayMetrics().widthPixels - dp(32))
                    / getResources().getDisplayMetrics().density));
    if (target >= 0) width = Math.min(width, WidgetStyle.size(this, target)[0]);
    try {
      View v = previewFor(width, previewHeight()).apply(this, live);
      live.removeAllViews();
      live.addView(v, new FrameLayout.LayoutParams(dp(width), -2, Gravity.CENTER_HORIZONTAL));
    } catch (Exception e) {
      live.removeAllViews();
      live.addView(label(t(R.string.restore_widget), 14, muted));
    }
  }

  private void save(boolean rebuild) {
    style.save(this, target);
    if (connected()) State.start(this);
    if (rebuild) buildUi();
    else updatePreview();
  }

  private interface Pick {
    void choose(String value);
  }

  private void choices(String[] names, String[] values, String selected, Pick onPick) {
    LinearLayout row = new LinearLayout(this);
    for (int i = 0; i < names.length; i++) {
      final String value = values[i];
      Button b = action(names[i], () -> onPick.choose(value), value.equals(selected));
      b.setTextSize(11);
      b.setPadding(dp(4), 0, dp(4), 0);
      b.setSelected(value.equals(selected));
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1);
      if (i < names.length - 1) lp.rightMargin = dp(6);
      row.addView(b, lp);
    }
    settings.addView(row);
    space(settings, 18);
  }

  private void toggle(
      String title, String detail, boolean checked, java.util.function.Consumer<Boolean> onChange) {
    LinearLayout row = new LinearLayout(this);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(dp(12), dp(14), dp(12), dp(14));
    row.setBackground(shape(surface, 16, 0));
    LinearLayout copy = new LinearLayout(this);
    copy.setOrientation(1);
    copy.addView(label(title, 14, text));
    space(copy, 6);
    copy.addView(label(detail, 11, muted));
    row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
    Switch s = new Switch(this);
    s.setContentDescription(title);
    s.setMinWidth(dp(56));
    s.setMinHeight(dp(48));
    s.setChecked(checked);
    s.setThumbTintList(ColorStateList.valueOf(colors.icon));
    s.setTrackTintList(
        new ColorStateList(
            new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
            new int[] {colors.secondary, colors.outline}));
    row.addView(s);
    s.setOnCheckedChangeListener((v, value) -> onChange.accept(value));
    settings.addView(row);
    space(settings, 10);
  }

  private void buildSettings() {
    if (settings == null) return;
    settings.removeAllViews();
    if (tab == 0) {
      heading(t(R.string.language_heading));
      choices(
          new String[] {t(R.string.language_ru), t(R.string.language_en)},
          new String[] {"ru", "en"},
          Language.code(this),
          this::chooseLanguage);
      explanation(t(R.string.language_hint));
      heading(t(R.string.theme_heading));
      choices(
          new String[] {
            t(R.string.theme_light),
            t(R.string.theme_dim),
            t(R.string.theme_black),
            t(R.string.theme_system)
          },
          new String[] {"light", "dim", "dark", "system"},
          style.theme,
          value -> {
            style.theme = value;
            save(true);
          });
      if (!style.theme.equals("dark")) {
        heading(t(R.string.colors_heading));
        choices(
            new String[] {t(R.string.colors_system), t(R.string.colors_neutral)},
            new String[] {"system", "neutral"},
            style.palette,
            value -> {
              style.palette = value;
              save(true);
            });
        explanation(t(R.string.colors_hint));
      } else explanation(t(R.string.black_hint));
      heading(t(R.string.layout_heading));
      choices(
          new String[] {
            t(R.string.layout_auto), t(R.string.layout_compact), t(R.string.layout_detailed)
          },
          new String[] {"auto", "compact", "detailed"},
          style.layout,
          value -> {
            style.layout = value;
            save(true);
          });
      explanation(t(R.string.layout_hint));
      heading(t(R.string.labels_heading));
      choices(
          new String[] {t(R.string.font_normal), t(R.string.font_large)},
          new String[] {"normal", "large"},
          style.font,
          value -> {
            style.font = value;
            save(true);
          });
      explanation(t(R.string.labels_hint));
      fullAction(
          t(R.string.reset_style),
          () -> {
            style = new WidgetStyle();
            save(true);
          });
    } else if (tab == 1) {
      heading(t(R.string.information_heading));
      toggle(
          t(R.string.show_time),
          t(R.string.show_time_hint),
          style.showTime,
          value -> {
            style.showTime = value;
            save(false);
          });
      toggle(
          t(R.string.show_battery),
          t(R.string.show_battery_hint),
          style.showBattery,
          value -> {
            style.showBattery = value;
            save(false);
          });
      space(settings, 14);
      heading(t(R.string.stale_heading));
      choices(
          new String[] {
            t(R.string.stale_15), t(R.string.stale_30), t(R.string.stale_60), t(R.string.stale_120)
          },
          new String[] {"15", "30", "60", "120"},
          String.valueOf(style.staleMinutes),
          value -> {
            style.staleMinutes = Integer.parseInt(value);
            save(false);
            buildSettings();
          });
      explanation(t(R.string.freshness_hint));
      fullAction(t(R.string.open_source), () -> openSource());
      explanation(t(R.string.notification_hint));
      fullAction(
          t(R.string.disconnect_data),
          () ->
              new AlertDialog.Builder(this)
                  .setTitle(t(R.string.disconnect_title))
                  .setMessage(t(R.string.disconnect_hint))
                  .setNegativeButton(t(R.string.cancel), null)
                  .setPositiveButton(t(R.string.disconnect), (d, w) -> disconnect())
                  .show());
    } else {
      heading(t(R.string.panel_heading));
      showButtons();
      fullAction(t(R.string.edit_buttons), () -> openButtonSettings());
      explanation(t(R.string.buttons_hint));
    }
  }

  private void showButtons() {
    try {
      android.content.res.Resources r =
          getPackageManager().getResourcesForApplication(State.SOURCE);
      View car = live.findViewById(r.getIdentifier("ll_widget_car", "id", State.SOURCE));
      if (!Appearance.visible(car)) {
        explanation(t(R.string.panel_sign_in));
        return;
      }
      for (int i = 1; i <= 4; i++) {
        TextView original =
            live.findViewById(r.getIdentifier("control_button_text" + i, "id", State.SOURCE));
        ImageView originalIcon =
            live.findViewById(r.getIdentifier("control_button_icon" + i, "id", State.SOURCE));
        if (original == null) continue;
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));
        row.setBackground(shape(surface, 16, 0));
        ImageView icon = new ImageView(this);
        if (originalIcon != null && originalIcon.getDrawable() != null) {
          Drawable d = originalIcon.getDrawable();
          icon.setImageDrawable(
              d.getConstantState() != null ? d.getConstantState().newDrawable().mutate() : d);
          icon.setColorFilter(text);
        }
        row.addView(icon, new LinearLayout.LayoutParams(dp(26), dp(26)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(1);
        copy.setPadding(dp(14), 0, 0, 0);
        String name = original.getText().toString();
        copy.addView(label(name.replace('\n', ' '), 14, text));
        space(copy, 5);
        copy.addView(label(Translations.description(this, name), 11, muted));
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        settings.addView(row);
        space(settings, 8);
      }
      space(settings, 12);
    } catch (Exception e) {
      explanation(t(R.string.panel_connect));
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (Build.VERSION.SDK_INT >= 33)
      registerReceiver(update, new IntentFilter(State.UPDATE), Context.RECEIVER_NOT_EXPORTED);
    else registerReceiver(update, new IntentFilter(State.UPDATE));
    if (colorsChanged()) buildUi();
    if (connected()) State.start(this);
    if (live != null) live.post(() -> updatePreview());
  }

  @Override
  protected void onPause() {
    unregisterReceiver(update);
    super.onPause();
  }

  private void openButtonSettings() {
    try {
      startActivity(buttonSettingsIntent());
    } catch (ActivityNotFoundException | SecurityException e) {
      new AlertDialog.Builder(this)
          .setTitle(t(R.string.source_required))
          .setMessage(t(R.string.source_version_editor))
          .setPositiveButton(t(R.string.ok), null)
          .show();
    }
  }

  static Intent buttonSettingsIntent() {
    return new Intent(Intent.ACTION_VIEW, Uri.parse("lixiang://com.chehejia.app/card/setting"))
        .setPackage(State.SOURCE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
  }

  private void connectSource() {
    if (connected()) {
      State.start(this);
      return;
    }
    ComponentName component = State.source(this);
    if (component == null) {
      new AlertDialog.Builder(this)
          .setTitle(t(R.string.install_source))
          .setMessage(t(R.string.source_version_hint))
          .setPositiveButton(t(R.string.ok), null)
          .show();
      return;
    }
    AppWidgetHost host = new AppWidgetHost(this, State.HOST);
    int old = State.prefs(this).getInt("pending_id", -1);
    if (old != -1) host.deleteAppWidgetId(old);
    int id = host.allocateAppWidgetId();
    State.prefs(this).edit().putInt("pending_id", id).apply();
    if (AppWidgetManager.getInstance(this).bindAppWidgetIdIfAllowed(id, component, State.options()))
      completeBinding(id);
    else {
      Intent bind =
          new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
              .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
              .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, component)
              .putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, State.options());
      try {
        startActivityForResult(bind, BIND);
      } catch (RuntimeException e) {
        host.deleteAppWidgetId(id);
        State.prefs(this).edit().remove("pending_id").apply();
        Toast.makeText(this, t(R.string.bind_error), Toast.LENGTH_LONG).show();
      }
    }
  }

  private void completeBinding(int id) {
    AppWidgetProviderInfo info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id);
    if (!State.validSource(info)) return;
    State.prefs(this).edit().putInt("source_id", id).remove("pending_id").apply();
    Freshness.clear(this);
    State.start(this);
    if (Build.VERSION.SDK_INT >= 33
        && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED)
      requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 72);
    buildUi();
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request != BIND) return;
    int id = State.prefs(this).getInt("pending_id", -1);
    if (id == -1) return;
    if (result == RESULT_OK) completeBinding(id);
    else {
      new AppWidgetHost(this, State.HOST).deleteAppWidgetId(id);
      State.prefs(this).edit().remove("pending_id").apply();
    }
  }

  private void finishOrPin() {
    style.save(this, target);
    if (!connected()) {
      connectSource();
      return;
    }
    State.start(this);
    if (target >= 0) {
      if (configured >= 0)
        setResult(
            RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, configured));
      finish();
      return;
    }
    AppWidgetManager m = AppWidgetManager.getInstance(this);
    if (m.isRequestPinAppWidgetSupported())
      m.requestPinAppWidget(new ComponentName(this, LiWidgetProvider.class), null, null);
    else
      new AlertDialog.Builder(this)
          .setMessage(t(R.string.add_manually))
          .setPositiveButton(t(R.string.ok), null)
          .show();
  }

  private void openSource() {
    Intent i = getPackageManager().getLaunchIntentForPackage(State.SOURCE);
    if (i == null) Toast.makeText(this, t(R.string.install_source), Toast.LENGTH_SHORT).show();
    else startActivity(i);
  }

  private void disconnect() {
    stopService(new Intent(this, BridgeService.class));
    int id = State.sourceId(this);
    if (id != -1) new AppWidgetHost(this, State.HOST).deleteAppWidgetId(id);
    State.prefs(this).edit().remove("source_id").apply();
    Freshness.clear(this);
    LiWidgetProvider.showEmpty(this, t(R.string.connect_title), t(R.string.restore_connection));
    buildUi();
  }
}
