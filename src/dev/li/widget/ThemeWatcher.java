package dev.li.widget;

import android.app.UiModeManager;
import android.content.Context;
import android.os.Build;

/** Contrast updates do not require reading wallpaper files or private settings. */
final class ThemeWatcher {
  private final Runnable unsubscribe;

  ThemeWatcher(Context context, Runnable changed) {
    unsubscribe = Build.VERSION.SDK_INT >= 34 ? Api34.subscribe(context, changed) : () -> {};
  }

  void close() { unsubscribe.run(); }

  private static final class Api34 {
    static Runnable subscribe(Context context, Runnable changed) {
      UiModeManager modes = context.getSystemService(UiModeManager.class);
      if (modes == null) return () -> {};
      UiModeManager.ContrastChangeListener listener = contrast -> changed.run();
      modes.addContrastChangeListener(context.getMainExecutor(), listener);
      return () -> modes.removeContrastChangeListener(listener);
    }
  }
}
