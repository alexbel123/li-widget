package dev.li.widget;

import android.content.Context;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Translates known widget text without changing vehicle names or original actions. */
final class Translations {
  private static final Map<String, Integer> RESOURCES = new LinkedHashMap<>();
  private static final Map<String, Integer> WORDS = new LinkedHashMap<>();
  private static final Map<Integer, Integer> DESCRIPTIONS = new LinkedHashMap<>();
  private static final String CHARGE_CALCULATING = "充电剩余时间计算中";
  private static final Pattern CHARGE_HOURS = Pattern.compile("剩余\\s*(\\d{1,3})\\s*小时\\s*(\\d{1,3})\\s*分钟");
  private static final Pattern CHARGE_MINUTES = Pattern.compile("剩余\\s*(\\d{1,3})\\s*分钟");

  static {
    entry("button_confirm_text", R.string.action_confirm, "再次点击", "点击确认", "再次点击确认");
    entry("widget_button_lock", R.string.action_locks, "车锁");
    entry("widget_button_unlock", R.string.action_unlock, "解锁");
    entry("widget_button_ac", R.string.action_climate, "空调");
    entry("widget_button_trunk", R.string.action_tailgate, "尾门");
    entry("widget_button_summon", R.string.action_move, "直线召唤");
    entry("widget_button_remote", R.string.action_access, "授权驾驶");
    entry("widget_button_window", R.string.action_windows, "车窗");
    entry("widget_button_search", R.string.action_find, "寻车");
    entry("widget_button_wheel", R.string.action_wheel, "方向盘加热");
    entry("widget_button_deicing", R.string.action_defrost, "除雪除冰");
    entry("widget_button_unknown", R.string.action_unknown, "未知");
    entry("widget_button_wake_up", R.string.action_wake, "唤醒");
    entry("widget_button_refresh", R.string.action_refresh, "刷新");
    entry("widget_login_test", R.string.source_sign_in, "登录解锁移动的家");
    entry("widget_no_car_test", R.string.source_no_car, "无车辆");
    entry(
        "widget_login_expired", R.string.source_login_expired, "登录已失效\n请重新登录", "\"登录已失效\n请重新登录\"");
    entry("widget_oppo_non_valable_vehicle", R.string.source_no_available_car, "无可用车辆");
    entry("widget_oppo_non_support_family", R.string.source_family_unsupported, "家人共享车辆暂未支持");
    entry("widget_un_car", R.string.source_select_car, "选择我的车辆", "未选择车辆");
    entry("widget_toast_no_vehicle", R.string.source_select_car);
    entry("widget_un_login", R.string.source_login, "请前往App内登录");
    entry("widget_status_offline", R.string.source_offline, "状态: 离线");
    entry("widget_status_online", R.string.source_online, "状态: 在线");
    entry("widget_toast_ac_toggling", R.string.source_climate_busy, "正在切换空调…");
    entry("widget_toast_control_not_init", R.string.source_unavailable, "控制接口未初始化");
    entry("widget_toast_locking", R.string.source_locking, "正在锁车…", "正在上锁");
    entry("widget_toast_unlocking", R.string.source_unlocking, "正在解锁…", "正在解锁");
    entry("widget_toast_refreshing", R.string.source_refreshing, "正在刷新…", "正在刷新...");
    entry("card_charge_time", R.string.source_charge_calculating, CHARGE_CALCULATING);
    WORDS.put("无法充电", R.string.source_charge_error);
    WORDS.put("执行成功", R.string.done);
    WORDS.put("理想汽车", R.string.li_auto);
    DESCRIPTIONS.put(R.string.action_locks, R.string.desc_locks);
    DESCRIPTIONS.put(R.string.action_unlock, R.string.desc_unlock);
    DESCRIPTIONS.put(R.string.action_climate, R.string.desc_climate);
    DESCRIPTIONS.put(R.string.action_tailgate, R.string.desc_tailgate);
    DESCRIPTIONS.put(R.string.action_move, R.string.desc_move);
    DESCRIPTIONS.put(R.string.action_access, R.string.desc_access);
    DESCRIPTIONS.put(R.string.action_windows, R.string.desc_windows);
    DESCRIPTIONS.put(R.string.action_find, R.string.desc_find);
    DESCRIPTIONS.put(R.string.action_wheel, R.string.desc_wheel);
    DESCRIPTIONS.put(R.string.action_defrost, R.string.desc_defrost);
    DESCRIPTIONS.put(R.string.action_confirm, R.string.desc_confirm);
    DESCRIPTIONS.put(R.string.action_refresh, R.string.desc_refresh);
  }

  private static void entry(String source, int target, String... aliases) {
    RESOURCES.put(source, target);
    for (String alias : aliases) WORDS.put(alias, target);
  }

  static String translate(Context local, Context source, String field, String value) {
    // A user-defined car name and the source clock are data, not UI labels.
    if (field.equals("widget_vehicle_name") || field.equals("widget_update_time")) return value;
    Integer known = WORDS.get(value);
    if (known != null) return Language.text(local, known);
    for (Map.Entry<String, Integer> item : RESOURCES.entrySet()) {
      int id = source.getResources().getIdentifier(item.getKey(), "string", State.SOURCE);
      if (id != 0 && value.equals(source.getString(id)))
        return Language.text(local, item.getValue());
    }
    if (field.equals("tv_widget_charge_desc")) {
      Matcher hours = CHARGE_HOURS.matcher(value);
      if (hours.matches())
        return Language.text(
            local,
            R.string.source_charge_hours,
            Integer.parseInt(hours.group(1)),
            Integer.parseInt(hours.group(2)));
      Matcher minutes = CHARGE_MINUTES.matcher(value);
      if (minutes.matches())
        return Language.text(
            local, R.string.source_charge_minutes, Integer.parseInt(minutes.group(1)));
    }
    if (field.equals("widget_current_mileage") || field.equals("widget_total_mileage")) {
      return value.replaceAll(
          "(?<=\\d)\\s*(?:km|км)\\b", " " + Language.text(local, R.string.unit_km));
    }
    // Unknown messages are retained so a real source error is never hidden.
    return value;
  }

  static boolean isCharging(CharSequence value) {
    String status = value.toString().trim();
    return status.equals(CHARGE_CALCULATING)
        || CHARGE_HOURS.matcher(status).matches()
        || CHARGE_MINUTES.matcher(status).matches();
  }

  static int chargeMinutes(CharSequence value) {
    String status = value.toString().trim();
    Matcher hours = CHARGE_HOURS.matcher(status);
    if (hours.matches())
      return Integer.parseInt(hours.group(1)) * 60 + Integer.parseInt(hours.group(2));
    Matcher minutes = CHARGE_MINUTES.matcher(status);
    return minutes.matches() ? Integer.parseInt(minutes.group(1)) : -1;
  }

  static String description(Context context, String label) {
    String normalized = label.replace('\n', ' ');
    for (Map.Entry<Integer, Integer> item : DESCRIPTIONS.entrySet()) {
      if (normalized.equals(Language.text(context, item.getKey()).replace('\n', ' ')))
        return Language.text(context, item.getValue());
    }
    return normalized;
  }
}
