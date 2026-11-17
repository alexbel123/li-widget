package dev.li.widget;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

/** China-market paint options. Display colors are illustrative, not paint codes. */
final class VehiclePaints {
  static final String[] MODELS = {"L6", "L7", "L8", "L9"};
  static final String DEFAULT = "l7.blue-gray";

  static final class PaintOption {
    final int model, body, upper;
    final String key, russian, english;

    PaintOption(int model, String key, String russian, String english, int body, int upper) {
      this.model = model;
      this.key = MODELS[model].toLowerCase(java.util.Locale.ROOT) + "." + key;
      this.russian = russian;
      this.english = english;
      this.body = body;
      this.upper = upper;
    }

    String label(Context context) {
      return Language.code(context).equals("ru") ? russian : english;
    }
  }

  static List<PaintOption> forModel(int model) {
    List<PaintOption> result = new ArrayList<>();
    for (PaintOption option : OPTIONS) if (option.model == model) result.add(option);
    return result;
  }

  static PaintOption find(String key) {
    for (PaintOption option : OPTIONS) if (option.key.equals(key)) return option;
    return null;
  }

  private static PaintOption single(int model, String key, String ru, String en, int color) {
    return new PaintOption(model, key, ru, en, color, color);
  }

  private static PaintOption livis(int model, String key, String ru, String en, int color) {
    return new PaintOption(model, key, ru, en, color, 0xffae9678);
  }

  // L6 (2026): lixiang.com/l6. L7 (Smart Refresh): Li Auto model/configuration guide.
  // L8/L9 (2026): lixiang.com/l8 and lixiang.com/l9, including the four Livis two-tones.
  private static final PaintOption[] OPTIONS = {
    single(0, "pearl-white", "Жемчужно-белый", "Pearl White", 0xffe8e7df),
    single(0, "gt-silver", "Серебро GT", "GT Silver", 0xffaeb7bd),
    single(0, "meteorite-gray", "Метеоритовый серый", "Meteorite Gray", 0xff555d61),
    single(0, "obsidian-black", "Чёрный обсидиан", "Obsidian Black", 0xff23262b),
    single(0, "elephant-gray", "Слоновый серый", "Elephant Gray", 0xff87867d),
    single(0, "dune-yellow", "Песчаная дюна", "Dune Yellow", 0xffb5a06b),
    single(0, "hyacinth-purple", "Сирень гиацинта", "Hyacinth Purple", 0xff79728a),
    single(1, "silver", "Серебристый металлик", "Silver Metallic", 0xffaeb6bb),
    single(1, "gray", "Серый металлик", "Gray Metallic", 0xff5a6168),
    single(1, "black", "Чёрный металлик", "Black Metallic", 0xff24262b),
    single(1, "pearl-white", "Жемчужно-белый", "Pearl White", 0xffe5e5df),
    single(1, "pearl-green", "Зелёный перламутр", "Green Pearl", 0xff526b59),
    single(1, "baby-blue", "Небесно-голубой", "Baby Blue", 0xffabc2cf),
    single(1, "blue-gray", "Сине-серый", "Blue Gray", 0xff748997),
    single(1, "winter-red", "Зимний красный · 2023", "Winter Red · 2023", 0xff8d3940),
    single(2, "gt-silver", "Серебро GT", "GT Silver", 0xffaab1b7),
    single(2, "diamond-black", "Бриллиантовый чёрный", "Diamond Black", 0xff25262c),
    single(2, "pearl-white", "Жемчужно-белый", "Pearl White", 0xffe7e7e2),
    single(2, "glacier-gray", "Ледниковый серый", "Glacier Gray", 0xff747d85),
    single(2, "emerald-green", "Изумрудно-зелёный", "Emerald Green", 0xff285e53),
    single(2, "truffle-gold", "Трюфельное золото", "Truffle Gold", 0xff67554a),
    livis(2, "livis-brown", "Коричневый / золото", "Brown / Gold", 0xff67554a),
    livis(2, "livis-black", "Чёрный / золото", "Black / Gold", 0xff25262c),
    livis(2, "livis-green", "Зелёный / золото", "Green / Gold", 0xff285e53),
    livis(2, "livis-purple", "Фиолетовый / золото", "Purple / Gold", 0xff514359),
    single(3, "pearl-white", "Жемчужно-белый", "Pearl White", 0xffe8e7e1),
    single(3, "polar-gray", "Полярный серый", "Polar Gray", 0xff7a848b),
    single(3, "diamond-black", "Бриллиантовый чёрный", "Diamond Black", 0xff25262c),
    single(3, "titanium-gold", "Титановое золото", "Titanium Gold", 0xffad967a),
    single(3, "emerald-green", "Изумрудно-зелёный", "Emerald Green", 0xff285e53),
    single(3, "truffle-gold", "Трюфельное золото", "Truffle Gold", 0xff67554a),
    single(3, "midnight-purple", "Полуночный фиолетовый", "Midnight Purple", 0xff514359),
    livis(3, "livis-brown", "Коричневый / золото", "Brown / Gold", 0xff67554a),
    livis(3, "livis-black", "Чёрный / золото", "Black / Gold", 0xff25262c),
    livis(3, "livis-green", "Зелёный / золото", "Green / Gold", 0xff285e53),
    livis(3, "livis-purple", "Фиолетовый / золото", "Purple / Gold", 0xff514359),
  };
}
