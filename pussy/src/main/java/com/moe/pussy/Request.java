package com.moe.pussy;
import android.net.Uri;
import java.io.File;
import android.widget.ImageView;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.graphics.Bitmap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class Request
{
	//拥有多个builder多个target，图片资源，网络请求，Uri/resid只有一个
	public static class Builder
	{
		private Object tag;
		private Uri uri;
		private int res=-1;
		private Pussy pussy;
		private int placeHolderResId=-1,errorResId=-1;
		private Drawable placeHolderDrawable,errorDrawable;
		private int cropGravity=0,insideGravity=0;
		private int gravity=0;
		private float degrees=0;
		private float pivotx=-1,pivoty=-1;
		private Bitmap.Config config;
		private int priority;
		private Transformation[] trans;
		private boolean memoryCache=true,diskCache=true;
		private String encodeTag;
		void setPussy(Pussy pussy)
		{
			this.pussy = pussy;
		}
		public Pussy getPussy(){
			return pussy;
		}
		public Builder(Uri uri)
		{
			this.tag = uri;
			this.uri = uri;
		}
		public Builder(int res)
		{
			this.tag = res;
			this.res = res;
		}
		public Builder(String url)
		{
			this.tag = url;
			if (url.startsWith("/"))
			{
				this.uri = Uri.parse("file://".concat(url));
			}
			else
			{
				this.uri = Uri.parse(url);
			}
		}
		public Builder(File file)
		{
			this.tag = file;
			this.uri = Uri.fromFile(file);
		}
		synchronized String getTag()
		{
			if (encodeTag == null)
			{
				try
				{
					encodeTag = new String(MessageDigest.getInstance("MD5").digest(toString().getBytes()), 16);
				}
				catch (NoSuchAlgorithmException e)
				{}
			}
			return encodeTag;
		}
		Uri getUri(){
			return uri;
		}
		int getResId(){
			return res;
		}
		public Builder placeholder(int placeholderResId)
		{
			if (placeHolderDrawable != null)
				throw new IllegalStateException("placeholder drawable has been set");
			this.placeHolderResId = placeholderResId;
			return this;
		}

		public Builder placeholder(android.graphics.drawable.Drawable placeholderDrawable)
		{
			if (placeHolderResId != -1)
				throw new IllegalStateException("placehonder res has been set");
			this.placeHolderDrawable = placeholderDrawable;
			return this;
		}

		public Builder error(int errorResId)
		{
			if (errorDrawable != null)
				throw new IllegalStateException("error drawable has been set");
			this.errorResId = errorResId;
			return this;
		}

		public Builder error(android.graphics.drawable.Drawable errorDrawable)
		{
			if (this.errorDrawable != null)
				throw new IllegalStateException("error res has been set");
			this.errorDrawable = errorDrawable;
			return this;
		}

		public Builder tag(java.lang.Object tag)
		{
			this.tag = tag;
			return this;
		}
		public Builder reSize(int width, int height)
		{
			return this;
		}
		public Builder crop(int gravity)
		{
			this.gravity = 1;
			cropGravity = gravity;
			return this;
		}
		public Builder inside(int gravity)
		{
			this.gravity = -1;
			insideGravity = gravity;
			return this;
		}
		public Builder rotate(float degrees)
		{
			this.degrees = degrees;
			return this;
		}

		public Builder rotate(float degrees, float pivotX, float pivotY)
		{
			this.degrees = degrees;
			this.pivotx = pivotX;
			this.pivoty = pivotY;
			return this;
		}

		public Builder config(android.graphics.Bitmap.Config config)
		{
			this.config = config;
			return this;
		}
		public Builder priority(int priority)
		{
			this.priority = priority;
			return this;
		}

		public Builder transform(Transformation... transformation)
		{
			this.trans = transformation;
			return this;
		}
		public Target into(File file)
		{
			return null;
		}
		public Target into(ImageView imageView)
		{
			return null;
		}
		public void into(Target target)
		{}
		public Builder noMemoryCache()
		{
			this.memoryCache = false;
			return this;
		}
		public Builder noDiskCache()
		{
			this.diskCache = false;
			return this;
		}

		@Override
		public String toString()
		{
			StringBuilder sb=new StringBuilder();
			sb.append("Uri:").append(uri);
			sb.append(" resId:").append(res);
			sb.append(" placeHolderResId:").append(placeHolderResId);
			sb.append(" errorResId:").append(errorResId);
			sb.append(" placeHolderDrawable:").append(placeHolderDrawable);
			sb.append(" errorDrawable:").append(errorDrawable);
			sb.append(" cropGravity:").append(cropGravity);
			sb.append(" insideGravity:").append(insideGravity);
			sb.append(" gravity:").append(gravity);
			sb.append(" degrees:").append(degrees);
			sb.append(" pivotx:").append(pivotx);
			sb.append(" pivoty:").append(pivoty);
			sb.append(" Bitmap.Config:").append(config);
			sb.append(" priority:").append(priority);
			sb.append(".Transformation[]:").append(trans);
			sb.append(".memoryCache:").append(memoryCache);
			sb.append(".diskCache:").append(diskCache);
			return sb.toString();
		}

	}
}
