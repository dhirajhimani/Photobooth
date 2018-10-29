package com.photobooth.photobooth;

import java.io.OutputStream;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.photobooth.photobooth.util.Util;

public class CameraNewActivity extends Activity implements PictureCallback {
	private FrameLayout previewContainer;
	private Preview mPreview;
	Camera mCamera;
	int numberOfCameras;
	int cameraCurrentlyLocked;
	

	// The first rear facing camera
	int defaultCameraId;
	
	private ImageView imgCameraMode;
	private ImageView imgFlashMode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Hide the window title.
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		// Create a RelativeLayout container that will hold a SurfaceView,
		// and set it as the content of our activity.		
		setContentView(R.layout.activity_camera_new);
		previewContainer = (FrameLayout) findViewById(R.id.previewContainer);
		mPreview = new Preview(this);
		mPreview.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
		previewContainer.addView(mPreview);
		
		imgCameraMode = (ImageView) findViewById(R.id.imgCameraMode);
		imgFlashMode = (ImageView) findViewById(R.id.imgFlashMode);

		// Find the total number of cameras available
		numberOfCameras = Camera.getNumberOfCameras();

		// Find the ID of the default camera
		CameraInfo cameraInfo = new CameraInfo();
		for (int i = 0; i < numberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
				defaultCameraId = i;
			}
		}
		
		boolean hasFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
		boolean hasFrontCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
		
		Util.LOG(String.format("Flash Available : %s", hasFlash ? "Yes" : "No"));
		Util.LOG(String.format("Front Camera Available : %s", hasFrontCamera ? "Yes" : "No"));
		
		imgCameraMode.setVisibility(hasFrontCamera ? View.VISIBLE : View.INVISIBLE);
		imgFlashMode.setVisibility(hasFlash ? View.VISIBLE : View.INVISIBLE);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Open the default i.e. the first rear facing camera.
		mCamera = Camera.open();
		mCamera.autoFocus(new AutoFocusCallback() {
             public void onAutoFocus(boolean success, Camera camera) {
             }
         });
		cameraCurrentlyLocked = defaultCameraId;
		mPreview.setCamera(mCamera);
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Because the Camera object is a shared resource, it's very
		// important to release it when the activity is paused.
		if (mCamera != null) {
			mPreview.setCamera(null);
			mCamera.release();
			mCamera = null;
		}
	}

	public void cameraModeClick(View v) {
		// Handle item selection
		// check for availability of multiple cameras
		if (numberOfCameras == 1) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Device has only one camera!").setNeutralButton(
					"Close", null);
			AlertDialog alert = builder.create();
			alert.show();
			return;
		}

		// OK, we have multiple cameras.
		// Release this camera -> cameraCurrentlyLocked
		if (mCamera != null) {
			mCamera.stopPreview();
			mPreview.setCamera(null);
			mCamera.release();
			mCamera = null;
		}

		// Acquire the next camera and request Preview to reconfigure
		// parameters.
		mCamera = Camera.open((cameraCurrentlyLocked + 1) % numberOfCameras);
		mCamera.autoFocus(new AutoFocusCallback() {
            public void onAutoFocus(boolean success, Camera camera) {
            }
        });
		cameraCurrentlyLocked = (cameraCurrentlyLocked + 1) % numberOfCameras;
		imgFlashMode.setVisibility(cameraCurrentlyLocked == CameraInfo.CAMERA_FACING_BACK ? View.VISIBLE : View.INVISIBLE);
		mPreview.switchCamera(mCamera);
		

		// Start the preview
		mCamera.startPreview();
	}
	
	public void flashModeClick(View v) {
		Parameters params = mCamera.getParameters();
		String currentFlashMode = params.getFlashMode();
		if(Parameters.FLASH_MODE_AUTO.equals(currentFlashMode)) {
			params.setFlashMode(Parameters.FLASH_MODE_OFF);
			imgFlashMode.setImageResource(R.drawable.flash_off);
		} else if(Parameters.FLASH_MODE_OFF.equals(currentFlashMode)) {
			params.setFlashMode(Parameters.FLASH_MODE_ON);
			imgFlashMode.setImageResource(R.drawable.flash_on);
		//} else if(Parameters.FLASH_MODE_ON.equals(currentFlashMode)) {
		//	params.setFlashMode(Parameters.FLASH_MODE_AUTO);
		//	imgFlashMode.setImageResource(R.drawable.flash_auto);
		} else {
			params.setFlashMode(Parameters.FLASH_MODE_AUTO);
			imgFlashMode.setImageResource(R.drawable.flash_auto);
		}
		
		//mCamera.stopPreview();
		
		mCamera.setParameters(params);
		//mCamera.startPreview();
	}

	public void takePhotoClick(View v) {
		Camera camera = this.mCamera;
		camera.takePicture(null, null, this);
	}

	@Override
	public void onPictureTaken(byte[] picture, Camera camera) {
		try {
			// Populate image metadata

			ContentValues image = new ContentValues();
			// additional picture metadata
			image.put(Media.DISPLAY_NAME, "photobooth" + new Date().getTime() + ".jpg");
			image.put(Media.MIME_TYPE, "image/jpg");
			image.put(Media.TITLE, "PhotoBooth");
			image.put(Media.DESCRIPTION, "PhotoBooth");
			image.put(Media.ORIENTATION, 90);
			
			// store the picture
			Uri uri = getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, image);

			try {
				Bitmap bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.length);
				OutputStream out = getContentResolver().openOutputStream(uri);
				boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out);
				out.close();
				if (!success) {
					setResult(RESULT_CANCELED);
					finish(); // image output failed without any error,
					// silently finish
				} else {
					Intent intent = new Intent();
					intent.setData(uri);
					setResult(RESULT_OK, intent);
					finish();
				}			
			} catch (Exception e) {
				e.printStackTrace();
				setResult(RESULT_CANCELED);
				finish(); // image output failed without any error,
			}

		} catch (Exception e) {
			e.printStackTrace();
			setResult(RESULT_CANCELED);
			finish(); // image output failed without any error,
		}
	}
}