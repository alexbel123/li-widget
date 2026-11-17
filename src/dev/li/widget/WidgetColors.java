package dev.li.widget;

import android.content.Context;
import android.content.res.Resources;
import android.app.UiModeManager;
import android.os.Build;
import java.util.Arrays;

/** Public Android 12 neutral palettes; no wallpaper or other-app access is needed. */
final class WidgetColors {
  final boolean dark, black, dynamic, glass, highContrast;
  final int background, panel, primary, secondary, icon, outline, surfaceRes, warning, confirmation;
  final int accent, onAccent, accentContainer, onAccentContainer, chargeAccent;

  WidgetColors(Context c, WidgetStyle style) {
    dark = style.dark(c);
    black = style.theme.equals("dark");
    UiModeManager modes = c.getSystemService(UiModeManager.class);
    highContrast = Build.VERSION.SDK_INT >= 34 && modes != null && modes.getContrast() >= .5f;
    glass = style.surface.equals("glass") && !black && !highContrast;
    int bg = black ? 0xff000000 : dark ? 0xff20201f : 0xffeeede9;
    int tile = black ? 0xff191919 : dark ? 0xff2d2c2a : 0xfff8f7f3;
    int fg = black ? 0xfff5f5f5 : dark ? 0xfff5f3ef : 0xff242320;
    int muted = black ? 0xffa6a6a6 : dark ? 0xffb9b5af : 0xff5e5b54;
    int glyph = black ? 0xffe2e2e2 : dark ? 0xffe7e3dd : 0xff393631;
    int border = black ? 0xff303030 : dark ? 0xff45423e : 0xffd5d1c8;
    int accentColor = dark ? 0xffe0c5a1 : 0xff6b4e2b;
    int accentInk = dark ? 0xff2b2117 : 0xfffff9f0;
    int accentFill = dark ? 0xff3b3228 : 0xffeee4d4;
    int selectedInk = accentColor;
    boolean fromSystem = style.palette.equals("system") && !black;
    if (fromSystem) {
      try {
        int[] values = {
          c.getColor(
              dark ? android.R.color.system_neutral1_900 : android.R.color.system_neutral1_100),
          c.getColor(
              dark ? android.R.color.system_neutral1_800 : android.R.color.system_neutral1_50),
          c.getColor(
              dark ? android.R.color.system_neutral1_50 : android.R.color.system_neutral1_900),
          c.getColor(
              dark ? android.R.color.system_neutral2_200 : android.R.color.system_neutral2_700),
          c.getColor(
              dark ? android.R.color.system_neutral1_100 : android.R.color.system_neutral1_800),
          c.getColor(
              dark ? android.R.color.system_neutral2_700 : android.R.color.system_neutral2_200)
        };
        int nextAccent = c.getColor(dark ? android.R.color.system_accent1_200 : android.R.color.system_accent1_600);
        int nextInk = c.getColor(dark ? android.R.color.system_accent1_900 : android.R.color.system_accent1_0);
        int nextFill = c.getColor(dark ? android.R.color.system_accent1_800 : android.R.color.system_accent1_100);
        int nextSelectedInk = c.getColor(dark ? android.R.color.system_accent1_100 : android.R.color.system_accent1_900);
        if (Build.VERSION.SDK_INT >= 34) {
          nextAccent = c.getColor(dark ? android.R.color.system_primary_dark : android.R.color.system_primary_light);
          nextInk = c.getColor(dark ? android.R.color.system_on_primary_dark : android.R.color.system_on_primary_light);
          nextFill = c.getColor(dark ? android.R.color.system_primary_container_dark : android.R.color.system_primary_container_light);
          nextSelectedInk = c.getColor(dark ? android.R.color.system_on_primary_container_dark : android.R.color.system_on_primary_container_light);
        }
        bg = values[0];
        tile = values[1];
        fg = values[2];
        muted = values[3];
        glyph = values[4];
        border = values[5];
        accentColor = nextAccent;
        accentInk = nextInk;
        accentFill = nextFill;
        selectedInk = nextSelectedInk;
      } catch (Resources.NotFoundException e) {
        fromSystem = false;
      }
    }
    dynamic = fromSystem;
    if (highContrast) {
      muted = fg;
      glyph = fg;
      border = mix(border, fg, .35f);
    }
    background = bg;
    panel = tile;
    primary = fg;
    secondary = muted;
    icon = glyph;
    outline = border;
    accent = readable(accentColor, background);
    onAccent = readable(accentInk, accent);
    accentContainer = accentFill;
    onAccentContainer = readable(selectedInk, accentContainer);
    chargeAccent = dynamic ? accent : dark ? 0xffb5cfa9 : 0xff3a6447;
    surfaceRes =
        black
            ? R.drawable.surface_dark
            : glass
                ? dynamic
                    ? (dark ? R.drawable.surface_glass_system_dark : R.drawable.surface_glass_system_light)
                    : (dark ? R.drawable.surface_glass_dim : R.drawable.surface_glass_light)
            : dynamic
                ? (dark ? R.drawable.surface_system_dark : R.drawable.surface_system_light)
                : (dark ? R.drawable.surface_dim : R.drawable.surface_light);
    warning = dark ? 0xffe8bd75 : 0xff8c5700;
    confirmation = dark ? 0xff302719 : 0xfff6e8ce;
  }

  int signature() {
    return Arrays.hashCode(
        new int[] {background, panel, primary, secondary, icon, outline, surfaceRes,
            accent, onAccent, accentContainer, onAccentContainer, highContrast ? 1 : 0});
  }

  static int mix(int a, int b, float amount) {
    int r = Math.round(android.graphics.Color.red(a) * (1 - amount) + android.graphics.Color.red(b) * amount);
    int g = Math.round(android.graphics.Color.green(a) * (1 - amount) + android.graphics.Color.green(b) * amount);
    int blue = Math.round(android.graphics.Color.blue(a) * (1 - amount) + android.graphics.Color.blue(b) * amount);
    return android.graphics.Color.rgb(r, g, blue);
  }

  private static int readable(int foreground, int background) {
    if (contrast(foreground, background) >= 4.5) return foreground;
    int extreme = luminance(background) > .18 ? 0xff000000 : 0xffffffff;
    for (int step = 1; step <= 10; step++) {
      int candidate = mix(foreground, extreme, step / 10f);
      if (contrast(candidate, background) >= 4.5) return candidate;
    }
    return extreme;
  }

  static double contrast(int first, int second) {
    double a = luminance(first), b = luminance(second);
    return (Math.max(a, b) + .05) / (Math.min(a, b) + .05);
  }

  private static double luminance(int color) {
    return .2126 * linear(android.graphics.Color.red(color))
        + .7152 * linear(android.graphics.Color.green(color))
        + .0722 * linear(android.graphics.Color.blue(color));
  }

  private static double linear(int channel) {
    double value = channel / 255d;
    return value <= .04045 ? value / 12.92 : Math.pow((value + .055) / 1.055, 2.4);
  }
}
