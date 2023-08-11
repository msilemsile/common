package me.msile.app.androidapp.common.storage;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import me.msile.app.androidapp.common.constants.AppCommonConstants;
import me.msile.app.androidapp.common.core.ApplicationHolder;
import me.msile.app.androidapp.common.log.LogHelper;
import me.msile.app.androidapp.common.rx.DefaultObserver;
import me.msile.app.androidapp.common.rx.RxTransformerUtils;
import me.msile.app.androidapp.common.utils.AndroidUtils;
import me.msile.app.androidapp.common.utils.FileUtils;

public class StorageHelper {

    //缓存文件目录
    public static String DIR_CACHE_PATH = "/" + AppCommonConstants.APP_PREFIX_TAG + "cache/";
    //分享文件目录
    public static String DIR_SHARE_PATH = "/" + AppCommonConstants.APP_PREFIX_TAG + "share/";
    //下载文件目录
    public static String DIR_DOWNLOAD_PATH = "/" + AppCommonConstants.APP_PREFIX_TAG + "download/";

    //下载目录名称(android 10 +)
    public static String RELATIVE_EXTERNAL_DIR_PATH = "/" + AppCommonConstants.APP_PREFIX_TAG + "/";

    public static String CONTENT_DOCUMENT_BASE_PATH = "content://com.android.externalstorage.documents/document/primary:";

    /**
     * 创建分享文件
     *
     * @param shareFileName 文件名字
     */
    public static File createShareFile(String shareFileName) {
        return createExternalFile(DIR_SHARE_PATH, shareFileName);
    }

    /**
     * 创建缓存文件
     *
     * @param cacheFileName 文件名字
     */
    public static File createCacheFile(String cacheFileName) {
        return createExternalFile(DIR_CACHE_PATH, cacheFileName);
    }

    /**
     * 创建下载文件
     *
     * @param downloadFileName 文件名字
     */
    public static File createDownloadFile(String downloadFileName) {
        return createExternalFile(DIR_DOWNLOAD_PATH, downloadFileName);
    }

    /**
     * 创建外置存储app内的文件 不需要外置存储权限
     *
     * @param parentFilePath 父目录
     * @param fileName       文件名字
     */
    private static File createExternalFile(String parentFilePath, String fileName) {
        String storageFileRootPath = getExternalFileRootPath();
        File parentDir = new File(storageFileRootPath + parentFilePath);
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        File cacheFile = new File(parentDir, fileName);
        return cacheFile;
    }

    /**
     * 获取缓存目录路径
     */
    public static String getExternalCacheDirPath() {
        return getExternalDirPath(DIR_CACHE_PATH);
    }

    /**
     * 获取分享目录路径
     */
    public static String getExternalShareDirPath() {
        return getExternalDirPath(DIR_SHARE_PATH);
    }

    /**
     * 获取下载目录路径
     */
    public static String getExternalDownloadDirPath() {
        return getExternalDirPath(DIR_DOWNLOAD_PATH);
    }

