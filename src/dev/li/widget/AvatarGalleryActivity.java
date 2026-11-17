package dev.li.widget;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public final class AvatarGalleryActivity extends Activity {
  static final String TARGET = "widget_target", AVATAR = "widget_avatar";
  private static final int PICK_PHOTO = 61;
  private SettingsLook ui;
  private ThemeWatcher themeWatcher;
  private int themeSignature;
  private int widgetId, model;
  private String selected;
  private ImageView car;
  private TextView paintName;
  private LinearLayout paints;
  private final Button[] models = new Button[VehiclePaints.MODELS.length];
  private Button importPhoto;
  private boolean savedSelection;

  @Override protected void attachBaseContext(Context context) { super.attachBaseContext(Language.context(context)); }

  @Override protected void onResume() {
    super.onResume();
    themeWatcher = new ThemeWatcher(this, this::updateTheme);
    updateTheme();
  }

  @Override protected void onPause() {
    if (themeWatcher != null) themeWatcher.close();
    themeWatcher = null;
    super.onPause();
  }

  private void updateTheme() {
    WidgetColors colors = new WidgetColors(this, WidgetStyle.read(this, widgetId));
    if (colors.signature() == themeSignature) return;
    themeSignature = colors.signature();
    ui = new SettingsLook(this, colors);
    showScreen();
  }

  @Override protected void onCreate(Bundle state) {
    super.onCreate(state);
    widgetId = getIntent().getIntExtra(TARGET, -1);
    if (widgetId >= 0 && !State.ownsWidget(this, widgetId)) {
      finish();
      return;
    }
    WidgetStyle style = WidgetStyle.read(this, widgetId);
    selected = state == null ? style.avatar : state.getString(AVATAR, style.avatar);
    if (selected.isEmpty()) selected = VehiclePaints.DEFAULT;
    VehiclePaints.PaintOption option = VehiclePaints.find(selected);
    model = option == null ? 1 : option.model;
    WidgetColors colors = new WidgetColors(this, style);
    themeSignature = colors.signature();
    ui = new SettingsLook(this, colors);
    showScreen();
  }

  private String text(int id) { return Language.text(this, id); }

  private void showScreen() {
    getWindow().setDecorFitsSystemWindows(false);
    LinearLayout root = ui.column();
    root.setBackground(ui.backdrop());
    root.setOnApplyWindowInsetsListener((view, insets) -> {
      android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
      root.setPadding(bars.left, bars.top, bars.right, bars.bottom);
      return insets;
    });
    LinearLayout header = new LinearLayout(this);
    header.setGravity(Gravity.CENTER_VERTICAL);
    header.setPadding(ui.dp(14), 0, ui.dp(18), 0);
    Button back = ui.button("‹", false, this::finish);
    back.setContentDescription(text(R.string.gallery_back));
    back.setTextSize(28);
    header.addView(back, new LinearLayout.LayoutParams(ui.dp(48), ui.dp(48)));
    TextView title = ui.text(text(R.string.avatar_gallery), 21, ui.ink, true);
    LinearLayout.LayoutParams titleLayout = new LinearLayout.LayoutParams(0, -2, 1);
    titleLayout.setMarginStart(ui.dp(12));
    header.addView(title, titleLayout);
    root.addView(header, new LinearLayout.LayoutParams(-1, ui.dp(54)));

    ScrollView scroll = new ScrollView(this);
    scroll.setVerticalScrollBarEnabled(false);
    LinearLayout content = ui.column();
    content.setPadding(ui.dp(16), ui.dp(8), ui.dp(16), ui.dp(12));
    scroll.addView(content);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

    LinearLayout lineup = new LinearLayout(this);
    lineup.setBackground(ui.track());
    lineup.setPadding(ui.dp(3), ui.dp(3), ui.dp(3), ui.dp(3));
    for (int index = 0; index < models.length; index++) {
      final int next = index;
      models[index] = ui.button(VehiclePaints.MODELS[index], false, () -> {
        model = next;
        select(VehiclePaints.forModel(model).get(0).key);
      });
      LinearLayout.LayoutParams part = new LinearLayout.LayoutParams(0, -2, 1);
      if (index > 0) part.setMarginStart(ui.dp(2));
      lineup.addView(models[index], part);
    }
    content.addView(lineup);

    LinearLayout display = ui.card();
    display.setGravity(Gravity.CENTER_HORIZONTAL);
    display.setPadding(ui.dp(15), ui.dp(12), ui.dp(15), ui.dp(12));
    car = new ImageView(this);
    car.setScaleType(ImageView.ScaleType.FIT_CENTER);
    display.addView(car, new LinearLayout.LayoutParams(-1, ui.dp(130)));
    paintName = ui.text("", 16, ui.ink, true);
    paintName.setGravity(Gravity.CENTER);
    display.addView(paintName, new LinearLayout.LayoutParams(-1, -2));
    add(content, display, 9);

    importPhoto = ui.button(text(R.string.avatar_import), false, this::pickPhoto);
    add(content, importPhoto, 9);
    paints = ui.column();
    add(content, paints, 10);

    LinearLayout footer = new LinearLayout(this);
    footer.setPadding(ui.dp(16), ui.dp(5), ui.dp(16), ui.dp(8));
    footer.addView(ui.button(text(R.string.avatar_none), false, () -> finishWith("")), new LinearLayout.LayoutParams(0, -2, 1));
    LinearLayout.LayoutParams primary = new LinearLayout.LayoutParams(0, -2, 1);
    primary.setMarginStart(ui.dp(8));
    footer.addView(ui.button(text(R.string.avatar_apply), true, () -> finishWith(selected)), primary);
    root.addView(footer);
    setContentView(root);
    ui.applySystemBars(root);
    refreshSelection();
  }

  private void add(LinearLayout parent, View child, int top) {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
    params.topMargin = ui.dp(top);
    parent.addView(child, params);
  }

  private void select(String key) {
    if (selected.equals(key)) return;
    WidgetAvatar.removeIfUnused(this, selected);
    selected = key;
    refreshSelection();
  }

  private void refreshSelection() {
    VehiclePaints.PaintOption option = VehiclePaints.find(selected);
    if (option != null) {
      car.setImageBitmap(VehicleArt.car(this, option));
      paintName.setText("Li " + VehiclePaints.MODELS[model] + " · " + option.label(this));
    } else {
      car.setImageBitmap(WidgetAvatar.icon(this, selected));
      paintName.setText(R.string.avatar_custom);
    }
    for (int i = 0; i < models.length; i++) ui.segment(models[i], model == i);
    paints.removeAllViews();
    List<VehiclePaints.PaintOption> options = VehiclePaints.forModel(model);
    for (int i = 0; i < options.size(); i += 2) {
      LinearLayout row = new LinearLayout(this);
      row.setBaselineAligned(false);
      row.addView(swatch(options.get(i)), new LinearLayout.LayoutParams(0, -1, 1));
      LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, -1, 1);
      right.setMarginStart(ui.dp(6));
      row.addView(i + 1 < options.size() ? swatch(options.get(i + 1)) : new View(this), right);
      add(paints, row, i == 0 ? 0 : 6);
    }
  }

  private View swatch(VehiclePaints.PaintOption option) {
    LinearLayout row = new LinearLayout(this);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(ui.dp(8), ui.dp(8), ui.dp(8), ui.dp(8));
    row.setMinimumHeight(ui.dp(52));
    boolean active = option.key.equals(selected);
    row.setBackground(ui.ripple(active ? ui.solid(ui.selectedFill, 13, ui.selectedStroke) : ui.panel(13), 13));
    View circle = new View(this);
    GradientDrawable sample = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
        new int[] {option.upper, option.upper, option.body, option.body});
    sample.setShape(GradientDrawable.OVAL);
    sample.setStroke(ui.dp(1), ui.divider);
    circle.setBackground(sample);
    row.addView(circle, new LinearLayout.LayoutParams(ui.dp(24), ui.dp(24)));
    TextView name = ui.text(option.label(this), 13, ui.ink, active);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
    params.setMarginStart(ui.dp(7));
    row.addView(name, params);
    row.setSelected(active);
    row.setContentDescription(option.label(this));
    name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    row.setOnClickListener(v -> select(option.key));
    return row;
  }

  private void pickPhoto() {
    if (Build.VERSION.SDK_INT >= 33) {
      try {
        startActivityForResult(new Intent(MediaStore.ACTION_PICK_IMAGES).setType("image/*"), PICK_PHOTO);
        return;
      } catch (ActivityNotFoundException unavailable) {
        // Some Android builds provide only the system document picker.
      }
    }
    try {
      startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
          .setType("image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), PICK_PHOTO);
    } catch (ActivityNotFoundException error) {
      Toast.makeText(this, R.string.avatar_import_error, Toast.LENGTH_LONG).show();
    }
  }

  @Override protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request != PICK_PHOTO || result != RESULT_OK || data == null) return;
    Uri uri = data.getData();
    importPhoto.setEnabled(false);
    importPhoto.setText(R.string.avatar_importing);
    new Thread(() -> {
      String key = null;
      try {
        key = WidgetAvatar.importPhoto(getApplicationContext(), uri);
      } catch (java.io.IOException | RuntimeException ignored) {
        // Keep the previous selection if the picker supplied an unreadable image.
      }
      String imported = key;
      runOnUiThread(() -> {
        if (isFinishing() || isDestroyed()) {
          WidgetAvatar.removeIfUnused(getApplicationContext(), imported);
          return;
        }
        importPhoto.setEnabled(true);
        importPhoto.setText(R.string.avatar_import);
        if (imported == null) Toast.makeText(this, R.string.avatar_import_error, Toast.LENGTH_LONG).show();
        else select(imported);
      });
    }, "widget-avatar-import").start();
  }

  private void finishWith(String key) {
    if (!selected.equals(key)) WidgetAvatar.removeIfUnused(this, selected);
    savedSelection = true;
    setResult(RESULT_OK, new Intent().putExtra(AVATAR, key).putExtra(TARGET, widgetId));
    finish();
  }

  @Override protected void onSaveInstanceState(Bundle state) {
    state.putString(AVATAR, selected);
    super.onSaveInstanceState(state);
  }

  @Override protected void onDestroy() {
    if (isFinishing() && !savedSelection) WidgetAvatar.removeIfUnused(this, selected);
    super.onDestroy();
  }
}
