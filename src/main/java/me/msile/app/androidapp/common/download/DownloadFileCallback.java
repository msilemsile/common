package me.msile.app.androidapp.common.download;

public interface DownloadFileCallback {

    void onDownloadStart(DownloadFileInfo downloadFileInfo);

    void onDownloadProgress(DownloadFileInfo downloadFileInfo, int progress);

    void onDownloadFail(DownloadFileInfo downloadFileInfo, String errorMsg);

    void onDownloadResult(DownloadFileInfo downloadFileInfo, String downloadResultMsg);
}
