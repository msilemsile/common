package me.msile.app.androidapp.common.extend;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import me.msile.app.androidapp.common.R;
import me.msile.app.androidapp.common.core.AppManager;
import me.msile.app.androidapp.common.log.LogHelper;
import me.msile.app.androidapp.common.permissions.PermissionHelper;
import me.msile.app.androidapp.common.permissions.callback.PermissionCallback;
import me.msile.app.androidapp.common.permissions.request.ReadStoragePermissionRequest;
import me.msile.app.androidapp.common.storage.model.CacheFileInfo;
import me.msile.app.androidapp.common.storage.callback.CopyCacheFileCallback;
import me.msile.app.androidapp.common.storage.StorageHelper;
import me.msile.app.androidapp.common.ui.activity.ImmerseActivity;
import me.msile.app.androidapp.common.ui.toast.AppToast;
import me.msile.app.androidapp.common.utils.AndroidUtils;

public class OpenFileProxyActivity extends ImmerseActivity {

    private final List<Uri> openFileUriList = new ArrayList<>();
    private boolean shouldRequestPermission = true;
    private FrameLayout flPbBuffering;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_file_proxy);
        flPbBuffering = (FrameLayout) findViewById(R.id.fl_pb_buffering);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            //handle system open||share file
            String intentAction = intent.getAction();
            if (TextUtils.equals(Intent.ACTION_VIEW, intentAction)) {
                Uri uri = intent.getData();
                if (uri != null) {
                    openFileUriList.clear();
                    openFileUriList.add(uri);
                    handleViewFileUriList();
                    return;
                }
            }
            if (TextUtils.equals(Intent.ACTION_SEND, intentAction) || TextUtils.equals(Intent.ACTION_SEND_MULTIPLE, intentAction)) {
                List<Uri> resultUriList = new ArrayList<>();
                ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    int clipDataCount = clipData.getItemCount();
                    if (clipDataCount > 0) {
                        for (int i = 0; i < clipDataCount; i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            Uri itemUri = item.getUri();
                            if (itemUri != null) {
                                LogHelper.print("OpenFileProxyActivity action share uri: " + itemUri);
                                String itemUriScheme = itemUri.getScheme();
                                if (TextUtils.equals(itemUriScheme, "file") || TextUtils.equals(itemUriScheme, "content")) {
                                    resultUriList.add(itemUri);
                                }
                            }
                        }
                    }
                }
                if (!resultUriList.isEmpty()) {
                    openFileUriList.clear();
                    openFileUriList.addAll(resultUriList);
                    handleViewFileUriList();
                    return;
                } else {
                    AppToast.toastMsg("暂不支持该操作");
                }
            }
        }
        finish();
    }

    private void handleViewFileUriList() {
        flPbBuffering.setVisibility(View.VISIBLE);
        StorageHelper.copyUriToCacheFile(openFileUriList, new CopyCacheFileCallback() {
            @Override
            public void onSuccess(List<CacheFileInfo> cacheFileInfoList) {
                OpenFileProxyHelper.INSTANCE.putCacheFileList(cacheFileInfoList);
                int appActivityCount = AppManager.INSTANCE.getAppActivityCount();
                LogHelper.print("OpenFileProxyActivity appActivityCount : " + appActivityCount);
                if (appActivityCount <= 1) {
                    LogHelper.print("OpenFileProxyActivity need launch app");
                    AndroidUtils.restartApp();
                } else {
                    Class mainActivityClass = OpenFileProxyHelper.INSTANCE.getMainActivityClass();
                    if (mainActivityClass != null) {
                        Intent mainIntent = new Intent(OpenFileProxyActivity.this, mainActivityClass);
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(mainIntent);
                    } else {
                        AppToast.toastLongMsg("OpenFileProxyHelper未设置mainActivityClass");
                        LogHelper.print("OpenFileProxyActivity app has launched");
                    }
                }
                finish();
            }

            @Override
            public void onError(String errorMsg) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (shouldRequestPermission) {
                        PermissionHelper.with(OpenFileProxyActivity.this)
                                .requestPermission(new ReadStoragePermissionRequest(), new PermissionCallback() {
                                    @Override
                                    public void onGranted() {
                                        handleViewFileUriList();
                                    }

                                    @Override
                                    public void onCancel() {
                                        AppToast.toastMsg("获取文件失败:没有权限");
                                        finish();
                                    }

                                    @Override
                                    public void onGoSetting() {
                                        finish();
                                    }
                                });
                        shouldRequestPermission = false;
                        return;
                    }
                }
                AppToast.toastMsg("获取文件失败:" + errorMsg);
                finish();
            }
        });
    }

}
