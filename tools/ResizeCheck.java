package dev.li.widget;

/** Launch-size contracts against the same compiled classes used in the APK. */
public final class ResizeCheck {
  private static void check(boolean valid, String label) {
    if (!valid) throw new AssertionError(label);
  }

  public static void main(String[] args) {
    WidgetStyle style = new WidgetStyle();
    int[][] frames = {{176, 64}, {352, 72}, {176, 176}, {373, 210}, {373, 321}, {373, 432}, {277, 432}, {176, 432}};
    WidgetFormat[] expected = {WidgetFormat.MINIMAL, WidgetFormat.FAVORITES, WidgetFormat.RANGE,
        WidgetFormat.FULL, WidgetFormat.LARGE, WidgetFormat.LARGE, WidgetFormat.LARGE, WidgetFormat.LARGE};
    for (int i = 0; i < frames.length; i++) {
      int w = frames[i][0], h = frames[i][1];
      check(style.format.presentation(style, w, h, 1f) == expected[i], w + "x" + h);
      check(style.heightWithin(h) == h, "Frame height");
    }
    check(WidgetFormat.actionRows(373, 321) == 1, "Wide overview");
    check(WidgetFormat.actionRows(373, 432) == 2, "Tall overview");
    check(WidgetFormat.actionRows(176, 176) == 2, "Square favorites");
    check(WidgetFormat.actionRows(352, 72) == 1, "Wide favorites");
    check(WidgetFormat.firstRowCount(110, 1) == 1 && WidgetFormat.firstRowCount(176, 1) == 2,
        "Space for the next-page target");
    for (String layout : new String[]{"compact", "auto", "detailed"}) {
      style.layout = layout;
      for (int w : new int[]{110, 176, 208, 238, 277, 322, 373, 560}) {
        for (int h = 48; h <= 480; h++) {
          for (float scale : new float[]{1f, 1.3f, 1.5f}) {
            if (style.format.presentation(style, w, h, scale) != WidgetFormat.LARGE) continue;
            WidgetFormat.OverviewSize size = new WidgetFormat.OverviewSize(w, h, 32);
            check(size.header + size.hero + size.actions + size.footer + 8 == h, "Allocated height");
            check(size.hero >= 30, "Metric height");
            check((size.actions - size.rows + 1) / size.rows >= 48, "Control height");
            int columns = 4 / size.rows;
            check((w - 2 * size.inset - 8 - (columns - 1)) / columns >= 48, "Control width");
          }
        }
      }
    }
    System.out.println("PASS: adaptive presentations, frame allocation and control geometry");
  }
}
