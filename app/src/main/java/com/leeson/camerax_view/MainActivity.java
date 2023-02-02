package com.leeson.camerax_view;

import android.Manifest;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.leeson.cameraxview.CameraXView;
import com.leeson.cameraxview.listener.ClickListener;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                Iterator<Map.Entry<String, Boolean>> iterator = result.entrySet().iterator();
                boolean permissionGranted = true;
                while (iterator.hasNext()) {
                    if (iterator.hasNext()) {
                        Map.Entry<String, Boolean> next = iterator.next();
                        if (next.getValue() != null) {
                            if (Objects.requireNonNull(next.getValue()).equals(false)) {
                                permissionGranted = false;
                            }
                        }
                    }
                }
                if (!permissionGranted) {
                    Toast.makeText(MainActivity.this, "获取权限失败", Toast.LENGTH_LONG).show();
                }else{

                }
            }
        }).launch(new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO});

        ImageView imageView = findViewById(R.id.iv);

        CameraXView cameraXView = findViewById(R.id.cameraXView);
        cameraXView.setiCamera(new CameraXView.ICamera() {
            @Override
            public void onTakePic(Uri uri, String path, String thumbnailPath, int mediaType) {

                Log.e("TAG", "onTakePic: uri "+uri);
                Log.e("TAG", "onTakePic: path "+path);
                Log.e("TAG", "onTakePic: thumbnailPath "+thumbnailPath);
                Log.e("TAG", "onTakePic: mediaType "+mediaType);
                Glide.with(MainActivity.this).load(thumbnailPath).into(imageView);
            }

            @Override
            public void onRecordShort(long time) {
                cameraXView.setWithAnimationTip("录制时间过短");
                Log.e("TAG", "onRecordShort: time "+time);
            }

            @Override
            public void onRecordStart() {
                Log.e("TAG", "onRecordStart: ");
            }

            @Override
            public void onRecordEnd(long time) {
                Log.e("TAG", "onRecordEnd: time "+time);
            }

            @Override
            public void onRecordZoom(float zoom) {
            }

            @Override
            public void onRecordError() {
                Log.e("TAG", "onRecordError:");
            }
        });
        cameraXView.setLeftClickListener(new ClickListener() {
            @Override
            public void onClick() {
                Log.e("TAG", "onClick: 关闭" );
                finish();
            }
        });
        cameraXView.setRightClickListener(new ClickListener() {
            @Override
            public void onClick() {
                Log.e("TAG", "onClick: 相册" );
            }
        });
//        cameraXView.setSwitchCameraRes(R.mipmap.ic_launcher);
//        cameraXView.setFeatures(CameraXView.BUTTON_STATE_ONLY_CAPTURE);
//        cameraXView.setTip("单击拍照，...");
//        cameraXView.setLeftIconSrc(R.drawable.ic_camera);
//        cameraXView.setRightIconSrc(R.drawable.ic_camera);
        cameraXView.setDuration(15*1000);
        cameraXView.setMinDuration(1000);
//        cameraXView.setSwitchCameraShow(false);
//        cameraXView.setLensFacing(CameraXView.LENS_FACING_FRONT);
        cameraXView.startCamera();
    }
}