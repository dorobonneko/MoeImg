package com.moe.tinyimage;

import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;

import android.widget.ImageView;
import java.lang.ref.SoftReference;
import com.moe.tinyimage.Pussy.BitmapCallback;
public class ImageViewTarget extends Target implements ViewTreeObserver.OnPreDrawListener
{
	private SoftReference<ImageView> view;
	private BitmapFactory.Options options;
	private Pussy.BitmapCallback brd;
	private int width,height;
	public ImageViewTarget(ImageView view)
	{
		this.view = new SoftReference<ImageView>(view);
		//view.getViewTreeObserver().addOnPreDrawListener(this);
		options = new BitmapFactory.Options();
		options.inTargetDensity = view.getResources().getDisplayMetrics().densityDpi;
		options.inScaled = true;
		options.inDensity = 160;
		options.inDither = true;
	}

	@Override
	public boolean onPreDraw()
	{
		if (brd != null && !getRequest().isCanceled())
			onResourceReady(brd);
		getView().getViewTreeObserver().removeOnPreDrawListener(this);
		return true;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof ImageViewTarget)
			return view.get().equals(((ImageViewTarget)obj).view.get());
		return super.equals(obj);
	}

	@Override
	public int getTargetWidth()
	{
		return getView().getWidth();
	}

	@Override
	public int getTargetHeight()
	{
		return getView().getHeight();
	}



	@Override
	public void onResourceReady(final BitmapCallback bc)
	{
		final BitmapDecoder brd=bc.getBitmapDecode();
		if (getRequest().isCanceled())return;
		synchronized (getRequest().getKey())
		{
			if (brd == null || brd.isRecycled())
			{
				//待处理
				//getRequest().getPussy().requestQueue().get(getRequest().getCacheKey()).add(ImageViewTarget.this);
				
				return;
			}
			Bitmap bitmap = bc.getBitmap();
			if (bitmap == null)
			{
				if (getRequest().isCanceled())return;
				new Thread(){
					public void run()
					{
						width = getView().getWidth();
						height = getView().getHeight();
						if (width != 0 || height != 0)
						{
							synchronized (getRequest().getKey())
							{
								if (brd.isRecycled() && !getRequest().isCanceled())
								{
									//getRequest().getPussy().requestQueue().get(getRequest().getCacheKey()).add(ImageViewTarget.this);
									//getBuilder().invalidate(ImageViewTarget.this);
									return;
								}
								options.inSampleSize = Utils.calculateInSampleSize(brd.getWidth(), brd.getHeight(), width, height);
								Bitmap buffer=null;
								if (getRequest().getTransForm() != null)
								{
									if (getRequest().isCanceled())return;
									buffer = onTransForm(brd, options, getTargetWidth(), getTargetHeight());
								}
								else
								{
									buffer = brd.decodeRegion(new Rect(0, 0, brd.getWidth(), brd.getHeight()), options);

								}
								//resize
								/*if(getRequest().getResize()!=null){
									Point point=getRequest().getResize();
									Bitmap resize=Bitmap.createScaledBitmap(buffer,point.x,point.y,false);
									if(resize!=buffer)
										buffer.recycle();
									buffer=resize;
								}*/
								if(buffer!=null)
								bc.setBitmap(buffer);
								if (getRequest().isCanceled())return;
								getRequest().getPussy().getHandler().postDelayed(new Runnable(){
										public void run()
										{
											onLoadSuccess(Pussy.TinyBitmapDrawable.create(ImageViewTarget.this,bc.getBitmap()));
										}},10);
							}
						}
						else
						{
							ImageViewTarget.this.brd = bc;
							try{getView().getViewTreeObserver().addOnPreDrawListener(ImageViewTarget.this);}catch(Exception e){}
							//view.requestLayout();
						}
					}}.start();

			}
			else
			{
				onLoadSuccess(Pussy.TinyBitmapDrawable.create(this,bc.getBitmap()));
			}
		}
	}

	@Override
	public void onLoadSuccess(Drawable d)
	{
		stop();
		getView().setImageDrawable(d);
		start();
	}


	@Override
	public void onLoadFailed(Exception e, Drawable d)
	{
		stop();
		getView().setImageDrawable(d);
		start();
	}

	@Override
	public void onLoadPrepared(Drawable d)
	{
		stop();
		getView().setImageDrawable(d);
		start();
	}

	@Override
	public void onProgressChanged(int progress)
	{
		// TODO: Implement this method
	}
	private void stop()
	{
		Drawable d=getView().getDrawable();
		if (d instanceof Animatable)
			((Animatable)d).stop();
	}
	private void start()
	{
		Drawable d=getView().getDrawable();
		if (d instanceof Animatable)
			((Animatable)d).start();
	}
	@Override
	public void onLoadCleared()
	{
		getView().setImageDrawable(null);
		//view.getViewTreeObserver().addOnScrollChangedListener(this);

	}
	public ImageView getView()
	{
		return view.get();
	}
}
