package dev.li.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

/** Paints the atlas's magenta bodywork while keeping glass, chrome and tires neutral. */
final class VehicleArt {
  private static Bitmap[] bases;

  private static synchronized Bitmap base(Context context, int model) {
    if (bases == null) {
      Bitmap atlas = BitmapFactory.decodeResource(context.getResources(), R.drawable.vehicle_avatar_atlas);
      int splitX = Math.round(atlas.getWidth() * (748f / 1536)), splitY = atlas.getHeight() / 2;
      int[] pixels = new int[atlas.getWidth() * atlas.getHeight()];
      atlas.getPixels(pixels, 0, atlas.getWidth(), 0, 0, atlas.getWidth(), atlas.getHeight());
      bases = new Bitmap[4];
      for (int index = 0; index < bases.length; index++) {
        int left = index % 2 == 0 ? 0 : splitX, right = index % 2 == 0 ? splitX : atlas.getWidth();
        int top = index < 2 ? 0 : splitY, bottom = index < 2 ? splitY : atlas.getHeight();
        Rect bounds = new Rect(right, bottom, left, top);
        for (int y = top; y < bottom; y++) {
          for (int x = left; x < right; x++) {
            if (Color.alpha(pixels[y * atlas.getWidth() + x]) <= 32) continue;
            bounds.left = Math.min(bounds.left, x);
            bounds.right = Math.max(bounds.right, x + 1);
            bounds.top = Math.min(bounds.top, y);
            bounds.bottom = Math.max(bounds.bottom, y + 1);
          }
        }
        if (bounds.isEmpty()) bounds.set(left, top, right, bottom);
        Bitmap art = Bitmap.createBitmap(480, Math.max(1, 480 * bounds.height() / bounds.width()), Bitmap.Config.ARGB_8888);
        new Canvas(art).drawBitmap(atlas, bounds, new Rect(0, 0, art.getWidth(), art.getHeight()), new Paint(Paint.FILTER_BITMAP_FLAG));
        bases[index] = art;
      }
    }
    return bases[model];
  }

  static Bitmap car(Context context, VehiclePaints.PaintOption option) {
    Bitmap base = base(context, option.model);
    int width = base.getWidth(), height = base.getHeight();
    int[] pixels = new int[width * height];
    base.getPixels(pixels, 0, width, 0, 0, width, height);
    for (int i = 0; i < pixels.length; i++) {
      int pixel = pixels[i];
      int red = Color.red(pixel), green = Color.green(pixel), blue = Color.blue(pixel);
      int magenta = Math.min(red, blue) - green;
      if (magenta <= 7 || red <= blue || Color.alpha(pixel) == 0) continue;
      float blend = Math.min(1f, (magenta - 7) / 24f);
      float shade = .32f + .88f * red / 255f;
      float highlight = Math.max(0, green - 105) * .45f;
      int color = i / width < height * .39f ? option.upper : option.body;
      pixels[i] = Color.argb(Color.alpha(pixel),
          recolor(red, Color.red(color), shade, highlight, blend),
          recolor(green, Color.green(color), shade, highlight, blend),
          recolor(blue, Color.blue(color), shade, highlight, blend));
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
  }

  private static int recolor(int original, int target, float shade, float highlight, float blend) {
    return Math.min(255, Math.round(original * (1 - blend) + (target * shade + highlight) * blend));
  }

  static Bitmap icon(Context context, VehiclePaints.PaintOption option, int size) {
    Bitmap icon = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(icon);
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    // Neutral backdrops keep the paint color legible without tinting it. Consider both
    // body and roof so the two-tone options retain contrast as well.
    int light = 0xffe8e8e8, dark = 0xff303030;
    double onLight = Math.min(WidgetColors.contrast(option.body, light), WidgetColors.contrast(option.upper, light));
    double onDark = Math.min(WidgetColors.contrast(option.body, dark), WidgetColors.contrast(option.upper, dark));
    boolean lightBackdrop = onLight >= onDark;
    paint.setShader(new RadialGradient(size * .37f, size * .3f, size * .76f,
        lightBackdrop ? 0xfff7f7f7 : dark,
        lightBackdrop ? light : 0xff181818, Shader.TileMode.CLAMP));
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
    paint.setShader(null);
    float stroke = Math.max(1f, size / 96f);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(stroke);
    paint.setColor(lightBackdrop ? 0xffcccccc : 0xff565656);
    canvas.drawCircle(size / 2f, size / 2f, (size - stroke) / 2f, paint);
    paint.setStyle(Paint.Style.FILL);
    Bitmap car = car(context, option);
    float w = size * .91f, h = w * car.getHeight() / car.getWidth();
    canvas.drawBitmap(car, null, new RectF((size - w) / 2, (size - h) / 2, (size + w) / 2, (size + h) / 2), paint);
    return icon;
  }
}
