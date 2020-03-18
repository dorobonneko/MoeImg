package com.moe.pussy.transformer;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.Element;
import android.renderscript.Allocation;
import android.content.Context;
import com.moe.pussy.Transformer;
import com.moe.pussy.BitmapPool;
import android.graphics.Canvas;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Paint;

public class BlurTransformer implements Transformer
{
	private int level;
	private Context c;
	public BlurTransformer(Context c,int level){
		this.level=level;
		this.c=c.getApplicationContext();
	}
	
	@Override
	public Bitmap onTransformer(BitmapPool bp,Bitmap source, int w, int h)
	{
		Bitmap out_bitmap=BitmapPool.getBitmap(source.getWidth(),source.getHeight(),Bitmap.Config.ARGB_8888);
		Canvas canvas=new Canvas(out_bitmap);
		canvas.setDrawFilter(new PaintFlagsDrawFilter(0,Paint.DITHER_FLAG|Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
		canvas.drawBitmap(source,0,0,null);
		RenderScript rs=RenderScript.create(c);
		Allocation in=Allocation.createFromBitmap(rs,out_bitmap);
		ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, in.getElement());
		
		//Allocation out=Allocation.createTyped(rs,in.getType());
		blur.setRadius(level);
		blur.setInput(in);
		blur.forEach(in);
		in.copyTo(out_bitmap);
		rs.destroy();
		if(out_bitmap!=source)
		bp.recycle(source);
		//return out_bitmap;
		return out_bitmap;
	}

	
	@Override
	public String getKey()
	{
		return "pussy&blur".concat(String.valueOf(level));
	}
	
}
