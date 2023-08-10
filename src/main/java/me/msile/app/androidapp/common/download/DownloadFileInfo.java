package me.msile.app.androidapp.common.download;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DownloadFileInfo {

    private String downloadId = "D_" + System.currentTimeMillis();
    private String fileName;
    private String url;
    private String localUri;
    private int downloadProgress;
    private String downloadResultMsg;
    private Object downloadTag;
    private Map<String,String> downloadExtras;

    public DownloadFileInfo(String url) {
        this.url = url;
    }

    public Object getDownloadTag() {
        return downloadTag;
    }

    public void setDownloadTag(Object downloadTag) {
        this.downloadTag = downloadTag;
    }

    public Map<String, String> getDownloadExtras() {
        return downloadExtras;
    }

    public void setDownloadExtras(Map<String, String> downloadExtras) {
        this.downloadExtras = downloadExtras;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLocalUri() {
        return localUri;
    }

    public void setLocalUri(String localUri) {
        this.localUri = localUri;
    }

    public int getDownloadProgress() {
        return downloadProgress;
    }

    public void setDownloadProgress(int downloadProgress) {
        this.downloadProgress = downloadProgress;
    }

    public String getDownloadResultMsg() {
        return downloadResultMsg;
    }

    public void setDownloadResultMsg(String downloadResultMsg) {
        this.downloadResultMsg = downloadResultMsg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DownloadFileInfo that = (DownloadFileInfo) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    public String getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(String downloadId) {
        this.downloadId = downloadId;
    }

    @Override
    public String toString() {
        return "DownloadFileInfo{" +
                "downloadId='" + downloadId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", url='" + url + '\'' +
                ", localUri='" + localUri + '\'' +
                ", downloadProgress=" + downloadProgress +
                ", downloadResultMsg='" + downloadResultMsg + '\'' +
                '}';
    }
}
