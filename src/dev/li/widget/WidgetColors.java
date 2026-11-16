package dev.li.widget;

import android.content.Context;
import android.content.res.Resources;
import java.util.Arrays;

/** Public Android 12 neutral palettes; no wallpaper or other-app access is needed. */
final class WidgetColors {
  final boolean dark, black, dynamic;
  final int background, panel, primary, secondary, icon, outline, surfaceRes, warning, confirmation;

  WidgetColors(Context c, WidgetStyle style) {
    dark = style.dark(c);
    black = style.theme.equals("dark");
    int bg = black ? 0xff000000 : dark ? 0xff1c1c1e : 0xffeceef0;
    int tile = black ? 0xff171717 : dark ? 0xff2b2b2e : 0xfffafbfc;
    int fg = dark ? 0xfff5f5f5 : 0xff191c20;
    int muted = black ? 0xffa6a6a6 : dark ? 0xffb4b4bb : 0xff5b626b;
    int glyph = black ? 0xffe2e2e2 : dark ? 0xffe2e2e5 : 0xff343b42;
    int border = black ? 0xff262626 : dark ? 0xff3b3b40 : 0xffd2d6db;
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
        bg = values[0];
        tile = values[1];
        fg = values[2];
        muted = values[3];
        glyph = values[4];
        border = values[5];
      } catch (Resources.NotFoundException e) {
        fromSystem = false;
      }
    }
    dynamic = fromSystem;
    background = bg;
    panel = tile;
    primary = fg;
    secondary = muted;
    icon = glyph;
    outline = border;
    surfaceRes =
        black
            ? R.drawable.surface_dark
            : dynamic
                ? (dark ? R.drawable.surface_system_dark : R.drawable.surface_system_light)
                : (dark ? R.drawable.surface_dim : R.drawable.surface_light);
    warning = dark ? 0xffe8bd75 : 0xff8c5700;
    confirmation = dark ? 0xff302719 : 0xfff6e8ce;
  }

  int signature() {
    return Arrays.hashCode(
        new int[] {background, panel, primary, secondary, icon, outline, surfaceRes});
  }
}
