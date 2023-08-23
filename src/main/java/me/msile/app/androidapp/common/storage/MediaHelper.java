package me.msile.app.androidapp.common.storage;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.msile.app.androidapp.common.constants.AppCommonConstants;
import me.msile.app.androidapp.common.core.AppManager;
import me.msile.app.androidapp.common.provider.FileProviderHelper;
import me.msile.app.androidapp.common.rx.DefaultObserver;
import me.msile.app.androidapp.common.rx.RxTransformerUtils;
import me.msile.app.androidapp.common.storage.callback.GetPublicDirFileCallback;
import me.msile.app.androidapp.common.storage.model.PublicDirFileInfo;
import me.msile.app.androidapp.common.ui.toast.AppToast;
import me.msile.app.androidapp.common.utils.FileUtils;

/**
 * 多媒体操作工具类
 */
public class MediaHelper {

    public static void insertPicToGallery(String picPath) {
        insertPicToGallery(picPath, false);
    }

    public static void insertPicToGallery(String picPath, boolean hasAlpha) {
        insertPicToGallery(picPath, hasAlpha, null);
    }

    public static void insertPicToGallery(String picPath, boolean hasAlpha, InsertMediaCallback mediaCallback) {
        if (TextUtils.isEmpty(picPath)) {
            if (mediaCallback != null) {
                mediaCallback.onFail();
            }
            return;
        }
        Observable.create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<String> emitter) throws Throwable {
                        String fileName = AppCommonConstants.APP_PREFIX_TAG + "_pic_" + System.currentTimeMillis() + (hasAlpha ? ".png" : ".jpg");
                        String mimeType = "image/*";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ContentResolver resolver = AppManager.INSTANCE.getApplication().getContentResolver();
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                            contentValues.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
                            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, StorageHelper.getRelativeDCIMDirPath());
                            //插入数据库
                            Uri contentUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                            //保存文件
                            OutputStream outputStream = resolver.openOutputStream(contentUri);
                            FileUtils.copyFileToOtherFile(new File(picPath), (FileOutputStream) outputStream);
                            emitter.onNext("");
                        } else {
                            File inFile = new File(picPath);
                            File outFile = new File(StorageHelper.getPublicDCIMDirPath(), fileName);
                            //保存文件
                            FileUtils.copyFileToOtherFile(inFile, outFile);
                            ContentValues values = new ContentValues();
                            String outFilePath = outFile.getAbsolutePath();
                            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                            values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
                            values.put(MediaStore.Images.Media.DATA, outFilePath);
                            //插入数据库
                            AppManager.INSTANCE.getApplication().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                            emitter.onNext(outFilePath);
                        }
                        emitter.onComplete();
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DefaultObserver<String>() {
                    @Override
                    protected void onSuccess(String s) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            sendUpdateGalleryBroadcast(s);
                        }
                        if (mediaCallback != null) {
                            mediaCallback.onSuccess();
                        } else {
                            AppToast.toastMsg("已保存到相册");
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        if (mediaCallback != null) {
                            mediaCallback.onFail();
                        } else {
                            AppToast.toastMsg("保存失败");
                        }
                    }
                });
    }

    public static void insertPicToGallery(Bitmap bitmap) {
        insertPicToGallery(bitmap, null);
    }

    public static void insertPicToGallery(Bitmap bitmap, InsertMediaCallback mediaCallback) {
        if (bitmap == null || bitmap.isRecycled()) {
            if (mediaCallback != null) {
                mediaCallback.onFail();
            }
            return;
        }
        WeakReference<Bitmap> bitmapWeakReference = new WeakReference<>(bitmap);
        Observable.create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<String> emitter) throws Throwable {
                        Bitmap bitmapRefer = bitmapWeakReference.get();
                        if (bitmapRefer == null || bitmapRefer.isRecycled()) {
                            emitter.onError(new Throwable());
                            return;
                        }
                        boolean hasAlpha = bitmapRefer.hasAlpha();
                        String fileName = AppCommonConstants.APP_PREFIX_TAG + "_pic_" + System.currentTimeMillis() + (hasAlpha ? ".png" : ".jpg");
                        String mimeType = "image/*";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ContentResolver resolver = AppManager.INSTANCE.getApplication().getContentResolver();
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                            contentValues.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
                            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, StorageHelper.getRelativeDCIMDirPath());
                            //插入数据库
                            Uri contentUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                            OutputStream outputStream = resolver.openOutputStream(contentUri);
                            //保存文件
                            bitmapRefer.compress(hasAlpha ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 100, outputStream);
                            emitter.onNext("");
                        } else {
                            File outFile = new File(StorageHelper.getPublicDCIMDirPath(), fileName);
                            //保存文件
                            bitmapRefer.compress(hasAlpha ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(outFile));
                            ContentValues values = new ContentValues();
                            String outFilePath = outFile.getAbsolutePath();
                            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                            values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
                            values.put(MediaStore.Images.Media.DATA, outFilePath);
                            //插入数据库
                            AppManager.INSTANCE.getApplication().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                            emitter.onNext(outFilePath);
                        }
                        emitter.onComplete();
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DefaultObserver<String>() {
                    @Override
                    protected void onSuccess(String s) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            sendUpdateGalleryBroadcast(s);
                        }
                        if (mediaCallback != null) {
                            mediaCallback.onSuccess();
                        } else {
                            AppToast.toastMsg("已保存到相册");
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        if (mediaCallback != null) {
                            mediaCallback.onFail();
                        } else {
                            AppToast.toastMsg("保存失败");
                        }
                    }
                });
    }

    public static void insertVideoToMedia(String videoPath) {
        insertVideoToMedia(videoPath, null);
    }

    public static void insertVideoToMedia(String videoPath, InsertMediaCallback mediaCallback) {
        if (TextUtils.isEmpty(videoPath)) {
            if (mediaCallback != null) {
                mediaCallback.onFail();
            }
            return;
        }
        Observable.create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<String> emitter) throws Throwable {
                        String fileName = AppCommonConstants.APP_PREFIX_TAG + "_video_" + System.currentTimeMillis() + ".mp4";
                        String mimeType = "video/*";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ContentResolver resolver = AppManager.INSTANCE.getApplication().getContentResolver();
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                            contentValues.put(MediaStore.Video.Media.MIME_TYPE, mimeType);
                            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, StorageHelper.getRelativeDCIMDirPath());
                            //插入数据库
                            Uri contentUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
                            OutputStream outputStream = resolver.openOutputStream(contentUri);
                            //保存文件
                            FileUtils.copyFileToOtherFile(new File(videoPath), (FileOutputStream) outputStream);
                            emitter.onNext("");
                        } else {
                            File inFile = new File(videoPath);
                            File outFile = new File(StorageHelper.getPublicDCIMDirPath(), fileName);
                            //保存文件
                            FileUtils.copyFileToOtherFile(inFile, outFile);
                            ContentValues values = new ContentValues();
                            String outFilePath = outFile.getAbsolutePath();
                            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                            values.put(MediaStore.Video.Media.MIME_TYPE, mimeType);
                            values.put(MediaStore.Video.Media.DATA, outFilePath);
                            //插入数据库
                            AppManager.INSTANCE.getApplication().getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                            emitter.onNext(outFilePath);
                        }
                        emitter.onComplete();
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DefaultObserver<String>() {
                    @Override
                    protected void onSuccess(String s) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            sendUpdateGalleryBroadcast(s);
                        }
                        if (mediaCallback != null) {
                            mediaCallback.onSuccess();
                        } else {
                            AppToast.toastMsg("已保存到相册");
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        if (mediaCallback != null) {
                            mediaCallback.onFail();
                        } else {
                            AppToast.toastMsg("保存失败");
                        }
                    }
                });
    }

    /**
     * 更新相册广播
     */
    private static void sendUpdateGalleryBroadcast(String path) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(path);
        mediaScanIntent.setData(FileProviderHelper.fromFile(f));
        FileProviderHelper.addFileWritePermission(mediaScanIntent);
        AppManager.INSTANCE.getApplication().sendBroadcast(mediaScanIntent);
    }

    /**
     * 列出保存在公共目录的文件名字(READ_EXTERNAL_STORAGE权限可选，未申请时之前app卸载的文件无法获取)
     * targetsdk 29 android 10以上只能获取本应用的媒体文件，非媒体文件不可以获取（部分厂商手机可以,原生安卓系统不可以）; 可通过SAF框架授权后获取，或者降低targetsdk
     */
    public static int TYPE_MEDIA_PUBLIC_DOWNLOAD = 1;
    public static int TYPE_MEDIA_PUBLIC_DCIM = 2;

    public static void getPubDownloadDirFile(GetPublicDirFileCallback fileCallback) {
        Observable.create(new ObservableOnSubscribe<List<PublicDirFileInfo>>() {

                    @Override
                    public void subscribe(@NonNull ObservableEmitter<List<PublicDirFileInfo>> emitter) {
                        try {
                            List<PublicDirFileInfo> publicDirFile = getPublicDirFile(TYPE_MEDIA_PUBLIC_DOWNLOAD);
                            emitter.onNext(publicDirFile);
                            emitter.onComplete();
                        } catch (Throwable e) {
                            e.printStackTrace();
                            emitter.onError(e);
                        }
                    }
                }).compose(RxTransformerUtils.mainSchedulers())
                .subscribe(new DefaultObserver<List<PublicDirFileInfo>>() {
                    @Override
                    protected void onSuccess(List<PublicDirFileInfo> publicDirFileInfos) {
                        if (fileCallback != null) {
                            fileCallback.onSuccess(publicDirFileInfos);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (fileCallback != null) {
                            fileCallback.onError(e.getMessage());
                        }
                    }
                });
    }

    public static void getPubDCIMDirFile(GetPublicDirFileCallback fileCallback) {
        Observable.create(new ObservableOnSubscribe<List<PublicDirFileInfo>>() {

                    @Override
                    public void subscribe(@NonNull ObservableEmitter<List<PublicDirFileInfo>> emitter) {
                        try {
                            List<PublicDirFileInfo> publicDirFile = getPublicDirFile(TYPE_MEDIA_PUBLIC_DCIM);
                            emitter.onNext(publicDirFile);
                            emitter.onComplete();
                        } catch (Throwable e) {
                            e.printStackTrace();
                            emitter.onError(e);
                        }
                    }
                }).compose(RxTransformerUtils.mainSchedulers())
                .subscribe(new DefaultObserver<List<PublicDirFileInfo>>() {
                    @Override
                    protected void onSuccess(List<PublicDirFileInfo> publicDirFileInfos) {
                        if (fileCallback != null) {
                            fileCallback.onSuccess(publicDirFileInfos);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (fileCallback != null) {
                            fileCallback.onError(e.getMessage());
                        }
                    }
                });
    }

    public static List<PublicDirFileInfo> getPublicDirFile(int publicDirType) {
        List<PublicDirFileInfo> publicDirFileInfoList = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (publicDirType == TYPE_MEDIA_PUBLIC_DCIM) {
                //ps:app卸载后，通过READ_EXTERNAL_STORAGE权限可获取之前的文件
                List<PublicDirFileInfo> imageFileList = queryPubDirFile(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                List<PublicDirFileInfo> videoFileList = queryPubDirFile(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                publicDirFileInfoList.addAll(imageFileList);
                publicDirFileInfoList.addAll(videoFileList);
            } else {
                //ps: app卸载后无法读取之前下载的文件
                List<PublicDirFileInfo> downloadFileList = queryPubDirFile(MediaStore.Downloads.EXTERNAL_CONTENT_URI);
                publicDirFileInfoList.addAll(downloadFileList);
            }
        } else {
            if (publicDirType == TYPE_MEDIA_PUBLIC_DCIM) {
                publicDirFileInfoList.addAll(StorageHelper.listPublicDirFileBelowQ(StorageHelper.TYPE_PUBLIC_DIR_DCIM));
            } else {
                publicDirFileInfoList.addAll(StorageHelper.listPublicDirFileBelowQ(StorageHelper.TYPE_PUBLIC_DIR_DOWNLOAD));
            }
        }
        Log.i("MediaHelper", "PublicDirFileList size: " + publicDirFileInfoList.size() + " fileNameList: " + Arrays.toString(publicDirFileInfoList.toArray()));
        return publicDirFileInfoList;
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static List<PublicDirFileInfo> queryPubDirFile(Uri publicDirUri) {
        List<PublicDirFileInfo> publicDirFileInfoList = new ArrayList<>();
        ContentResolver contentResolver = AppManager.INSTANCE.getApplication().getContentResolver();
        Cursor cursor = contentResolver.query(
                publicDirUri,
                null,
                MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME + "=?",
                new String[]{AppCommonConstants.APP_PREFIX_TAG}, null
        );
        if (cursor != null) {
            int fileIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
            int fileNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
            int relativePathIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.RELATIVE_PATH);
            int dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
            while (cursor.moveToNext()) {
                //文件名
                String fileName = cursor.getString(fileNameIndex);
                //文件id
                long fileId = cursor.getLong(fileIdIndex);
                //文件相对路径
                String relativePath = cursor.getString(relativePathIndex);
                //文件真实路径
                String data = cursor.getString(dataIndex);
                publicDirFileInfoList.add(new PublicDirFileInfo(fileId, fileName, relativePath, data));
            }
            cursor.close();
        }
        return publicDirFileInfoList;
    }

    public interface InsertMediaCallback {
        void onSuccess();

        void onFail();
    }

}
