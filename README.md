# camerax_view
```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}


dependencies {
            implementation 'com.github.lisen87:camerax_view:v1.2'
    }


  <com.leeson.cameraxview.CameraXView
        android:id="@+id/cameraXView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />





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
                Log.e("TAG", "onClick: " );
            }
        });
//        cameraXView.setSwitchCameraRes(R.mipmap.ic_launcher);
//        cameraXView.setFeatures(CameraXView.BUTTON_STATE_ONLY_CAPTURE);
//        cameraXView.setTip("单击拍照，...");
//        cameraXView.setLeftIconSrc(R.drawable.ic_camera);
//        cameraXView.setRightIconSrc(R.drawable.ic_camera);
//        cameraXView.setDuration(15*1000);
 //       cameraXView.setMinDuration(1000);
//        cameraXView.setSwitchCameraShow(false);
//        cameraXView.setLensFacing(CameraXView.LENS_FACING_FRONT);
        cameraXView.startCamera();

```