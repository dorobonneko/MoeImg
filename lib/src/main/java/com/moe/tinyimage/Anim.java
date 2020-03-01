package com.moe.tinyimage;
import android.graphics.drawable.Drawable;
import android.animation.ValueAnimator;
import android.animation.Animator;
import android.graphics.ColorFilter;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Bitmap;

public abstract class Anim
{
	private Drawable mDrawable;
	private ValueAnimator animator;
	public Anim(){
		animator=new ValueAnimator();
		animator.setFloatValues(0,1);
		animator.addUpdateListener(new Update());
	}
	public abstract void onUpdate(Drawable d,ValueAnimator animator);
	Drawable setDrawable(Drawable d){
		return mDrawable=d;
	}
	public boolean hw(){
		return true;
	}
	void start(){
		animator.start();
	}
	void stop(){
		animator.cancel();
	}
	public void duration(long time){
		animator.setDuration(time);
	}
	boolean isRunning(){
		return animator.isRunning();
	}

	@Override
	public String toString()
	{
		return getKey();
	}
	public abstract String getKey();
	class Update implements ValueAnimator.AnimatorUpdateListener
	{

		@Override
		public void onAnimationUpdate(ValueAnimator p1)
		{
			onUpdate(mDrawable,p1);
		}
	}
	public static Anim fade(final long time){
		Anim a= new Anim(){
			public String getKey(){
				return "fade"+time;
			}
			@Override
			public void onUpdate(Drawable d, ValueAnimator animator)
			{
				d.setAlpha((int)(animator.getAnimatedFraction()*255));
			}
		};
		a.duration(time);
		return a;
	}
	public static Anim scale(final long time){
		Anim a= new Anim(){
			private Matrix m=new Matrix();
			public String getKey(){
				return "scale"+time;
			}
			public Drawable setDrawable(Drawable d){
				return super.setDrawable(new MatrixDrawable(d));
			}
			@Override
			public void onUpdate(Drawable d, ValueAnimator animator)
			{
				m.setScale(animator.getAnimatedFraction(),animator.getAnimatedFraction(),d.getIntrinsicWidth()/2,d.getIntrinsicHeight()/2);
				((MatrixDrawable)d).setMatrix(m);
			}
		};
		a.duration(time);
		return a;
	}
	public static Anim cicle(final long time){
		Anim a= new Anim(){
			public String getKey(){
				return "cicle"+time;
			}
			public boolean hw(){return false;}
			public Drawable setDrawable(Drawable d){
				return super.setDrawable(new CicleDrawable(d));
			}
			@Override
			public void onUpdate(Drawable d, ValueAnimator animator)
			{
				((CicleDrawable)d).setRadius((float)(Math.sqrt(Math.pow(d.getIntrinsicWidth(),2)+Math.pow(d.getIntrinsicHeight(),2))*animator.getAnimatedFraction()));
			}
		};
		a.duration(time);
		return a;
	}
	class AnimDrawable extends Drawable implements Animatable{
		private Drawable d;
		public AnimDrawable(Drawable d){
			this.d=d;
			}

		@Override
		public void start()
		{
			if(d instanceof Animatable)
				((Animatable)d).start();
		}

		@Override
		public void stop()
		{
			if(d instanceof Animatable)
				((Animatable)d).stop();
		}

		@Override
		public boolean isRunning()
		{
			if(d instanceof Animatable)
				return ((Animatable)d).isRunning();
			return false;
		}



	@Override
		public void draw(Canvas p1)
		{
			d.draw(p1);
		}

		@Override
		public void setBounds(int left, int top, int right, int bottom)
		{
			d.setBounds(left, top, right, bottom);
		}

		@Override
		public void setAlpha(int p1)
		{
			d.setAlpha(p1);
		}

		@Override
		public void setColorFilter(ColorFilter p1)
		{
			d.setColorFilter(p1);
		}

		@Override
		public int getOpacity()
		{
			return d.getOpacity();
		}

		@Override
		public int getIntrinsicWidth()
		{
			return d.getIntrinsicWidth();
		}

		@Override
		public int getIntrinsicHeight()
		{
			return d.getIntrinsicHeight();
		}
		public void invalidate(){
			Callback c=getCallback();
			if(c!=null)c.invalidateDrawable(this);
		}

		@Override
		public Drawable getCurrent()
		{
			return d.getCurrent();
		}
		
	}
	class MatrixDrawable extends AnimDrawable
	{
		private Matrix matrix;
		public MatrixDrawable(Drawable d){
			super(d);
			matrix=new Matrix();
		}

		
		public void setMatrix(Matrix matrix){
			this.matrix.set(matrix);
			invalidate();
		}
		@Override
		public void draw(Canvas p1)
		{
			Matrix mat=new Matrix();
			mat.setConcat(p1.getMatrix(),matrix);
			p1.setMatrix(mat);
			super.draw(p1);
		}
		
	}
	class CicleDrawable extends AnimDrawable{
		private float radius;
		private Paint paint;
		public CicleDrawable(Drawable d){
			super(d);
			paint=new Paint();
		}
		public void setRadius(float radius){
			this.radius=radius;
			invalidate();
		}

		@Override
		public void draw(Canvas p1)
		{
			Bitmap bitmap=((BitmapDrawable)getCurrent()).getBitmap();
			if(bitmap==null||bitmap.isRecycled())return;
			p1.drawCircle(getIntrinsicWidth()/2,getIntrinsicHeight()/2,radius,paint);
			
			paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
			p1.drawBitmap(bitmap,0,0,paint);
			paint.setXfermode(null);
		}
		
	}
}
