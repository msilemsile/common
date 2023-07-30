package me.msile.app.androidapp.common.camera;

import android.Manifest;

import me.msile.app.androidapp.common.R;
import me.msile.app.androidapp.common.core.ApplicationHolder;
import me.msile.app.androidapp.common.permissions.base.BasePermissionRequest;

public class SimpleCameraPermission extends BasePermissionRequest {

    @Override
    public String[] getPermissions() {
        return new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    }

    @Override
    public String getRequestExplain() {
        return "请求允许使用你的相机和录音功能";
    }

    @Override
    public String getRationaleReason() {
        return "你已禁止使用相机和录音功能，如需开启，请到该应用的系统设置页面打开。";
    }

    @Override
    public String getRequestTitle() {
        return ApplicationHolder.getAppContext().getString(R.string.app_name) + "请求使用相机和录音功能";
    }

    @Override
    public String getAgainRequestExplain() {
        return "你已禁止使用相机和录音功能，如需使用请允许。";
    }

}
