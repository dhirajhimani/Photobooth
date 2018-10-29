package com.photobooth.photobooth;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.http.AccessToken;
import twitter4j.http.OAuthAuthorization;
import twitter4j.util.ImageUpload;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.ImageView;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.photobooth.photobooth.facebook.BaseRequestListener;
import com.photobooth.photobooth.facebook.SessionEvents;
import com.photobooth.photobooth.facebook.SessionEvents.AuthListener;
import com.photobooth.photobooth.facebook.SessionEvents.LogoutListener;
import com.photobooth.photobooth.facebook.SessionStore;
import com.photobooth.photobooth.util.Constant;
import com.photobooth.photobooth.util.Util;
import com.photobooth.view.PhotoBoothView;
import com.photobooth.view.PhotoBoothView.Img;
import com.twitter.android.TwitterApp;
import com.twitter.android.TwitterApp.TwDialogListener;
import com.twitter.android.TwitterSession;

public class PhotoBoothNewActivity extends Activity {
	
	public static final String EXTRA_CHANGE_ACTION = "change_action";
	public static final int CHANGE_ACTION_TAKE_PHOTO = 1001;
	public static final int CHANGE_ACTION_PHOTO_LIBRARY = 1002;

	private int mChangeAction = CHANGE_ACTION_TAKE_PHOTO;

	// private ImageView imgPhoto;
	private FrameLayout vRoot;

	private PhotoBoothView mPhotoView;
	
	// footer images (operations)
	private ImageView imgScale;
	private ImageView imgHelmet;
	private ImageView imgChange;
	private ImageView imgCancel;
	private ImageView imgDone;
	private ImageView imgDelete;
	private ImageView imgRaise;
	private ImageView imgLower;
	private ImageView imgNew;
	private ImageView imgEdit;
	private ImageView imgShare;
	
	// object picker view
	private View vObjectPicker;
	private Gallery galleryObjects;
	private ObjectsAdapterNewAssets mGalleryObjectsAccessoriesAdapter;
	private ObjectsAdapterNewAssets mGalleryObjectsJerseyAdapter;
	private ObjectsAdapterNewAssets mGalleryObjectsHatAdapter;
	private ObjectsAdapterNewAssets mGalleryObjectsHelmetAdapter;
	private ImageView imgJerseyTab;	//extension OP => Object Picker
	private ImageView imgHelmetTab;
	private ImageView imgHatsTab;
	private ImageView imgAccessoryTab;
	private ImageView imgArrowLeft;
	private ImageView imgArrowRight;
	
	// share view
	private View vShare;
	private ImageView imgInstagram;
	private ImageView imgTwitter;
	private ImageView imgFacebook;
	private ImageView imgEmail;
	private ImageView imgSave;
	
	// facebook
	private Facebook mFacebook;
	private AsyncFacebookRunner mAsyncRunner;
	private ProgressDialog pd;
	
