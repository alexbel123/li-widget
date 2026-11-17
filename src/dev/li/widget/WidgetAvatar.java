package dev.li.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.util.AtomicFile;
import android.util.LruCache;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

final class WidgetAvatar {
  private static final int SIZE = 128;
  private static final String PREFIX = "photo.";
  private static final LruCache<String, Bitmap> ICONS = new LruCache<>(12);

  static boolean isPhoto(String key) {
    return key != null && key.matches("photo\\.[0-9a-f]{32}");
  }

  private static File file(Context context, String key) {
    return new File(new File(context.getFilesDir(), "avatars"), key + ".png");
  }

  static String importPhoto(Context context, Uri uri) throws IOException {
    if (uri == null || !"content".equals(uri.getScheme())) throw new IOException("Missing photo");
    Bitmap original =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), uri),
            (decoder, info, source) -> {
              int width = info.getSize().getWidth(), height = info.getSize().getHeight();
              if (width < 1 || height < 1 || (long) width * height > 100_000_000)
                throw new IllegalArgumentException("Unsupported image size");
              float scale = Math.min(1f, 384f / Math.max(width, height));
              decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
              decoder.setTargetSize(Math.max(1, Math.round(width * scale)), Math.max(1, Math.round(height * scale)));
            });
    int side = Math.min(original.getWidth(), original.getHeight());
    Bitmap crop = Bitmap.createBitmap(original, (original.getWidth() - side) / 2,
        (original.getHeight() - side) / 2, side, side);
    Bitmap avatar = Bitmap.createScaledBitmap(crop, SIZE, SIZE, true);
    String key = PREFIX + UUID.randomUUID().toString().replace("-", "");
    File destination = file(context, key);
    if (!destination.getParentFile().isDirectory() && !destination.getParentFile().mkdirs())
      throw new IOException("Cannot create avatar directory");
    AtomicFile out = new AtomicFile(destination);
    FileOutputStream stream = out.startWrite();
    try {
      if (!avatar.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw new IOException("Cannot encode avatar");
      out.finishWrite(stream);
      return key;
    } catch (IOException | RuntimeException error) {
      out.failWrite(stream);
      throw error;
    }
  }

  static Bitmap icon(Context context, String key) {
    if (key == null || key.isEmpty()) return null;
    Bitmap cached = ICONS.get(key);
    if (cached != null) return cached;
    Bitmap icon;
    VehiclePaints.PaintOption option = VehiclePaints.find(key);
    if (option != null) icon = VehicleArt.icon(context, option, SIZE);
    else if (isPhoto(key)) {
      Bitmap photo = BitmapFactory.decodeFile(file(context, key).getAbsolutePath());
      if (photo == null) return null;
      icon = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
      Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
      paint.setShader(new BitmapShader(photo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
      new Canvas(icon).drawCircle(SIZE / 2f, SIZE / 2f, SIZE / 2f, paint);
    } else return null;
    ICONS.put(key, icon);
    return icon;
  }

  static void removeIfUnused(Context context, String key) {
    if (!isPhoto(key) || State.prefs(context).getAll().containsValue(key)) return;
    new AtomicFile(file(context, key)).delete();
    ICONS.remove(key);
  }
}
