package com.airtel.core;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class ZxingUtils {
    private static String TAG = ZxingUtils.class.getSimpleName();


    /**
     * Check if the device's camera has a Flashlight.
     * @return true if there is Flashlight, otherwise false.
     */
    public static boolean hasFlash(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    public static Point getScreenResolution(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;

        Point screenResolution = new Point();
        if (android.os.Build.VERSION.SDK_INT >= 13) {
            display.getSize(screenResolution);
        } else {
            screenResolution.set(width, height);
        }
        return screenResolution;
    }

    public static int getScreenOrientation(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = wm.getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && height > width
                || (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && width > height) {
            switch (rotation) {

                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    orientation = Configuration.ORIENTATION_LANDSCAPE;
                    break;

                default:
                    orientation = Configuration.ORIENTATION_PORTRAIT;

            }
        }
        // if the device's natural orientation is landscape or if the device is square:
        else {
            switch (rotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    orientation = Configuration.ORIENTATION_LANDSCAPE;
                    break;
                default:
                    Log.e(TAG, "Unknown screen orientation. Defaulting to " + "portrait.");
                    orientation = Configuration.ORIENTATION_LANDSCAPE;
                    break;
            }
        }
        return orientation;
    }

//    private int getScreenOrientation(Context context) {
//        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//        int rotation = wm.getDefaultDisplay().getRotation();
//        DisplayMetrics dm = new DisplayMetrics();
//        wm.getDefaultDisplay().getMetrics(dm);
//
//        int width = dm.widthPixels;
//        int height = dm.heightPixels;
//        int orientation;
//        // if the device's natural orientation is portrait:
//        if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && height > width
//                || (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && width > height) {
//            switch (rotation) {
//                case Surface.ROTATION_0:
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
//                    break;
//                case Surface.ROTATION_90:
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
//                    break;
//                case Surface.ROTATION_180:
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
//                    break;
//                case Surface.ROTATION_270:
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
//                    break;
//                default:
//                    Log.e(TAG, "Unknown screen orientation. Defaulting to " + "portrait.");
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
//                    break;
//            }
//        }
//        // if the device's natural orientation is landscape or if the device
//        // is square:
//        else {
//            switch (rotation) {
//                case Surface.ROTATION_0:
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
//                    break;
//                case Surface.ROTATION_90:
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
//                    break;
//                case Surface.ROTATION_180:
//                    orientation =
//                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
//                    break;
//                case Surface.ROTATION_270:
//                    orientation =
//                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
//                    break;
//                default:
//                    Log.e(TAG, "Unknown screen orientation. Defaulting to " + "portrait.");
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
//                    break;
//            }
//        }
//
//        return orientation;
//    }
}
