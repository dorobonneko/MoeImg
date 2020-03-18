package com.moe.pussy;
import android.graphics.drawable.Animatable;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public interface DrawableAnimator extends Animatable
{
	public abstract void draw(Canvas canvas,Bitmap bitmap);
	public abstract void setCallback(Drawable callback);
}
