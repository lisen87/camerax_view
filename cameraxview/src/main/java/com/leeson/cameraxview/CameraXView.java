package com.leeson.cameraxview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.common.util.concurrent.ListenableFuture;
import com.leeson.cameraxview.listener.CaptureListener;
import com.leeson.cameraxview.listener.ClickListener;
import com.leeson.cameraxview.listener.TypeListener;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.window.WindowManager;

/**
 * Created by robot on 2021/10/27 16:12.
 *
 * @author robot < robot >
 */

public class CameraXView extends FrameLayout {
    public static final int BUTTON_STATE_ONLY_CAPTURE = 0x101;      //只能拍照
    public static final int BUTTON_STATE_ONLY_RECORDER = 0x102;     //只能录像
    public static final int BUTTON_STATE_BOTH = 0x103;              //两者都可以

    //回调监听
    private ClickListener leftClickListener;
    private ClickListener rightClickListener;

    private Context mContext;
    private ImageView mSwitchCamera;
    private CaptureLayout mCaptureLayout;
    private FoucsView mFoucsView;
    private Preview preview;
    private PreviewView previewView;
    private ImageView image_photo;
    private VideoView videoView;

    private int duration = 10 * 1000;       //录制时间

    private ProcessCameraProvider processCameraProvider;
    private ImageCapture imageCapture;
    private VideoCapture videoCapture;
    private Camera camera;
    private ExecutorService cameraExecutor;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private static final double RATIO_4_3_VALUE = 4.0 / 3.0;
    private static final double  RATIO_16_9_VALUE = 16.0 / 9.0;


    public CameraXView(@NonNull Context context) {
        this(context,null);
    }

