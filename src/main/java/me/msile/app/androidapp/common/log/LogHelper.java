package me.msile.app.androidapp.common.log;

import android.util.Log;

import me.msile.app.androidapp.common.constants.AppCommonConstants;

public class LogHelper {

    public static void print(String tag, String msg) {
        Log.i(tag, msg);
    }

    public static void print(String msg) {
        Log.i(AppCommonConstants.APP_PREFIX_TAG, msg);
    }

}
