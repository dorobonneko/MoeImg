package com.moe.pussy.transformer;
import android.graphics.*;

import android.view.Gravity;
import com.moe.pussy.Transformer;
import com.moe.pussy.BitmapPool;

public class CropTransformer implements Transformer
{
	private int gravity;
	public CropTransformer(int gravity)
	{
		this.gravity = gravity;
	}

	@Override
	public String getKey()
	{
		return "pussy&Crop".concat(String.valueOf(gravity));
	}

	
	@Override
	public Bitmap onTransformer(BitmapPool bp,Bitmap source,int w, int h)
	{
		if(source==null)return null;
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
			displayHeight =(int) (source.getHeight() * scale);
		}
		else if (w == -2 && h == -2)
		{
			return source;
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
		
		Rect rect=new Rect(0, 0, (int)(displayWidth / scale), (int)Math.round(displayHeight / scale));
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
		if(rect.width()<=0||rect.height()<=0){
			source.recycle();
			return null;
			}
		Bitmap buff=BitmapPool.getBitmap(rect.width(),rect.height(),Bitmap.Config.ARGB_8888);
			Canvas canvas=new Canvas(buff);
			canvas.setDrawFilter(new PaintFlagsDrawFilter(0,Paint.ANTI_ALIAS_FLAG|Paint.DITHER_FLAG|Paint.FILTER_BITMAP_FLAG));
			canvas.translate(-rect.left,-rect.top);
			canvas.drawBitmap(source,0,0,null);
			//Bitmap buff=source.createBitmap(source,rect.left,rect.top,rect.width(),rect.height());
			if(buff!=source)
			BitmapPool.recycle(source);
			//source.recycle();
		return buff;
	}




	
}
