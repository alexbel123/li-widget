package dev.li.widget;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Values displayed by the source widget, not a claim of live vehicle telemetry. */
final class ChargeReading {
  private static final Pattern PERCENT = Pattern.compile("([0-9]{1,3})\\s*[%％]");
  final boolean charging;
  private final String vehicle;
  private final int percent, minutes;

  private ChargeReading(boolean charging, String vehicle, int percent, int minutes) {
    this.charging = charging;
    this.vehicle = vehicle;
    this.percent = percent;
    this.minutes = minutes;
  }

  static ChargeReading read(Context c, View root) {
    if (root == null) return null;
    try {
      Resources res = c.getPackageManager().getResourcesForApplication(State.SOURCE);
      if (!Appearance.visible(field(res, root, "ll_widget_car"))) return null;
      if (!text(res, root, "widget_text").isEmpty()) return null;
      String description = text(res, root, "tv_widget_charge_desc");
      boolean charging = Translations.isCharging(description);
      // Li Auto replaces the EV range with the percent and hides the other percent field.
      String value = text(res, root, charging ? "widget_current_mileage" : "widget_battery_percent");
      Matcher match = PERCENT.matcher(value.replace('\u00a0', ' ').replace('\u202f', ' '));
      int percent = match.matches() ? Integer.parseInt(match.group(1)) : -1;
      if (percent > 100) percent = -1;
      return new ChargeReading(charging, text(res, root, "widget_vehicle_name"), percent,
          charging ? Translations.chargeMinutes(description) : -1);
    } catch (android.content.pm.PackageManager.NameNotFoundException unavailable) {
      return null;
    }
  }

  boolean changedFrom(ChargeReading previous) {
    // First attachment, a different vehicle, and clock-only redraws are not charge updates.
    return charging && previous != null && !vehicle.isEmpty() && vehicle.equals(previous.vehicle)
        && ((percent >= 0 && percent != previous.percent)
            || (minutes >= 0 && minutes != previous.minutes));
  }

  int percentValue() { return percent; }

  private static View field(Resources res, View root, String name) {
    return root.findViewById(res.getIdentifier(name, "id", State.SOURCE));
  }

  private static String text(Resources res, View root, String name) {
    View view = field(res, root, name);
    return view instanceof TextView && Appearance.visible(view)
        ? ((TextView) view).getText().toString().trim() : "";
  }
}
