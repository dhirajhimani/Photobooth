package com.photobooth.photobooth.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;

import com.photobooth.photobooth.R;

/**
 * 
 * @author Kevin<br>
 *         created date : 29 June 2011
 * 
 */
public class Util {
	
	public static void LOG(String message) {
		if(message == null)
			return;
		
		if(Constant.DEBUG_MODE)
			Log.d(Constant.LOG_TAG, message);
	}

	/**
	 * It shows the alert message
	 * 
	 * @param context
	 *            - context in which alert message will be displayed
	 * @param title
	 *            - title of the alert box
	 * @param message
	 *            - message to show
	 */
	public static void alertbox(Context context, String message) {
		alertbox(context, message, null);
	}

	public static void alertbox(Context context, String message,
			OnClickListener clickListener) {

		new AlertDialog.Builder(context)
				.setMessage(message)
				.setTitle(context.getString(R.string.message))
				.setCancelable(true)
				.setNeutralButton(
						context.getString(android.R.string.ok),
						clickListener != null ? clickListener
								: new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
									}
								}).show();
	}

	/**
	 * Save preference in shared preference
	 * 
	 * @param context
	 * @param preference
	 */
	public static void savePreference(Context context,
			HashMap<String, String> preference) {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		Set<String> keys = preference.keySet();
		for (String key : keys) {
			editor.putString(key, preference.get(key)).commit();
		}
	}

	/**
	 * Get value from shared preference
	 * 
	 * @param context
	 * @param key
	 * @return preference value if available, empty string otherwise
	 */
	public static String getPreferenceValue(Context context, String key) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String value = pref.getString(key, "");
		return value;
	}

	/**
	 * This method check the network.
	 * 
	 * @param context
	 *            - context from which this method is called
	 * @return true if network connectivity is avaialbe, false otherwise.
	 */
	public static final boolean isNetworkAvailable(Context context) {
		if (context != null) {
			ConnectivityManager cm = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo netInfo = cm.getActiveNetworkInfo();
			if (netInfo != null && netInfo.isConnectedOrConnecting()) {
				return true;
			}
			return false;
		}
		return false;
	}

	/**
	 * This method print specified input stream in log
	 * 
	 * @param in
	 *            - input stream to be printed in logcat
	 * @throws IOException
	 */
	public static void printInputStream(InputStream in) throws IOException {
		if (in != null) {
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = "";
			while ((line = br.readLine()) != null) {
				Log.d("PRINT_INPUT_STREAM", line);
			}
		}
	}

	/**
	 * Convert input stream to string
	 * 
	 * @param in
	 *            - input stream to convert
	 * @return - string conversion of given input stream, or null if input
	 *         stream is null
	 * @throws IOException
	 */
	public static String inputStreamToString(InputStream in) throws IOException {
		if (in != null) {
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			StringBuffer sb = new StringBuffer();
			String line = "";
			while ((line = br.readLine()) != null) {
				sb.append(line);
				// Log.d("PRINT_INPUT_STREAM", line);
			}
			return sb.toString();
		}
		return null;
	}

	/**
	 * Extract the tag value from xml response
	 * 
	 * @param xml
	 *            - xml data
	 * @param startTag
	 *            - start tag
	 * @param endTag
	 *            - end tag
	 * @return value of tag
	 */
	public static String getTagValue(String xml, String startTag, String endTag) {
		String value = "";
		value = xml.substring(xml.indexOf(startTag) + startTag.length(),
				xml.indexOf(endTag));
		return value;
	}

	/**
	 * Animate view
	 * 
	 * @param context
	 * @param v
	 * @param animationId
	 */
	public static void animate(Context context, View v, int animationId) {
		v.clearAnimation();

		Animation animation = AnimationUtils.loadAnimation(context, animationId);
		v.setAnimation(animation);
		animation.start();
	}
	
	/**
	 * Animate view
	 * 
	 * @param context
	 * @param v
	 * @param animationId
	 */
	public static void animate(Context context, View v, int animationId, AnimationListener listener) {
		v.clearAnimation();

		Animation animation = AnimationUtils.loadAnimation(context, animationId);
		animation.setAnimationListener(listener);
		v.setAnimation(animation);
		animation.start();
	}

	/**
	 * Modify bitmap to have rounded corners
	 * 
	 * @param bitmap
	 * @return bitmap with rounded corners
	 */
	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx) {
		if (bitmap != null) {
			Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
					bitmap.getHeight(), Config.ARGB_8888);
			Canvas canvas = new Canvas(output);

			final int color = 0xff424242;
			final Paint paint = new Paint();
			final Rect rect = new Rect(0, 0, bitmap.getWidth(),
					bitmap.getHeight());
			final RectF rectF = new RectF(rect);
			// final float roundPx = 6;

			paint.setAntiAlias(true);
			canvas.drawARGB(0, 0, 0, 0);
			paint.setColor(color);
			canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

			paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
			canvas.drawBitmap(bitmap, rect, rect, paint);

			return output;
		}
		return bitmap;
	}

	/**
	 * Validate string with particular pattern
	 * 
	 * @param strPattern
	 * @param strToValidate
	 * @return true if strToValidate is match with strPattern, false otherwise
	 */
	public static boolean matchPattern(String strPattern, String strToValidate) {
		return Pattern.matches(strPattern, strToValidate);
	}

	/**
	 * Get the unique device id
	 * 
	 * @param context
	 * @return device id
	 */
	public static String getDeviceId(Context context) {
//		String deviceId = Secure.getString(context.getContentResolver(),
//                Secure.ANDROID_ID);
//		return deviceId;
		return ((TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
	}

	/**
	 * show/hide soft keyboard
	 * @param context
	 * @param v
	 * @param visible
	 */
	public static void setKeyboardVisible(Context context, View v, boolean visible) {
		InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		if(visible) {
			imm.showSoftInput(v, 0);
		} else {
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
		}
	}
	
	public static String saveBitmapToSDCard(ContentResolver cr, Bitmap _bitmapScaled) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		_bitmapScaled.compress(Bitmap.CompressFormat.JPEG, 40, bytes);

		//you can create a new file name "test.jpg" in sdcard folder.
		String fileName = new Date().getTime() + ".jpg";
		File f = new File(Environment.getExternalStorageDirectory() + File.separator + fileName);
		boolean saved = f.createNewFile();
		//write the bytes in file
		FileOutputStream fo = new FileOutputStream(f);
		fo.write(bytes.toByteArray());
		fo.close();
		if(saved) {
			String imageURI = MediaStore.Images.Media.insertImage(cr, _bitmapScaled, fileName, "");
			return imageURI;
		} else {
			return null;
		}
	}
	
	public static String[] getFileListFromAssets(Context context, String folderName) {
		try {
			String[] fileList = context.getAssets().list(folderName);
			LOG(fileList.toString());
			return fileList;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static Drawable loadImageFromAssets(AssetManager manager, String imageURI) {
		// load image
		try {
			// get input stream
			InputStream ims = manager.open(imageURI);
			// load image as Drawable
			Drawable d = Drawable.createFromStream(ims, null);
			// set image to ImageView
			return d;
		} catch (IOException ex) {
			return null;
		}
	}
	
	public static boolean isApplicationInstalled(Context context, String appPackage) {
    	try{
    	    ApplicationInfo info = context.getPackageManager().getApplicationInfo(appPackage, 0);
    	    System.out.println("info : " + info);
    	    return true;
    	} catch( PackageManager.NameNotFoundException e ){
    	    return false;
    	}
    }
	
	
	
	// public Bitmap scaleBitmap(Bitmap source) {
	// BitmapDrawable bmp = new BitmapDrawable(source);
	// int width = bmp.getIntrinsicWidth();
	// int height = bmp.getIntrinsicHeight();
	//
	// float ratio = (float)width/(float)height;
	//
	// float scaleWidth = width;
	// float scaleHeight = height;
	//
	// if((float)mMaxWidth/(float)mMaxHeight > ratio) {
	// scaleWidth = (float)mMaxHeight * ratio;
	// }
	// else {
	// scaleHeight = (float)mMaxWidth / ratio;
	// }
	//
	// Matrix matrix = new Matrix();
	// matrix.postScale(scaleWidth, scaleHeight);
	//
	// Bitmap out = Bitmap.createBitmap(bmp.getBitmap(),
	// 0, 0, width, height, matrix, true);
	// }
}