package com.photobooth.photobooth;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

public class MainActivity extends Activity {
	private static final int TAKE_PHOTO_REQUEST_CODE = 1001;
	private static final int PHOTO_LIBRARY_REQUEST_CODE = 1002;
	//private Uri capturedImageUri;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
	}

	public void takePhotoClick(View v) {
		/*Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra("return-data", true);
		File file = new File(Environment.getExternalStorageDirectory(), new Date().getTime() + ".jpg");
		capturedImageUri = Uri.fromFile(file);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
		startActivityForResult(intent, TAKE_PHOTO_REQUEST_CODE);*/
		
		Intent intent = new Intent(this, CameraNewActivity.class);
		startActivityForResult(intent, TAKE_PHOTO_REQUEST_CODE);
	}

	public void photoLibraryClick(View v) {
		Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(i, PHOTO_LIBRARY_REQUEST_CODE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case TAKE_PHOTO_REQUEST_CODE:
			if (resultCode == RESULT_OK) 
			{
				Intent photoBoothIntent = new Intent(this, PhotoBoothNewActivity.class);
				//photoBoothIntent.setData(capturedImageUri);
				photoBoothIntent.setData(data.getData());
				photoBoothIntent.putExtra(PhotoBoothNewActivity.EXTRA_CHANGE_ACTION, PhotoBoothNewActivity.CHANGE_ACTION_TAKE_PHOTO);
				startActivity(photoBoothIntent);
			}
			break;

		case PHOTO_LIBRARY_REQUEST_CODE:
			if (resultCode == RESULT_OK) {
				Uri u = data.getData();
				Intent photoBoothIntent = new Intent(this, PhotoBoothNewActivity.class);
				photoBoothIntent.setData(u);
				photoBoothIntent.putExtra(PhotoBoothNewActivity.EXTRA_CHANGE_ACTION, PhotoBoothNewActivity.CHANGE_ACTION_PHOTO_LIBRARY);
				startActivity(photoBoothIntent);
			}
			break;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}
}