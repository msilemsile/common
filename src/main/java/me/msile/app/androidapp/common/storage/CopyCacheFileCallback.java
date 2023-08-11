package me.msile.app.androidapp.common.storage;

import java.util.List;

public interface CopyCacheFileCallback {
    void onSuccess(List<CacheFileInfo> cacheFileInfoList);

    void onError(String errorMsg);
}
