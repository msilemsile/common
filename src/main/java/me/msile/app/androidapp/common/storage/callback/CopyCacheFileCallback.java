package me.msile.app.androidapp.common.storage.callback;

import java.util.List;

import me.msile.app.androidapp.common.storage.model.CacheFileInfo;

public interface CopyCacheFileCallback {
    void onSuccess(List<CacheFileInfo> cacheFileInfoList);

    void onError(String errorMsg);
}
