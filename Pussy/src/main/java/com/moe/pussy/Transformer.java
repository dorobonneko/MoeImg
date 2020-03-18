package com.moe.pussy;
import android.graphics.Bitmap;

public interface Transformer
{
	public Bitmap onTransformer(BitmapPool mBitmapPool,Bitmap bitmap,int w,int h);
	public String getKey();
}
