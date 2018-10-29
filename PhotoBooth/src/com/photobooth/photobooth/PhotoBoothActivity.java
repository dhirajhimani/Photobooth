package com.photobooth.photobooth;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.Gallery;

import com.photobooth.photobooth.util.Util;
import com.photobooth.view.PhotoBoothView;

public class PhotoBoothActivity extends Activity implements OnItemClickListener
{
	
//	public static final String EXTRA_PHOTO_URI = "photo_uri";
	public static final String EXTRA_CHANGE_ACTION = "change_action";
	public static final int CHANGE_ACTION_TAKE_PHOTO = 1001;
	public static final int CHANGE_ACTION_PHOTO_LIBRARY = 1002;
	
	private Uri capturedImageUri;	
	private int mChangeAction = CHANGE_ACTION_TAKE_PHOTO;
	
	//private ImageView imgPhoto;
	private FrameLayout vRoot;
	
	private Gallery galleryObjects;
	private ObjectsAdapterNew mGalleryObjectsAdapter;
	
	private PhotoBoothView mPhotoView;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_photo_booth);
		
		vRoot = (FrameLayout) findViewById(R.id.vRoot);
		//imgPhoto = (ImageView) findViewById(R.id.imgPhoto);
		galleryObjects = (Gallery) findViewById(R.id.galleryObjects);
		
		mPhotoView = new PhotoBoothView(this);
		vRoot.addView(mPhotoView, 0);
		
		Uri uri = getIntent().getData();
		//imgPhoto.setImageURI(uri);
		//mPhotoView.setMainImage(uriToBitmap(uri));
		hideAllButtons();
		findViewById(R.id.btnDone).setVisibility(View.VISIBLE);
		mPhotoView.addImage(uriToBitmap(uri), PhotoBoothView.TYPE_MAIN_IMAGE);
		
		mChangeAction = getIntent().getIntExtra(EXTRA_CHANGE_ACTION, CHANGE_ACTION_TAKE_PHOTO);
		
		// setup object gallery
		/*ArrayList<Drawable> data = new ArrayList<Drawable>();
		data.add(getResources().getDrawable(R.drawable.banner01));
		data.add(getResources().getDrawable(R.drawable.beaney01));
		data.add(getResources().getDrawable(R.drawable.cap01));
		data.add(getResources().getDrawable(R.drawable.helmet01));
		data.add(getResources().getDrawable(R.drawable.jersey01));
		data.add(getResources().getDrawable(R.drawable.shirt01));
		data.add(getResources().getDrawable(R.drawable.trophy01));
		data.add(getResources().getDrawable(R.drawable.trophy02));*/
		
		ArrayList<Integer> data = new ArrayList<Integer>();
		/*data.add(R.drawable.banner01);
		data.add(R.drawable.beaney01);
		data.add(R.drawable.cap01);
		data.add(R.drawable.helmet01);
		data.add(R.drawable.jersey01);
		data.add(R.drawable.shirt01);
		data.add(R.drawable.trophy01);
		data.add(R.drawable.trophy02);*/
		
		mGalleryObjectsAdapter = new ObjectsAdapterNew(this, data);
		galleryObjects.setAdapter(mGalleryObjectsAdapter);
		galleryObjects.setOnItemClickListener(this);
		galleryObjects.setVisibility(View.GONE);
		
		
		Util.alertbox(this, getString(R.string.setup_photo));
	}
	
	/*
	 * Change button click
	 */
	public void changeClick(View v) 
	{
		if(mChangeAction == CHANGE_ACTION_TAKE_PHOTO) 
		{
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra("return-data", true);
			File file = new File(Environment.getExternalStorageDirectory(), new Date().getTime() + ".jpg");
			capturedImageUri = Uri.fromFile(file);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
			startActivityForResult(intent, CHANGE_ACTION_TAKE_PHOTO);
		} 
		else if(mChangeAction == CHANGE_ACTION_PHOTO_LIBRARY)
		{
			Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(i, CHANGE_ACTION_PHOTO_LIBRARY);
		}
	}
	
	/*
	 * Lower button click
	 */
	public void lowerClick(View v) 
	{
		mPhotoView.moveSelectedObjectToLower();
	}
	
	/*
	 * Upper button click
	 */
	public void upperClick(View v) 
	{
		mPhotoView.moveSelectedObjectToUpper();
	}
	
	/*
	 * Objects button click
	 */
	public void objectsClick(View v) 
	{
		if(galleryObjects.getVisibility() == View.VISIBLE)
			galleryObjects.setVisibility(View.GONE);
		else
			galleryObjects.setVisibility(View.VISIBLE);
	}
	
	/*
	 * Remove button click
	 */
	public void removeClick(View v) {
		mPhotoView.removeCurrentImage();
	}
	
	/*
	 * Done button click
	 */
	public void doneClick(View v) {
		showAllButtons();
		v.setVisibility(View.GONE);
		mPhotoView.getMainImage().setLocked(true);
		mPhotoView.getMainImage().setSelected(false);
	}
	
	/*
	 * Save button click
	 */
	public void saveClick(View v) {
		mPhotoView.setDrawingCacheEnabled(true);
		mPhotoView.setDrawingCacheQuality(ViewGroup.DRAWING_CACHE_QUALITY_HIGH);
		Bitmap photo = mPhotoView.getDrawingCache();			
		
		try {
			if(Util.saveBitmapToSDCard(getContentResolver(), photo) != null) {
				Util.alertbox(this, getString(R.string.photo_saved));
			} else {
				Util.alertbox(this, getString(R.string.photo_not_saved));
			}
		} catch (IOException e) {
			Util.alertbox(this, getString(R.string.photo_not_saved));
			e.printStackTrace();
		}
		
		mPhotoView.setDrawingCacheEnabled(false);
		photo = null;
	}
	
	private void showAllButtons() {
		findViewById(R.id.btnLower).setVisibility(View.VISIBLE);
		findViewById(R.id.btnUpper).setVisibility(View.VISIBLE);
		findViewById(R.id.btnChange).setVisibility(View.VISIBLE);
		findViewById(R.id.btnDone).setVisibility(View.VISIBLE);
		findViewById(R.id.btnObjects).setVisibility(View.VISIBLE);
		findViewById(R.id.btnRemove).setVisibility(View.VISIBLE);
		findViewById(R.id.btnSave).setVisibility(View.VISIBLE);
	}
	
	private void hideAllButtons() {
		findViewById(R.id.btnLower).setVisibility(View.GONE);
		findViewById(R.id.btnUpper).setVisibility(View.GONE);
		findViewById(R.id.btnChange).setVisibility(View.GONE);
		findViewById(R.id.btnDone).setVisibility(View.GONE);
		findViewById(R.id.btnObjects).setVisibility(View.GONE);
		findViewById(R.id.btnRemove).setVisibility(View.GONE);
		findViewById(R.id.btnSave).setVisibility(View.GONE);
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		switch (requestCode) 
		{
		case CHANGE_ACTION_TAKE_PHOTO:
			if (resultCode == RESULT_OK) 
			{					
				mPhotoView.addImage(uriToBitmap(capturedImageUri), PhotoBoothView.TYPE_MAIN_IMAGE);
				mPhotoView.getMainImage().setLocked(false);
				mPhotoView.getMainImage().setSelected(true);
				hideAllButtons();
				findViewById(R.id.btnDone).setVisibility(View.VISIBLE);
				//mPhotoView.setMainImage(uriToBitmap(capturedImageUri));
				//imgPhoto.setImageURI(capturedImageUri);
			}
			break;

		case CHANGE_ACTION_PHOTO_LIBRARY:
			if (resultCode == RESULT_OK) 
			{
				Uri u = data.getData();
				mPhotoView.addImage(uriToBitmap(u), PhotoBoothView.TYPE_MAIN_IMAGE);
				mPhotoView.getMainImage().setLocked(false);
				mPhotoView.getMainImage().setSelected(true);
				hideAllButtons();
				findViewById(R.id.btnDone).setVisibility(View.VISIBLE);
				//mPhotoView.setMainImage(uriToBitmap(u));
				//imgPhoto.setImageURI(u);
			}
			break;

		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	
	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) 
	{
		if(parent == galleryObjects)
		{
			/*ImageView imgObject = new ImageView(this);
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
			imgObject.setLayoutParams(params);
			imgObject.setImageDrawable(mGalleryObjectsAdapter.getItem(position));
			
			vRoot.addView(imgObject);*/
			Drawable d = getResources().getDrawable(mGalleryObjectsAdapter.getItem(position));
			mPhotoView.addImage(d);
			galleryObjects.setVisibility(View.GONE);
		}
	}	
	
	private Bitmap uriToBitmap(Uri uri) {
		Bitmap bitmap = null;
		try {
			bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);			
			/*Matrix matrix = new Matrix();
			matrix.postRotate(90);			
			bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);*/
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bitmap;
	}
}
