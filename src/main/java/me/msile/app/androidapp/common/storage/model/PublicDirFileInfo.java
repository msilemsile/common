package me.msile.app.androidapp.common.storage.model;

/**
 * 本地app创建的文件
 * ps: build uri
 * long fileId = cursor.getLong(fileIdIndex);
 * Uri pathUri = downloadUri.buildUpon().appendPath(String.valueOf(fileId)).build();
 */
public class PublicDirFileInfo {
    private long fileId;
    private String displayName;
    private String relativePath;
    private String data;

    public PublicDirFileInfo(String displayName) {
        this.displayName = displayName;
    }


    public PublicDirFileInfo(long fileId, String displayName, String relativePath, String data) {
        this.fileId = fileId;
        this.displayName = displayName;
        this.relativePath = relativePath;
        this.data = data;
    }

    public long getFileId() {
        return fileId;
    }

    public void setFileId(long fileId) {
        this.fileId = fileId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "PublicDirFileInfo{" +
                "fileId=" + fileId +
                ", displayName='" + displayName + '\'' +
                ", relativePath='" + relativePath + '\'' +
                ", data='" + data + '\'' +
                '}';
    }
}