    /**
     * 获取存储目录
     *
     * @param dirPath /目录名称/
     */
    private static String getExternalDirPath(String dirPath) {
        String storageFileRootPath = getExternalFileRootPath();
        File dir = new File(storageFileRootPath + dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.getAbsolutePath();
    }

    /**
     * 获取文件 (先获取外置目录[/Android/data/x.x.x/files/]，失败获取app安装文件目录[data/data/x.x.x/files])
     */
    private static String getExternalFileRootPath() {
        File externalFile = ApplicationHolder.getAppContext().getExternalFilesDir(null);
        if (externalFile != null) {
            return externalFile.getAbsolutePath();
        } else {
            return ApplicationHolder.getAppContext().getFilesDir().getAbsolutePath();
        }
    }

    // ----------- api < android 10  start-------------

    /**
     * 获取系统常用文件夹（下载目录）(api < android 10)
     */
    public static String getPublicDownloadsDirPath() {
        return getPublicDirPath(Environment.DIRECTORY_DOWNLOADS);
    }

    /**
     * 获取系统常用文件夹（相册目录）(api < android 10)
     */
    public static String getPublicDCIMDirPath() {
        return getPublicDirPath(Environment.DIRECTORY_DCIM);
    }

    /**
     * 获取系统常用文件夹
     *
     * @param dirType Environment.DIRECTORY_xxxx
     */
    private static String getPublicDirPath(String dirType) {
        File dcimFile = Environment.getExternalStoragePublicDirectory(dirType);
        File realDir = new File(dcimFile.getAbsolutePath() + RELATIVE_EXTERNAL_DIR_PATH);
        if (!realDir.exists()) {
            realDir.mkdirs();
        }
        return realDir.getAbsolutePath();
    }

    // ----------- api < android 10  end-------------


    // ----------- api >= android 10  start-------------

    /**
     * 获取系统常用文件夹（下载目录）(api >= android 10)
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static String getRelativeDownloadsDirPath() {
        return Environment.DIRECTORY_DOWNLOADS + StorageHelper.RELATIVE_EXTERNAL_DIR_PATH;
    }

    /**
     * 获取系统常用文件夹（相册目录）(api >= android 10)
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static String getRelativeDCIMDirPath() {
        return Environment.DIRECTORY_DCIM + StorageHelper.RELATIVE_EXTERNAL_DIR_PATH;
    }

    // ----------- api >= android 10  end-------------

    public static String getContentPubDownloadDirPath() {
        return CONTENT_DOCUMENT_BASE_PATH + Environment.DIRECTORY_DOWNLOADS + Uri.encode("/" + AppCommonConstants.APP_PREFIX_TAG);
    }

    public static String getContentPubDCIMDirPath() {
        return CONTENT_DOCUMENT_BASE_PATH + Environment.DIRECTORY_DCIM + Uri.encode("/" + AppCommonConstants.APP_PREFIX_TAG);
    }

    /**
     * 列出保存在公共目录的文件名字(READ_EXTERNAL_STORAGE权限可选，未申请时之前app卸载的文件无法获取)
     */
    public static int TYPE_PUBLIC_DIR_DOWNLOAD = 0;
    public static int TYPE_PUBLIC_DIR_DCIM_IMAGE = 1;
    public static int TYPE_PUBLIC_DIR_DCIM_VIDEO = 2;

    public static List<String> listPubDownloadDirFileName() {
        return listPublicDirFileName(TYPE_PUBLIC_DIR_DOWNLOAD);
    }

    public static List<String> listPubDCIMDirFileName() {
        List<String> imageDirFileName = listPublicDirFileName(TYPE_PUBLIC_DIR_DCIM_IMAGE);
        List<String> videoDirFileName = listPublicDirFileName(TYPE_PUBLIC_DIR_DCIM_VIDEO);
        List<String> dcimDirList = new ArrayList<>();
        dcimDirList.addAll(imageDirFileName);
        dcimDirList.addAll(videoDirFileName);
        return dcimDirList;
    }

    public static List<String> listPublicDirFileName(int publicDirType) {
        List<String> fileNameList = new ArrayList<>();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver contentResolver = ApplicationHolder.getAppContext().getContentResolver();
                Uri publicDirUri;
                if (publicDirType == TYPE_PUBLIC_DIR_DCIM_IMAGE) {
                    publicDirUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if (publicDirType == TYPE_PUBLIC_DIR_DCIM_VIDEO) {
                    publicDirUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else {
                    publicDirUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                }
                Cursor cursor = contentResolver.query(
                        publicDirUri,
                        null,
                        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME + "=?",
                        new String[]{AppCommonConstants.APP_PREFIX_TAG}, null
                );
                if (cursor != null) {
//                    int fileIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                    int fileNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                    while (cursor.moveToNext()) {
                        //文件名
                        String fileName = cursor.getString(fileNameIndex);
                        fileNameList.add(fileName);
                        //uri
//                        long fileId = cursor.getLong(fileIdIndex);
//                        Uri pathUri = downloadUri.buildUpon().appendPath(String.valueOf(fileId)).build();
                    }
                    cursor.close();
                }
            } else {
                String publicDirPath;
                if (publicDirType == TYPE_PUBLIC_DIR_DCIM_IMAGE) {
                    publicDirPath = getPublicDCIMDirPath();
                } else {
                    publicDirPath = getPublicDownloadsDirPath();
                }
                File downloadDir = new File(publicDirPath);
                if (downloadDir.exists() && downloadDir.isDirectory()) {
                    String[] fileNames = downloadDir.list();
                    if (fileNames != null) {
                        fileNameList.addAll(Arrays.asList(fileNames));
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        Log.i("StorageHelper", "listDownloadFilesName size: " + fileNameList.size() + " fileNameList: " + Arrays.toString(fileNameList.toArray()));
        return fileNameList;
    }

    public static void copyUriToCacheFile(final List<Uri> fileUriList, final CopyCacheFileCallback copyCacheFileCallback) {
        if (fileUriList == null || fileUriList.isEmpty() || copyCacheFileCallback == null) {
            return;
        }
        Observable.create(new ObservableOnSubscribe<List<CacheFileInfo>>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<List<CacheFileInfo>> emitter) {
                        final List<CacheFileInfo> cacheFileInfoList = new ArrayList<>();
                        for (int i = 0; i < fileUriList.size(); i++) {
                            Uri viewFileUri = fileUriList.get(i);
                            String cacheFileName = "cache_" + System.currentTimeMillis();
                            File cacheFile = StorageHelper.createCacheFile(cacheFileName);
                            String fileRealNameFromUri = AndroidUtils.getFileRealNameFromUri(viewFileUri);
                            if (TextUtils.isEmpty(fileRealNameFromUri)) {
                                fileRealNameFromUri = cacheFileName;
                            }
                            LogHelper.print("copyFileToOtherFile start copy cacheFile: " + cacheFileName);
                            try {
                                FileInputStream fileInputStream = (FileInputStream) ApplicationHolder.getAppContext().getContentResolver().openInputStream(viewFileUri);
                                boolean copyFileToOtherFile = FileUtils.copyFileToOtherFile(fileInputStream, cacheFile);
                                if (copyFileToOtherFile) {
                                    CacheFileInfo cacheFileInfo = new CacheFileInfo(cacheFile);
                                    cacheFileInfo.setOriginFileUri(viewFileUri);
                                    cacheFileInfo.setOriginFileName(fileRealNameFromUri);
                                    cacheFileInfoList.add(cacheFileInfo);
                                    LogHelper.print("copyFileToOtherFile copy success cache file path: " + cacheFile.getAbsolutePath());
                                } else {
                                    LogHelper.print("copyFileToOtherFile copy error");
                                }
                                emitter.onNext(cacheFileInfoList);
                            } catch (Throwable e) {
                                e.printStackTrace();
                                LogHelper.print("copyFileToOtherFile try catch error");
                                //delete all cache files
                                try {
                                    cacheFile.delete();
                                    for (int j = 0; j < cacheFileInfoList.size(); j++) {
                                        File file = cacheFileInfoList.get(i).getFile();
                                        file.delete();
                                    }
                                } catch (Throwable delError) {
                                    e.printStackTrace();
                                    LogHelper.print("copyFileToOtherFile delete all cache file");
                                }
                                emitter.onError(e);
                            }
                        }
                    }
                }).compose(RxTransformerUtils.mainSchedulers())
                .subscribe(new DefaultObserver<List<CacheFileInfo>>() {
                    @Override
                    protected void onSuccess(List<CacheFileInfo> files) {
                        copyCacheFileCallback.onSuccess(files);
                    }

                    @Override
                    public void onError(Throwable e) {
                        copyCacheFileCallback.onError(e.getMessage());
                    }
                });
    }

}
