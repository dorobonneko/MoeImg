package com.moe.pussy;
import android.graphics.drawable.Drawable;
import android.graphics.ColorFilter;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable.Callback;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Paint;
import java.lang.ref.WeakReference;
import android.graphics.PorterDuff;

public class PussyDrawable extends Drawable implements Animatable
{
	public Bitmap bitmap;
	private boolean recycle;
	private Pussy.Refresh refresh;
	private DrawableAnimator da;
	public PussyDrawable(Bitmap bitmap,Pussy.Refresh r)
	{
		this.bitmap = bitmap;
		refresh=r;
	}
	
	public void setAnimator(DrawableAnimator da)
	{
		this.da = da;
		if(da!=null)
		da.setCallback(this);
	}

	@Override
	public void start()
	{
		if(da!=null)
			da.start();
	}

	@Override
	public void stop()
	{
		if(da!=null)
			da.stop();
	}

	@Override
	public boolean isRunning()
	{
		if(da!=null)
			return da.isRunning();
		return false;
	}

	public Pussy.Refresh getRefresh()
	{
		return refresh;
	}
	@Override
	public void draw(Canvas p1)
	{
		Bitmap bitmap=this.bitmap;
		if (bitmap != null){
			synchronized (bitmap)
			{
				if (bitmap.isRecycled()||BitmapPool.isRecycled(bitmap)){
					if(refresh!=null)
						refresh.refresh();
				}else
				{
					p1.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.DITHER_FLAG|Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
					if (da != null)
						da.draw(p1, bitmap);
					else
						p1.drawBitmap(bitmap, 0, 0, null);
				}
				
			}
			}else if(refresh!=null){
				refresh.refresh();
			}
	}

	@Override
	public void setAlpha(int p1)
	{
	}

	@Override
	public void setColorFilter(ColorFilter p1)
	{
	}

	@Override
	public int getOpacity()
	{
		return 0;
	}
	public int getByteCount()
	{
		Bitmap bitmap=this.bitmap;
		if (bitmap != null)
			return bitmap.getByteCount();
		return 0;
	}
	public void recycle()
	{
		Bitmap bitmap=this.bitmap;
		if (bitmap != null)
			synchronized (bitmap)
			{
				bitmap.recycle();}
		recycle = true;
	}
	public boolean isRecycled()
	{
		return recycle;
	}
	public Bitmap getBitmap()
	{
		Bitmap bitmap=this.bitmap;
		return bitmap;
	}

	@Override
	public int getIntrinsicWidth()
	{
		Bitmap bitmap=this.bitmap;
		if (bitmap != null)
			return bitmap.getWidth();
		return super.getIntrinsicWidth();
	}

	@Override
	public int getIntrinsicHeight()
	{
		Bitmap bitmap=this.bitmap;
		if (bitmap != null)
			return bitmap.getHeight();
		return super.getIntrinsicHeight();
	}


}
