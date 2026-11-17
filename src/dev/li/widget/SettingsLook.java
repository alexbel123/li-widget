package dev.li.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The settings surfaces share one ink, accent and translucent material. */
final class SettingsLook {
  private final Context context;
  private final WidgetColors colors;
  final boolean dark;
  final int ink, muted, accent, divider, selectedFill, selectedStroke;
  private final int primaryFill, primaryInk, border;

  SettingsLook(Context context, boolean dark) {
    this(context, neutral(context, dark));
  }

  private static WidgetColors neutral(Context context, boolean dark) {
    WidgetStyle style = new WidgetStyle();
    style.theme = dark ? "dim" : "light";
    style.palette = "neutral";
    return new WidgetColors(context, style);
  }

  SettingsLook(Context context, WidgetColors colors) {
    this.context = context;
    this.colors = colors;
    this.dark = colors.dark;
    ink = colors.primary;
    muted = colors.secondary;
    accent = colors.accent;
    primaryFill = colors.accent;
    primaryInk = colors.onAccent;
    divider = colors.outline;
    border = colors.outline;
    selectedFill = colors.accentContainer;
    selectedStroke = WidgetColors.mix(colors.outline, colors.accent, .45f);
  }

  int dp(float n) { return Math.round(n * context.getResources().getDisplayMetrics().density); }

  TextView text(String value, float size, int color, boolean strong) {
    TextView label = new TextView(context);
    label.setText(value);
    label.setTextSize(size);
    label.setTextColor(color);
    label.setTypeface(Typeface.create(strong ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
    label.setIncludeFontPadding(false);
    return label;
  }

  GradientDrawable solid(int fill, int radius, int border) {
    GradientDrawable shape = new GradientDrawable();
    shape.setColor(fill);
    shape.setCornerRadius(dp(radius));
    if (border != 0) shape.setStroke(dp(1), border);
    return shape;
  }

  Drawable panel(int radius) {
    if (!colors.glass) return solid(colors.panel, radius, border);
    int[] stops = {alpha(WidgetColors.mix(colors.panel, colors.accent, .06f), 247), alpha(colors.panel, 235)};
    GradientDrawable glass = new GradientDrawable(GradientDrawable.Orientation.TL_BR, stops);
    glass.setCornerRadius(dp(radius));
    glass.setStroke(dp(1), border);
    return glass;
  }

  RippleDrawable ripple(Drawable background, int radius) {
    return new RippleDrawable(ColorStateList.valueOf(alpha(accent, dark ? 40 : 34)),
        background, solid(0xffffffff, radius, 0));
  }

  LinearLayout column() {
    LinearLayout view = new LinearLayout(context);
    view.setOrientation(LinearLayout.VERTICAL);
    return view;
  }

  LinearLayout card() {
    LinearLayout view = column();
    view.setBackground(panel(18));
    view.setClipToOutline(true);
    return view;
  }

  Button button(String title, boolean primary, Runnable click) {
    Button button = new Button(context);
    button.setText(title);
    button.setTextSize(14);
    button.setAllCaps(false);
    button.setIncludeFontPadding(false);
    button.setMinHeight(0);
    button.setMinimumHeight(dp(48));
    button.setMinWidth(0);
    button.setMinimumWidth(0);
    button.setPadding(dp(12), dp(8), dp(12), dp(8));
    button.setElevation(0);
    button.setStateListAnimator(null);
    button.setTextColor(primary ? primaryInk : ink);
    button.setBackground(ripple(primary ? solid(primaryFill, 13, 0) : panel(13), 13));
    button.setOnClickListener(v -> click.run());
    return button;
  }

  Drawable track() {
    return solid(WidgetColors.mix(colors.background, colors.panel, .4f), 15, border);
  }

  void segment(Button button, boolean active) {
    button.setSelected(active);
    button.setTextColor(active ? colors.onAccentContainer : muted);
    button.setBackground(ripple(active ? solid(selectedFill, 11, 0) : null, 11));
  }

  AlertDialog.Builder dialog() {
    return new AlertDialog.Builder(context, colors.dynamic
        ? (dark ? R.style.LiDialogDynamicDark : R.style.LiDialogDynamicLight)
        : (dark ? R.style.LiDialogDark : R.style.LiDialogLight));
  }

  void applySystemBars(View root) {
    // The controller may be unavailable until the content is attached to its window.
    root.post(() -> {
      WindowInsetsController controller = root.getWindowInsetsController();
      if (controller == null || !root.isAttachedToWindow()) return;
      int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
          | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
      controller.setSystemBarsAppearance(dark ? 0 : light, light);
    });
  }

  private static int alpha(int color, int alpha) { return (color & 0x00ffffff) | (alpha << 24); }

  Drawable backdrop() {
    return new Drawable() {
      private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
      private Shader base, glow;

      @Override protected void onBoundsChange(Rect bounds) {
        if (bounds.isEmpty()) return;
        base = new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
            colors.background, colors.black ? colors.background
                : WidgetColors.mix(colors.background, dark ? 0xff000000 : 0xffffffff, .18f), Shader.TileMode.CLAMP);
        glow = new RadialGradient(bounds.width() * .83f, bounds.height() * .16f,
            bounds.width() * .91f, alpha(accent, colors.glass ? 24 : 0), alpha(accent, 0), Shader.TileMode.CLAMP);
      }

      @Override public void draw(Canvas canvas) {
        paint.setShader(base);
        canvas.drawRect(getBounds(), paint);
        paint.setShader(glow);
        canvas.drawRect(getBounds(), paint);
      }

      @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }

      @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }

      @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    };
  }
}
