package com.moe.pussy.transformer;

import android.graphics.*;
import com.moe.pussy.Transformer;
import com.moe.pussy.BitmapPool;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class RoundTransformer implements Transformer
{
	private float radius;//圆角值

	public RoundTransformer(float radius)
	{
		this.radius = radius;
	}
	public RoundTransformer(DisplayMetrics dm,int radius)
	{
		this.radius =TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,radius,dm);
	}

	@Override
	public String getKey()
	{
		// TODO: Implement this method
		return "pussy&Round".concat(String.valueOf(radius));
	}

	

	@Override
	public Bitmap onTransformer(BitmapPool bp,Bitmap source, int w, int h)
	{
		if(source==null)return null;
		int width = source.getWidth();
		int height = source.getHeight();
		float scale=Math.min(w/(float)width,h/(float)height);
		float radius=this.radius/scale;
		//画板
		Bitmap bitmap = BitmapPool.getBitmap(width, height,Bitmap.Config.ARGB_8888);
		Paint paint = new Paint();
		Canvas canvas = new Canvas(bitmap);//创建同尺寸的画布
		//canvas.drawColor(0,PorterDuff.Mode.CLEAR);
		paint.setAntiAlias(true);//画笔抗锯齿
		paint.setDither(true);
		//paint.setColor(0);
		//paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
		//画圆角背景
		RectF rectF = new RectF(0, 0, canvas.getWidth(), canvas.getHeight());//赋值
		canvas.drawRoundRect(rectF, radius, radius, paint);//画圆角矩形
		//
		paint.setFilterBitmap(true);
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(source, 0, 0, paint);
		if(bitmap!=source)
		bp.recycle(source);//释放

		return bitmap;
	}
}
