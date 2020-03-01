package com.moe.tinyimage;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import com.moe.tinyimage.ImageViewTarget;
import android.view.View;

public class Request
{
	private Builder mBuilder;
	private String key,cacheKey;
	private boolean mCanceled;
	Request(Builder mBuilder)
	{
		this.mBuilder = mBuilder;
	}
	String getCacheKey()
	{
		if (cacheKey == null)
			cacheKey = Utils.encode(getUrl());
		return cacheKey;
	}
	public float getResizePercent(){
		return mBuilder.percent;
	}
	public String getUrl()
	{
		return mBuilder.url;
	}
	
	public Anim getAnim(){
		return mBuilder.anim;
	}
	public boolean isCanceled()
	{
		return mCanceled;
	}
	public void cancel()
	{
		mCanceled = true;
	}
	public Drawable getPlaceHolder()
	{
		return mBuilder.placeHolder == 0 ?mBuilder.placeHolderDrawable: getPussy().mContext.getResources().getDrawable(mBuilder.placeHolder, getPussy().theme);
	}
	public Drawable getError()
	{
		return mBuilder.error == 0 ?mBuilder.errorDrawable: getPussy().mContext.getResources().getDrawable(mBuilder.error, getPussy().theme);
	}
	public synchronized String getKey()
	{
		if (key == null)
		{
			StringBuilder sb=new StringBuilder();
			sb.append(mBuilder.url).append(getAnim()).append(getPlaceHolder() == null).append(getError() == null).append(getConfig());
			Pussy.TransForm[] tf=getTransForm();
			if (tf != null)
				for (Pussy.TransForm trans:tf)
					sb.append(trans.key());
			sb.append(mBuilder.target);
			key = Utils.encode(sb.toString());
		}
		return key;
	}
	public Bitmap.Config getConfig()
	{
		return mBuilder.config;
	}
	public Pussy.TransForm[] getTransForm()
	{
		return mBuilder.mTransForm;
	}
	public Pussy getPussy()
	{
		return mBuilder.mPussy;
	}
	public Point getResize(){
		return mBuilder.resize;
	}
	public static class Builder
	{
		private int placeHolder,error;
		private Pussy.TransForm[] mTransForm;
		private Drawable placeHolderDrawable,errorDrawable;
		private Bitmap.Config config=Bitmap.Config.RGB_565;
		private Anim anim;

		private String url;
		private Pussy mPussy;
		private float percent;
		private Point resize;
		private String target;
		public Builder(Pussy pussy, String url)
		{
			this.mPussy = pussy;
			this.url = url;
		}
		public void into(ImageView view)
		{
			if(anim!=null)
				view.setLayerType(anim.hw()?View.LAYER_TYPE_HARDWARE:View.LAYER_TYPE_SOFTWARE, null);
			Target t=(Target) view.getTag();
			if (t == null)
				view.setTag(t = new ImageViewTarget(view));
			into(t);
		}
		public void into(Target target)
		{
			this.target=target.toString();
			mPussy.cancel(target);
			target.setRequest(target.onHandleRequest(this));
			Loader loader=mPussy.requestQueue.get(target.getRequest().getCacheKey());
			if(loader==null)
				mPussy.requestQueue.put(target.getRequest().getCacheKey(),loader=new Loader(mPussy,url));
			loader.add(target);

		}
		public void preload(){
			Request request=new Request(this);
			Loader loader=mPussy.requestQueue.get(request.getCacheKey());
			if(loader==null)
				mPussy.requestQueue.put(request.getCacheKey(),loader=new Loader(mPussy,url));
			loader.preload();
		}
		public Builder Anim(Anim anim)
		{
			this.anim = anim;
			return this;
		}
		public Builder config(Bitmap.Config config)
		{
			this.config = config;
			return this;
		}

		public Builder placeHolder(int width, int height, int color, float radius)
		{
			return placeHolder(new Pussy.PlaceHolderDrawable(width, height, color, radius));
		}
		public Builder placeHolder(Drawable placeHolder)
		{
			this.placeHolder = 0;
			placeHolderDrawable = placeHolder;
			return this;
		}
		public Builder placeHolder(int resId)
		{
			placeHolderDrawable = null;
			placeHolder = resId;
			return this;
		}
		public Builder error(Drawable error)
		{
			this.error = 0;
			this.errorDrawable = error;
			return this;
		}
		public Builder error(int resId)
		{
			this.error = resId;
			return this;
		}
		public Builder transForm(Pussy.TransForm... trans)
		{
			this.mTransForm = trans;
			return this;
		}
		/*public Builder percent(float percent){
		 this.percent=percent;
		 return this;
		 }*/
		public Builder reSize(int width,int height){
			resize=new Point(width,height);
			return this;
		}
	}
}
