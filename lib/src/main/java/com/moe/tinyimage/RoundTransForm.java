package com.moe.tinyimage;

import android.graphics.*;

public class RoundTransForm implements Pussy.TransForm
{
	private int radius;//圆角值

	public RoundTransForm(int radius)
	{
		this.radius = radius;
	}

	@Override
	public String key()
	{
		// TODO: Implement this method
		return "tiny&Round".concat(String.valueOf(radius));
	}

	@Override
	public boolean canDecode()
	{
		return false;
	}

	@Override
	public Bitmap onTransForm(BitmapDecoder brd, BitmapFactory.Options options, int w, int h)
	{
		return null;
	}

	@Override
	public Bitmap onTransForm(Bitmap source, int w, int h)
	{
		int width = source.getWidth();
		int height = source.getHeight();
		//画板
		Bitmap bitmap = Bitmap.createBitmap(width, height, source.getConfig());
		Paint paint = new Paint();
		Canvas canvas = new Canvas(bitmap);//创建同尺寸的画布
		paint.setAntiAlias(true);//画笔抗锯齿
		paint.setDither(true);
		//paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
		//画圆角背景
		RectF rectF = new RectF(new Rect(0, 0, width, height));//赋值
		canvas.drawRoundRect(rectF, radius, radius, paint);//画圆角矩形
		//
		paint.setFilterBitmap(true);
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(source, 0, 0, paint);
		source.recycle();//释放

		return bitmap;
	}
}
