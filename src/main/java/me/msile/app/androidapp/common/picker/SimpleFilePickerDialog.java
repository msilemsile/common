package me.msile.app.androidapp.common.picker;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.msile.app.androidapp.common.R;
import me.msile.app.androidapp.common.ui.dialog.BaseRecyclerDialog;

/**
 * 选择文件弹窗（文件、相册、拍照、录像）
 */
public class SimpleFilePickerDialog extends BaseRecyclerDialog {

    private LinearLayout llPickContent;
    private FilePickerHelper.OnPickFileListener pickFileListener;

    private boolean allowPickMultiFile;
    private boolean showGallery;
    private boolean showGalleryMulti;
    private boolean showFilePick;
    private boolean showImageCapture;
    private boolean showVideoCapture;
    private boolean showAllPickOperate;
    private int autoStartPickType;
    private int currentPickType;
    private FilePickerHelper mFilePickerHelper;

    public SimpleFilePickerDialog() {
        setCancelable(false);
    }

    @Override
    protected int getLayoutResId() {
        return me.msile.app.androidapp.common.R.layout.dialog_app_pick_file;
    }

    @Override
    protected void initViews(View rootView) {
        llPickContent = rootView.findViewById(R.id.ll_pick_content);
        llPickContent.removeAllViews();
    }

