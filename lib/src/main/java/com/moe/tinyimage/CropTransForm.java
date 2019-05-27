package com.moe.tinyimage;
import android.graphics.*;

import android.view.Gravity;

public class CropTransForm implements Pussy.TransForm
{
	private int gravity;
	public CropTransForm(int gravity)
	{
		this.gravity = gravity;
	}

	@Override
	public String key()
	{
		return "tiny&Crop".concat(String.valueOf(gravity));
	}

	@Override
	public boolean canDecode()
	{
		return true;
	}

	@Override
	public Bitmap onTransForm(BitmapRegionDecoder source, BitmapFactory.Options options, int w, int h)
	{
		float scale=1;
		int displayWidth=0,displayHeight=0,image_width=source.getWidth(),image_height=source.getHeight();
		if (w == -2)
		{
			//用高度计算
			scale = (float) h / (float) source.getHeight();
			displayHeight = h;
			displayWidth = (int)(source.getWidth() * scale);
		}
		else if (h == -2)
		{
			//用宽度计算
			scale = (float) w / (float) source.getWidth();
			displayWidth = w;
			displayHeight = (int)(source.getHeight() * scale);
		}
		else if (w == -2 && h == -2)
		{
			return source.decodeRegion(new Rect(0, 0, source.getWidth(), source.getHeight()), options);
		}
		else
		{
			if (source.getWidth() * h > w * source.getHeight())
			{
				scale = (float) h / (float) source.getHeight();
			}
			else
			{
				scale = (float) w / (float) source.getWidth();
			}
			displayWidth = w;
			displayHeight = h;
		}
		Rect rect=new Rect(0, 0, (int)(displayWidth / scale), (int)(displayHeight / scale));
		if((gravity&Gravity.RIGHT)==Gravity.RIGHT||(gravity&Gravity.END)==Gravity.END){
			if(image_width>rect.width()){
				rect.set(image_width-rect.width(),rect.top,image_width,rect.bottom);
			}
		}
		if((gravity&Gravity.BOTTOM)==Gravity.BOTTOM){
			if(image_height>rect.height()){
				rect.set(rect.left,image_height-rect.height(),rect.right,image_height);
			}
		}
		if(Gravity.isVertical(gravity)){
			if(image_width>rect.width()){
				rect.offset((image_width-rect.width())/2,0);
			}
		}
		if(Gravity.isHorizontal(gravity)){
			if(image_height>rect.height()){
				rect.offset(0,(image_height-rect.height())/2);
			}
		}
		try{
		return source.decodeRegion(rect, options);
		}catch(Exception e){}
	return null;
	}




	@Override
	public Bitmap onTransForm(Bitmap source, int w, int h)
	{
		return null;
	}
}
