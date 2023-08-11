package me.msile.app.androidapp.common.storage;

import android.net.Uri;

import java.io.File;

public class CacheFileInfo {
    private File file;
    private String originFileName;
    private Uri originFileUri;

    public CacheFileInfo(File file) {
        this.file = file;
    }

    public Uri getOriginFileUri() {
        return originFileUri;
    }

    public void setOriginFileUri(Uri originFileUri) {
        this.originFileUri = originFileUri;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getOriginFileName() {
        return originFileName;
    }

    public void setOriginFileName(String originFileName) {
        this.originFileName = originFileName;
    }

    @Override
    public String toString() {
        return "CacheFileInfo{" +
                "file=" + file.getAbsolutePath() +
                ", originFileName='" + originFileName + '\'' +
                ", originFileUri=" + originFileUri +
                '}';
    }
}
