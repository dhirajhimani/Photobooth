package com.photobooth.photobooth;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

public class ObjectsAdapter extends ArrayAdapter<Drawable> {

	public ObjectsAdapter(Context context, ArrayList<Drawable> data) {
		super(context, R.layout.raw_objects, data);
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if(convertView == null) {
			convertView = ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.raw_objects, null);
			holder = new ViewHolder();
			holder.img = (ImageView) convertView.findViewById(R.id.imgObject);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		Drawable drawableObject = getItem(position);
		holder.img.setImageDrawable(drawableObject);
		
		return convertView;
	}
	
	private static class ViewHolder {
		ImageView img;
	}
}
