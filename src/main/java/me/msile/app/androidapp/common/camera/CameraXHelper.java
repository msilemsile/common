package me.msile.app.androidapp.common.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.OutputResults;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.msile.app.androidapp.common.core.ActivityWeakRefHolder;
import me.msile.app.androidapp.common.storage.StorageHelper;
import me.msile.app.androidapp.common.ui.toast.AppToast;

/**
 * camerax帮助类
 */
public class CameraXHelper extends ActivityWeakRefHolder {

    private static final String TAG = "CameraXHelper";
    private ProcessCameraProvider processCameraProvider;
    private PreviewView previewView;
    private Preview preview;
    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    private Recording mMediaRecorder;
    private boolean isPrepared;
    private boolean isCaptureStarted;
    private boolean isFrontCamera;
    private OnCameraHelperCallback onCameraHelperCallback;
    private String mCameraOutputPath;
    private boolean isFirstInit = true;

    public CameraXHelper(@NonNull Activity activity) {
        super(activity);
    }

    public void setCameraHelperCallback(OnCameraHelperCallback onCameraHelperCallback) {
        this.onCameraHelperCallback = onCameraHelperCallback;
    }

    @Override
    protected void onActivityPause() {
        isPrepared = false;
        try {
            if (isCaptureStarted) {
                if (onCameraHelperCallback != null) {
                    onCameraHelperCallback.onCaptureVideoError("releaseMediaRecorder");
                }
            }
            if (processCameraProvider != null) {
                processCameraProvider.unbindAll();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResume() {
        if (!isFirstInit && !isPrepared && previewView != null) {
            prepare(previewView, isFrontCamera);
        }
    }

    public void prepare(PreviewView previewView) {
        prepare(previewView, false);
    }

    public void prepare(PreviewView previewView, boolean isFrontCamera) {
        Activity activityWithCheck = getHolderActivityWithCheck();
        if (activityWithCheck == null) {
            return;
        }
        this.previewView = previewView;
        this.isFrontCamera = isFrontCamera;
        if (processCameraProvider == null) {
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(activityWithCheck);
            cameraProviderFuture.addListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        processCameraProvider = cameraProviderFuture.get();
                        startPreview();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }, ContextCompat.getMainExecutor(activityWithCheck));
        } else {
            processCameraProvider.unbindAll();
            startPreview();
        }
    }

    public void startPreview() {
        try {
            if (processCameraProvider != null) {
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(isFrontCamera ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK)
                        .build();
                preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                processCameraProvider.bindToLifecycle((LifecycleOwner) getHolderActivity(), cameraSelector, preview);
                isPrepared = true;
                isFirstInit = false;
            }
            Log.i(TAG, "startPreview");
        } catch (Throwable e) {
            e.printStackTrace();
            Log.i(TAG, "startPreview error: " + e.getMessage());
        }

    }

    public void switchCamera() {
        if (!isPrepared) {
            AppToast.toastMsg("未准备就绪");
            return;
        }
        prepare(previewView, !isFrontCamera);
    }

    public void pauseCaptureVideo() {
        if (!isPrepared) {
            AppToast.toastMsg("未准备就绪");
            return;
        }
        if (!isCaptureStarted) {
            AppToast.toastMsg("拍摄未开始");
            return;
        }
        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.pause();
                Log.i(TAG, "pauseCaptureVideo MediaRecorder pause");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            Log.i(TAG, "pauseCaptureVideo error: " + e.getMessage());
        }
    }

