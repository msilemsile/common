package me.msile.app.androidapp.common.camera;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import me.msile.app.androidapp.common.R;
import me.msile.app.androidapp.common.glide.GlideApp;
import me.msile.app.androidapp.common.permissions.PermissionHelper;
import me.msile.app.androidapp.common.permissions.callback.PermissionCallback;
import me.msile.app.androidapp.common.rx.AutoDisposeUtils;
import me.msile.app.androidapp.common.rx.DefaultDisposeObserver;
import me.msile.app.androidapp.common.storage.MediaInsertHelper;
import me.msile.app.androidapp.common.ui.activity.ImmerseFullScreenActivity;
import me.msile.app.androidapp.common.ui.dialog.AppAlertDialog;
import me.msile.app.androidapp.common.ui.widget.shapelayout.ShapeImageView;

public class SimpleCameraActivity extends ImmerseFullScreenActivity {

    private SurfaceView surfaceView;
    private ShapeImageView ivCenter;
    private TextView tvCaptureStatus;
    private ImageView ivCamera;
    private ImageView ivClose;
    private ImageView ivSwitch;
    private CameraHelper cameraHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_camera);
        initView();
        PermissionHelper.with(this)
                .requestPermission(new SimpleCameraPermission(), new PermissionCallback() {
                    @Override
                    public void onGranted() {
                        initData();
                    }

                    @Override
                    public void onCancel() {

                    }
                });
    }

    private void initView() {
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        tvCaptureStatus = (TextView) findViewById(R.id.tv_capture_status);
        ivClose = (ImageView) findViewById(R.id.iv_close);
        ivSwitch = (ImageView) findViewById(R.id.iv_switch);
        ivClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        ivSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraHelper.switchCamera();
            }
        });
        ivCamera = (ImageView) findViewById(R.id.iv_camera);
        ivCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraHelper.isCaptureStarted()) {
                    cameraHelper.stopCaptureVideo();
                } else {
                    cameraHelper.takePicture();
                }
            }
        });
        ivCamera.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                cameraHelper.startCaptureVideo();
                return true;
            }
        });
        ivCenter = (ShapeImageView) findViewById(R.id.iv_center);
        ivCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ivCenter.setVisibility(View.GONE);
            }
        });
    }

    private void initData() {
        cameraHelper = getWeakRefHolder(CameraHelper.class);
        cameraHelper.prepare(surfaceView);
        cameraHelper.setCameraHelperCallback(new CameraHelper.OnCameraHelperCallback() {
            @Override
            public void onTakePictureSuccess(String picPath) {
                ivCenter.setVisibility(View.VISIBLE);
                GlideApp.with(SimpleCameraActivity.this).load(picPath).into(ivCenter);
                MediaInsertHelper.insertPicToGallery(picPath);
                cameraHelper.startPreview();
            }

            @Override
            public void onTakePictureError(String message) {
                AppAlertDialog.build().setTitleText("拍照失败")
                        .setContentText(message)
                        .setConfirmText("确定")
                        .show(SimpleCameraActivity.this);
                cameraHelper.startPreview();
            }

            @Override
            public void onCaptureVideoSuccess(String videoPath) {
                refreshStopCaptureStatus();
                ivCenter.setVisibility(View.VISIBLE);
                GlideApp.with(SimpleCameraActivity.this).load(videoPath).into(ivCenter);
                MediaInsertHelper.insertVideoToMedia(videoPath);
            }

            @Override
            public void onCaptureVideoError(String message) {
                refreshStopCaptureStatus();
                AppAlertDialog.build().setTitleText("录像失败")
                        .setContentText(message)
                        .setConfirmText("确定")
                        .show(SimpleCameraActivity.this);
            }

            @Override
            public void onCaptureVideoStart() {
                refreshStartCaptureStatus();
            }
        });
    }

    private DefaultDisposeObserver<Long> captureStatusObserver;
    private long captureTime;

    private void refreshStartCaptureStatus() {
        refreshStopCaptureStatus();
        tvCaptureStatus.setText("正在录制");
        ivCamera.setImageResource(R.mipmap.icon_record_press);
        captureTime = 0;
        captureStatusObserver = new DefaultDisposeObserver<Long>() {
            @Override
            protected void onSuccess(Long aLong) {
                captureTime++;
                tvCaptureStatus.setText("正在录制: " + captureTime + "秒");
            }
        };
        Observable.interval(1, 1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .to(AutoDisposeUtils.onDestroyDispose(this))
                .subscribe(captureStatusObserver);
    }

    private void refreshStopCaptureStatus() {
        if (captureStatusObserver != null) {
            captureStatusObserver.dispose();
            captureStatusObserver = null;
        }
        tvCaptureStatus.setText("点击拍照,长按录像");
        ivCamera.setImageResource(R.mipmap.icon_record_normal);
    }

    public static void goToPage(Context context) {
        Intent intent = new Intent(context, SimpleCameraActivity.class);
        context.startActivity(intent);
    }
}
