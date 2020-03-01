package com.moe.tinyimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import com.moe.tinyimage.Utils;

public abstract class Target
{
	Request onHandleRequest(Request.Builder builder)
	{
		return new Request(builder);
	}
	private Request mRequest;
	public int getTargetWidth(){
		return -2;
	}
	public int getTargetHeight(){
		return -2;
	}

	public void onResourceReady(final Pussy.BitmapCallback bc)
	{
		final BitmapDecoder brd=bc.getBitmapDecode();
		synchronized (getRequest().getKey())
		{
			Bitmap bitmap=getRequest().getPussy().mMemoryCache.get(getRequest().getKey());
			if (bitmap != null)
			{
				onLoadSuccess(Pussy.TinyBitmapDrawable.create(this,bc.getBitmap()));
			}
			else
			{
				new Thread(){
					public void run()
					{
						synchronized (getRequest().getKey())
						{
							BitmapFactory.Options bo=new BitmapFactory.Options();
							bo.inPreferredConfig = getRequest().getConfig();
							bo.inTargetDensity = getRequest().getPussy().mContext.getResources().getDisplayMetrics().densityDpi;
							bo.inScaled = true;
							bo.inDensity = 160;
							Bitmap bitmap= onTransForm(brd, bo, brd.getWidth(), brd.getHeight());
							//resize
							if(getRequest().getResize()!=null){
								Point point=getRequest().getResize();
								Bitmap resize=Bitmap.createScaledBitmap(bitmap,point.x,point.y,false);
								if(resize!=bitmap)
									bitmap.recycle();
								bitmap=resize;
							}
							bc.setBitmap(bitmap);
							getRequest().getPussy().getHandler().post(new Runnable(){

									@Override
									public void run()
									{
										if (getRequest().isCanceled())return;
										onLoadSuccess(Pussy.TinyBitmapDrawable.create(Target.this,bc.getBitmap()));
									}
								});
						}
					}
				}.start();
			}
		}
	}
	abstract public void onLoadSuccess(Drawable d);
	abstract public void onLoadFailed(Exception e, Drawable d);
	abstract public void onLoadPrepared(Drawable d);
	abstract public void onProgressChanged(int progress);
	abstract public void onLoadCleared();
	void setRequest(Request request)
	{
		this.mRequest = request;
	}
	public Request getRequest()
	{
		return mRequest;
	}
	Bitmap onTransForm(BitmapDecoder brd, BitmapFactory.Options options, int width, int height)
	{
		if (getRequest().getTransForm() == null)return brd.decodeRegion(new Rect(0, 0, brd.getWidth(), brd.getHeight()), options);
		Bitmap bitmap=null;
		for (Pussy.TransForm tf:getRequest().getTransForm())
		{
			if (tf.canDecode())
				bitmap = tf.onTransForm(brd, options, width, height);
			else
			{
				if (bitmap == null&&(brd.getWidth()!=0&&brd.getHeight()!=0))
					bitmap = brd.decodeRegion(new Rect(0, 0, brd.getWidth(), brd.getHeight()), options);
				if(bitmap!=null)
					bitmap = tf.onTransForm(bitmap, width, height);
			}
		}
		return bitmap;
	}
	public Bitmap onTransForm(Bitmap bitmap,int width,int height){
		if (getRequest().getTransForm() == null)
			return bitmap;
		for (Pussy.TransForm tf:getRequest().getTransForm())
		{
			bitmap = tf.onTransForm(bitmap, width, height);
		}
		return bitmap;
	}
	public void onResourceReady(Drawable drawable){
		onLoadSuccess(Pussy.TinyBitmapDrawable.create(this,onTransForm(Utils.drawable2Bitmap(drawable),drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight())));
	}
}
