package dev.li.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;
import java.util.Locale;

/** One language preference for the app, all its widgets, and its notification. */
final class Language {
  private Language() {}

  static String code(Context context) {
    return "en".equals(State.prefs(context).getString("language", "ru")) ? "en" : "ru";
  }

  static Context context(Context base) {
    String language = code(base);
    if (language.equals(base.getResources().getConfiguration().getLocales().get(0).getLanguage()))
      return base;
    Configuration config = new Configuration();
    // Nested configuration contexts do not inherit the parent's font-scale override.
    config.fontScale = base.getResources().getConfiguration().fontScale;
    Locale locale = Locale.forLanguageTag(language);
    config.setLocales(new LocaleList(locale));
    config.setLayoutDirection(locale);
    return base.createConfigurationContext(config);
  }

  static String text(Context context, int resource, Object... args) {
    return context(context).getString(resource, args);
  }

  static void select(Context context, String language) {
    if (!"ru".equals(language) && !"en".equals(language))
      throw new IllegalArgumentException("Unsupported language");
    State.prefs(context).edit().putString("language", language).apply();
    AppWidgetManager manager = AppWidgetManager.getInstance(context);
    for (int id : State.ownIds(context))
      manager.updateAppWidget(id, BridgeService.forWidget(context, id));
    if (State.connected(context)) State.start(context);
  }
}
