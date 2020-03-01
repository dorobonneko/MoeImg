package com.moe.tinyimage;
import android.graphics.BitmapRegionDecoder;
import java.io.IOException;
import android.graphics.Rect;
import android.graphics.BitmapFactory.Options;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapDecoder
{
	private boolean isRecycle;
	private String path;
	private BitmapRegionDecoder brd;
	private int width,height;
	public BitmapDecoder(String path){
		try
		{
			brd = BitmapRegionDecoder.newInstance(path, false);
			width=brd.getWidth();
			height=brd.getHeight();
		}
		catch (IOException e)
		{
			this.path=path;
			BitmapFactory.Options bo=new BitmapFactory.Options();
			bo.inJustDecodeBounds=true;
			BitmapFactory.decodeFile(path,bo);
			width=bo.outWidth;
			height=bo.outHeight;
		}
	}

	public boolean isRecycled()
	{
		return isRecycle;
	}

	public int getWidth()
	{
		return width;
	}

	public int getHeight()
	{
		return height;
	}

	public Bitmap decodeRegion(Rect rect, BitmapFactory.Options options)
	{
		if(rect.width()<=0||rect.height()<=0)return null;
		if(brd!=null)
			return brd.decodeRegion(rect,options);
		Bitmap bitmap=BitmapFactory.decodeFile(path,options);
		Bitmap buff=bitmap.createBitmap(bitmap,rect.left,rect.top,rect.width(),rect.height());
		if(bitmap!=buff)
			bitmap.recycle();
		return buff;
	}

	public void recycle()
	{
		if(isRecycled())return;
		isRecycle=true;
		if(brd!=null)
			brd.recycle();
	}
}