    public void resumeCaptureVideo() {
        if (!isPrepared) {
            AppToast.toastMsg("未准备就绪");
            return;
        }
        if (!isCaptureStarted) {
            AppToast.toastMsg("拍摄未开始");
            return;
        }
        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.resume();
                Log.i(TAG, "resumeCaptureVideo MediaRecorder resume");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            Log.i(TAG, "resumeCaptureVideo error: " + e.getMessage());
        }
    }

    public void takePicture() {
        if (!isPrepared) {
            AppToast.toastMsg("未准备就绪");
            return;
        }
        try {
            if (processCameraProvider != null) {
                if (imageCapture != null) {
                    processCameraProvider.unbind(imageCapture);
                }
                imageCapture = new ImageCapture.Builder().build();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(isFrontCamera ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK)
                        .build();
                processCameraProvider.bindToLifecycle((LifecycleOwner) getHolderActivity(), cameraSelector, imageCapture);
                mCameraOutputPath = null;
                File cacheFile = StorageHelper.createCacheFile("takePicture_cache_" + System.currentTimeMillis() + ".jpg");
                mCameraOutputPath = cacheFile.getAbsolutePath();
                ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(cacheFile)
                        .build();
                imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(getHolderActivity()), new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.i(TAG, "takePicture onTakePictureSuccess: " + mCameraOutputPath);
                        if (onCameraHelperCallback != null) {
                            onCameraHelperCallback.onTakePictureSuccess(mCameraOutputPath);
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.i(TAG, "takePicture onTakePictureError: " + exception.getMessage());
                        if (onCameraHelperCallback != null) {
                            onCameraHelperCallback.onTakePictureError(exception.getMessage());
                        }
                    }
                });
            }
            Log.i(TAG, "takePicture");
        } catch (Throwable e) {
            e.printStackTrace();
            Log.i(TAG, "takePicture error: " + e.getMessage());
        }
    }

    public void stopCaptureVideo() {
        if (!isPrepared) {
            AppToast.toastMsg("未准备就绪");
            return;
        }
        if (!isCaptureStarted) {
            AppToast.toastMsg("拍摄未开始");
            return;
        }
        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.stop();
                mMediaRecorder = null;
            }
            Log.i(TAG, "stopCaptureVideo");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        isCaptureStarted = false;
    }

    public boolean isCaptureStarted() {
        return isCaptureStarted;
    }

    @Override
    public void onClear() {
    }

    @SuppressLint("MissingPermission")
    public void startCaptureVideo() {
        if (!isPrepared) {
            AppToast.toastMsg("未准备就绪");
            return;
        }
        if (isCaptureStarted) {
            AppToast.toastMsg("正在录制中");
            return;
        }
        try {
            if (processCameraProvider != null) {
                if (videoCapture != null) {
                    processCameraProvider.unbind(videoCapture);
                }
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(isFrontCamera ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK)
                        .build();
                List<CameraInfo> cameraInfoList = cameraSelector.filter(processCameraProvider.getAvailableCameraInfos());
                QualitySelector qualitySelector = null;
                if (!cameraInfoList.isEmpty()) {
                    qualitySelector = QualitySelector.fromOrderedList(QualitySelector.getSupportedQualities(cameraInfoList.get(0)), FallbackStrategy.lowerQualityOrHigherThan(Quality.SD));
                } else {
                    List<Quality> qualityList = new ArrayList<>();
                    qualityList.add(Quality.UHD);
                    qualityList.add(Quality.FHD);
                    qualityList.add(Quality.HD);
                    qualityList.add(Quality.SD);
                    qualitySelector = QualitySelector.fromOrderedList(qualityList,
                            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD));
                }
                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(qualitySelector)
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);
                processCameraProvider.bindToLifecycle((LifecycleOwner) getHolderActivity(), cameraSelector, videoCapture);
                mCameraOutputPath = null;
                //output cache file
                File captureVideoFile = StorageHelper.createCacheFile("captureVideo_cache_" + System.currentTimeMillis() + ".mp4");
                mCameraOutputPath = captureVideoFile.getAbsolutePath();
                FileOutputOptions fileOutputOptions = new FileOutputOptions.Builder(captureVideoFile).build();
                mMediaRecorder = recorder.prepareRecording(getHolderActivity(), fileOutputOptions)
                        .withAudioEnabled()
                        .start(ContextCompat.getMainExecutor(getHolderActivity()), new Consumer<VideoRecordEvent>() {
                            @Override
                            public void accept(VideoRecordEvent videoRecordEvent) {
                                if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                                    if (onCameraHelperCallback != null) {
                                        onCameraHelperCallback.onCaptureVideoStart();
                                    }
                                    isCaptureStarted = true;
                                    Log.i(TAG, "startCaptureVideo onCaptureVideoStart");
                                } else if (videoRecordEvent instanceof VideoRecordEvent.Pause) {
                                    Log.i(TAG, "startCaptureVideo Pause");
                                } else if (videoRecordEvent instanceof VideoRecordEvent.Resume) {
                                    Log.i(TAG, "startCaptureVideo Resume");
                                } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                                    VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) videoRecordEvent;
                                    // Handles a finalize event for the active recording, checking Finalize.getError()
                                    int error = finalizeEvent.getError();
                                    if (error != VideoRecordEvent.Finalize.ERROR_NONE) {
                                        if (onCameraHelperCallback != null) {
                                            onCameraHelperCallback.onCaptureVideoError("finalizeEvent error: " + error);
                                        }
                                    } else {
                                        OutputResults outputResults = finalizeEvent.getOutputResults();
                                        Log.i(TAG, "VideoRecordEvent.Finalize output : " + outputResults.getOutputUri());
                                        if (onCameraHelperCallback != null) {
                                            onCameraHelperCallback.onCaptureVideoSuccess(mCameraOutputPath);
                                        }
                                    }
                                    Log.i(TAG, "startCaptureVideo finalizeEvent");
                                    isCaptureStarted = false;
                                }
                            }
                        });
            }
            Log.i(TAG, "startCaptureVideo");
        } catch (Throwable e) {
            e.printStackTrace();
            if (onCameraHelperCallback != null) {
                onCameraHelperCallback.onCaptureVideoError(e.getMessage());
            }
            isCaptureStarted = false;
        }
    }

    public interface OnCameraHelperCallback {
        default void onTakePictureSuccess(String picPath) {
        }

        default void onTakePictureError(String message) {
        }

        default void onCaptureVideoSuccess(String videoPath) {
        }

        default void onCaptureVideoError(String message) {
        }

        default void onCaptureVideoStart() {
        }
    }
}
