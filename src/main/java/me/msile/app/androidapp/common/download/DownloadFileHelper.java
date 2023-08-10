package me.msile.app.androidapp.common.download;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.RequiresPermission;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.msile.app.androidapp.common.core.ApplicationHolder;
import me.msile.app.androidapp.common.core.MainThreadHolder;
import me.msile.app.androidapp.common.log.LogHelper;
import me.msile.app.androidapp.common.net.callback.DefaultOkHttpCallback;
import me.msile.app.androidapp.common.net.okhttp.OkHttpManager;
import me.msile.app.androidapp.common.rx.DefaultObserver;
import me.msile.app.androidapp.common.storage.StorageHelper;
import me.msile.app.androidapp.common.ui.toast.AppToast;
import me.msile.app.androidapp.common.utils.AndroidUtils;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public enum DownloadFileHelper {
    INSTANCE;

    private final Set<DownloadFileCallback> downloadFileCallbackSet = new HashSet<>();
    private final List<DownloadFileInfo> downloadingFileInfoList = Collections.synchronizedList(new ArrayList<DownloadFileInfo>());

    public List<DownloadFileInfo> getDownloadingFileInfoList() {
        return downloadingFileInfoList;
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public void downloadFile(final DownloadFileInfo downloadFileInfo) {
        if (downloadFileInfo == null || TextUtils.isEmpty(downloadFileInfo.getUrl())) {
            for (DownloadFileCallback callback : downloadFileCallbackSet) {
                callback.onDownloadFail(downloadFileInfo, "uri-null");
            }
            LogHelper.print("DownloadFileHelper_downloadFile uri-null");
            return;
        }
        Observable.create(new ObservableOnSubscribe<DownloadFileInfo>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<DownloadFileInfo> emitter) {
                        //check fileName and rename file
                        String originFileName = downloadFileInfo.getFileName();
                        if (TextUtils.isEmpty(originFileName)) {
                            originFileName = AndroidUtils.getFileUriEndName(downloadFileInfo.getUrl());
                            if (TextUtils.isEmpty(originFileName)) {
                                originFileName = downloadFileInfo.getDownloadId();
                            }
                        }
                        String fileStartName = originFileName;
                        String fileSuffix = "";
                        int lastIndexOf = originFileName.lastIndexOf(".");
                        if (lastIndexOf != -1) {
                            fileStartName = originFileName.substring(0, lastIndexOf);
                            fileSuffix = originFileName.substring(lastIndexOf);
                        }
                        LogHelper.print("DownloadFileHelper_downloadFile originFileName: " + originFileName + " fileStartName: " + fileStartName + " fileSuffix: " + fileSuffix);
                        List<String> localHistoryList = StorageHelper.listPubDownloadDirFileName();
                        String tempFileName;
                        int tempSuffixIndex = -1;
                        while (true) {
                            tempFileName = fileStartName;
                            tempSuffixIndex++;
                            if (tempSuffixIndex != 0) {
                                tempFileName = tempFileName + "(" + tempSuffixIndex + ")" + fileSuffix;
                            } else {
                                tempFileName = tempFileName + fileSuffix;
                            }
                            LogHelper.print("DownloadFileHelper_downloadFile check fileName: " + tempFileName + " tempSuffixIndex: " + tempSuffixIndex);
                            boolean hasSameFileName = false;
                            //1.check local history file name
                            if (!localHistoryList.isEmpty()) {
                                for (int i = 0; i < localHistoryList.size(); i++) {
                                    String historyFileName = localHistoryList.get(i);
                                    if (TextUtils.equals(tempFileName, historyFileName)) {
                                        hasSameFileName = true;
                                        break;
                                    }
                                }
                            }
                            if (hasSameFileName) {
                                continue;
                            }
                            //2.check downloading history file name
                            if (!downloadingFileInfoList.isEmpty()) {
                                for (int i = 0; i < downloadingFileInfoList.size(); i++) {
                                    DownloadFileInfo downloadingFile = downloadingFileInfoList.get(i);
                                    if (TextUtils.equals(tempFileName, downloadingFile.getFileName())) {
                                        hasSameFileName = true;
                                        break;
                                    }
                                }
                            }
                            if (!hasSameFileName) {
                                break;
                            }
                        }
                        LogHelper.print("DownloadFileHelper_downloadFile success fileName: " + tempFileName);
                        downloadFileInfo.setFileName(tempFileName);
                        emitter.onNext(downloadFileInfo);
                        emitter.onComplete();
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DefaultObserver<DownloadFileInfo>() {
                    @Override
                    protected void onSuccess(DownloadFileInfo downloadFileInfo) {
                        if (downloadingFileInfoList.contains(downloadFileInfo)) {
                            AppToast.toastMsg("文件下载中,请稍后..");
                            return;
                        }
                        for (DownloadFileCallback callback : downloadFileCallbackSet) {
                            callback.onDownloadStart(downloadFileInfo);
                        }
                        downloadingFileInfoList.add(downloadFileInfo);
                        LogHelper.print("DownloadFileHelper_downloadFile start download: " + downloadFileInfo);
                        Request.Builder requestBuilder = new Request.Builder();
                        //request url
                        requestBuilder.url(downloadFileInfo.getUrl());
                        //request header
                        Map<String, String> downloadExtras = downloadFileInfo.getDownloadExtras();
                        if (downloadExtras != null) {
                            for (Map.Entry<String, String> entry : downloadExtras.entrySet()) {
                                String entryKey = entry.getKey();
                                String entryValue = entry.getValue();
                                if (!TextUtils.isEmpty(entryKey) && !TextUtils.isEmpty(entryValue)) {
                                    requestBuilder.header(entryKey, entryValue);
                                }
                            }
                        }
                        //request method
                        requestBuilder.get();
                        OkHttpManager.INSTANCE.getAppHttpClient()
                                .newCall(requestBuilder.build())
                                .enqueue(new DefaultOkHttpCallback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {
                                        MainThreadHolder.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                handleDownloadFileError(downloadFileInfo, "onFailure");
                                            }
                                        });
                                    }

                                    @Override
                                    public void onResponse(Call call, Response response) {
                                        try {
                                            if (response.isSuccessful()) {
                                                ResponseBody responseBody = response.body();
                                                if (responseBody != null) {
                                                    long contentLength = responseBody.contentLength();
                                                    InputStream inputStream = responseBody.byteStream();
                                                    handleWriteDownloadFile(downloadFileInfo, inputStream, contentLength);
                                                } else {
                                                    MainThreadHolder.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            handleDownloadFileError(downloadFileInfo, "onResponse empty response body");
                                                        }
                                                    });
                                                }
                                            } else {
                                                MainThreadHolder.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        handleDownloadFileError(downloadFileInfo, "onResponse error code" + response.code());
                                                    }
                                                });
                                            }
                                        } catch (Throwable e) {
                                            e.printStackTrace();
                                            MainThreadHolder.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    handleDownloadFileError(downloadFileInfo, "onResponse try catch error" + e.getMessage());
                                                }
                                            });
                                        }
                                    }
                                });
                    }

                    @Override
                    public void onError(Throwable e) {
                        handleDownloadFileError(downloadFileInfo, "thread run try catch error" + e.getMessage());
                    }
                });
    }

    private void handleWriteDownloadFile(DownloadFileInfo downloadFileInfo, InputStream inputStream, long contentLength) {
        LogHelper.print("DownloadFileHelper_downloadFile handleWriteDownloadFile contentLength: " + contentLength + " download: " + downloadFileInfo);
        String tempFileName = downloadFileInfo.getFileName() + ".temp";
        OutputStream outputStream = null;
        Uri tempFileContentUri = null;
        File tempFile = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = ApplicationHolder.getAppContext().getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, tempFileName);
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, StorageHelper.getRelativeDownloadsDirPath());
                tempFileContentUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                outputStream = resolver.openOutputStream(tempFileContentUri);
                LogHelper.print("DownloadFileHelper_downloadFile handleWriteDownloadFile >=android Q ContentResolver uri: " + tempFileContentUri);
            } else {
                String downloadFilePath = StorageHelper.getPublicDownloadsDirPath() + "/" + tempFileName;
                tempFile = new File(downloadFilePath);
                outputStream = new FileOutputStream(tempFile);
                LogHelper.print("DownloadFileHelper_downloadFile handleWriteDownloadFile <=android Q downloadFilePath path: " + downloadFilePath);
            }
            //write buffer and notify progress
            byte[] buf = new byte[1024 * 128];
            int len;
            long sum = 0;
            int lastProgress = 0;
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
                sum += len;
                int progress = (int) (sum * 100 / contentLength);
                if (progress > lastProgress) {
                    lastProgress = progress;
                    downloadFileInfo.setDownloadProgress(progress);
                    int finalLastProgress = lastProgress;
                    MainThreadHolder.post(new Runnable() {
                        @Override
                        public void run() {
                            for (DownloadFileCallback callback : downloadFileCallbackSet) {
                                callback.onDownloadProgress(downloadFileInfo, finalLastProgress);
                            }
                        }
                    });
                    LogHelper.print("DownloadFileHelper_downloadFile handleWriteDownloadFile progress: " + lastProgress + " downloadFileName: " + downloadFileInfo.getFileName());
                }
            }
            //write success rename temp file
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (tempFileContentUri != null) {
                    LogHelper.print("DownloadFileHelper_downloadFile handleWriteDownloadFile >=android Q uri rename start downloadFileName: " + downloadFileInfo.getFileName());
                    ContentResolver resolver = ApplicationHolder.getAppContext().getContentResolver();
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, downloadFileInfo.getFileName());
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, StorageHelper.getRelativeDownloadsDirPath());
                    resolver.update(tempFileContentUri, contentValues, null, null);
                    LogHelper.print("DownloadFileHelper_downloadFile handleWriteDownloadFile >=android Q uri rename end downloadFileName: " + downloadFileInfo.getFileName());
                    downloadFileInfo.setLocalUri(tempFileContentUri.toString());
                    MainThreadHolder.post(new Runnable() {
                        @Override
                        public void run() {
                            handleDownloadResult(downloadFileInfo, "下载成功");
                        }
                    });
                } else {
                    LogHelper.print("DownloadFileHelper_downloadFile handleWriteDownloadFile >android Q file rename error download uri is null");
                    MainThreadHolder.post(new Runnable() {
                        @Override
                        public void run() {
                            handleDownloadFileError(downloadFileInfo, "download uri is null");
                        }
                    });
                }
            } else {
                if (tempFile != null) {
                    LogHelper.print("DownloadFileHelper_downloadFile handleWriteDownloadFile <=android Q file rename start downloadFileName: " + downloadFileInfo.getFileName());
                    String downloadFilePath = StorageHelper.getPublicDownloadsDirPath() + "/" + downloadFileInfo.getFileName();
                    tempFile.renameTo(new File(downloadFilePath));
                    LogHelper.print("DownloadFileHelper_downloadFile handleWriteDownloadFile <=android Q file rename end downloadFileName: " + downloadFileInfo.getFileName());
                    downloadFileInfo.setLocalUri(downloadFilePath);
                    MainThreadHolder.post(new Runnable() {
                        @Override
                        public void run() {
                            handleDownloadResult(downloadFileInfo, "下载成功");
                        }
                    });
                } else {
                    LogHelper.print("DownloadFileHelper_downloadFile handleWriteDownloadFile <=android Q file rename error download uri is null");
                    MainThreadHolder.post(new Runnable() {
                        @Override
                        public void run() {
                            handleDownloadFileError(downloadFileInfo, "download uri is null");
                        }
                    });
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
            try {
                if (tempFileContentUri != null) {
                    ContentResolver contentResolver = ApplicationHolder.getAppContext().getContentResolver();
                    contentResolver.delete(tempFileContentUri, null, null);
                    LogHelper.print("DownloadFileHelper_downloadFile handleWriteDownloadFile >=android Q uri try catch error delete uri");
                }
            } catch (Throwable e1) {
                e.printStackTrace();
            }
            try {
                if (tempFile != null) {
                    tempFile.delete();
                    LogHelper.print("DownloadFileHelper_downloadFile handleWriteDownloadFile <=android Q file try catch error delete file");
                }
            } catch (Throwable e2) {
                e.printStackTrace();
            }
            MainThreadHolder.post(new Runnable() {
                @Override
                public void run() {
                    handleDownloadFileError(downloadFileInfo, "WriteDownloadFile catch error:" + e.getMessage());
                }
            });
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            try {
                inputStream.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private void handleDownloadResult(DownloadFileInfo downloadFileInfo, String downloadResultMsg) {
        downloadingFileInfoList.remove(downloadFileInfo);
        for (DownloadFileCallback callback : downloadFileCallbackSet) {
            callback.onDownloadResult(downloadFileInfo, downloadResultMsg);
        }
        LogHelper.print("DownloadFileHelper_downloadFileName: " + downloadFileInfo.getFileName() + " handleDownloadSuccess uploadResultMsg: " + downloadResultMsg);
    }

    private void handleDownloadFileError(DownloadFileInfo downloadFileInfo, String errorMsg) {
        downloadingFileInfoList.remove(downloadFileInfo);
        for (DownloadFileCallback callback : downloadFileCallbackSet) {
            callback.onDownloadFail(downloadFileInfo, errorMsg);
        }
        LogHelper.print("DownloadFileHelper_downloadFileName: " + downloadFileInfo.getFileName() + " handleDownloadFileError error: " + errorMsg);
    }

    public void addDownloadFileCallback(DownloadFileCallback callback) {
        if (callback != null) {
            downloadFileCallbackSet.add(callback);
        }
    }

    public void removeDownloadFileCallback(DownloadFileCallback callback) {
        downloadFileCallbackSet.remove(callback);
    }

}