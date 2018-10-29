package com.photobooth.view;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.metalev.multitouch.controller.MultiTouchController;
import org.metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas;
import org.metalev.multitouch.controller.MultiTouchController.PointInfo;
import org.metalev.multitouch.controller.MultiTouchController.PositionAndScale;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.photobooth.photobooth.util.Util;

public class PhotoBoothView extends View implements
		MultiTouchObjectCanvas<PhotoBoothView.Img> {

	private static int count = 0;

	public static final int TYPE_MAIN_IMAGE = 10001;
	public static final int TYPE_SUB_IMAGE = 10010;

	// private static final int[] IMAGES = { R.drawable.m74hubble,
	// R.drawable.catarina, R.drawable.tahiti, R.drawable.sunset,
	// R.drawable.lake };

	private ArrayList<Img> mImages = new ArrayList<Img>();

	// --

	private MultiTouchController<Img> multiTouchController = new MultiTouchController<Img>(
			this);

	// --

	private PointInfo currTouchPoint = new PointInfo();

	private boolean mShowDebugInfo = false;

	private static final int UI_MODE_ROTATE = 1, UI_MODE_ANISOTROPIC_SCALE = 2;

	private int mUIMode = UI_MODE_ROTATE;

	// --

	private Paint mLinePaintTouchPointCircle = new Paint();

	// ---------------------------------------------------------------------------------------------------

	public PhotoBoothView(Context context) {
		this(context, null);
	}

	public PhotoBoothView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PhotoBoothView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		/*
		 * Resources res = context.getResources(); for (int i = 0; i <
		 * IMAGES.length; i++) mImages.add(new Img(IMAGES[i], res));
		 */

		mLinePaintTouchPointCircle.setColor(Color.YELLOW);
		mLinePaintTouchPointCircle.setStrokeWidth(5);
		mLinePaintTouchPointCircle.setStyle(Style.STROKE);
		mLinePaintTouchPointCircle.setAntiAlias(true);
		setBackgroundColor(Color.TRANSPARENT);
	}

	// public void setMainImage(Bitmap mainImage) {
	// this.mMainImage = new BitmapDrawable(getResources(), mainImage);
	// invalidate();
	// }

	public void addImage(Bitmap bitmap, int type) {
		if (type == TYPE_MAIN_IMAGE) {
			setMainImage(new BitmapDrawable(getResources(), bitmap));
		} else {
			addImage(new BitmapDrawable(getResources(), bitmap));
		}
	}

	public void addImage(Drawable drawable) {
		Resources res = getContext().getResources();
		Img img = new Img(drawable, res, count++);
		img.setType(TYPE_SUB_IMAGE);
		selectObject(img, currTouchPoint);
		mImages.add(img);
		img.load(res);
		invalidate();
	}

	private void setMainImage(Drawable drawable) {
		Img mainImg = getMainImage();
		if (mainImg == null) {
			Resources res = getContext().getResources();
			Img img = new Img(drawable, res, count++);
			img.setType(TYPE_MAIN_IMAGE);
			mImages.add(img);
			img.load(res);
		} else {
			mainImg.drawable = drawable;
			mainImg.load(getResources());
		}
		invalidate();
	}

	public Img getMainImage() {
		Img mainImg = null;
		for (Img img : mImages) {
			if (img.isMainImage()) {
				mainImg = img;
				break;
			}
		}
		return mainImg;
	}
	
	public void deselectCurrentImage() {
		for(Img img : mImages) {
			if(img.isSelected())
				img.setSelected(false);
		}
		invalidate();
	}
	
	public void lockAllImages() {
		for(Img img : mImages) {
			if(img.isMainImage())
				continue;
			img.setLocked(true);
		}
		invalidate();
	}
	
	public void unlockAllImages() {
		for(Img img : mImages) {
			if(img.isMainImage())
				continue;
			img.setLocked(false);
		}
		invalidate();
	}
	
	public int getImageCount() {
		return mImages.size() - 1;	// image count without main image
	}

	public void removeCurrentImage() {
		Img img = getDraggableObjectAtPoint(currTouchPoint);
		if(mImages.remove(img)) {
			Util.LOG("Current image deleted.....");
			img.unload();
			invalidate();
		}
	}

	/** Called by activity's onResume() method to load the images */
	public void loadImages(Context context) {
		Resources res = context.getResources();
		int n = mImages.size();
		for (int i = 0; i < n; i++)
			mImages.get(i).load(res);
	}

	/**
	 * Called by activity's onPause() method to free memory used for loading the
	 * images
	 */
	public void unloadImages() {
		int n = mImages.size();
		for (int i = 0; i < n; i++)
			mImages.get(i).unload();
	}
	
	public byte[] toByteArray() {
		setDrawingCacheEnabled(true);
		setDrawingCacheQuality(ViewGroup.DRAWING_CACHE_QUALITY_HIGH);
        Bitmap photo = getDrawingCache();            
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();            
        try {stream.close();} catch (IOException e1) {e1.printStackTrace();}
        photo = null;            
        setDrawingCacheEnabled(false);
        return byteArray;
	}

	// ---------------------------------------------------------------------------------------------------

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		int n = mImages.size();
		for (int i = 0; i < n; i++) {
			Img img = mImages.get(i);
			if(img != null)
				img.draw(canvas);
		}
		if (mShowDebugInfo)
			drawMultitouchDebugMarks(canvas);
	}

	// ---------------------------------------------------------------------------------------------------

	public void trackballClicked() {
		mUIMode = (mUIMode + 1) % 3;
		invalidate();
	}

	private void drawMultitouchDebugMarks(Canvas canvas) {
		if (currTouchPoint.isDown()) {
			float[] xs = currTouchPoint.getXs();
			float[] ys = currTouchPoint.getYs();
			float[] pressures = currTouchPoint.getPressures();
			int numPoints = Math.min(currTouchPoint.getNumTouchPoints(), 2);
			for (int i = 0; i < numPoints; i++)
				canvas.drawCircle(xs[i], ys[i], 50 + pressures[i] * 80,
						mLinePaintTouchPointCircle);
			if (numPoints == 2)
				canvas.drawLine(xs[0], ys[0], xs[1], ys[1],
						mLinePaintTouchPointCircle);
		}
	}

	// ---------------------------------------------------------------------------------------------------

	/** Pass touch events to the MT controller */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return multiTouchController.onTouchEvent(event);
	}

	/**
	 * Get the image that is under the single-touch point, or return null
	 * (canceling the drag op) if none
	 */
	public Img getDraggableObjectAtPoint(PointInfo pt) {

		float x = pt.getX(), y = pt.getY();
		int n = mImages.size();
		for (int i = n - 1; i >= 0; i--) {
			Img im = mImages.get(i);

			if (!im.isLocked() && im.containsPoint(x, y))
				return im;
		}
		return null;
	}

	/**
	 * Select an object for dragging. Called whenever an object is found to be
	 * under the point (non-null is returned by getDraggableObjectAtPoint()) and
	 * a drag operation is starting. Called with null when drag op ends.
	 */
	public void selectObject(Img img, PointInfo touchPoint) {
		// deselect all objects
		currTouchPoint.set(touchPoint);
		if (img != null) {
			
			int n = mImages.size();
			for (int i = n - 1; i >= 0; i--) {
				mImages.get(i).setSelected(false);
			}

			// Move image to the top of the stack when selected
			//mImages.remove(img);
			//mImages.add(img);
			img.setSelected(true);
		} else {
			// Called with img == null when drag stops.
		}
		invalidate();
	}

	/**
	 * Get the current position and scale of the selected image. Called whenever
	 * a drag starts or is reset.
	 */
	public void getPositionAndScale(Img img, PositionAndScale objPosAndScaleOut) {

		// FIXME affine-izem (and fix the fact that the anisotropic_scale part
		// requires averaging the two scale factors)
		objPosAndScaleOut.set(img.getCenterX(), img.getCenterY(),
				(mUIMode & UI_MODE_ANISOTROPIC_SCALE) == 0,
				(img.getScaleX() + img.getScaleY()) / 2,// ORIGINAL
				(mUIMode & UI_MODE_ANISOTROPIC_SCALE) != 0, img.getScaleX(),
				img.getScaleY(), (mUIMode & UI_MODE_ROTATE) != 0,
				img.getAngle());
	}

	/** Set the position and scale of the dragged/stretched image. */
	public boolean setPositionAndScale(Img img,
			PositionAndScale newImgPosAndScale, PointInfo touchPoint) {
		currTouchPoint.set(touchPoint);
		boolean ok = img.setPos(newImgPosAndScale);
		if (ok)
			invalidate();
		return ok;
	}

	private Img getSelectedImg() {
		Img selectedImg = null;
		for (Img img : mImages) {
			if (img.isSelected()) {
				selectedImg = img;
				break;
			}
		}
		return selectedImg;
	}

	public void moveSelectedObjectToLower() {
		Img selectedImg = getSelectedImg();

		if (selectedImg == null)
			return;

		Util.LOG("MoveToLower() - Object Tag : " + selectedImg.tag);
		mImages.remove(selectedImg);
		mImages.add(1, selectedImg);
		invalidate();
	}

	public void moveSelectedObjectToUpper() {
		Img selectedImg = getSelectedImg();

		if (selectedImg == null)
			return;

		Util.LOG("MoveToUpper() - Object Tag : " + selectedImg.tag);
		mImages.remove(selectedImg);
		mImages.add(selectedImg);
		invalidate();
	}

	// ----------------------------------------------------------------------------------------------

	public class Img {
		// private int resId;

		private int mType;
		private boolean mainImagePositionSet;

		private Drawable drawable;

		private boolean firstLoad;

		private int width, height, displayWidth, displayHeight;

		private float centerX, centerY, scaleX, scaleY, angle;

		private float minX, maxX, minY, maxY;

		private static final float SCREEN_MARGIN = 100;

		private boolean selected;
		private Paint selectedPaint;

		private boolean locked;

		// tag to keep track of object index (position)
		private int tag;
		
		/*
		 * public Img(int resId, Resources res) { this.resId = resId;
		 * this.firstLoad = true; getMetrics(res); }
		 */

		public Img(Drawable image, Resources res, int tag) {
			this.drawable = image;
			this.firstLoad = true;
			getMetrics(res);

			selectedPaint = new Paint();
			selectedPaint.setStrokeWidth(2.0f);
			selectedPaint.setStyle(Style.STROKE);
			selectedPaint.setColor(Color.RED);
			selectedPaint.setAntiAlias(true);
			
			this.tag = tag;
		}

		/**
		 * Type of image set - MAIN_IMAGE can't move, while SUB_IMAGE can.
		 * 
		 * @param type
		 *            - MAIN_IMAGE / SUB_IMAGE
		 */
		public void setType(int type) {
			mType = type;
		}

		public int getType() {
			return mType;
		}

		public void setMainImagePositionSet(boolean positionSet) {
			mainImagePositionSet = positionSet;
		}

		public boolean isMainImagePositionSet() {
			return mainImagePositionSet;
		}

		public boolean isMainImage() {
			return mType == TYPE_MAIN_IMAGE;
		}

		private void getMetrics(Resources res) {
			DisplayMetrics metrics = res.getDisplayMetrics();
			// The DisplayMetrics don't seem to always be updated on screen
			// rotate, so we hard code a portrait
			// screen orientation for the non-rotated screen here...
			// this.displayWidth = metrics.widthPixels;
			// this.displayHeight = metrics.heightPixels;
			this.displayWidth = res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE 
					? Math.max(metrics.widthPixels, metrics.heightPixels) 
					: Math.min(metrics.widthPixels, metrics.heightPixels);
			this.displayHeight = res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE 
					? Math.min(metrics.widthPixels, metrics.heightPixels) 
					: Math.max(metrics.widthPixels, metrics.heightPixels);
		}

		/** Called by activity's onResume() method to load the images */
		public void load(Resources res) {
			getMetrics(res);
			this.width = drawable.getIntrinsicWidth();
			this.height = drawable.getIntrinsicHeight();
			
			float cx, cy, sx, sy;
			
			if (firstLoad) {
				if(isMainImage()) {
					cx = (float) 0.5 * displayWidth;
					cy = (float) 0.5 * displayHeight;
				} else {
					cx = SCREEN_MARGIN + (float) (0.5 * (displayWidth - 2 * SCREEN_MARGIN));
					cy = SCREEN_MARGIN + (float) (0.5 * (displayHeight - 2 * SCREEN_MARGIN));
				}
				//float sc = (float) (Math.max(displayWidth, displayHeight) / (float) Math.max(width, height) * 0.09 + 0.2);	// ORIGINAL
				float sc = (float) (Math.max(displayWidth, displayHeight) / (float) Math.max(width, height) * 0.5 + 0.2);
				sx = sy = sc;
				firstLoad = false;
			} else {
				// Reuse position and scale information if it is available
				// FIXME this doesn't actually work because the whole activity
				// is torn down and re-created on rotate
				cx = this.centerX;
				cy = this.centerY;
				sx = this.scaleX;
				sy = this.scaleY;
				// Make sure the image is not off the screen after a screen
				// rotation
//				if (this.maxX < SCREEN_MARGIN)
//					cx = SCREEN_MARGIN;
//				else if (this.minX > displayWidth - SCREEN_MARGIN)
//					cx = displayWidth - SCREEN_MARGIN;
//				if (this.maxY > SCREEN_MARGIN)
//					cy = SCREEN_MARGIN;
//				else if (this.minY > displayHeight - SCREEN_MARGIN)
//					cy = displayHeight - SCREEN_MARGIN;
			}
			//setPos(cx, cy, sx, sy, height > width ? 1.5707963267949f : 0.0f);
			setPos(cx, cy, sx, sy, 0.0f);
		}

		/**
		 * Called by activity's onPause() method to free memory used for loading
		 * the images
		 */
		public void unload() {
			this.drawable = null;
		}

		/** Set the position and scale of an image in screen coordinates */
		public boolean setPos(PositionAndScale newImgPosAndScale) {
			return setPos(
					newImgPosAndScale.getXOff(),
					newImgPosAndScale.getYOff(),
					(mUIMode & UI_MODE_ANISOTROPIC_SCALE) != 0 ? newImgPosAndScale
							.getScaleX() : newImgPosAndScale.getScale(),
					(mUIMode & UI_MODE_ANISOTROPIC_SCALE) != 0 ? newImgPosAndScale
							.getScaleY() : newImgPosAndScale.getScale(),
					newImgPosAndScale.getAngle());
			// FIXME: anisotropic scaling jumps when axis-snapping
			// FIXME: affine-ize
			// return setPos(newImgPosAndScale.getXOff(),
			// newImgPosAndScale.getYOff(),
			// newImgPosAndScale.getScaleAnisotropicX(),
			// newImgPosAndScale.getScaleAnisotropicY(), 0.0f);
		}

		/** Set the position and scale of an image in screen coordinates */
		private boolean setPos(float centerX, float centerY, float scaleX,
				float scaleY, float angle) {

			if (isMainImagePositionSet())
				return true;

			float ws = (width / 2) * scaleX;
			float hs = (height / 2) * scaleY;
			
			float newMinX = centerX - ws; 
			float newMinY = centerY - hs; 
			float newMaxX = centerX	+ ws;
			float newMaxY = centerY + hs;
			
			if (newMinX > displayWidth - SCREEN_MARGIN
					|| newMaxX < SCREEN_MARGIN
					|| newMinY > displayHeight - SCREEN_MARGIN
					|| newMaxY < SCREEN_MARGIN)
				return false;
			this.centerX = centerX;
			this.centerY = centerY;
			this.scaleX = scaleX;
			this.scaleY = scaleY;
			this.angle = angle;
			this.minX = newMinX;
			this.minY = newMinY;
			this.maxX = newMaxX;
			this.maxY = newMaxY;
			return true;
		}

		/** Return whether or not the given screen coords are inside this image */
		public boolean containsPoint(float scrnX, float scrnY) {
			// FIXME: need to correctly account for image rotation
			return (scrnX >= minX && scrnX <= maxX && scrnY >= minY && scrnY <= maxY);
		}

		public void draw(Canvas canvas) {
			if(drawable == null)
				return;
			
			canvas.save();
			float dx = (maxX + minX) / 2;
			float dy = (maxY + minY) / 2;
			//Util.LOG(String.format("MaxX : %f    MaxY : %f   MinX : %f   MinY : %f   dx : %f    dy : %f", maxX, maxY, minX, minY, dx, dy));
			drawable.setBounds((int) minX, (int) minY, (int) maxX, (int) maxY);
			canvas.translate(dx, dy);
			canvas.rotate(angle * 180.0f / (float) Math.PI);
			canvas.translate(-dx, -dy);
			if (isSelected())
				canvas.drawRect((int) minX, (int) minY, (int) maxX, (int) maxY,
						selectedPaint);
			drawable.draw(canvas);
			canvas.restore();
		}

		public Drawable getDrawable() {
			return drawable;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public float getCenterX() {
			return centerX;
		}

		public float getCenterY() {
			return centerY;
		}

		public float getScaleX() {
			return scaleX;
		}

		public float getScaleY() {
			return scaleY;
		}

		public float getAngle() {
			return angle;
		}

		// FIXME: these need to be updated for rotation
		public float getMinX() {
			return minX;
		}

		public float getMaxX() {
			return maxX;
		}

		public float getMinY() {
			return minY;
		}

		public float getMaxY() {
			return maxY;
		}

		public boolean isSelected() {
			return selected;
		}

		public void setSelected(boolean selected) {
			this.selected = selected;
		}

		public boolean isLocked() {
			return locked;
		}

		public void setLocked(boolean locked) {
			this.locked = locked;
		}
	}
}
