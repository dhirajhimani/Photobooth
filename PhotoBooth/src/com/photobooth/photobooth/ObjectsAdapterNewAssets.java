package com.photobooth.photobooth;

import java.util.ArrayList;

import com.photobooth.photobooth.util.ImageLoader;
import com.photobooth.photobooth.util.Util;
import com.photobooth.photobooth.util.ImageLoader.ImageLoaderListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class ObjectsAdapterNewAssets extends ArrayAdapter<String> implements ImageLoaderListener {
	
	private ImageLoader imageLoader;

	public ObjectsAdapterNewAssets(Context context, ArrayList<String> data) {
		super(context, R.layout.raw_objects, data);
		imageLoader = new ImageLoader(context, true);
		imageLoader.setImageLoaderListener(this);
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if(convertView == null) {
			convertView = ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.raw_objects, null);
			holder = new ViewHolder();
			holder.img = (ImageView) convertView.findViewById(R.id.imgObject);
			holder.pb = (ProgressBar) convertView.findViewById(R.id.pb);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		String url = getItem(position);
		Util.LOG(String.format("DisplayImage URL(%d) (%s)", position, url));
		holder.img.setTag(url);
		holder.pb.setVisibility(View.VISIBLE);
		imageLoader.DisplayImage(url, holder.img);
		
		//holder.img.setImageResource(getItem(position));
		
		return convertView;
	}
	
	private static class ViewHolder {
		ImageView img;
		ProgressBar pb;
	}

	@Override
	public void onImageSet(ImageView imageView, Bitmap bitmap) {
		View parent = (View) imageView.getParent();
		View progressBar = parent.findViewById(R.id.pb);
		if(progressBar != null) {
			progressBar.setVisibility(View.GONE);
		}
	}
}
