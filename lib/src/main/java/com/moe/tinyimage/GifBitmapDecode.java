package com.moe.tinyimage;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class GifBitmapDecode
{
	private Bitmap bitmap;
	private boolean recycle;
	private String path;
	public GifBitmapDecode(String path){
		this.path=path;
	}
	public Bitmap decodeBitmap(){
		if(recycle)return null;
		if(bitmap==null)
			bitmap=BitmapFactory.decodeFile(path);
			return bitmap;
	}
	public void recycle(){
		if(recycle)return;
		if(bitmap!=null)bitmap.recycle();
		recycle=true;
	}
}
