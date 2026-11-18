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
    WidgetFormat display = style.format.presentation(style, width, height, context.getResources().getConfiguration().fontScale);
    if (!display.standard()) {
      if (display.favorites()) return sampleActions(context, style, width, height, WidgetFormat.actionRows(width, height), false);
      return VariantAppearance.sample(context, style, width, height).apply(context, new android.widget.FrameLayout(context));
    }
    WidgetColors colors = WidgetColors.forWidget(context, style);
    SettingsLook ui = new SettingsLook(context, colors);
    boolean compact = display == WidgetFormat.FULL && style.compact(height);
    boolean overview = display == WidgetFormat.LARGE;
    WidgetFormat.OverviewSize geometry = new WidgetFormat.OverviewSize(width, height, style.showTime ? 23 : 0);
    LinearLayout sample = ui.column();
    int inset = overview ? geometry.inset : Appearance.actionInset(width);
    int sidePadding = ui.dp(inset);
    sample.setPadding(sidePadding, overview ? 0 : ui.dp(6), sidePadding, ui.dp(8));
    sample.setBackgroundResource(colors.surfaceRes);

    LinearLayout header = new LinearLayout(context);
    header.setGravity(Gravity.CENTER_VERTICAL);
    Bitmap avatar = compact || width < 210 ? null : WidgetAvatar.icon(context, style.avatar);
    if (avatar != null) {
      ImageView photo = new ImageView(context);
      photo.setId(R.id.animated_avatar);
      photo.setImageBitmap(avatar);
      LinearLayout.LayoutParams avatarSize = new LinearLayout.LayoutParams(ui.dp(44), ui.dp(44));
      avatarSize.setMarginEnd(ui.dp(10));
      header.addView(photo, avatarSize);
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
    int headerHeight = overview ? geometry.header : compact ? 40 : Appearance.DETAILED_HEADER_DP;
    sample.addView(header, new LinearLayout.LayoutParams(-1, ui.dp(headerHeight)));

    if (!compact) {
      int hero = overview ? geometry.hero : 58;
      View readings = VariantAppearance.sampleOverview(context, style, width, hero)
          .apply(context, new android.widget.FrameLayout(context));
      readings.setPadding(0, 0, 0, 0);
      sample.addView(readings, new LinearLayout.LayoutParams(-1, ui.dp(hero)));
    }

    int controlsHeight = overview ? geometry.actions : height < 132 ? 52 : 57;
    int rows = overview ? geometry.rows : 1;
    View controls = sampleActions(context, style, width - 2 * inset, controlsHeight, rows, true);
    sample.addView(controls, new LinearLayout.LayoutParams(-1, ui.dp(controlsHeight)));
    if (style.showTime && height >= 148) {
      TextView update = ui.text(Language.text(context, R.string.sample_update), 11, colors.secondary, false);
      update.setId(R.id.status_text);
      LinearLayout.LayoutParams text = new LinearLayout.LayoutParams(-1, -2);
      text.topMargin = ui.dp(overview ? 3 : 8);
      sample.addView(update, text);
    }
    return sample;
  }

  private static View sampleActions(Context context, WidgetStyle style, int width, int height, int rows, boolean embedded) {
    WidgetColors colors = WidgetColors.forWidget(context, style);
    SettingsLook ui = new SettingsLook(context, colors);
    LinearLayout root = ui.column();
    if (embedded) root.setBackground(ui.solid(colors.panel, 16, 0));
    else root.setBackgroundResource(colors.surfaceRes);
    int separator = rows - 1;
    int pad = embedded ? 0 : Math.min(6, Math.max(0, (height - rows * 48 - separator) / 2));
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
      int count = embedded ? 4 / rows : WidgetFormat.firstRowCount(width, rows);
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
        int glyph = rowHeight >= 70 ? 34 : rowHeight >= 56 ? 30 : 24;
        tile.addView(icon, new LinearLayout.LayoutParams(ui.dp(glyph), ui.dp(glyph)));
        TextView name = ui.text(Language.text(context, ACTION_NAMES[i]), style.fontSize(), colors.primary, false);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        name.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams label = new LinearLayout.LayoutParams(-1, -2);
        label.topMargin = ui.dp(4);
        tile.addView(name, label);
        row.addView(tile, new LinearLayout.LayoutParams(0, -1, 1));
      }
      if (rows == 1 && count < 4) {
        int pages = (4 + count - 1) / count;
        TextView next = ui.text("1/" + pages + " ›", 12, colors.secondary, true);
        next.setGravity(Gravity.CENTER);
        row.addView(next, new LinearLayout.LayoutParams(ui.dp(48), -1));
      }
      root.addView(row, new LinearLayout.LayoutParams(-1, ui.dp(rowHeight)));
    }
    return root;
  }

}
