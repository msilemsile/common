package me.msile.app.androidapp.common.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.view.WindowManager;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;

public class ScreenUtils {

    public static boolean isPortraitScreen(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public static void setActivityFullScreen(Activity activity, boolean fullScreen) {
        if (fullScreen) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public static void setScreenOrientation(Activity activity, int orientation) {
        activity.setRequestedOrientation(orientation);
    }

    /**
     * displayCutoutMode values:
     *     WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT,
     *     WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES,
     *     WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER,
     *     WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    public static void setDisplayCutoutMode(Activity activity, int displayCutoutMode){
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.layoutInDisplayCutoutMode = displayCutoutMode;
        activity.getWindow().setAttributes(lp);
    }

}
