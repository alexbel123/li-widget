# Device themes

The widget, editor and avatar gallery share a palette on Android 12 and later.

- **System theme** follows the device light/dark mode. Explicit Light, Dark and Black choices remain available per widget.
- **Android palette** reads public system neutral and accent colors. On Android 14 and later, semantic primary/container colors are used for accents. Neutral uses the app's restrained warm gray/graphite palette. Black retains an opaque black background.
- **Matte / Glass** selects a solid surface or a lightly translucent gradient with a fine outline. Glass is a visual treatment, not a wallpaper-blur API. No wallpaper image is read.
- In light mode with the Android palette, Glass uses deeper system neutral tones and a slightly brighter control panel. These tones follow the device palette.
- Corner radii use `android:dimen/system_app_widget_background_radius` so the surface follows the platform's widget geometry.
- On Android 14+, high device contrast strengthens text/icons and disables translucency. Contrast listeners are removed with the activity/service lifecycle. Night/palette configuration changes and activity resume re-resolve system resources.
- Existing animation settings and the system animation enablement still apply. Charging pulses only when the displayed charge percentage or remaining time changes.

System themes from device vendors can expose different palettes. Proprietary icon packs, launcher shaders, custom blur and theme-store artwork are not imported. When the required palette resources are unavailable, the neutral palette is used.

No additional permissions, network requests, analytics or access to wallpaper files are required. Original vehicle actions and their confirmation behavior remain with Li Auto.

Sources: [Android widget dynamic colors](https://developer.android.com/develop/ui/views/appwidgets/enhance), [UiModeManager contrast](https://developer.android.com/reference/android/app/UiModeManager), [system widget corner radius](https://developer.android.com/reference/android/R.dimen#system_app_widget_background_radius), [Android window blur scope](https://source.android.com/docs/core/display/window-blurs).