    @Override
    protected void initData(boolean isFirstInit, @Nullable Bundle savedInstanceState) {
        if (mFilePickerHelper == null) {
            mFilePickerHelper = new FilePickerHelper(mActivity);
        }
        mFilePickerHelper.setAppPickFileListener(new FilePickerHelper.OnPickFileListener() {
            @Override
            public void onPickFile(@Nullable Uri[] uri) {
                if (pickFileListener != null) {
                    pickFileListener.onPickFile(uri);
                }
                dismiss();
            }

            @Override
            public void onPickCancel() {
                if (pickFileListener != null) {
                    pickFileListener.onPickCancel();
                }
                dismiss();
            }
        });
        if (savedInstanceState != null) {
            allowPickMultiFile = savedInstanceState.getBoolean("allowPickMultiFile");
            showGallery = savedInstanceState.getBoolean("showGallery");
            showGalleryMulti = savedInstanceState.getBoolean("showGalleryMulti");
            showFilePick = savedInstanceState.getBoolean("showFilePick");
            showImageCapture = savedInstanceState.getBoolean("showImageCapture");
            showVideoCapture = savedInstanceState.getBoolean("showVideoCapture");
            showAllPickOperate = savedInstanceState.getBoolean("showAllPickOperate");
            autoStartPickType = savedInstanceState.getInt("autoStartPickType");
            currentPickType = savedInstanceState.getInt("currentPickType");
            Log.d("AppPickDialog", "initData restore savedInstanceState");
        }
        if (autoStartPickType != 0) {
            onClickPickFileItem(autoStartPickType);
        } else {
            initPickItems();
            //最后一个item 不展示底部分割线
            int childCount = llPickContent.getChildCount();
            if (childCount > 0) {
                View childAt = llPickContent.getChildAt(childCount - 1);
                View divider = childAt.findViewById(R.id.bottom_divider);
                if (divider != null) {
                    divider.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("allowPickMultiFile", allowPickMultiFile);
        outState.putBoolean("showGallery", showGallery);
        outState.putBoolean("showFilePick", showFilePick);
        outState.putBoolean("showImageCapture", showImageCapture);
        outState.putBoolean("showVideoCapture", showVideoCapture);
        outState.putBoolean("showAllPickOperate", showAllPickOperate);
        outState.putInt("autoStartPickType", autoStartPickType);
        outState.putInt("currentPickType", currentPickType);
        Log.d("AppPickDialog", "onSaveInstanceState");
    }

    private void initPickItems() {
        //拍照
        if (showImageCapture || showAllPickOperate) {
            buildPickFileItem(FilePickerHelper.PICK_TYPE_IMAGE_CAPTURE);
        }
        //录像
        if (showVideoCapture || showAllPickOperate) {
            buildPickFileItem(FilePickerHelper.PICK_TYPE_VIDEO_CAPTURE);
        }
        //相册
        if (showGallery || showAllPickOperate) {
            buildPickFileItem(FilePickerHelper.PICK_TYPE_GALLERY);
        }
        //文件
        if (showFilePick || showAllPickOperate) {
            buildPickFileItem(FilePickerHelper.PICK_TYPE_FILE);
        }
        //相册多选
        if (showGalleryMulti || showAllPickOperate) {
            buildPickFileItem(FilePickerHelper.PICK_TYPE_GALLERY_MULTI);
        }
        //取消
        buildPickFileItem(0);
    }

    public SimpleFilePickerDialog setShowAllPickOperate(boolean showAllPickOperate) {
        this.showAllPickOperate = showAllPickOperate;
        return this;
    }

    public SimpleFilePickerDialog setAllowPickMultiFile(boolean allowPickMultiFile) {
        this.allowPickMultiFile = allowPickMultiFile;
        return this;
    }

    public SimpleFilePickerDialog setShowFilePick(boolean showFilePick) {
        this.showFilePick = showFilePick;
        return this;
    }

    public SimpleFilePickerDialog setShowGallery(boolean showGallery) {
        this.showGallery = showGallery;
        return this;
    }

    public SimpleFilePickerDialog setShowImageCapture(boolean showImageCapture) {
        this.showImageCapture = showImageCapture;
        return this;
    }

    public SimpleFilePickerDialog setShowVideoCapture(boolean showVideoCapture) {
        this.showVideoCapture = showVideoCapture;
        return this;
    }

    public SimpleFilePickerDialog setAutoStartPickType(int autoStartPickType) {
        this.autoStartPickType = autoStartPickType;
        return this;
    }

    public SimpleFilePickerDialog setPickFileListener(FilePickerHelper.OnPickFileListener appPickFileListener) {
        this.pickFileListener = appPickFileListener;
        return this;
    }

    private void buildPickFileItem(int pickType) {
        View itemView = LayoutInflater.from(mActivity).inflate(R.layout.item_app_pick_file, llPickContent, false);
        TextView tvPick = itemView.findViewById(R.id.tv_pick);
        switch (pickType) {
            case FilePickerHelper.PICK_TYPE_FILE:
                tvPick.setText(allowPickMultiFile ? "文件(可多选)" : "文件");
                break;
            case FilePickerHelper.PICK_TYPE_GALLERY:
                tvPick.setText("相册");
                break;
            case FilePickerHelper.PICK_TYPE_IMAGE_CAPTURE:
                tvPick.setText("拍照");
                break;
            case FilePickerHelper.PICK_TYPE_VIDEO_CAPTURE:
                tvPick.setText("录像");
                break;
            case FilePickerHelper.PICK_TYPE_GALLERY_MULTI:
                tvPick.setText("相册(可多选)");
                break;
            case 0:
                tvPick.setText("取消");
                tvPick.setTextColor(0xff999999);
                break;
        }
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickPickFileItem(pickType);
            }
        });
        llPickContent.addView(itemView);
    }

    private void onClickPickFileItem(int pickType) {
        switch (pickType) {
            case FilePickerHelper.PICK_TYPE_FILE:
                mFilePickerHelper.startPickFile();
                break;
            case FilePickerHelper.PICK_TYPE_GALLERY:
                mFilePickerHelper.startPickFromGallery();
                break;
            case FilePickerHelper.PICK_TYPE_IMAGE_CAPTURE:
                mFilePickerHelper.startImageCapture();
                break;
            case FilePickerHelper.PICK_TYPE_VIDEO_CAPTURE:
                mFilePickerHelper.startVideoRecord();
                break;
            case FilePickerHelper.PICK_TYPE_GALLERY_MULTI:
                mFilePickerHelper.startPickFromGalleryMulti();
                break;
            case 0:
                if (pickFileListener != null) {
                    pickFileListener.onPickCancel();
                }
                dismiss();
                break;
        }
        currentPickType = pickType;
    }

    public static SimpleFilePickerDialog build() {
        return new SimpleFilePickerDialog();
    }
}
