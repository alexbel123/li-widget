package dev.li.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** In-app sample for unbound widgets; never provides launcher data or car actions. */
final class SampleWidget {
  static final int[] ACTION_ICONS = {R.drawable.preview_lock, R.drawable.preview_climate, R.drawable.preview_tailgate, R.drawable.preview_move};
  static final int[] ACTION_NAMES = {R.string.action_locks, R.string.action_climate, R.string.action_tailgate, R.string.action_move};

  static View create(Context context, WidgetStyle style, int width, int height) {
    if (!style.format.standard()) return special(context, style, width, height);
    WidgetColors colors = WidgetColors.forWidget(context, style);
    SettingsLook ui = new SettingsLook(context, colors);
    boolean compact = style.compact(height);
    LinearLayout sample = ui.column();
    int sidePadding = ui.dp(Appearance.actionInset(width));
    sample.setPadding(sidePadding, ui.dp(6), sidePadding, ui.dp(8));
    sample.setBackgroundResource(colors.surfaceRes);

    LinearLayout header = new LinearLayout(context);
    header.setGravity(Gravity.CENTER_VERTICAL);
    Bitmap avatar = compact ? null : WidgetAvatar.icon(context, style.avatar);
    if (avatar != null) {
      ImageView photo = new ImageView(context);
      photo.setId(R.id.animated_avatar);
      photo.setImageBitmap(avatar);
      LinearLayout.LayoutParams inset = new LinearLayout.LayoutParams(ui.dp(44), ui.dp(44));
      inset.setMarginEnd(ui.dp(10));
      header.addView(photo, inset);
    }
    VehiclePaints.PaintOption car = VehiclePaints.find(style.avatar);
    String title = "Li Auto " + (car == null ? "L7" : VehiclePaints.MODELS[car.model]);
    TextView name = ui.text(title, compact ? 16 : Appearance.detailedNameSize(style), colors.primary, true);
    name.setMaxLines(2);
    name.setEllipsize(TextUtils.TruncateAt.END);
    LinearLayout identity = ui.column();
    identity.addView(name);
    if (compact) {
      TextView amounts = ui.text("186 " + Language.text(context, R.string.sample_km) + "   ·   742 "
          + Language.text(context, R.string.sample_km) + (style.showBattery ? "   ·   76%" : ""),
          12, colors.secondary, true);
      amounts.setMaxLines(1);
      amounts.setEllipsize(TextUtils.TruncateAt.END);
      identity.addView(amounts);
    }
    header.addView(identity, new LinearLayout.LayoutParams(0, -2, 1));
    TextView config = ui.text(Language.text(context, R.string.sample_widget_badge), 11, colors.secondary, false);
    header.addView(config);
    sample.addView(header, new LinearLayout.LayoutParams(-1, ui.dp(compact ? 40 : Appearance.DETAILED_HEADER_DP)));

    if (!compact) {
      LinearLayout readings = new LinearLayout(context);
      readings.setGravity(Gravity.CENTER_VERTICAL);
      addReading(readings, ui, context, R.string.electric_range, "186",
          style.showBattery ? "  ·  76%" : "", colors);
      addReading(readings, ui, context, R.string.fuel_range, "742", "", colors);
      sample.addView(readings, new LinearLayout.LayoutParams(-1, ui.dp(58)));
    }

    LinearLayout controls = new LinearLayout(context);
    controls.setGravity(Gravity.CENTER_VERTICAL);
    controls.setBackground(ui.solid(colors.panel, 16, 0));
    int controlsHeight = compact ? (height < 132 ? 52 : 57) : 66;
    for (int index = 0; index < ACTION_NAMES.length; index++) {
      if (index > 0) {
        View line = new View(context);
        line.setBackgroundColor(colors.actionDivider);
        controls.addView(line, new LinearLayout.LayoutParams(ui.dp(1), ui.dp(Appearance.actionDividerHeight(controlsHeight))));
      }
      LinearLayout tile = ui.column();
      tile.setGravity(Gravity.CENTER);
      tile.setPadding(ui.dp(3), ui.dp(2), ui.dp(3), ui.dp(2));
      ImageView icon = new ImageView(context);
      icon.setImageResource(ACTION_ICONS[index]);
      icon.setColorFilter(colors.icon);
      int glyph = ui.dp(compact ? 23 : 25);
      tile.addView(icon, new LinearLayout.LayoutParams(glyph, glyph));
      TextView label = ui.text(Language.text(context, ACTION_NAMES[index]), style.fontSize(), colors.primary, false);
      label.setGravity(Gravity.CENTER);
      label.setMaxLines(2);
      LinearLayout.LayoutParams textLayout = new LinearLayout.LayoutParams(-1, -2);
      textLayout.topMargin = ui.dp(4);
      tile.addView(label, textLayout);
      controls.addView(tile, new LinearLayout.LayoutParams(0, -1, 1));
    }
    sample.addView(controls, new LinearLayout.LayoutParams(-1, ui.dp(controlsHeight)));
    if (style.showTime && height >= 148) {
      TextView update = ui.text(Language.text(context, R.string.sample_update), 11, colors.secondary, false);
      update.setId(R.id.status_text);
      LinearLayout.LayoutParams text = new LinearLayout.LayoutParams(-1, -2);
      text.topMargin = ui.dp(8);
      sample.addView(update, text);
    }
    return sample;
  }

