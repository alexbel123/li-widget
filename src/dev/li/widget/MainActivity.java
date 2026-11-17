package dev.li.widget;

import android.Manifest;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.*;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
  private static final int BIND = 41, CHANGE_AVATAR = 42;
  private int target = -1, configured = -1, tab = 0;
  private WidgetStyle style;
  private WidgetColors colors;
  private SettingsLook ui;
  private ThemeWatcher themeWatcher;
  private ScrollView scroll;
  private LinearLayout settings;
  private FrameLayout live;
  private TextView sectionTitle;
  private boolean previewIsSample;
  private Button customize;
  private final Button[] tabs = new Button[3];

  private final BroadcastReceiver update = new BroadcastReceiver() {
    @Override public void onReceive(Context context, Intent intent) {
      if (colorsChanged()) {
        buildUi();
        return;
      }
      updatePreview();
      if (tab == 2) buildSettings();
    }
  };

  @Override protected void attachBaseContext(Context base) {
    super.attachBaseContext(Language.context(base));
  }

  private String t(int id, Object... args) { return Language.text(this, id, args); }
  private int dp(float value) { return ui.dp(value); }

  protected boolean connected() {
    return State.connected(this);
  }

  private void chooseLanguage(String code) {
    if (code.equals(Language.code(this))) return;
    Language.select(this, code);
    recreate();
  }

  @Override public void onCreate(Bundle saved) {
    super.onCreate(saved);
    configured = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
    setResult(RESULT_CANCELED);
    if (!State.ownsWidget(this, configured)) configured = -1;
    int[] ids = State.ownIds(this);
    for (int id : ids) WidgetStyle.read(this, id);
    target = saved != null ? saved.getInt("target", -1)
        : configured >= 0 ? configured : ids.length > 0 ? ids[0] : -1;
    if (target >= 0 && !State.ownsWidget(this, target)) target = -1;
    tab = saved == null ? 0 : Math.max(0, Math.min(2, saved.getInt("tab", 0)));
    buildUi();
    if (saved != null) restoreScroll(saved.getInt("scroll", 0));
  }

  @Override protected void onSaveInstanceState(Bundle state) {
    state.putInt("target", target);
    state.putInt("tab", tab);
    state.putInt("scroll", scroll == null ? 0 : scroll.getScrollY());
    super.onSaveInstanceState(state);
  }

  private boolean colorsChanged() {
    return colors == null || colors.signature() != new WidgetColors(this, WidgetStyle.read(this, target)).signature();
  }

  private void buildUi() {
    int previousScroll = scroll == null ? 0 : scroll.getScrollY();
    style = WidgetStyle.read(this, target);
    colors = new WidgetColors(this, style);
    ui = new SettingsLook(this, colors);
    getWindow().setDecorFitsSystemWindows(false);
    LinearLayout root = ui.column();
    root.setBackground(ui.backdrop());
    root.setOnApplyWindowInsetsListener((view, insets) -> {
      android.graphics.Insets bars =
          insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
      view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
      return insets;
    });
    root.addView(toolbar());
    FrameLayout navigation = new FrameLayout(this);
    navigation.setPadding(dp(16), dp(2), dp(16), dp(8));
    navigation.addView(tabBar(), new FrameLayout.LayoutParams(-1, -2));
    root.addView(navigation);

    scroll = new ScrollView(this);
    scroll.setVerticalScrollBarEnabled(false);
    LinearLayout content = ui.column();
    content.setPadding(dp(16), 0, dp(16), dp(10));
    scroll.addView(content);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

    content.addView(previewHeading());
    // This preview can be scrolled, but taps and accessibility actions must not dispatch car commands.
    live = new FrameLayout(this) {
      @Override public boolean onInterceptTouchEvent(MotionEvent event) { return true; }
      @Override public boolean onTouchEvent(MotionEvent event) { return true; }
    };
    live.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    live.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    add(content, live, 7);
    settings = ui.column();
    add(content, settings, 10);

    LinearLayout footer = ui.column();
    footer.setPadding(dp(16), dp(5), dp(16), dp(8));
    customize = ui.button(t(R.string.edit_buttons), false, this::openButtonSettings);
    customize.setTextColor(ui.accent);
    customize.setTextSize(13);
    customize.setBackground(ui.ripple(null, 13));
    footer.addView(customize, new LinearLayout.LayoutParams(-1, -2));
    footer.addView(ui.button(!connected() ? t(R.string.connect)
        : target < 0 ? t(R.string.add_widget) : t(R.string.done), true, this::finishOrPin),
        new LinearLayout.LayoutParams(-1, -2));
    root.addView(footer);
    setContentView(root);
    root.requestApplyInsets();
    ui.applySystemBars(root);

    updatePreview();
    showTab();
    live.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
      if (right - left != oldRight - oldLeft && right > left && live == view) view.post(this::updatePreview);
    });
    restoreScroll(previousScroll);
  }

  private void restoreScroll(int position) {
    if (position > 0) scroll.post(() -> scroll.scrollTo(0, position));
  }

  private LinearLayout toolbar() {
    LinearLayout row = new LinearLayout(this);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setMinimumHeight(dp(52));
    row.setPadding(dp(16), dp(6), dp(16), dp(4));
    TextView name = ui.text(t(R.string.app_name), 21, ui.ink, true);
    row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
    LinearLayout status = new LinearLayout(this);
    status.setGravity(Gravity.CENTER_VERTICAL);
    status.setPadding(dp(7), dp(5), 0, dp(5));
    View dot = new View(this);
    dot.setBackground(ui.solid(connected() ? ui.accent : ui.muted, 4, 0));
    status.addView(dot, new LinearLayout.LayoutParams(dp(6), dp(6)));
    TextView label = ui.text(t(connected() ? R.string.connected : R.string.connection_off), 11, ui.muted, false);
    LinearLayout.LayoutParams labelSize = new LinearLayout.LayoutParams(-2, -2);
    labelSize.setMarginStart(dp(6));
    label.setMaxWidth(dp(153));
    status.addView(label, labelSize);
    row.addView(status, new LinearLayout.LayoutParams(-2, -2));
    return row;
  }

  private LinearLayout previewHeading() {
    LinearLayout row = new LinearLayout(this);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setMinimumHeight(dp(34));
    sectionTitle = ui.text("", 14, ui.ink, true);
    sectionTitle.setAccessibilityHeading(true);
    LinearLayout.LayoutParams heading = new LinearLayout.LayoutParams(0, -2, 1);
    heading.setMarginEnd(dp(8));
    row.addView(sectionTitle, heading);

    final int[] ids = State.ownIds(this);
    if (ids.length > 0) {
      String[] names = new String[ids.length + 1];
      names[0] = t(R.string.new_widget);
      int selection = 0;
      for (int i = 0; i < ids.length; i++) {
        WidgetStyle item = WidgetStyle.read(this, ids[i]);
        names[i + 1] = t(R.string.widget_number, i + 1, t(item.format.title));
        if (target == ids[i]) selection = i + 1;
      }
      Button chooser = ui.button(names[selection] + "  ›", false,
          () -> pick(t(R.string.editor_target), names, targetIndex(ids), index -> {
            target = index == 0 ? -1 : ids[index - 1];
            scroll.scrollTo(0, 0);
            buildUi();
          }));
      chooser.setTextSize(12);
      chooser.setMaxLines(2);
      chooser.setMaxWidth(dp(getResources().getConfiguration().screenWidthDp < 360 ? 132 : 172));
      chooser.setEllipsize(TextUtils.TruncateAt.END);
      row.addView(chooser);
    }
    return row;
  }

  private int targetIndex(int[] ids) {
    for (int i = 0; i < ids.length; i++) if (ids[i] == target) return i + 1;
    return 0;
  }

  private LinearLayout tabBar() {
    LinearLayout row = new LinearLayout(this);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setBackground(ui.track());
    row.setPadding(dp(3), dp(3), dp(3), dp(3));
    row.setBaselineAligned(false);
    String[] labels = {t(R.string.tab_appearance), t(R.string.tab_data), t(R.string.tab_buttons)};
    for (int i = 0; i < tabs.length; i++) {
      final int index = i;
      tabs[i] = ui.button(labels[i], false, () -> {
        if (tab == index) return;
        tab = index;
        showTab();
        scroll.scrollTo(0, 0);
      });
      tabs[i].setTextSize(13);
      tabs[i].setPadding(dp(4), dp(6), dp(4), dp(6));
      LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(0, -1, 1);
      if (i > 0) size.setMarginStart(dp(2));
      row.addView(tabs[i], size);
    }
    return row;
  }

  private void showTab() {
    for (int n = 0; n < tabs.length; n++) ui.segment(tabs[n], n == tab);
    updatePreviewTitle();
    live.setVisibility(tab == 2 ? View.GONE : View.VISIBLE);
    customize.setVisibility(tab == 2 && connected() ? View.VISIBLE : View.GONE);
    buildSettings();
  }

  private void add(LinearLayout parent, View view, int top) {
    LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(-1, -2);
    size.topMargin = dp(top);
    parent.addView(view, size);
  }

  private void updatePreviewTitle() {
    if (sectionTitle != null) sectionTitle.setText(tab == 2 ? t(R.string.panel_heading)
        : t(R.string.preview_title) + (previewIsSample ? " · " + t(R.string.sample_widget_badge) : ""));
  }

  private int previewHeight() {
    return target >= 0 ? style.heightWithin(WidgetStyle.size(this, target)[1]) : style.previewHeight();
  }

  protected RemoteViews previewFor(int width, int height) {
    return BridgeService.editorPreview(this, target, width, height);
  }

  private void updatePreview() {
    if (live == null) return;
    int width = Math.max(1, Math.round((live.getWidth() > 0 ? live.getWidth()
        : getResources().getDisplayMetrics().widthPixels - dp(32))
        / getResources().getDisplayMetrics().density));
    if (target >= 0) width = Math.min(width, WidgetStyle.size(this, target)[0]);
    else if (!style.format.standard()) width = Math.min(width, style.format.width);
    int height = previewHeight();
    View preview = null;
    try {
      RemoteViews remote = previewFor(width, height);
      if (remote != null) preview = remote.apply(this, live);
    } catch (RuntimeException unavailable) {
      // Keep the editor usable while the host is reconnecting or switching its source layout.
    }
    previewIsSample = preview == null;
    if (previewIsSample) preview = SampleWidget.create(this, style, width, height);
    updatePreviewTitle();
    live.removeAllViews();
    live.addView(preview, new FrameLayout.LayoutParams(dp(width), -2, Gravity.CENTER_HORIZONTAL));
  }

  private void save(boolean rebuild) {
    style.save(this, target);
    if (connected()) State.start(this);
    else {
      AppWidgetManager manager = AppWidgetManager.getInstance(this);
      for (int id : State.ownIds(this)) manager.updateAppWidget(id, BridgeService.forWidget(this, id));
    }
    if (rebuild) buildUi();
    else updatePreview();
  }

  private void pick(String title, String[] values, int current, java.util.function.IntConsumer choose) {
    ui.dialog()
        .setTitle(title)
        .setSingleChoiceItems(values, current, (dialog, selected) -> {
          dialog.dismiss();
          if (selected != current) choose.accept(selected);
        })
        .setNegativeButton(t(R.string.cancel), null)
        .show();
  }

  private void choice(LinearLayout card, String title, String[] names, String[] values, String current,
      java.util.function.Consumer<String> onSelect) {
    int index = Math.max(0, Arrays.asList(values).indexOf(current));
    entry(card, null, title, null, names[index],
        () -> pick(title, names, index, selected -> onSelect.accept(values[selected])));
  }

  private String themeName(String theme) {
    if (theme.equals("dark")) return t(R.string.theme_black);
    if (theme.equals("dim")) return t(R.string.theme_dim);
    if (theme.equals("light")) return t(R.string.theme_light);
    return t(R.string.theme_system);
  }

  private void separator(LinearLayout card) {
    if (card.getChildCount() == 0) return;
    View line = new View(this);
    line.setBackgroundColor(ui.divider);
    LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(-1, dp(1));
    size.setMargins(dp(12), 0, dp(12), 0);
    card.addView(line, size);
  }

  private LinearLayout settingLine(LinearLayout card, String title, String detail, ImageView icon) {
    separator(card);
    LinearLayout row = new LinearLayout(this);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setMinimumHeight(dp(49));
    row.setPadding(dp(13), dp(7), dp(12), dp(7));
    if (icon != null) {
      LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(34), dp(34));
      size.setMarginEnd(dp(9));
      row.addView(icon, size);
    }
    LinearLayout copy = ui.column();
    copy.addView(ui.text(title, 15, ui.ink, true));
    if (detail != null) add(copy, ui.text(detail, 11, ui.muted, false), 4);
    row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
    card.addView(row);
    return row;
  }

  private void entry(LinearLayout card, ImageView icon, String title, String detail, String value, Runnable click) {
    LinearLayout row = settingLine(card, title, detail, icon);
    row.setBackground(ui.ripple(null, 0));
    if (value != null) {
      TextView current = ui.text(value, 12, ui.accent, false);
      current.setMaxWidth(dp(138));
      current.setMaxLines(2);
      current.setEllipsize(TextUtils.TruncateAt.END);
      current.setGravity(Gravity.END);
      LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-2, -2);
      layout.setMarginStart(dp(8));
      row.addView(current, layout);
    }
    TextView arrow = ui.text("›", 24, ui.muted, false);
    arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    LinearLayout.LayoutParams next = new LinearLayout.LayoutParams(-2, -2);
    next.setMarginStart(dp(7));
    row.addView(arrow, next);
    row.setOnClickListener(v -> click.run());
  }

  private void toggle(LinearLayout card, String title, boolean checked, java.util.function.Consumer<Boolean> change) {
    LinearLayout row = settingLine(card, title, null, null);
    Switch control = new Switch(this);
    control.setMinHeight(dp(42));
    control.setContentDescription(title);
    control.setChecked(checked);
    control.setThumbTintList(ColorStateList.valueOf(ui.dark ? 0xfff5f2ed : 0xfffffdf7));
    control.setTrackTintList(new ColorStateList(
        new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
        new int[] {ui.dark ? 0xffaa8d69 : 0xff876b4e, ui.dark ? 0xff534f49 : 0xffa39c91}));
    row.addView(control);
    control.setOnCheckedChangeListener((view, value) -> change.accept(value));
    row.setOnClickListener(v -> control.toggle());
    row.setBackground(ui.ripple(null, 0));
  }

  private void buildSettings() {
    if (settings == null) return;
    settings.removeAllViews();
    if (tab == 0) appearanceSettings();
    else if (tab == 1) dataSettings();
    else buttonSettings();
  }

  private void appearanceSettings() {
    LinearLayout card = ui.card();
    if (target < 0) {
      WidgetFormat[] formats = WidgetFormat.values();
      String[] names = new String[formats.length], keys = new String[formats.length];
      for (int i = 0; i < formats.length; i++) { names[i] = t(formats[i].title); keys[i] = formats[i].key; }
      choice(card, t(R.string.widget_type), names, keys, style.format.key, value -> {
        style.format = WidgetFormat.parse(value);
        style.layout = style.format == WidgetFormat.LARGE ? "detailed" : "auto";
        save(true);
      });
    } else {
      settingLine(card, t(style.format.title), t(R.string.widget_type_fixed), null);
    }
    if (style.format == WidgetFormat.FULL) choice(card, t(R.string.editor_format),
        new String[] {t(R.string.layout_auto), t(R.string.layout_compact), t(R.string.editor_detailed)},
        new String[] {"auto", "compact", "detailed"}, style.layout, value -> {
          style.layout = value;
          save(true);
        });

    if (style.format.standard()) {
    Bitmap image = WidgetAvatar.icon(this, style.avatar);
    ImageView avatar = null;
    if (image != null) {
      avatar = new ImageView(this);
      avatar.setImageBitmap(image);
    }
    VehiclePaints.PaintOption option = VehiclePaints.find(style.avatar);
    String name = option != null ? "Li " + VehiclePaints.MODELS[option.model]
        : WidgetAvatar.isPhoto(style.avatar) ? t(R.string.avatar_custom) : t(R.string.avatar_none);
    entry(card, avatar, t(R.string.avatar_title), t(R.string.avatar_hint), name,
        () -> startActivityForResult(new Intent(this, AvatarGalleryActivity.class)
            .putExtra(AvatarGalleryActivity.TARGET, target), CHANGE_AVATAR));
    }

    choice(card, t(R.string.editor_theme),
        new String[] {themeName("light"), themeName("dim"), themeName("dark"), themeName("system")},
        new String[] {"light", "dim", "dark", "system"}, style.theme, value -> {
          style.theme = value;
          save(true);
        });
    if (!style.theme.equals("dark")) {
      choice(card, t(R.string.editor_palette), new String[] {t(R.string.colors_system), t(R.string.colors_neutral)},
          new String[] {"system", "neutral"}, style.palette, value -> {
            style.palette = value;
            save(true);
          });
      choice(card, t(R.string.editor_surface), new String[] {t(R.string.surface_matte), t(R.string.surface_glass)},
          new String[] {"matte", "glass"}, style.surface, value -> {
            style.surface = value;
            save(true);
          });
      if (colors.highContrast) {
        TextView hint = ui.text(t(R.string.surface_contrast_hint), 11, ui.muted, false);
        hint.setPadding(dp(12), 0, dp(12), dp(10));
        card.addView(hint);
      }
    }
    choice(card, t(R.string.editor_labels), new String[] {t(R.string.font_normal), t(R.string.font_large)},
        new String[] {"normal", "large"}, style.font, value -> {
          style.font = value;
          save(true);
        });
    if (style.format.standard() || style.format == WidgetFormat.CHARGING) {
    String[] motionModes = style.format.standard() ? new String[] {"fade", "avatar", "off"} : new String[] {"fade", "off"};
    String[] motionNames = style.format.standard() ? new String[] {t(R.string.motion_fade), t(R.string.motion_avatar), t(R.string.motion_off)}
        : new String[] {t(R.string.motion_fade), t(R.string.motion_off)};
    int selectedMotion = Math.max(0, Arrays.asList(motionModes).indexOf(style.motion));
    entry(card, null, t(R.string.editor_motion), t(R.string.motion_hint), motionNames[selectedMotion],
        () -> pick(t(R.string.editor_motion), motionNames, selectedMotion, index -> {
          style.motion = motionModes[index];
          save(true);
          if (WidgetMotion.enabled(style)) {
            ScrollView current = scroll;
            current.post(() -> {
              current.scrollTo(0, 0);
              if (scroll == current) WidgetMotion.preview(live, style);
            });
          }
        }));
    }
    choice(card, t(R.string.editor_language), new String[] {t(R.string.language_ru), t(R.string.language_en)},
        new String[] {"ru", "en"}, Language.code(this), this::chooseLanguage);
    add(settings, card, 0);

    Button reset = ui.button(t(R.string.reset_style), false, () -> {
      WidgetFormat format = style.format;
      style = new WidgetStyle();
      style.format = format;
      if (format == WidgetFormat.LARGE) style.layout = "detailed";
      save(true);
    });
    reset.setTextColor(ui.muted);
    reset.setTextSize(13);
    reset.setBackground(ui.ripple(null, 13));
    add(settings, reset, 4);
  }

  private void dataSettings() {
    LinearLayout card = ui.card();
    if (style.format.favorites()) settingLine(card, t(R.string.format_controls_only), t(R.string.favorite_source_hint), null);
    if (!style.format.favorites() && style.format != WidgetFormat.MINIMAL)
      toggle(card, t(R.string.show_time), style.showTime, value -> {
      style.showTime = value;
      save(false);
    });
    if (style.format.standard() || style.format == WidgetFormat.RANGE)
      toggle(card, t(R.string.show_battery), style.showBattery, value -> {
      style.showBattery = value;
      save(false);
    });
    if (!style.format.favorites()) choice(card, t(R.string.editor_stale),
        new String[] {t(R.string.stale_15), t(R.string.stale_30), t(R.string.stale_60), t(R.string.stale_120)},
        new String[] {"15", "30", "60", "120"}, Integer.toString(style.staleMinutes), value -> {
          style.staleMinutes = Integer.parseInt(value);
          save(false);
          buildSettings();
        });
    add(settings, card, 0);
    LinearLayout links = ui.card();
    entry(links, null, t(R.string.open_source), null, null, this::openSource);
    if (connected()) entry(links, null, t(R.string.disconnect_data), null, null, () ->
        ui.dialog()
            .setTitle(t(R.string.disconnect_title))
            .setMessage(t(R.string.disconnect_hint))
            .setNegativeButton(t(R.string.cancel), null)
            .setPositiveButton(t(R.string.disconnect), (dialog, index) -> disconnect())
            .show());
    add(settings, links, 10);
  }

  private void buttonSettings() {
    List<View> actions = new ArrayList<>(4);
    try {
      android.content.res.Resources source = getPackageManager().getResourcesForApplication(State.SOURCE);
      View car = live.findViewById(source.getIdentifier("ll_widget_car", "id", State.SOURCE));
      if (car != null && car.getVisibility() == View.VISIBLE) {
        for (int i = 1; i <= 4; i++) {
          TextView original = live.findViewById(source.getIdentifier("control_button_text" + i, "id", State.SOURCE));
          if (original == null || original.length() == 0) continue;
          ImageView image = live.findViewById(source.getIdentifier("control_button_icon" + i, "id", State.SOURCE));
          Drawable glyph = image == null ? null : image.getDrawable();
          Drawable.ConstantState state = glyph == null ? null : glyph.getConstantState();
          actions.add(actionCell(original.getText().toString(), state == null ? null : state.newDrawable().mutate()));
        }
      }
    } catch (PackageManager.NameNotFoundException unavailable) {
      // The example panel is available before installing Li Auto.
    }
    boolean sample = actions.isEmpty();
    if (sample) {
      add(settings, ui.text(t(R.string.sample_widget_badge), 12, ui.muted, false), 0);
      for (int i = 0; i < SampleWidget.ACTION_NAMES.length; i++) {
        actions.add(actionCell(t(SampleWidget.ACTION_NAMES[i]), getDrawable(SampleWidget.ACTION_ICONS[i]).mutate()));
      }
    }

    LinearLayout panel = ui.card();
    for (int i = 0; i < actions.size(); i += 2) {
      if (i > 0) {
        View line = new View(this);
        line.setBackgroundColor(ui.divider);
        panel.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
      }
      LinearLayout row = new LinearLayout(this);
      row.setBaselineAligned(false);
      row.addView(actions.get(i), new LinearLayout.LayoutParams(0, -1, 1));
      if (i + 1 < actions.size()) {
        View line = new View(this);
        line.setBackgroundColor(ui.divider);
        row.addView(line, new LinearLayout.LayoutParams(dp(1), -1));
        row.addView(actions.get(i + 1), new LinearLayout.LayoutParams(0, -1, 1));
      }
      panel.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }
    add(settings, panel, sample ? 8 : 0);
    int hint = !sample ? R.string.buttons_hint : connected() ? R.string.panel_sign_in : R.string.panel_connect;
    TextView guidance = ui.text(t(hint), 12, ui.muted, false);
    guidance.setPadding(dp(3), 0, dp(3), 0);
    add(settings, guidance, 10);
  }

  private View actionCell(String title, Drawable glyph) {
    LinearLayout cell = new LinearLayout(this);
    cell.setGravity(Gravity.CENTER_VERTICAL);
    cell.setMinimumHeight(dp(77));
    cell.setPadding(dp(12), dp(10), dp(12), dp(10));
    String name = title.replace('\n', ' ');
    if (glyph != null) {
      ImageView icon = new ImageView(this);
      icon.setImageDrawable(glyph);
      icon.setColorFilter(ui.ink);
      icon.setPadding(dp(7), dp(7), dp(7), dp(7));
      icon.setBackground(ui.solid(ui.selectedFill, 11, 0));
      LinearLayout.LayoutParams space = new LinearLayout.LayoutParams(dp(38), dp(38));
      space.setMarginEnd(dp(9));
      cell.addView(icon, space);
    }
    TextView label = ui.text(name, 14, ui.ink, true);
    label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    cell.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
    cell.setScreenReaderFocusable(true);
    String description = Translations.description(this, title);
    cell.setContentDescription(name.equals(description) ? name : name + ". " + description);
    return cell;
  }

  @Override
  protected void onResume() {
    super.onResume();
    themeWatcher = new ThemeWatcher(this, () -> { if (colorsChanged()) buildUi(); });
    if (Build.VERSION.SDK_INT >= 33)
      registerReceiver(update, new IntentFilter(State.UPDATE), Context.RECEIVER_NOT_EXPORTED);
    else registerReceiver(update, new IntentFilter(State.UPDATE));
    if (colorsChanged()) buildUi();
    if (connected()) State.start(this);
    if (live != null) live.post(() -> {
      updatePreview();
      if (tab == 2) buildSettings();
    });
  }

  @Override
  protected void onPause() {
    if (themeWatcher != null) themeWatcher.close();
    themeWatcher = null;
    unregisterReceiver(update);
    super.onPause();
  }

  private void openButtonSettings() {
    try {
      startActivity(buttonSettingsIntent());
    } catch (ActivityNotFoundException | SecurityException e) {
      ui.dialog()
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
      ui.dialog()
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
    if (request == CHANGE_AVATAR) {
      if (result != RESULT_OK || data == null) return;
      String key = data.getStringExtra(AvatarGalleryActivity.AVATAR);
      int id = data.getIntExtra(AvatarGalleryActivity.TARGET, -1);
      if (key == null) return;
      if (id >= 0 && !State.ownsWidget(this, id)) {
        WidgetAvatar.removeIfUnused(this, key);
        return;
      }
      WidgetStyle edited = WidgetStyle.read(this, id);
      edited.avatar = key;
      edited.save(this, id);
      if (connected()) State.start(this);
      buildUi();
      return;
    }
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
      m.requestPinAppWidget(style.format.component(this), null, null);
    else
      ui.dialog()
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
