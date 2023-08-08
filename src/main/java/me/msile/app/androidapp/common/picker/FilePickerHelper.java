package me.msile.app.androidapp.common.picker;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import java.io.File;

import me.msile.app.androidapp.common.core.ActivityMethodProxy;
import me.msile.app.androidapp.common.core.ActivityWeakRefHolder;
import me.msile.app.androidapp.common.permissions.PermissionHelper;
import me.msile.app.androidapp.common.permissions.callback.PermissionCallback;
import me.msile.app.androidapp.common.permissions.request.CameraPermissionRequest;
import me.msile.app.androidapp.common.provider.FileProviderHelper;
import me.msile.app.androidapp.common.storage.StorageHelper;
import me.msile.app.androidapp.common.ui.activity.BaseActivity;
import me.msile.app.androidapp.common.ui.toast.AppToast;

/**
 * 选择文件bang帮助类（文件、相册、拍照、录像）
 */
public class FilePickerHelper extends ActivityWeakRefHolder implements ActivityMethodProxy {

    public static final int PICK_TYPE_FILE = 1;
    public static final int PICK_TYPE_GALLERY = 2;
    public static final int PICK_TYPE_IMAGE_CAPTURE = 3;
    public static final int PICK_TYPE_VIDEO_CAPTURE = 4;
    public static final int PICK_TYPE_GALLERY_MULTI = 5;

    private OnPickFileListener pickFileListener;

    public FilePickerHelper(@NonNull Activity activity) {
        super(activity);
        if (activity instanceof BaseActivity) {
            BaseActivity baseActivity = (BaseActivity) activity;
            baseActivity.addActivityMethodCallback(this);
        }
    }

    public void setAppPickFileListener(OnPickFileListener onPickFileListener) {
        this.pickFileListener = onPickFileListener;
    }

    public static FilePickerHelper with(Activity activity) {
        return new FilePickerHelper(activity);
    }

    /**
     * 选择文件
     */
    public void startPickFile() {
        try {
            Intent pickFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
            pickFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
            pickFileIntent.setType("*/*");
            pickFileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            getHolderActivity().startActivityForResult(pickFileIntent, PICK_TYPE_FILE);
        } catch (Exception e) {
            AppToast.toastMsg("选择文件失败");
            Log.d("AppPickDialog", "startPickFile error");
        }
    }

    /**
     * 相册(多选)
     */
    public void startPickFromGalleryMulti() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
                int pickImagesMaxLimit = MediaStore.getPickImagesMaxLimit();
                intent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, pickImagesMaxLimit);
                getHolderActivity().startActivityForResult(intent, PICK_TYPE_GALLERY_MULTI);
            } catch (Exception e) {
                AppToast.toastMsg("打开相册失败");
                Log.d("AppPickDialog", "startPickFromGallery error");
            }
        } else {
            AppToast.toastMsg("系统版本太低(>=13)，不支持相册多选");
        }
    }

    /**
     * 相册
     */
    public void startPickFromGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            getHolderActivity().startActivityForResult(intent, PICK_TYPE_GALLERY);
        } catch (Exception e) {
            AppToast.toastMsg("打开相册失败");
            Log.d("AppPickDialog", "startPickFromGallery error");
        }
    }

    //处理拍照和录像 返回data可能null
    private Uri cachePickFileUri = null;
    private String cachePickFilePath = null;

    public String getCachePickFilePath() {
        return cachePickFilePath;
    }

    public Uri getCachePickFileUri() {
        return cachePickFileUri;
    }

    /**
     * 拍照
     */
    public void startImageCapture() {
        PermissionHelper.with((FragmentActivity) getHolderActivity()).requestPermission(new CameraPermissionRequest(), new PermissionCallback() {
            @Override
            public void onGranted() {
                try {
                    Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    String fileName = "image_capture_" + System.currentTimeMillis() + ".jpg";
                    File cacheImageFile = StorageHelper.createShareFile(fileName);
                    Uri uri = FileProviderHelper.fromFile(cacheImageFile);
                    cachePickFileUri = uri;
                    cachePickFilePath = cacheImageFile.getAbsolutePath();
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                    getHolderActivity().startActivityForResult(captureIntent, PICK_TYPE_IMAGE_CAPTURE);
                } catch (Exception e) {
                    AppToast.toastMsg("打开相机失败");
                    Log.d("AppPickDialog", "onClickImageCapture error");
                }
            }

            @Override
            public void onCancel() {
                AppToast.toastMsg("取消授权");
            }

            @Override
            public void onGoSetting() {

            }
        });
    }

    /**
     * 录像
     */
    public void startVideoRecord() {
        PermissionHelper.with((FragmentActivity) getHolderActivity()).requestPermission(new CameraPermissionRequest(), new PermissionCallback() {
            @Override
            public void onGranted() {
                try {
                    Intent captureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    String fileName = "video_record_" + System.currentTimeMillis() + ".mp4";
                    File cacheVideoFile = StorageHelper.createShareFile(fileName);
                    Uri uri = FileProviderHelper.fromFile(cacheVideoFile);
                    cachePickFileUri = uri;
                    cachePickFilePath = cacheVideoFile.getAbsolutePath();
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                    getHolderActivity().startActivityForResult(captureIntent, PICK_TYPE_VIDEO_CAPTURE);
                } catch (Exception e) {
                    AppToast.toastMsg("打开录像失败");
                    Log.d("AppPickDialog", "onClickImageCapture error");
                }
            }

            @Override
            public void onCancel() {
                AppToast.toastMsg("取消授权");
            }

            @Override
            public void onGoSetting() {

            }
        });
    }

    /**
     * 处理选择文件/多选相册回调
     */
    private void handlePickFileResult(Intent intent) {
        Uri[] results = null;
        if (intent != null) {
            String dataString = intent.getDataString();
            ClipData clipData = intent.getClipData();
            if (clipData != null) {
                int clipDataCount = clipData.getItemCount();
                if (clipDataCount > 0) {
                    results = new Uri[clipDataCount];
                    for (int i = 0; i < clipDataCount; i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
            }
            if (dataString != null) {
                results = new Uri[]{intent.getData()};
            }
        }
        if (pickFileListener != null) {
            pickFileListener.onPickFile(results);
        }
    }

    /**
     * 处理媒体文件
     */
    private void handlePickMediaFileResult(Intent data) {
        Uri result = null;
        if (data != null) {
            result = data.getData();
        }
        if (result == null) {
            if (cachePickFileUri != null) {
                result = cachePickFileUri;
            }
        }
        if (pickFileListener != null) {
            pickFileListener.onPickFile(new Uri[]{result});
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case PICK_TYPE_FILE:
                case PICK_TYPE_GALLERY_MULTI:
                    handlePickFileResult(data);
                    break;
                case PICK_TYPE_GALLERY:
                case PICK_TYPE_IMAGE_CAPTURE:
                case PICK_TYPE_VIDEO_CAPTURE:
                    handlePickMediaFileResult(data);
                    break;
            }
        } else {
            if (pickFileListener != null) {
                pickFileListener.onPickCancel();
            }
        }
    }

    @Override
    public void onClear() {
        pickFileListener = null;
    }

    public interface OnPickFileListener {
        default void onPickFile(@Nullable Uri[] uri) {
        }

        default void onPickCancel() {
        }
    }
}
