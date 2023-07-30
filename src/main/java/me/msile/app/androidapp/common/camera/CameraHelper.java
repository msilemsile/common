package me.msile.app.androidapp.common.camera;

import android.app.Activity;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

import me.msile.app.androidapp.common.core.ActivityWeakRefHolder;
import me.msile.app.androidapp.common.storage.StorageHelper;
import me.msile.app.androidapp.common.ui.toast.AppToast;

/**
 * camera1帮助类
 */
public class CameraHelper extends ActivityWeakRefHolder implements SurfaceHolder.Callback {

    private static final String TAG = "CameraHelper";

    private boolean isPrepared;
    private boolean isCaptureStarted;
    private boolean isFrontCamera;
    private SurfaceView mSurfaceView;

    private Camera mCamera;
    private MediaRecorder mMediaRecorder;
    private OnCameraHelperCallback cameraHelperCallback;

    private String mCameraOutputPath;

    public CameraHelper(@NonNull Activity activity) {
        super(activity);
    }

    public void prepare(SurfaceView surfaceView) {
        prepare(surfaceView, false);
    }

    public void prepare(SurfaceView surfaceView, boolean isFrontCamera) {
        Activity activityWithCheck = getHolderActivityWithCheck();
        if (activityWithCheck == null) {
            return;
        }
        if (mMediaRecorder != null) {
            releaseMediaRecorder();
        }
        int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        if (mCamera != null) {
            releaseCamera();
        }
        if (isFrontCamera) {
            mCamera = openFrontCamera();
            cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCamera = openBackCamera();
        }
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activityWithCheck.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
        mSurfaceView = surfaceView;
        this.isFrontCamera = isFrontCamera;
        SurfaceHolder viewHolder = surfaceView.getHolder();
        viewHolder.addCallback(this);
        try {
            if (!viewHolder.isCreating()) {
                mCamera.setPreviewDisplay(viewHolder);
                setCameraInitParams(surfaceView.getWidth(), surfaceView.getHeight());
                mCamera.startPreview();
                isPrepared = true;
                Log.i(TAG, "prepare viewHolder created");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            AppToast.toastMsg("相机预览失败!");
        }
    }

    private void setCameraInitParams(int width, int height) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            int[] bestPreviewSize = getBestPreviewSize(width, height);
            parameters.setPreviewSize(bestPreviewSize[0], bestPreviewSize[1]);
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            mCamera.setParameters(parameters);
        }
    }

    public void setCameraHelperCallback(OnCameraHelperCallback cameraHelperCallback) {
        this.cameraHelperCallback = cameraHelperCallback;
    }

    public String getCameraOutputPath() {
        return mCameraOutputPath;
    }

    public void takePicture() {
        if (!isPrepared) {
            AppToast.toastMsg("未准备就绪");
            return;
        }
        try {
            if (mCamera != null) {
                mCameraOutputPath = null;
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        try {
                            File cacheFile = StorageHelper.createCacheFile("takePicture_cache_" + System.currentTimeMillis() + ".jpg");
                            mCameraOutputPath = cacheFile.getAbsolutePath();
                            FileOutputStream fileOutputStream = new FileOutputStream(cacheFile);
                            fileOutputStream.write(data);
                            fileOutputStream.close();
                            //fix pic orientation
                            ExifInterface exifInterface = new ExifInterface(mCameraOutputPath);
                            // 修正图片的旋转角度，设置其不旋转。这里也可以设置其旋转的角度，可以传值过去，
                            // 例如旋转90度，传值ExifInterface.ORIENTATION_ROTATE_90，需要将这个值转换为String类型的
                            String picOrientation;
                            if (isFrontCamera) {
                                picOrientation = String.valueOf(ExifInterface.ORIENTATION_ROTATE_270);
                            } else {
                                picOrientation = String.valueOf(ExifInterface.ORIENTATION_ROTATE_90);
                            }
                            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, picOrientation);
                            exifInterface.saveAttributes();
                            if (cameraHelperCallback != null) {
                                cameraHelperCallback.onTakePictureSuccess(mCameraOutputPath);
                            }
                            Log.i(TAG, "takePicture cache file: " + mCameraOutputPath);
                        } catch (Throwable e) {
                            e.printStackTrace();
                            if (cameraHelperCallback != null) {
                                cameraHelperCallback.onTakePictureError(e.getMessage());
                            }
                            Log.i(TAG, "takePicture write output error: " + e.getMessage());
                        }
                    }
                });
            }
        } catch (Throwable e) {
            e.printStackTrace();
            if (cameraHelperCallback != null) {
                cameraHelperCallback.onTakePictureError(e.getMessage());
            }
            Log.i(TAG, "takePicture error: " + e.getMessage());
        }
    }

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
            if (mCamera != null) {
                mCameraOutputPath = null;
                //output cache file
                File captureVideoFile = StorageHelper.createCacheFile("captureVideo_cache_" + System.currentTimeMillis() + ".mp4");
                mCameraOutputPath = captureVideoFile.getAbsolutePath();
                if (mMediaRecorder == null) {
                    mMediaRecorder = new MediaRecorder();
                }

                // Step 1: Unlock and set camera to MediaRecorder
                mCamera.unlock();
                mMediaRecorder.setCamera(mCamera);

                if (isFrontCamera) {
                    mMediaRecorder.setOrientationHint(270);
                } else {
                    mMediaRecorder.setOrientationHint(90);
                }

                // Step 2: Set sources
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

                // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
                mMediaRecorder.setProfile(getBestCamcorderProfile());

                // Step 4: Set output file
                mMediaRecorder.setOutputFile(captureVideoFile.getAbsolutePath());

                // Step 5: Set the preview output
                mMediaRecorder.setPreviewDisplay(mSurfaceView.getHolder().getSurface());

                // Step 6: Prepare configured MediaRecorder
                mMediaRecorder.prepare();

                mMediaRecorder.start();

                isCaptureStarted = true;
                Log.i(TAG, "startCaptureVideo MediaRecorder.start");
                if (cameraHelperCallback != null) {
                    cameraHelperCallback.onCaptureVideoStart();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            releaseMediaRecorder();
            try {
                if (mCamera != null) {
                    mCamera.lock();
                    Log.i(TAG, "startCaptureVideo error lock camera");
                }
            } catch (Throwable error) {
                error.printStackTrace();
            }
            isCaptureStarted = false;
            Log.i(TAG, "startCaptureVideo error: " + e.getMessage());
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
                mMediaRecorder.reset();
                if (cameraHelperCallback != null) {
                    cameraHelperCallback.onCaptureVideoSuccess(mCameraOutputPath);
                }
                Log.i(TAG, "stopCaptureVideo MediaRecorder stop and reset");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            if (cameraHelperCallback != null) {
                cameraHelperCallback.onCaptureVideoError(e.getMessage());
            }
            Log.i(TAG, "stopCaptureVideo error: " + e.getMessage());
        }
        isCaptureStarted = false;
    }

    public void switchCamera() {
        if (!isPrepared) {
            AppToast.toastMsg("未准备就绪");
            return;
        }
        if (isCaptureStarted) {
            AppToast.toastMsg("正在录制中,请先停止拍摄!");
            return;
        }
        prepare(mSurfaceView, !isFrontCamera);
    }

    public boolean isCaptureStarted() {
        return isCaptureStarted;
    }

    public boolean isPrepared() {
        return isPrepared;
    }

    public void stopPreview() {
        try {
            if (mCamera != null) {
                mCamera.stopPreview();
                Log.i(TAG, "stopPreview");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            Log.i(TAG, "stopPreview error: " + e.getMessage());
        }
        isPrepared = false;
    }

    public void startPreview() {
        if (!isPrepared) {
            AppToast.toastMsg("未准备就绪");
            return;
        }
        try {
            if (mCamera != null) {
                mCamera.startPreview();
                Log.i(TAG, "startPreview");
                isPrepared = true;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            Log.i(TAG, "startPreview error: " + e.getMessage());
        }
    }

    private void releaseMediaRecorder() {
        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.reset();   // clear recorder configuration
                mMediaRecorder.release(); // release the recorder object
                mMediaRecorder = null;
                Log.i(TAG, "releaseMediaRecorder");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            Log.i(TAG, "releaseMediaRecorder error: " + e.getMessage());
        } finally {
            mMediaRecorder = null;
        }
    }

    private void releaseCamera() {
        try {
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
                Log.i(TAG, "releaseCamera");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            Log.i(TAG, "releaseCamera error: " + e.getMessage());
        } finally {
            mCamera = null;
        }
    }

    @Override
    public void onClear() {
        releaseMediaRecorder();
        releaseCamera();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            isPrepared = true;
            Log.i(TAG, "surfaceCreated prepared");
        } catch (Throwable e) {
            e.printStackTrace();
            AppToast.toastMsg("相机预览失败!");
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (holder.getSurface() == null) {
            // preview surface does not exist
            Log.i(TAG, "surfaceChanged surface does not exist");
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Throwable e) {
            e.printStackTrace();
            Log.i(TAG, "surfaceChanged mCamera.stopPreview error: " + e.getMessage());
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(holder);
            setCameraInitParams(width, height);
            mCamera.startPreview();
            isPrepared = true;
            Log.i(TAG, "surfaceChanged prepared");
        } catch (Throwable e) {
            e.printStackTrace();
            Log.i(TAG, "surfaceChanged mCamera.setPreviewDisplay error:" + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
    }

    private CamcorderProfile getBestCamcorderProfile() {
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_CIF);
        if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_1080P)) {
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
        } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        }
        return profile;
    }

    private int[] getBestPreviewSize(int width, int height) {
        int[] result = new int[]{width, height};
        if (mCamera != null) {
            final Camera.Parameters p = mCamera.getParameters();
            //特别注意此处需要规定rate的比是大的比小的，不然有可能出现rate = height/width，但是后面遍历的时候，current_rate = width/height,所以我们限定都为大的比小的。
            float rate = (float) Math.max(width, height) / (float) Math.min(width, height);
            float tmp_diff;
            float min_diff = -1f;
            for (Camera.Size size : p.getSupportedPreviewSizes()) {
                float current_rate = (float) Math.max(size.width, size.height) / (float) Math.min(size.width, size.height);
                tmp_diff = Math.abs(current_rate - rate);
                if (min_diff < 0) {
                    min_diff = tmp_diff;
                    result[0] = size.width;
                    result[1] = size.height;
                }
                if (tmp_diff < min_diff) {
                    min_diff = tmp_diff;
                    result[0] = size.width;
                    result[1] = size.height;
                }
            }
        }
        Log.i(TAG, "getBestPreviewSize width: " + width + " height: " + height + " result: " + Arrays.toString(result));
        return result;
    }

    public static Camera openBackCamera() {
        Camera camera = null;
        try {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK); // attempt to get a Camera instance
        } catch (Throwable e) {
            e.printStackTrace();
            AppToast.toastMsg("打开相机失败");
        }
        return camera;
    }

    public static Camera openFrontCamera() {
        Camera camera = null;
        try {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT); // attempt to get a Camera instance
        } catch (Throwable e) {
            e.printStackTrace();
            AppToast.toastMsg("打开相机失败");
        }
        return camera;
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