    public CameraXView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CameraXView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initView();
        previewView.post(new Runnable() {
            @Override
            public void run() {
                setUpCamera();
            }
        });
    }
    /**
     * 是否有后摄像头
     */
    private boolean hasBackCamera(){
        if(processCameraProvider == null){
            return false;
        }
        try {
            return processCameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA);
        } catch (CameraInfoUnavailableException e) {
            e.printStackTrace();
        }
        return false;
    }
    /**
     * 是否有前摄像头
     */
    private boolean hasFrontCamera(){
        if(processCameraProvider == null){
            return false;
        }
        try {
            return processCameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA);
        } catch (CameraInfoUnavailableException e) {
            e.printStackTrace();
        }
        return false;
    }
    private void setUpCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor();
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(mContext);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    processCameraProvider = cameraProviderFuture.get();
                    // 构建并绑定照相机用例
                    bindCameraUseCases();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(mContext));
    }

    @SuppressLint("RestrictedApi")
    private void bindCameraUseCases() {
        //获取屏幕的分辨率
        WindowManager windowManager = new WindowManager(mContext);
        Rect displayMetrics = windowManager.getCurrentWindowMetrics().getBounds();
        //获取宽高比
        int screenAspectRatio = aspectRatio(displayMetrics.width(), displayMetrics.height());

        int rotation = previewView.getDisplay().getRotation();
        Log.e("rotation", "bindCameraUseCases: "+rotation );
        if(processCameraProvider == null){
            Toast.makeText(mContext,"相机初始化失败",Toast.LENGTH_SHORT).show();
            return;
        }


        ProcessCameraProvider cameraProvider = processCameraProvider;

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        Preview.Builder pBuilder = new Preview.Builder();

        preview = pBuilder
                //设置宽高比
                .setTargetAspectRatio(screenAspectRatio)
                //设置当前屏幕的旋转
                .setTargetRotation(rotation)
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder();

        imageCapture = builder
                //优化捕获速度，可能降低图片质量
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                //设置宽高比
                .setTargetAspectRatio(screenAspectRatio)
                //设置初始的旋转角度
                .setTargetRotation(rotation)
                .build();

//        CamcorderProfile.get(lensFacing).videoBitRate = 16 * 100000;
        videoCapture = new VideoCapture.Builder()
                //设置当前旋转
                .setTargetRotation(rotation)
                //设置宽高比
                .setTargetAspectRatio(screenAspectRatio)
//                分辨率
//                .setTargetResolution(resolution)
                //视频帧率  越高视频体积越大
//                .setVideoFrameRate(20)
                //bit率  越大视频体积越大
                .setBitRate(16 * 100000)
                .build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                Log.e("TAG", "analyze: "+image );
                image.close();
            }
        });

        try {
            //重新绑定之前必须先取消绑定
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle((LifecycleOwner) mContext,
                    cameraSelector,preview, imageCapture,videoCapture);
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private int aspectRatio(int widthPixels, int heightPixels) {
        double previewRatio = (double) Math.max(widthPixels, heightPixels) / (double) Math.min(widthPixels, heightPixels);
        if (Math.abs(previewRatio - RATIO_4_3_VALUE) <= Math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    /**
     *
     * @param duration 毫秒
     */
    public void setDuration(int duration) {
        this.duration = duration;
        mCaptureLayout.setDuration(duration);
    }

    private String getPathTimeName(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        return simpleDateFormat.format(new Date(System.currentTimeMillis()));
    }

    private Uri mediaUri;
    private String mediaPath;
    private int mediaType = 0;
    private boolean isShort;
    @SuppressLint("RestrictedApi")
    private void initView() {
        setWillNotDraw(false);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.camerax_view, this);
        previewView = view.findViewById(R.id.view_finder);
        videoView = view.findViewById(R.id.video_preview);
        image_photo = view.findViewById(R.id.image_photo);
        mSwitchCamera = view.findViewById(R.id.image_switch);
        mCaptureLayout = view.findViewById(R.id.capture_layout);
        mCaptureLayout.setDuration(duration);
        mFoucsView = view.findViewById(R.id.fouce_view);
        //切换摄像头
        mSwitchCamera.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CameraSelector.LENS_FACING_FRONT == lensFacing){
                    lensFacing = CameraSelector.LENS_FACING_BACK;
                }else{
                    lensFacing = CameraSelector.LENS_FACING_FRONT;
                }
                bindCameraUseCases();
            }
        });
        //确认 取消
        mCaptureLayout.setTypeLisenter(new TypeListener() {
            @Override
            public void cancel() {
                mCaptureLayout.resetCaptureLayout();
                mSwitchCamera.setVisibility(VISIBLE);
                image_photo.setVisibility(GONE);
                image_photo.setImageBitmap(null);
                videoView.setVisibility(GONE);
                previewView.setVisibility(VISIBLE);

            }

            @Override
            public void confirm() {
                if (iCamera != null){
                    String thumbnailPath = null;
                    if (mediaType == MEDIA_VIDEO){
                        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(mediaPath, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
                        try {
                            thumbnailPath = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES) + File.separator + getPathTimeName() + ".jpg";
                            FileOutputStream out = new FileOutputStream(thumbnailPath, false);
                            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                                out.flush();
                                out.close();
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }else{
                        thumbnailPath = mediaPath;
                    }
                    iCamera.onTakePic(mediaUri,mediaPath,thumbnailPath,mediaType);
                }
            }
        });
        //拍照 录像

        mCaptureLayout.setCaptureLisenter(new CaptureListener() {
            @Override
            public void takePictures() {
                mSwitchCamera.setVisibility(INVISIBLE);
                mediaType = MEDIA_IMAGE;
                mediaPath = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES) + File.separator + getPathTimeName() + ".jpg";
                ImageCapture.Metadata metadata = new ImageCapture.Metadata();
                metadata.setReversedHorizontal(lensFacing == CameraSelector.LENS_FACING_FRONT);
                ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions
                        .Builder(new File(mediaPath)).setMetadata(metadata).build();
                imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(mContext), new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        mediaUri = outputFileResults.getSavedUri();

                        if (lensFacing == CameraSelector.LENS_FACING_FRONT){
                            //mediaUri 保存的图片，前相机有镜像和宽高问题，使用glide 重新保存一下就正常了。
                            Glide.with(mContext).asBitmap().listener(new RequestListener<Bitmap>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                    return false;
                                }
                                @Override
                                public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Matrix matrix = new Matrix();
                                            Bitmap bitmap = Bitmap.createBitmap(resource, 0, 0, resource.getWidth(), resource.getHeight(), matrix, true);
                                            try {
                                                FileOutputStream out = new FileOutputStream(mediaPath, false);
                                                if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                                                    out.flush();
                                                    out.close();
                                                }
                                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mCaptureLayout.startTypeBtnAnimator();
                                                        previewView.setVisibility(GONE);
                                                    }
                                                });
                                            }catch (Exception e){
                                                e.printStackTrace();
                                            }
                                        }
                                    }).start();
                                    return false;
                                }
                            }).load(mediaUri).centerCrop().into(image_photo);
                            image_photo.setVisibility(VISIBLE);
                        }else{
                            mCaptureLayout.startTypeBtnAnimator();
                            image_photo.setVisibility(VISIBLE);
                            Glide.with(mContext).load(mediaUri).centerCrop().into(image_photo);
                            previewView.setVisibility(GONE);
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                    }
                });
            }

            @SuppressLint("MissingPermission")
            @Override
            public void recordStart() {
                isShort = false;
                mSwitchCamera.setVisibility(INVISIBLE);
                if (iCamera != null){
                    iCamera.onRecordStart();
                }
                mediaType = MEDIA_VIDEO;
                mediaPath = getContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES) + File.separator + getPathTimeName() + ".mp4";
                VideoCapture.Metadata metadata = new VideoCapture.Metadata();
                VideoCapture.OutputFileOptions build = new VideoCapture.OutputFileOptions.Builder(new File(mediaPath)).setMetadata(metadata).build();
                videoCapture.startRecording(build, ContextCompat.getMainExecutor(mContext), new VideoCapture.OnVideoSavedCallback() {
                    @Override
                    public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                        mediaUri = outputFileResults.getSavedUri();
                        if (isShort){
                            new File(mediaPath).delete();
                        }else{
                            videoView.setVideoURI(mediaUri);
                            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mediaPlayer) {
                                    mediaPlayer.setLooping(true);
                                    previewView.setVisibility(GONE);
                                    videoView.start();
                                }
                            });
                            videoView.setVisibility(VISIBLE);
                        }
                    }

                    @Override
                    public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                        new File(mediaPath).delete();
                    }
                });
            }

            @Override
            public void recordShort(final long time) {
                mSwitchCamera.setVisibility(VISIBLE);
                if (iCamera != null){
                    iCamera.onRecordShort(time);
                }
//                mCaptureLayout.setTextWithAnimation("录制时间过短");

                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCaptureLayout.resetCaptureLayout();
                        videoCapture.stopRecording();
                        isShort = true;
                    }
                }, 1500 - time);
            }

            @Override
            public void recordEnd(long time) {
                if (iCamera != null){
                    iCamera.onRecordEnd(time);
                }
                mCaptureLayout.startTypeBtnAnimator();
                videoCapture.stopRecording();
            }

            @Override
            public void recordZoom(float zoom) {
                if (iCamera != null){
                    iCamera.onRecordZoom(zoom);
                }
                if (zoom > 0){
                    int scaleRate = (int) (zoom / 40);
                    camera.getCameraControl().setZoomRatio(scaleRate);
                }
            }

            @Override
            public void recordError() {
                if (iCamera != null){
                    iCamera.onRecordError();
                }
            }
        });

        mCaptureLayout.setLeftClickListener(new ClickListener() {
            @Override
            public void onClick() {
                if (leftClickListener != null) {
                    leftClickListener.onClick();
                }
            }
        });
        mCaptureLayout.setRightClickListener(new ClickListener() {
            @Override
            public void onClick() {
                if (rightClickListener != null) {
                    rightClickListener.onClick();
                }
            }
        });
    }
    @Override
    protected void onDetachedFromWindow() {
        cameraExecutor.shutdown();
        super.onDetachedFromWindow();
    }

    public void setTip(String tip){
        mCaptureLayout.setTip(tip);
    }
    public void setWithAnimationTip(String tip){
        mCaptureLayout.setTextWithAnimation(tip);
    }

    public void setRightIconSrc(@DrawableRes int res) {
        mCaptureLayout.setRightIconSrc(res);
    }
    public void setLeftIconSrc(@DrawableRes  int res) {
        mCaptureLayout.setLeftIconSrc(res);
    }
    /**
     *
     * @param state BUTTON_STATE_BOTH
     *              BUTTON_STATE_ONLY_CAPTURE
     *              BUTTON_STATE_ONLY_RECORDER
     */
    public void setFeatures(int state) {
        this.mCaptureLayout.setButtonFeatures(state);
    }
    public void setSwitchCameraRes(@DrawableRes int res){
        mSwitchCamera.setImageResource(res);
    }
    public static int MEDIA_IMAGE = 0;
    public static int MEDIA_VIDEO = 1;
    public interface ICamera{
        /**
         *
         * @param uri
         * @param path
         * @param thumbnailPath MEDIA_VIDEO 时 thumbnailPath 视频缩略图
         * @param mediaType 0 MEDIA_IMAGE 1 MEDIA_VIDEO
         */
        void onTakePic(Uri uri,String path,String thumbnailPath,int mediaType);

        void onRecordShort(long time);

        void onRecordStart();

        void onRecordEnd(long time);

        void onRecordZoom(float zoom);

        void onRecordError();

    }
    private ICamera iCamera;

    public void setiCamera(ICamera iCamera) {
        this.iCamera = iCamera;
    }

    public void setLeftClickListener(ClickListener leftClickListener) {
        this.leftClickListener = leftClickListener;
    }

    public void setRightClickListener(ClickListener rightClickListener) {
        this.rightClickListener = rightClickListener;
    }
}
