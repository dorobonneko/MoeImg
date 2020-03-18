package com.moe.pussy.target;
import com.moe.pussy.Target;
import com.moe.pussy.PussyDrawable;
import android.widget.ImageView;
import com.moe.pussy.Transformer;
import android.view.ViewTreeObserver;
import android.graphics.Bitmap;
import com.moe.pussy.DrawableAnimator;
import android.graphics.drawable.Drawable;
import com.moe.pussy.BitmapPool;

public class ImageViewTarget extends Target implements ViewTreeObserver.OnPreDrawListener
{
	private ImageView view;
	private Bitmap pd;
	private Transformer[] trans;
	public ImageViewTarget(ImageView view){
		this.view=view;
	}

	@Override
	public Bitmap onResourceReady(Bitmap bitmap, Transformer[] trans)
	{
		this.pd=bitmap;
		
		this.trans=trans;
		if(view.getWidth()==0||view.getHeight()==0){
			view.post(new Runnable(){
				public void run(){
					view.getViewTreeObserver().addOnPreDrawListener(ImageViewTarget.this);
					}
					});
			//view.requestLayout();
		}else{
			Bitmap b=bitmap;
			if(b!=null)
			for(Transformer t:trans){
				b=t.onTransformer(BitmapPool.get(),b,view.getWidth(),view.getHeight());
			}
			final PussyDrawable pd=putCache(b);
			view.post(new Runnable(){

					@Override
					public void run()
					{
						onSucccess(pd);
					}
				});
			return null;
		}
		return null;
	}
	
	@Override
	public void onSucccess(PussyDrawable pd)
	{
		if(pd!=null){
		pd.stop();
		view.setImageDrawable(pd);
		pd.setAnimator(getAnim());
		pd.start();}else{
			error(null,null);
		}
	}

	@Override
	public void error(Throwable e,Drawable d)
	{
		if(getAnim()!=null)getAnim().stop();
		view.setImageDrawable(d);
	}

	@Override
	public void placeHolder(Drawable placeHolder)
	{
		if(getAnim()!=null)getAnim().stop();
		view.setImageDrawable(placeHolder);
	}

	@Override
	public boolean onPreDraw()
	{
		view.getViewTreeObserver().removeOnPreDrawListener(this);
		new Thread(){
			public void run(){
				onResourceReady(pd,trans);
				pd=null;
				trans=null;
			}
		}.start();
		return false;
	}
	
	
}
