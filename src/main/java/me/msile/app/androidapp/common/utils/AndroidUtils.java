package me.msile.app.androidapp.common.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebSettings;

import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;

import me.msile.app.androidapp.common.core.AppManager;
import me.msile.app.androidapp.common.provider.FileProviderHelper;

public class AndroidUtils {

    /**
     * 获取当前版本
     */
    public static String getAppVersionName() {
        PackageManager packageManager = AppManager.INSTANCE.getApplication().getPackageManager();
        try {
            PackageInfo packInfo = packageManager.getPackageInfo(AppManager.INSTANCE.getApplication().getPackageName(), 0);
            return packInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("AndroidUtils", "getAppVersionName error");
        }
        return "";
    }

    /**
     * [获取应用程序版本名称信息]
     *
     * @return 当前应用的版本名称
     */
    public static int getVersionCode() {
        try {
            PackageManager packageManager = AppManager.INSTANCE.getApplication().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(AppManager.INSTANCE.getApplication().getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取应用包名
     */
    public static String getAppPackageName() {
        return AppManager.INSTANCE.getApplication().getPackageName();
    }

    /**
     * 获取MetaData
     */
    public static String getMetaDataValue(String metaKey) {
        Bundle metaData = null;
        String apiKey = "";
        if (TextUtils.isEmpty(metaKey)) {
            return "";
        }
        try {
            Context appContext = AppManager.INSTANCE.getApplication();
            ApplicationInfo ai = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(),
                    PackageManager.GET_META_DATA);
            if (null != ai) {
                metaData = ai.metaData;
            }
            if (null != metaData) {
                apiKey = metaData.getString(metaKey);
            }
        } catch (Exception e) {
            Log.d("AndroidUtils", "getMetaDataValue error");
        }
        return apiKey;
    }

    /**
     * 重启App
     */
    public static void restartApp() {
        Application context = AppManager.INSTANCE.getApplication();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(launchIntent);
    }

    /**
     * 杀掉App界面和进程
     */
    public static void killApp(Activity activity) {
        ActivityCompat.finishAffinity(activity);
        Process.killProcess(Process.myPid());
    }

    /**
     * 安装.apk文件
     */
    public static void installApk(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri apkFileUri = FileProviderHelper.fromFile(file);
            intent.setDataAndType(apkFileUri, "application/vnd.android.package-archive");
            FileProviderHelper.addFileReadPermission(intent);
            AppManager.INSTANCE.getApplication().startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取webView userAgent
     */
    public static String getWebUserAgent() {
        return WebSettings.getDefaultUserAgent(AppManager.INSTANCE.getApplication());
    }

    public static String getFileUriEndName(Uri uri) {
        if (uri != null) {
            String decodeDataString = Uri.decode(uri.toString());
            return getFileUriEndName(decodeDataString);
        }
        return "";
    }

    public static String getFileUriEndName(String uriString) {
        if (uriString != null) {
            int filenamePos = uriString.lastIndexOf('/');
            String filename = 0 <= filenamePos ? uriString.substring(filenamePos + 1) : uriString;
            return filename;
        }
        return "";
    }

    public static String getFileRealNameFromUri(Uri fileUri) {
        try {
            if (fileUri != null) {
                String fileName = null;
                //1.check document file name
                DocumentFile documentFile = DocumentFile.fromSingleUri(AppManager.INSTANCE.getApplication(), fileUri);
                if (documentFile != null) {
                    fileName = documentFile.getName();
                    Log.i("AndroidUtils", "getFileRealNameFromUri DocumentFile fileName: " + fileName);
                }
                //2.check file url end name
                if (TextUtils.isEmpty(fileName)) {
                    fileName = getFileUriEndName(fileUri);
                    Log.i("AndroidUtils", "getFileRealNameFromUri FileUriEndName fileName: " + fileName);
                }
                return fileName;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "";
    }

}
