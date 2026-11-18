package dev.li.widget;

import android.content.*;
import android.text.format.DateFormat;
import java.util.*;
import java.util.regex.*;

/** Li Auto provides HH:mm without a date. Never invent a date for a cached stamp. */
final class Freshness {
  final String original;
  final long anchor;
  final long observedSince;

  Freshness(String stamp, long at) { this(stamp, at, 0); }

  private Freshness(String stamp, long at, long observedSince) {
    this.original = stamp;
    this.anchor = at;
    this.observedSince = observedSince;
  }

  static Freshness observe(Context c, String stamp, long now) {
    return observe(c, stamp, now, false);
  }

  static Freshness observe(Context c, String stamp, long now, boolean readingsChanged) {
    SharedPreferences p = State.prefs(c);
    String old = p.getString("fresh_stamp", "");
    long at = p.getLong("fresh_anchor", 0);
    long since = p.getLong("fresh_first_seen", 0);
    if (!stamp.equals(old) || readingsChanged || since == 0 || since > now || at > now) {
      if (!stamp.equals(old)) {
        at = old.isEmpty() ? 0 : anchorForObservedChange(stamp, now, DateFormat.is24HourFormat(c));
      } else if (readingsChanged || since > now || at > now) {
        at = 0;
      }
      since = now;
      p.edit().putString("fresh_stamp", stamp).putLong("fresh_anchor", at)
          .putLong("fresh_first_seen", since).apply();
    }
    return new Freshness(stamp, at, since);
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
    long start = anchor > 0 ? anchor : observedSince;
    return start > 0 && now >= start && now - start >= threshold * 60000L;
  }

  String text(Context c, long now, int threshold) {
    return display(c, now, threshold, false);
  }

  String compactText(Context c, long now, int threshold) {
    return display(c, now, threshold, true);
  }

  private String display(Context c, long now, int threshold, boolean compact) {
    if (anchor <= 0 || now < anchor) {
      if (stale(now, threshold))
        return Language.text(c, R.string.no_new_readings_age, age(c, (now - observedSince) / 60000L));
      Matcher m = Pattern.compile("\\d{1,2}:\\d{2}").matcher(original);
      return m.find()
          ? Language.text(c, compact ? R.string.updated_at_short : R.string.updated_at, m.group())
          : Language.text(
              c, compact ? R.string.update_time_unknown_short : R.string.update_time_unknown);
    }
    long minutes = (now - anchor) / 60000L;
    if (minutes == 0)
      return Language.text(c, compact ? R.string.updated_now_short : R.string.updated_now);
    String age = age(c, minutes);
    if (stale(now, threshold)) return Language.text(c, R.string.stale_age, age);
    return compact ? age : Language.text(c, R.string.updated_age, age);
  }

  private static String age(Context c, long minutes) {
    if (minutes < 60) return Language.text(c, R.string.age_minutes, minutes);
    if (minutes >= 1440) return Language.text(c, R.string.age_days, minutes / 1440);
    String hours = Language.text(c, R.string.age_hours, minutes / 60);
    return minutes % 60 == 0 ? hours : hours + " " + Language.text(c, R.string.age_minutes, minutes % 60);
  }

  static void clear(Context c) {
    State.prefs(c).edit().remove("fresh_stamp").remove("fresh_anchor").remove("fresh_first_seen").apply();
  }
}
