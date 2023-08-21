package me.msile.app.androidapp.common.permissions.request;

import android.Manifest;

import me.msile.app.androidapp.common.R;
import me.msile.app.androidapp.common.core.AppManager;
import me.msile.app.androidapp.common.permissions.base.BasePermissionRequest;

/**
 * 请求写入sd卡权限
 */
public class WriteStoragePermissionRequest extends BasePermissionRequest {

    @Override
    public String[] getPermissions() {
        return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    @Override
    public String getRequestExplain() {
        return "请求允许写入外置存储功能";
    }

    @Override
    public String getRationaleReason() {
        return "你已禁止写入外置存储功能，如需开启，请到该应用的系统设置页面打开。";
    }

    @Override
    public String getRequestTitle() {
        return AppManager.INSTANCE.getApplication().getString(R.string.app_name) + "请求写入外置存储功能";
    }

    @Override
    public String getAgainRequestExplain() {
        return "你已禁止写入外置存储功能，如需使用请允许。";
    }
}
