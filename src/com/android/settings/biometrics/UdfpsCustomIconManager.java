package com.android.settings.biometrics;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class UdfpsCustomIconManager {

    private static final String TAG = "UdfpsCustomIconManager";

    public static final String KEY_UDFPS_ICON_STYLE = "udfps_icon_style";
    public static final String KEY_UDFPS_CUSTOM_ICON_PATH = "udfps_custom_icon_path";
    public static final String KEY_UDFPS_ICON_RESOURCE_NAME = "udfps_icon_resource_name";

    public static final int STYLE_AOSP = 0;
    public static final int STYLE_CUSTOM_IMAGE = 4;

    public static final String CUSTOM_IMAGE_PATH = "/data/system/udfps_custom_icon.webp";
    public static final String CUSTOM_IMAGE_LEGACY_PNG = "/data/system/udfps_custom_icon.png";

    public static int getIconStyle(Context context) {
        return Settings.System.getInt(context.getContentResolver(), KEY_UDFPS_ICON_STYLE, STYLE_AOSP);
    }

    public static void setIconStyle(Context context, int style) {
        if (style == STYLE_AOSP) {
            cleanPreviousFiles(context);
        }
        Settings.System.putInt(context.getContentResolver(), KEY_UDFPS_ICON_STYLE, style);
    }

    public static void cleanPreviousFiles(Context context) {
        try {
            if (context != null) {
                File internalDest = new File(context.getFilesDir(), "udfps_custom_icon.webp");
                if (internalDest.exists()) internalDest.delete();
            }
            File imgWebp = new File(CUSTOM_IMAGE_PATH);
            if (imgWebp.exists()) imgWebp.delete();
            File imgPng = new File(CUSTOM_IMAGE_LEGACY_PNG);
            if (imgPng.exists()) imgPng.delete();
        } catch (Exception ignored) {}
    }

    public static void resetToAosp(Context context) {
        cleanPreviousFiles(context);
        Settings.System.putString(context.getContentResolver(), KEY_UDFPS_CUSTOM_ICON_PATH, "");
        Settings.System.putString(context.getContentResolver(), KEY_UDFPS_ICON_RESOURCE_NAME, "");
        setIconStyle(context, STYLE_AOSP);
    }

    public static boolean saveCustomImage(Context context, Uri uri) {
        cleanPreviousFiles(context);
        Bitmap src = null;

        // 1. Direct stream reading from ContentResolver (reliable across all pickers)
        try {
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] temp = new byte[16384];
                    int nRead;
                    while ((nRead = is.read(temp, 0, temp.length)) != -1) {
                        buffer.write(temp, 0, nRead);
                    }
                    buffer.flush();
                    byte[] rawBytes = buffer.toByteArray();
                    if (rawBytes.length > 0) {
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inJustDecodeBounds = true;
                        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.length, opts);

                        opts.inSampleSize = 1;
                        int maxDim = Math.max(opts.outWidth, opts.outHeight);
                        while (maxDim / opts.inSampleSize > 2048) {
                            opts.inSampleSize *= 2;
                        }
                        opts.inJustDecodeBounds = false;
                        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        src = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.length, opts);
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Stream decode failed, trying ImageDecoder: " + t.getMessage());
        }

        // 2. Fallback to ImageDecoder
        if (src == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
                src = ImageDecoder.decodeBitmap(source, (decoder, info, s) -> {
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                });
            } catch (Throwable t) {
                Log.e(TAG, "ImageDecoder decode failed: " + t.getMessage(), t);
            }
        }

        if (src == null) {
            Log.e(TAG, "Failed to decode bitmap from uri: " + uri);
            return false;
        }

        try {
            // 3. Create exact 720x720 px circular masked bitmap
            final int TARGET_SIZE = 720;
            Bitmap circularBitmap = Bitmap.createBitmap(TARGET_SIZE, TARGET_SIZE, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(circularBitmap);

            Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
            maskPaint.setColor(0xFFFFFFFF);

            // Draw antialiased circle mask (radius 360px)
            float center = TARGET_SIZE / 2.0f;
            canvas.drawCircle(center, center, center, maskPaint);

            // Apply PorterDuff SRC_IN to clip the image to the circle
            maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

            int srcWidth = src.getWidth();
            int srcHeight = src.getHeight();
            int minDim = Math.min(srcWidth, srcHeight);
            int srcLeft = (srcWidth - minDim) / 2;
            int srcTop = (srcHeight - minDim) / 2;
            Rect srcRect = new Rect(srcLeft, srcTop, srcLeft + minDim, srcTop + minDim);
            Rect dstRect = new Rect(0, 0, TARGET_SIZE, TARGET_SIZE);

            canvas.drawBitmap(src, srcRect, dstRect, maskPaint);

            ByteArrayOutputStream webpStream = new ByteArrayOutputStream();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                circularBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 95, webpStream);
            } else {
                circularBitmap.compress(Bitmap.CompressFormat.PNG, 100, webpStream);
            }
            byte[] webpBytes = webpStream.toByteArray();

            // 1. Save in Settings app files dir (guaranteed writable)
            File internalDest = new File(context.getFilesDir(), "udfps_custom_icon.webp");
            try (FileOutputStream fos = new FileOutputStream(internalDest)) {
                fos.write(webpBytes);
                fos.flush();
            }
            internalDest.setReadable(true, false);
            internalDest.setWritable(true, false);

            // 2. Also save to /data/system/udfps_custom_icon.webp
            try {
                File sysDest = new File(CUSTOM_IMAGE_PATH);
                try (FileOutputStream fos = new FileOutputStream(sysDest)) {
                    fos.write(webpBytes);
                    fos.flush();
                }
                sysDest.setReadable(true, false);
                sysDest.setWritable(true, false);
            } catch (Exception ignored) {}

            Settings.System.putString(context.getContentResolver(), KEY_UDFPS_ICON_RESOURCE_NAME, "");
            Settings.System.putString(context.getContentResolver(), KEY_UDFPS_CUSTOM_ICON_PATH, internalDest.getAbsolutePath());
            setIconStyle(context, STYLE_CUSTOM_IMAGE);

            Log.i(TAG, "Custom UDFPS icon circular 720x720 saved: " + internalDest.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving 720x720 circular custom icon", e);
            return false;
        }
    }

    public static Bitmap loadCustomImageBitmap(Context context) {
        if (context == null) return null;
        try {
            // 1. Try file path from settings
            String path = Settings.System.getString(context.getContentResolver(), KEY_UDFPS_CUSTOM_ICON_PATH);
            if (path != null && !path.isEmpty()) {
                File f = new File(path);
                if (f.exists()) {
                    Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
                    if (b != null) return b;
                }
            }
            // 2. Try internal files dir
            File internalDest = new File(context.getFilesDir(), "udfps_custom_icon.webp");
            if (internalDest.exists()) {
                Bitmap b = BitmapFactory.decodeFile(internalDest.getAbsolutePath());
                if (b != null) return b;
            }
            // 3. Try /data/system/
            File sysDest = new File(CUSTOM_IMAGE_PATH);
            if (sysDest.exists()) {
                Bitmap b = BitmapFactory.decodeFile(sysDest.getAbsolutePath());
                if (b != null) return b;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error loading custom UDFPS icon bitmap", e);
        }
        return null;
    }

    public static Drawable getLockscreenWallpaper(Context context) {
        try {
            WallpaperManager wm = WallpaperManager.getInstance(context);
            Drawable d = wm.getDrawable();
            if (d != null) return d;
        } catch (Exception ignored) {}
        return null;
    }
}