	// twitter
	private TwitterApp mTwitter;
	private String mTwitterPhotoPath;
	private Uri mTwitterPhotoURI;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_photo_booth_new);

		// find views
		findViews();
		
		Uri uri = getIntent().getData();
		initMainImage(uri);
		mChangeAction = getIntent().getIntExtra(EXTRA_CHANGE_ACTION, CHANGE_ACTION_TAKE_PHOTO);
				
		galleryObjects.setAdapter(getJerseyAdapter());
		galleryObjects.setOnItemClickListener(galleryObjectItemClickListener);
		galleryObjects.setOnItemSelectedListener(galleryObjectItemSelectListener);
		
		// facebook
		mFacebook = new Facebook(Constant.FACEBOOK_APP_ID);
       	mAsyncRunner = new AsyncFacebookRunner(mFacebook);
       	pd = new ProgressDialog(this);
       	pd.setCancelable(false);
       	//imgFacebook.init(this, mFacebook);
       	//SessionStore.restore(mFacebook, this);
		SessionEvents.addAuthListener(new SampleAuthListener());
		SessionEvents.addLogoutListener(new SampleLogoutListener());
		
		// twitter
		mTwitter = new TwitterApp(this, Constant.TWITTER_CONSUMER_KEY, Constant.TWITTER_CONSUMER_SECRET);		
		mTwitter.setListener(mTwLoginDialogListener);
	}
	
	private void findViews() {
		vRoot = (FrameLayout) findViewById(R.id.vRoot);

		mPhotoView = new PhotoBoothView(this);
		vRoot.addView(mPhotoView, 0);
		
		// footer images (operations)
		imgHelmet = (ImageView) findViewById(R.id.imgHelmet);
		imgScale = (ImageView) findViewById(R.id.imgScale);
		imgChange = (ImageView) findViewById(R.id.imgChange);
		imgDone = (ImageView) findViewById(R.id.imgDone);
		imgDelete = (ImageView) findViewById(R.id.imgDelete);
		imgRaise = (ImageView) findViewById(R.id.imgRaise);
		imgLower = (ImageView) findViewById(R.id.imgLower);
		imgNew = (ImageView) findViewById(R.id.imgNew);
		imgEdit = (ImageView) findViewById(R.id.imgEdit);
		imgShare = (ImageView) findViewById(R.id.imgShare);
		imgCancel = (ImageView) findViewById(R.id.imgCancel);

		// object picker view
		vObjectPicker = findViewById(R.id.vObjectsPicker);
		vObjectPicker.setVisibility(View.GONE);
		galleryObjects = (Gallery) findViewById(R.id.galleryObjects);
		imgJerseyTab = (ImageView) findViewById(R.id.imgJerseyOP);
		imgHelmetTab = (ImageView) findViewById(R.id.imgHelmetOP);
		imgHatsTab = (ImageView) findViewById(R.id.imgHatsOP);
		imgAccessoryTab = (ImageView) findViewById(R.id.imgAccessorysOP);
		imgArrowLeft = (ImageView) findViewById(R.id.imgArrowLeft);
		imgArrowRight = (ImageView) findViewById(R.id.imgArrowRight);
		
		// share view
		vShare = findViewById(R.id.vShare);
		vShare.setVisibility(View.GONE);
		imgInstagram = (ImageView) findViewById(R.id.imgInstagram);
		imgTwitter = (ImageView) findViewById(R.id.imgTwitter);
		imgFacebook = (ImageView) findViewById(R.id.imgFacebook);
		imgEmail = (ImageView) findViewById(R.id.imgEMail);
		imgSave = (ImageView) findViewById(R.id.imgSave);
	}
	
	@Override
	protected void onDestroy() {
		mPhotoView.unloadImages();
		super.onDestroy();
	}
	
	/**
	 * Create Accessories adapter
	 * @return
	 */
	private ObjectsAdapterNewAssets getAccessoriesAdapter() {
		if(mGalleryObjectsAccessoriesAdapter == null) {
			ArrayList<String> data = new ArrayList<String>();
			try {
				String[] fileList = getAssets().list(Constant.ASSETS_ACCESSORIES_FOLDER);
				for(String fileName : fileList) {
					data.add(Constant.ASSETS_ACCESSORIES_FOLDER + "/" + fileName);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			mGalleryObjectsAccessoriesAdapter = new ObjectsAdapterNewAssets(this, data);
		} 
		return mGalleryObjectsAccessoriesAdapter;
	}
	
	/**
	 * Create hats adapter
	 * @return
	 */
	private ObjectsAdapterNewAssets getHatAdapter() {
		if(mGalleryObjectsHatAdapter == null) {
			ArrayList<String> data = new ArrayList<String>();
			try {
				String[] fileList = getAssets().list(Constant.ASSETS_HAT_FOLDER);
				for(String fileName : fileList) {
					data.add(Constant.ASSETS_HAT_FOLDER + "/" + fileName);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			mGalleryObjectsHatAdapter = new ObjectsAdapterNewAssets(this, data);
		} 
		return mGalleryObjectsHatAdapter;
	}
	
	/**
	 * Create helmet adapter
	 * @return
	 */
	private ObjectsAdapterNewAssets getHelmetAdapter() {
		if(mGalleryObjectsHelmetAdapter == null) {
			ArrayList<String> data = new ArrayList<String>();
			try {
				String[] fileList = getAssets().list(Constant.ASSETS_HELMET_FOLDER);
				for(String fileName : fileList) {
					data.add(Constant.ASSETS_HELMET_FOLDER + "/" + fileName);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			mGalleryObjectsHelmetAdapter = new ObjectsAdapterNewAssets(this, data);
		} 
		return mGalleryObjectsHelmetAdapter;
	}
	
	private ObjectsAdapterNewAssets getJerseyAdapter() {
		if(mGalleryObjectsJerseyAdapter == null) {
			ArrayList<String> data = new ArrayList<String>();
			try {
				String[] fileList = getAssets().list(Constant.ASSETS_JERSEY_FOLDER);
				for(String fileName : fileList) {
					data.add(Constant.ASSETS_JERSEY_FOLDER + "/" + fileName);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			mGalleryObjectsJerseyAdapter = new ObjectsAdapterNewAssets(this, data);
		} 
		return mGalleryObjectsJerseyAdapter;
	}
	
	public int getOrientation(Context context, Uri photoUri) {
	    /* it's on the external media. */
	    Cursor cursor = context.getContentResolver().query(photoUri,
	            new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);

	    if (cursor.getCount() != 1) {
	        return -1;
	    }

	    cursor.moveToFirst();
	    int orientation = cursor.getInt(0);
	    cursor.close();
	    return orientation;
	}
	int MAX_IMAGE_DIMENSION = 2000;
	public Bitmap getCorrectlyOrientedImage(Context context, Uri photoUri) throws IOException {
	    /*InputStream is = context.getContentResolver().openInputStream(photoUri);
	    BitmapFactory.Options dbo = new BitmapFactory.Options();
	    dbo.inJustDecodeBounds = true;
	    BitmapFactory.decodeStream(is, null, dbo);
	    is.close();

	    int rotatedWidth, rotatedHeight;
	    int orientation = getOrientation(context, photoUri);

	    if (orientation == 90 || orientation == 270) {
	        rotatedWidth = dbo.outHeight;
	        rotatedHeight = dbo.outWidth;
	    } else {
	        rotatedWidth = dbo.outWidth;
	        rotatedHeight = dbo.outHeight;
	    }

	    Bitmap srcBitmap;
	    is = context.getContentResolver().openInputStream(photoUri);
	    if (rotatedWidth > MAX_IMAGE_DIMENSION || rotatedHeight > MAX_IMAGE_DIMENSION) {
	        float widthRatio = ((float) rotatedWidth) / ((float) MAX_IMAGE_DIMENSION);
	        float heightRatio = ((float) rotatedHeight) / ((float) MAX_IMAGE_DIMENSION);
	        float maxRatio = Math.max(widthRatio, heightRatio);

	        // Create the bitmap from file
	        BitmapFactory.Options options = new BitmapFactory.Options();
	        options.inSampleSize = (int) maxRatio;
	        srcBitmap = BitmapFactory.decodeStream(is, null, options);
	    } else {
	        srcBitmap = BitmapFactory.decodeStream(is);
	    }
	    is.close();*/

	    /*
	     * if the orientation is not 0 (or -1, which means we don't know), we
	     * have to do a rotation.
	     */
		int orientation = getOrientation(context, photoUri);
		Bitmap srcBitmap = uriToBitmap(photoUri);
	    if (orientation > 0) {
	        Matrix matrix = new Matrix();
	        matrix.postRotate(orientation);

	        srcBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(),
	                srcBitmap.getHeight(), matrix, true);
	    }

	    return srcBitmap;
	}
	
	private void initMainImage(Uri uri) {
		Bitmap bitmap = null;
		try {
			bitmap = getCorrectlyOrientedImage(this, uri);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//mPhotoView.addImage(uriToBitmap(uri), PhotoBoothView.TYPE_MAIN_IMAGE);
		mPhotoView.addImage(bitmap, PhotoBoothView.TYPE_MAIN_IMAGE);
		hideAllButtons();
		showViews(imgScale, imgChange);
		Img mainImg = mPhotoView.getMainImage();
		mainImg.setLocked(true);
		mainImg.setSelected(false);
	}

	/**
	 * Hide all footer buttons except Helmet
	 */
	private void hideAllButtons() {
		hideViews(imgScale, imgChange, imgDone, 
				imgDelete, imgRaise, imgLower, 
				imgNew, imgEdit, imgShare, imgCancel);		
	}
	
	/**
	 * Show views
	 * @param views
	 */
	private void showViews(View... views) {
		for(View v : views) {
			v.setVisibility(View.VISIBLE);
			v.setEnabled(true);
		}
	}
	
	/**
	 * Hide views
	 * @param views
	 */
	private void hideViews(View... views) {
		for(View v : views) { 
			v.setVisibility(View.GONE);
			v.setEnabled(false);
		}
	}
	
	/**
	 * Object Picker view's topbar image deselect operation
	 */
	private void deselectObjectPickerTopbarImages() {
		imgAccessoryTab.setImageResource(R.drawable.accessorys_normal);
		imgJerseyTab.setImageResource(R.drawable.jersey_normal);
		imgHatsTab.setImageResource(R.drawable.hats_normal);
		imgHelmetTab.setImageResource(R.drawable.helmet_normal);
	}
	
	/**
	 * Object Picker view's topbar image select operation
	 */
	private void selectObjectPickerTopbarImage(ImageView img) {
		if(img == imgAccessoryTab)
			img.setImageResource(R.drawable.accessorys_selected);
		else if(img == imgJerseyTab)
			img.setImageResource(R.drawable.jersey_selected);
		else if(img == imgHatsTab)
			img.setImageResource(R.drawable.hats_selected);
		else if(img == imgHelmetTab)
			img.setImageResource(R.drawable.helmet_selected);		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case CHANGE_ACTION_TAKE_PHOTO:
		case CHANGE_ACTION_PHOTO_LIBRARY:
			if (resultCode == RESULT_OK) {
				Uri u = data.getData();
				initMainImage(u);
			}
			break;
		default:
			mFacebook.authorizeCallback(requestCode, resultCode, data);
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public void onBackPressed() {
		mPhotoView.unloadImages();		
		super.onBackPressed();
	}
	
	private Bitmap uriToBitmap(Uri uri) {
		Bitmap bitmap = null;
		try {
			bitmap = MediaStore.Images.Media.getBitmap(
					this.getContentResolver(), uri);
			/*
			 * Matrix matrix = new Matrix(); matrix.postRotate(90); bitmap =
			 * Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
			 * bitmap.getHeight(), matrix, false);
			 */

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bitmap;
	}
	
	public Bitmap getCurrentPicture() {
		mPhotoView.setDrawingCacheEnabled(true);
		mPhotoView.setDrawingCacheQuality(ViewGroup.DRAWING_CACHE_QUALITY_HIGH);
		Bitmap photo = mPhotoView.getDrawingCache();
		mPhotoView.setDrawingCacheEnabled(false);
		return photo;
	}

	// =====================================
	// MARK: Footer buttons click events
	// =====================================
	
	/**
	 * Main image scale event
	 * @param v
	 */
	public void scaleClick(View v) {
		imgScale.setEnabled(false);
		Img mainImg = mPhotoView.getMainImage();
		mainImg.setLocked(false);
		mainImg.setSelected(true);
		mPhotoView.invalidate();
	}
	
	/**
	 * Main image change event
	 * @param v
	 */
	public void changeClick(View v) {
		if(mChangeAction == CHANGE_ACTION_PHOTO_LIBRARY) {
			Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(i, CHANGE_ACTION_PHOTO_LIBRARY);
		} else if(mChangeAction == CHANGE_ACTION_TAKE_PHOTO) {
			Intent intent = new Intent(this, CameraNewActivity.class);
			startActivityForResult(intent, CHANGE_ACTION_TAKE_PHOTO);
		}
	}
	
	/**
	 * Open/close object picker
	 * @param v
	 */
	public void helmetClick(View v) {
		// do not allow user to add objects after done button is clicked
		if(imgEdit.getVisibility() == View.VISIBLE)
			return;
		
		if(vObjectPicker.getVisibility() == View.GONE) {
			hideAllButtons();
			imgHelmet.setImageResource(R.drawable.bottom_helmet_selected);
			showViews(vObjectPicker);			
		} else {
			hideViews(vObjectPicker);
			imgHelmet.setImageResource(R.drawable.bottom_helmet);
			mPhotoView.getMainImage().setLocked(true);
			if(mPhotoView.getImageCount() == 0) {
				showViews(imgScale, imgChange);				
			} else {
				showViews(imgDone, imgDelete, imgRaise, imgLower);
			}
		}
	}
	
	/**
	 * Done image editing
	 * @param v
	 */
	public void doneClick(View v) {
		mPhotoView.deselectCurrentImage();
		mPhotoView.lockAllImages();
		hideAllButtons();
		showViews(imgNew, imgEdit, imgShare);
	}
	
	/**
	 * Delete any added object
	 * @param v
	 */
	public void deleteClick(View v) {
		mPhotoView.removeCurrentImage();
	}
	
	/**
	 * Raise object to top level
	 * @param v
	 */
	public void raiseClick(View v) {
		mPhotoView.moveSelectedObjectToUpper();
	}
	
	/**
	 * Lower object to bottom level
	 * @param v
	 */
	public void lowerClick(View v) {
		mPhotoView.moveSelectedObjectToLower();
	}
	
	/**
	 * Create new photo
	 * @param v
	 */
	public void newClick(View v) {		
		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		finish();
	}
	
	/**
	 * Edit photo
	 * @param v
	 */
	public void editClick(View v) {
		mPhotoView.getMainImage().setLocked(true);
		hideAllButtons();
		
		if(mPhotoView.getImageCount() == 0) {
			showViews(imgScale, imgChange);
		} else {
			mPhotoView.unlockAllImages();
			showViews(imgDone, imgDelete, imgRaise, imgLower);
		}
	}
	
	/**
	 * Share button click event 
	 * Show/hide share view with animation
	 * @param v
	 */
	public void shareClick(View v) {
		if(vShare.getVisibility() == View.GONE) {
			vShare.setVisibility(View.VISIBLE);
			//Util.animate(this, vShare, R.anim.grow_from_bottom);
		} else {
			vShare.setVisibility(View.GONE);
			/*Util.animate(this, vShare, R.anim.shrink_to_bottom, new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
				}
				
				@Override
				public void onAnimationRepeat(Animation animation) {
				}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					vShare.setVisibility(View.GONE);
				}
			});*/
		}
	}
	
	/**
	 * Share picture on instagram
	 * @param v
	 */
	public void instagramClick(View v) {
		vShare.setVisibility(View.GONE);
		if(Util.isApplicationInstalled(getApplicationContext(), Constant.INSTAGRAM_PACKAGE_NAME)) {
			mPhotoView.setDrawingCacheEnabled(true);
			mPhotoView.setDrawingCacheQuality(ViewGroup.DRAWING_CACHE_QUALITY_HIGH);
			Bitmap photo = mPhotoView.getDrawingCache();
			try {
				String path = Util.saveBitmapToSDCard(getContentResolver(), photo);
				if(path != null) {
					Uri pathURI = Uri.parse(path);
					String filePath = getRealPathFromURI(pathURI);
					Intent i = new Intent(Intent.ACTION_SEND);
		        	i.setType("image/jpeg");
		        	i.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + filePath));
		        	startActivity(Intent.createChooser(i, "Share Image"));
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		} else {
			Util.alertbox(this, "Please download instagram application to share photo.");
		}
	}
	
	/**
	 * Share picture on twitter
	 * @param v
	 */
	public void twitterClick(View v) {
		//Util.alertbox(this, "Under development...");
		vShare.setVisibility(View.GONE);
		if(mTwitter.hasAccessToken()) {
			uploadPhotoToTwitter();
		} else {
			mTwitter.authorize();
		}
	}
	
	/**
	 * Share picture on facebook
	 * @param v
	 */
	public void facebookClick(View v) {
		vShare.setVisibility(View.GONE);
		if(mFacebook.isSessionValid()) {
			uploadPhotoToFacebookWall(mPhotoView.toByteArray());
		} else {
			mFacebook.authorize(this, new String[] { "publish_stream" }, new LoginDialogListener());
		}
		// login dialog events => SampleAuthListener events communication
		
        //SessionEvents.addLogoutListener(new SampleLogoutListener());
	}
	
	/**
	 * Email picture
	 * @param v
	 */
	public void emailClick(View v) {
		vShare.setVisibility(View.GONE);
		//Util.alertbox(this, "Under development...");
		mPhotoView.setDrawingCacheEnabled(true);
		mPhotoView.setDrawingCacheQuality(ViewGroup.DRAWING_CACHE_QUALITY_HIGH);
		Bitmap photo = mPhotoView.getDrawingCache();
		try {
			String path = Util.saveBitmapToSDCard(getContentResolver(), photo);
			if(path != null) {
				Uri pathURI = Uri.parse(path);
				mTwitterPhotoURI = pathURI;
				mTwitterPhotoPath = getRealPathFromURI(pathURI);
				
				Intent sendIntent = new Intent(Intent.ACTION_SEND);
			    sendIntent.setType("jpeg/image");
			    sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {Constant.EMAIL_ID});
			    sendIntent.putExtra(Intent.EXTRA_SUBJECT, Constant.EMAIL_SUBJECT);
			    Util.LOG("Attachment file : " + mTwitterPhotoPath);
			    sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(String.format("file://%s", mTwitterPhotoPath)));
			    sendIntent.putExtra(Intent.EXTRA_TEXT,String.format("%s %s", Constant.EMAIL_BODY, getString(R.string.play_store_link)) );

			    startActivity(Intent.createChooser(sendIntent, "Email:"));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mPhotoView.setDrawingCacheEnabled(false);
			photo = null;
		}
	}
	
	/**
	 * Save picture to sdcard
	 * @param v
	 */
	public void saveClick(View v) {
		vShare.setVisibility(View.GONE);
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
		} finally {
			mPhotoView.setDrawingCacheEnabled(false);
			photo = null;
		}
	}
	
	/**
	 * Buy button click handler
	 * @param v
	 */
	public void buyClick(View v) {		
		Intent intent = new Intent(this, BuyActivity.class);
		startActivity(intent);
	}
	// =====================================
	// MARK: Tabbar of Object Picker
	// =====================================
	
	/**
	 * Object picker tabbar images click event
	 * @param v
	 */
	public void changeObjectClick(View v) {
		deselectObjectPickerTopbarImages();
		selectObjectPickerTopbarImage((ImageView) v);
		
		if(v == imgJerseyTab) 
			galleryObjects.setAdapter(getJerseyAdapter());
		else if(v == imgHelmetTab)
			galleryObjects.setAdapter(getHelmetAdapter());
		else if(v == imgHatsTab)
			galleryObjects.setAdapter(getHatAdapter());
		else if(v == imgAccessoryTab)
			galleryObjects.setAdapter(getAccessoriesAdapter());
		
		((ObjectsAdapterNewAssets)galleryObjects.getAdapter()).notifyDataSetChanged();
	}
	
	// ==========================================
	// MARK: Gallery view item click listener
	// ==========================================
	
	private OnItemClickListener galleryObjectItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
			ObjectsAdapterNewAssets adapter = (ObjectsAdapterNewAssets) galleryObjects.getAdapter();
			String imageAssetsURI = adapter.getItem(position);
			Util.LOG(String.format("Image URI : " + imageAssetsURI));
			Drawable d = Util.loadImageFromAssets(getAssets(), imageAssetsURI);
			mPhotoView.addImage(d);
			helmetClick(imgHelmet);
		}
	};
	
	private OnItemSelectedListener galleryObjectItemSelectListener = new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
			imgArrowLeft.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
			imgArrowRight.setVisibility(position == parent.getCount()-1 ? View.INVISIBLE : View.VISIBLE);
		}
		
		@Override
		public void onNothingSelected(AdapterView<?> parent) {	
		}
	};
	
	// ==========================================
	// MARK: Facebook Callbacks
	// ==========================================
	
	private void uploadPhotoToFacebookWall(byte[] photo) {
		//WallPostBean bean = new WallPostBean();
		//bean.description = String.format("%s %s", Constant.FACEBOOK_STATUS_MSG, getString(R.string.play_store_link));		
		
		Bundle params = new Bundle();
		params.putString("message", String.format("%s %s", Constant.FACEBOOK_STATUS_MSG, getString(R.string.play_store_link)));
//      params.putString("method", "photos.upload");
		//params.putString("attachment", bean.toJson());
		params.putByteArray("picture", photo);
		pd.setMessage(getString(R.string.facebook_uploading_photo));
		pd.show();
        //mAsyncRunner.request("me/photos", params, "POST", new SampleUploadListener(), null);
		mAsyncRunner.request("me/photos", params, "POST", new SampleUploadListener(), null);
	}
	
	// step 1: for Facebook Authentication
	private final class LoginDialogListener implements DialogListener {
        public void onComplete(Bundle values) {
        	SessionStore.save(mFacebook, PhotoBoothNewActivity.this);
            SessionEvents.onLoginSuccess();
        }

        public void onFacebookError(FacebookError error) {
            SessionEvents.onLoginError(error.getMessage());
        }
        
        public void onError(DialogError error) {
            SessionEvents.onLoginError(error.getMessage());
        }

        public void onCancel() {
            SessionEvents.onLoginError("Action Canceled");
        }
    }
	
	// Step 2: after facebook authentication success / failure
		// if succeeded then share photo on facebook
	public class SampleAuthListener implements AuthListener {

		public void onAuthSucceed() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					uploadPhotoToFacebookWall(mPhotoView.toByteArray());
				}
			});
		}

		public void onAuthFail(final String error) {
			Util.LOG("Facebook Authentication Error : " + error);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if(pd.isShowing())
						pd.dismiss();
					Util.alertbox(PhotoBoothNewActivity.this, "Facebook: " + error);
				}
			});
		}
	}

	public class SampleLogoutListener implements LogoutListener {
		public void onLogoutBegin() {
		}

		public void onLogoutFinish() {
		}
	}

