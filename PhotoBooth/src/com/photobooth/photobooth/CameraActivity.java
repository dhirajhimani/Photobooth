package com.photobooth.photobooth;

import java.io.OutputStream;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.photobooth.photobooth.util.Util;

public class CameraActivity extends Activity implements PictureCallback {
	private SurfaceView preview = null;
	private SurfaceHolder previewHolder = null;
	SurfaceHolder.Callback surfaceCallback;
	private Camera camera = null;
	private ImageView imgCameraMode;
	private ImageView imgFlashMode;

	private boolean inPreview = false;
	private boolean cameraConfigured = false;
	
	private boolean hasFlash = false;
	private boolean hasFrontCamera = false;
	
	private int currentCamId = Camera.CameraInfo.CAMERA_FACING_BACK;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_camera);

		preview = (SurfaceView) findViewById(R.id.surfaceView);
		previewHolder = preview.getHolder();
		previewHolder.addCallback(surfaceCallback);
		previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		imgCameraMode = (ImageView) findViewById(R.id.imgCameraMode);
		imgFlashMode = (ImageView) findViewById(R.id.imgFlashMode);
		
		hasFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
		hasFrontCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
		
		Util.LOG(String.format("Flash Available : %s", hasFlash ? "Yes" : "No"));
		Util.LOG(String.format("Front Camera Available : %s", hasFrontCamera ? "Yes" : "No"));
		
		imgCameraMode.setVisibility(hasFrontCamera ? View.VISIBLE : View.INVISIBLE);
		imgFlashMode.setVisibility(hasFlash ? View.VISIBLE : View.INVISIBLE);
		
//		frontFaceCamId = findFrontFacingCamera();
//		backFaceCamId = findBackFacingCamera();		
	}

	@Override
	public void onResume() {
		super.onResume();
		camera = Camera.open();
		currentCamId = CameraInfo.CAMERA_FACING_BACK;
		startPreview();
	}

	@Override
	public void onPause() {
		/*if (inPreview) {
			camera.stopPreview();
		}
		camera.release();		
		camera = null;
		inPreview = false;
		
		camera.stopPreview();
        camera.setPreviewCallback(null);
        camera.release();
        camera = null;
        previewHolder.removeCallback(surfaceCallback);
        previewHolder = null;*/
		
		stopCamera();

		super.onPause();
	}
	
	private void initPreview(int width, int height) {
		if (camera != null && previewHolder.getSurface() != null) {
			try {
				camera.setPreviewDisplay(previewHolder);
			} catch (Throwable t) {
				Log.e("PreviewDemo-surfaceCallback","Exception in setPreviewDisplay()", t);
				Toast.makeText(CameraActivity.this, t.getMessage(),Toast.LENGTH_LONG).show();
			}

			if (!cameraConfigured) {
				Camera.Parameters parameters = camera.getParameters();
				Camera.Size size = getBestPreviewSize(width, height, parameters);				
				
				if (size != null) {
					parameters.setPreviewSize(size.width, size.height);
					camera.setParameters(parameters);
					camera.setDisplayOrientation(90);
					cameraConfigured = true;
				}
			}
		}
	}

	private void startPreview() {
		if (cameraConfigured && camera != null) {
			camera.startPreview();
			inPreview = true;
		}
	}
	
	private void stopCamera(){
	    System.out.println("stopCamera method");
	    if (camera != null){
	    	if(inPreview)
	    		camera.stopPreview();
	        camera.release();
	        camera = null;	        
	        previewHolder.removeCallback(surfaceCallback);
	        //cameraConfigured = false;
	        inPreview = false;
	    }
	}

	public void takePhotoClick(View v) {
		Camera camera = this.camera;
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

	private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {		
		Camera.Size result = null;
		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			if (size.width <= width && size.height <= height) {
				if (result == null) {
					result = size;
				} else {
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;

					if (newArea > resultArea) {
						result = size;
					}
				}
			}
		}
		return (result);
	}

	
	
	public void flashModeClick(View v) {
		Parameters params = camera.getParameters();
		String currentFlashMode = params.getFlashMode();
		if(Parameters.FLASH_MODE_AUTO.equals(currentFlashMode))
			params.setFlashMode(Parameters.FLASH_MODE_OFF);
		else if(Parameters.FLASH_MODE_OFF.equals(currentFlashMode))
			params.setFlashMode(Parameters.FLASH_MODE_ON);
		else if(Parameters.FLASH_MODE_ON.equals(currentFlashMode))
			params.setFlashMode(Parameters.FLASH_MODE_AUTO);
		else
			params.setFlashMode(Parameters.FLASH_MODE_AUTO);
	}
	
	@SuppressLint("NewApi")
	public void cameraModeClick(View v) {
		if(!hasFrontCamera)
			return;
		
		if (Camera.getNumberOfCameras() > 1 && currentCamId < Camera.getNumberOfCameras() - 1) {
            startCamera(currentCamId + 1);
        } else {
            startCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
	}
	
	private void startCamera(int cameraId) {
	    if (camera != null) {
	        stopCamera();
	    }       
	    previewHolder = preview.getHolder();
	    surfaceCallback = new SurfaceHolder.Callback() {

	        @Override
	        public void surfaceDestroyed(SurfaceHolder holder) {
	        }

	        @Override
	        public void surfaceCreated(SurfaceHolder holder) {	           
	        }

	        @Override
	        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	        	initPreview(width, height);
				startPreview();
	        }
	    };
	    previewHolder.addCallback(surfaceCallback);
	    
	    currentCamId = cameraId;	    
	}
		
//	SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
//		public void surfaceCreated(SurfaceHolder holder) {
//			// no-op -- wait until surfaceChanged()
//		}
//
//		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//			initPreview(width, height);
//			startPreview();
//		}
//
//		public void surfaceDestroyed(SurfaceHolder holder) {
//			// no-op
//		}
//	};
}