  private static View special(Context context, WidgetStyle style, int width, int height) {
    if (!style.format.favorites())
      return VariantAppearance.sample(context, style, width, height)
          .apply(context, new android.widget.FrameLayout(context));
    WidgetColors colors = WidgetColors.forWidget(context, style);
    SettingsLook ui = new SettingsLook(context, colors);
    LinearLayout root = ui.column();
    root.setBackgroundResource(colors.surfaceRes);
    int rows = style.format == WidgetFormat.FAVORITES_GRID ? 2 : 1;
    int separator = rows - 1;
    int pad = Math.min(6, Math.max(0, (height - rows * 48 - separator) / 2));
    int rowHeight = Math.max(1, (height - 2 * pad - separator) / rows);
    root.setPadding(ui.dp(4), ui.dp(pad), ui.dp(4), ui.dp(pad));
    for (int r = 0; r < rows; r++) {
      if (r > 0) {
        View line = new View(context);
        line.setBackgroundColor(colors.actionDivider);
        LinearLayout.LayoutParams rule = new LinearLayout.LayoutParams(-1, ui.dp(1));
        rule.setMargins(ui.dp(12), 0, ui.dp(12), 0);
        root.addView(line, rule);
      }
      LinearLayout row = new LinearLayout(context);
      row.setGravity(Gravity.CENTER_VERTICAL);
      int count = 4 / rows;
      for (int i = r * count; i < (r + 1) * count; i++) {
        if (i > r * count) {
          View line = new View(context);
          line.setBackgroundColor(colors.actionDivider);
          row.addView(line, new LinearLayout.LayoutParams(ui.dp(1), ui.dp(Appearance.actionDividerHeight(rowHeight))));
        }
        LinearLayout tile = ui.column();
        tile.setGravity(Gravity.CENTER);
        ImageView icon = new ImageView(context);
        icon.setImageResource(ACTION_ICONS[i]);
        icon.setColorFilter(colors.icon);
        tile.addView(icon, new LinearLayout.LayoutParams(ui.dp(32), ui.dp(32)));
        TextView name = ui.text(Language.text(context, ACTION_NAMES[i]), style.fontSize(), colors.primary, false);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        name.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams label = new LinearLayout.LayoutParams(-1, -2);
        label.topMargin = ui.dp(4);
        tile.addView(name, label);
        row.addView(tile, new LinearLayout.LayoutParams(0, -1, 1));
      }
      root.addView(row, new LinearLayout.LayoutParams(-1, ui.dp(rowHeight)));
    }
    return root;
  }

  private static void addReading(LinearLayout row, SettingsLook ui, Context context,
      int label, String value, String detail, WidgetColors colors) {
    LinearLayout cell = ui.column();
    String km = Language.text(context, R.string.sample_km);
    TextView number = ui.text(value + " " + km, 23, colors.primary, true);
    number.setMaxLines(1);
    cell.addView(number);
    TextView caption = ui.text(Language.text(context, label) + detail, 11, colors.secondary, false);
    LinearLayout.LayoutParams space = new LinearLayout.LayoutParams(-1, -2);
    space.topMargin = ui.dp(3);
    cell.addView(caption, space);
    row.addView(cell, new LinearLayout.LayoutParams(0, -2, 1));
  }
}
