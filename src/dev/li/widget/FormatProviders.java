package dev.li.widget;

/** Separate providers let launchers reserve the correct number of cells. */
public final class FormatProviders {
  private FormatProviders() {}
  public static class Large extends LiWidgetProvider {}
  public static class Minimal extends LiWidgetProvider {}
  public static class Range extends LiWidgetProvider {}
  public static class Favorites extends LiWidgetProvider {}
  public static class FavoritesGrid extends LiWidgetProvider {}
  public static class Charging extends LiWidgetProvider {}
}