//	public class SampleRequestListener extends BaseRequestListener {
//
//		public void onComplete(final String response, final Object state) {
//		}
//	}

	public class SampleUploadListener extends BaseRequestListener {
		
		@Override
		public void onFacebookError(final FacebookError e, Object state) {
			super.onFacebookError(e, state);
			Util.LOG("Photo upload error : " + e + ", state : " + state);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if(pd.isShowing()) pd.dismiss();
					Util.alertbox(PhotoBoothNewActivity.this, "Error Occured:\n" + e.getMessage());
				}
			});			
		}

		public void onComplete(final String response, final Object state) {
			Util.LOG("Photo uploaded : " + response + ", sate : " + state);
			try {
				JSONObject jsonResponse = new JSONObject(response);
				if(jsonResponse.getString("id") != null) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if(pd.isShowing()) pd.dismiss();
							Util.alertbox(PhotoBoothNewActivity.this, getString(R.string.facebook_photo_uploaded_successfully));
						}
					});
				}
			} catch (JSONException e) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if(pd.isShowing()) pd.dismiss();
					}
				});
				e.printStackTrace();
			}
		}
	}
	
	// ==========================================
	// MARK: Twitter Callbacks
	// ==========================================
	private void uploadPhotoToTwitter() {
		mPhotoView.setDrawingCacheEnabled(true);
		mPhotoView.setDrawingCacheQuality(ViewGroup.DRAWING_CACHE_QUALITY_HIGH);
		Bitmap photo = mPhotoView.getDrawingCache();
		try {
			String path = Util.saveBitmapToSDCard(getContentResolver(), photo);
			if(path != null) {
				Uri pathURI = Uri.parse(path);
				mTwitterPhotoURI = pathURI;
				mTwitterPhotoPath = getRealPathFromURI(pathURI);
				Util.LOG("Twitter tmp saved photo path : " + mTwitterPhotoPath);
				new ImageSender().execute();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mPhotoView.setDrawingCacheEnabled(false);
			photo = null;
		}
	}
	
	public String getRealPathFromURI(Uri contentUri) {
        String [] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery( contentUri, proj, null, null,null);        
        if (cursor == null) return null;        
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);        
        cursor.moveToFirst();
        return cursor.getString(column_index);
	}
	
	private final TwDialogListener mTwLoginDialogListener = new TwDialogListener() {
		@Override
		public void onComplete(String value) {
			Util.LOG("Twitter authentication successful : " + value);
			uploadPhotoToTwitter();
		}
		
		@Override
		public void onError(String value) {
			Util.LOG("Twitter authentication error : " + value);
		}
	};
	
	private class ImageSender extends AsyncTask<URL, Integer, Long> {
    	private String url;
    	
    	protected void onPreExecute() {
			pd.setMessage(getString(R.string.twitter_uploading_photo));
			pd.show();
		}
    	
        protected Long doInBackground(URL... urls) {            
            long result = 0;
                       
            TwitterSession twitterSession = new TwitterSession(PhotoBoothNewActivity.this);            
            AccessToken accessToken = twitterSession.getAccessToken();
			
			Configuration conf = new ConfigurationBuilder()                 
            	.setOAuthConsumerKey(Constant.TWITTER_CONSUMER_KEY) 
            	.setOAuthConsumerSecret(Constant.TWITTER_CONSUMER_SECRET) 
            	.setOAuthAccessToken(accessToken.getToken()) 
            	.setOAuthAccessTokenSecret(accessToken.getTokenSecret()) 
            	.build(); 
			
			OAuthAuthorization auth = new OAuthAuthorization (conf, conf.getOAuthConsumerKey (), conf.getOAuthConsumerSecret (),
	                new AccessToken (conf.getOAuthAccessToken (), conf.getOAuthAccessTokenSecret ()));
	        
	        ImageUpload upload = ImageUpload.getTwitpicUploader (Constant.TWITPIC_API_KEY, auth);
	        
	        Util.LOG("Start sending image...");
	        
	        try {
	        	url = upload.upload(new File(mTwitterPhotoPath));
	        	result = 1;
	        	
	        	StringBuilder statusMsg = new StringBuilder();
	        	statusMsg.append(Constant.TWITTER_STATUS_MSG_LINE_1).append(url);
	        	//statusMsg.append("\n");
	        	//statusMsg.append(Constant.TWITTER_STATUS_MSG_LINE_2).append(getString(R.string.play_store_link));
	        	
	        	mTwitter.updateStatus(statusMsg.toString());
	        	Util.LOG("Image uploaded, Twitpic url is " + url);
	        } catch (Exception e) {
	        	Util.LOG("Failed to send image");
	        	
	        	e.printStackTrace();
	        }
	        
            return result;
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Long result) {
        	pd.dismiss();
        	if(result == 1) {
        		Util.LOG("Removing twitter tmp image...");
        		File tmpTwitterPhoto = new File(mTwitterPhotoPath);
        		
        		if(tmpTwitterPhoto.exists() && tmpTwitterPhoto.delete()) {
        			Util.LOG("Twitter Tmp photo deleted");
        			int deleteRowCount = getContentResolver().delete(mTwitterPhotoURI, null, null);
        			Util.LOG("Uri delete row count : " + deleteRowCount);
        		} else {
        			Util.LOG("Can't delete Twitter tmp photo");
        		}
        		
        		
        	}
        		
        	String message = getString(result == 1 ? R.string.twitter_photo_uploaded_successfully : R.string.twitter_photo_upload_error);
        	Util.alertbox(PhotoBoothNewActivity.this, message);
        	
        }
    }
}