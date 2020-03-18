package com.moe.pussy;
import android.graphics.Bitmap;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import android.graphics.drawable.Drawable;

public abstract class Target implements SizeReady
{
	private Content content;
	private int width,height;
	public void placeHolder(Drawable placeHolder)
	{
	}
	Content getContent(){
		return content;
	}
	@Override
	public void onSizeReady(int w,int h){
		width=w;
		height=h;
	}
	public Bitmap onResourceReady(Bitmap bitmap,Transformer... trans){
		for(Transformer t:trans){
			bitmap=t.onTransformer(BitmapPool.get(),bitmap,width,height);
		}
		return bitmap;
	}
	public abstract void onSucccess(PussyDrawable pd);
	public abstract void error(Throwable e,Drawable d);
	public PussyDrawable putCache(Bitmap pd){
		if(pd==null)return null;
		Resource res=new Resource(content.getKey(),pd);
		res.acquire();
		content.getRequest().getPussy().getActiveResource().add(res);
		if(content.getCache()==DiskCache.Cache.MASK){
			//持久化
			Bitmap bitmap=pd;
			if (bitmap != null)
			{
				try
				{
					bitmap.compress(Bitmap.CompressFormat.WEBP, 99, new FileOutputStream(content.getRequest().getPussy().mDiskCache.getCache(content.getKey())));
				}
				catch (FileNotFoundException e)
				{}
			}
		}
		return new PussyDrawable(pd,getRefresh());
	}
	protected void onAttachContent(Content c){
		if(this.content!=null){
			this.content.clearTarget();
		}
		this.content=c;
	}
	protected DrawableAnimator getAnim(){
		if(content!=null)
		return content.getAnim();
		return null;
	}
	protected Pussy.Refresh getRefresh(){
		return content.getRefresh();
	}
	
}
