package me.msile.app.androidapp.common.storage.callback;

import java.util.List;

import me.msile.app.androidapp.common.storage.model.PublicDirFileInfo;

public interface GetPublicDirFileCallback {
    void onSuccess(List<PublicDirFileInfo> publicDirFileInfoList);

    void onError(String message);
}
