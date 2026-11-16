package dev.li.widget;

import android.content.*;
import android.text.format.DateFormat;
import java.util.*;
import java.util.regex.*;

/** Li Auto provides HH:mm without a date. Never invent a date for a cached stamp. */
final class Freshness {
  final String original;
  final long anchor;

  Freshness(String stamp, long at) {
    original = stamp;
    anchor = at;
  }

  static Freshness observe(Context c, String stamp, long now) {
    SharedPreferences p = State.prefs(c);
    String old = p.getString("fresh_stamp", "");
    long at = p.getLong("fresh_anchor", 0);
    if (!stamp.equals(old)) {
      at =
          old.length() > 0
              ? anchorForObservedChange(
                  stamp, now, DateFormat.is24HourFormat(c.getApplicationContext()))
              : 0;
      p.edit().putString("fresh_stamp", stamp).putLong("fresh_anchor", at).apply();
    }
    if (at > now + 5000) at = 0;
    return new Freshness(stamp, at);
  }

  static long anchorForObservedChange(String stamp, long now, boolean is24) {
    Matcher m = Pattern.compile("(?<!\\d)(\\d{1,2}):(\\d{2})(?!\\d)").matcher(stamp);
    if (!m.find()) return 0;
    int h = Integer.parseInt(m.group(1)), minute = Integer.parseInt(m.group(2));
    if (minute > 59 || h > (is24 ? 23 : 12) || (!is24 && h == 0)) return 0;
    // Only a changed source clock matching this minute or the preceding minute is dated.
    for (int back = 0; back <= 1; back++) {
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(now - back * 60000L);
      int expected = is24 ? cal.get(Calendar.HOUR_OF_DAY) : cal.get(Calendar.HOUR);
      if (!is24 && expected == 0) expected = 12;
      if (h == expected && minute == cal.get(Calendar.MINUTE)) {
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
      }
    }
    return 0;
  }

  boolean stale(long now, int threshold) {
    return anchor > 0 && now >= anchor && now - anchor >= threshold * 60000L;
  }

  String text(Context c, long now, int threshold) {
    return display(c, now, threshold, false);
  }

  String compactText(Context c, long now, int threshold) {
    return display(c, now, threshold, true);
  }

  private String display(Context c, long now, int threshold, boolean compact) {
    if (anchor <= 0 || now < anchor) {
      Matcher m = Pattern.compile("\\d{1,2}:\\d{2}").matcher(original);
      return m.find()
          ? Language.text(c, compact ? R.string.updated_at_short : R.string.updated_at, m.group())
          : Language.text(
              c, compact ? R.string.update_time_unknown_short : R.string.update_time_unknown);
    }
    long minutes = (now - anchor) / 60000L;
    if (minutes == 0)
      return Language.text(c, compact ? R.string.updated_now_short : R.string.updated_now);
    String age;
    if (minutes < 60) age = Language.text(c, R.string.age_minutes, minutes);
    else if (minutes < 1440)
      age =
          Language.text(c, R.string.age_hours, minutes / 60)
              + (minutes % 60 > 0
                  ? " " + Language.text(c, R.string.age_minutes, minutes % 60)
                  : "");
    else age = Language.text(c, R.string.age_days, minutes / 1440);
    if (stale(now, threshold)) return Language.text(c, R.string.stale_age, age);
    return compact ? age : Language.text(c, R.string.updated_age, age);
  }

  static void clear(Context c) {
    State.prefs(c).edit().remove("fresh_stamp").remove("fresh_anchor").apply();
  }
}
